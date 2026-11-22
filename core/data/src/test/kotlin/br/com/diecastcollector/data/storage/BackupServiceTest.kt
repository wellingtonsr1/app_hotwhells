package br.com.diecastcollector.data.storage

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.entity.BrandEntity
import br.com.diecastcollector.data.db.entity.CarModelEntity
import br.com.diecastcollector.data.db.entity.ExemplarEntity
import br.com.diecastcollector.data.db.entity.ManufacturerEntity
import br.com.diecastcollector.data.db.entity.PhotoEntity
import br.com.diecastcollector.data.db.entity.VariantEntity
import br.com.diecastcollector.domain.backup.BackupException
import br.com.diecastcollector.domain.backup.BackupFailure
import br.com.diecastcollector.domain.backup.RestoreMode
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.json.JSONArray
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pacote de backup (T108/T110, FR-140–FR-145) no banco real.
 *
 * Cobre o que o usuário arrisca perder: o manifesto declara contagens e hash conferíveis,
 * um byte alterado é recusado **sem gravar nada**, e a restauração devolve banco e fotos
 * exatamente como estavam.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class BackupServiceTest {

    @get:Rule
    public val temp: TemporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var service: FileBackupService
    private val imagePath = "images/e1/front.jpg"

    @Before
    public fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DB_NAME)
        context.filesDir.resolve("images").deleteRecursively()

        database = openDatabase()
        service = FileBackupService(
            context = context,
            database = database,
            clock = Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneOffset.UTC),
        )

        runBlocking { seed() }
        writeImage(ORIGINAL_IMAGE)
    }

    @After
    public fun tearDown() {
        runCatching { database.close() }
        context.deleteDatabase(DB_NAME)
        context.filesDir.resolve("images").deleteRecursively()
        context.cacheDir.listFiles { file -> file.name.startsWith("backup-") }
            ?.forEach { it.deleteRecursively() }
    }

    @Test
    public fun pacoteTrazManifestoFotosEContagens() = runBlocking {
        val packageFile = createPackage()

        val entries = entryNames(packageFile)
        assertThat(entries).containsAtLeast(
            "manifest.json",
            "README.txt",
            "data/diecast.db",
            imagePath,
            "data/exemplar.json",
            "CSV/exemplar.csv",
        )

        val manifest = JSONObject(readEntry(packageFile, "manifest.json").toString(Charsets.UTF_8))
        assertThat(manifest.getInt("format_version")).isEqualTo(1)
        assertThat(manifest.getInt("schema_version")).isEqualTo(2)
        assertThat(manifest.getString("app_version")).isEqualTo("0.1.0 (1)")
        assertThat(manifest.getJSONObject("counts").getInt("exemplar")).isEqualTo(1)
        assertThat(manifest.getJSONObject("counts").getInt("photo")).isEqualTo(1)
        assertThat(manifest.getJSONObject("counts").getInt("images")).isEqualTo(1)
        val declaredPaths = (0 until manifest.getJSONArray("files").length()).map { index ->
            manifest.getJSONArray("files").getJSONObject(index).getString("path")
        }
        assertThat(declaredPaths).containsAtLeast(
            "README.txt",
            "data/diecast.db",
            imagePath,
            "data/exemplar.json",
            "CSV/exemplar.csv",
        )
        assertThat(manifest.getInt("missing_images")).isEqualTo(0)

        // Exportação aberta: o mesmo conteúdo, legível sem o aplicativo (FR-142).
        val exemplarRows = JSONArray(
            readEntry(packageFile, "data/exemplar.json").toString(Charsets.UTF_8),
        )
        assertThat(exemplarRows.length()).isEqualTo(1)
        assertThat(exemplarRows.getJSONObject(0).getString("internal_code"))
            .isEqualTo("HW-0001")
        assertThat(exemplarRows.getJSONObject(0).isNull("location_id")).isTrue()

        val exemplarCsv = readEntry(packageFile, "CSV/exemplar.csv")
            .toString(Charsets.UTF_8)
            .trim()
            .lines()
        assertThat(exemplarCsv.first()).contains("internal_code")
        assertThat(exemplarCsv[1]).contains("HW-0001")

        val validated = service.inspect(packageFile)
        assertThat(validated.summary.imageCount).isEqualTo(1)
        assertThat(validated.summary.counts["variant"]).isEqualTo(1)
        assertThat(validated.summary.createdAt).isEqualTo(Instant.parse("2026-11-19T12:00:00Z"))
    }

    @Test
    public fun fotoAlteradaRecusaRestauracaoSemTocarNosDados() = runBlocking {
        val packageFile = createPackage()
        val tampered = temp.newFile("tampered.zip")
        rewrite(packageFile, tampered) { name, bytes ->
            if (name == imagePath) "corrompida".toByteArray() else bytes
        }

        val failure = runCatching { service.inspect(tampered) }.exceptionOrNull()

        assertThat(failure).isInstanceOf(BackupException::class.java)
        assertThat((failure as BackupException).failure).isEqualTo(BackupFailure.SIZE_MISMATCH)
        // Nada foi restaurado: o banco em memória continua com a linha semeada.
        assertThat(database.exemplarDao().countAll()).isEqualTo(1)
    }

    @Test
    public fun manifestoDeVersaoMaisNovaERecusado() = runBlocking {
        val packageFile = createPackage()
        val newer = temp.newFile("newer.zip")
        rewrite(packageFile, newer) { name, bytes ->
            if (name != "manifest.json") {
                bytes
            } else {
                JSONObject(bytes.toString(Charsets.UTF_8))
                    .put("schema_version", 99)
                    .toString()
                    .toByteArray()
            }
        }

        val failure = runCatching { service.inspect(newer) }.exceptionOrNull()

        assertThat((failure as BackupException).failure).isEqualTo(BackupFailure.NEWER_SCHEMA)
    }

    @Test
    public fun mesclarJuntaOsDoisLadosSemSobrescrever() = runBlocking {
        val packageFile = createPackage()

        // Depois do backup o usuário apagou o exemplar original e cadastrou outro.
        database.photoDao().delete("p1")
        database.exemplarDao().deleteById("e1")
        database.exemplarDao().insert(
            ExemplarEntity(
                id = "e2",
                variantId = "v1",
                internalCode = "HW-0002",
                condition = "LOOSE",
                packagingCondition = null,
                locationId = null,
                status = "OWNED",
                notes = null,
                rowVersion = 0,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        context.filesDir.resolve("images").deleteRecursively()

        val summary = service.restore(packageFile, RestoreMode.MERGE)

        // Os dois lados continuam lá: nada foi apagado nem sobrescrito.
        assertThat(database.exemplarDao().countAll()).isEqualTo(2)
        assertThat(database.exemplarDao().findById("e2")?.internalCode).isEqualTo("HW-0002")
        assertThat(database.exemplarDao().findById("e1")?.internalCode).isEqualTo("HW-0001")
        assertThat(database.photoDao().findById("p1")?.path).isEqualTo(imagePath)
        assertThat(context.filesDir.resolve(imagePath).readBytes()).isEqualTo(ORIGINAL_IMAGE)
        assertThat(summary.counts["exemplar"]).isEqualTo(2)

        // Fabricante com o mesmo nome é reaproveitado, não duplicado.
        val manufacturers = database.openHelper.readableDatabase
            .query("SELECT COUNT(*) FROM manufacturer")
            .use { cursor -> cursor.moveToFirst(); cursor.getInt(0) }
        assertThat(manufacturers).isEqualTo(1)

        val decisions = database.openHelper.readableDatabase
            .query("SELECT COUNT(*) FROM history_entry WHERE source = 'restore:merge'")
            .use { cursor -> cursor.moveToFirst(); cursor.getInt(0) }
        assertThat(decisions).isAtLeast(1)
    }

    @Test
    public fun mesclarDuasVezesNaoDuplicaItens() = runBlocking {
        val packageFile = createPackage()

        database.photoDao().delete("p1")
        database.exemplarDao().deleteById("e1")

        service.restore(packageFile, RestoreMode.MERGE)
        service.restore(packageFile, RestoreMode.MERGE)

        // O código interno já existente é mantido; a segunda mesclagem não insere de novo.
        assertThat(database.exemplarDao().countAll()).isEqualTo(1)
        assertThat(database.photoDao().countByExemplar("e1")).isEqualTo(1)
    }

    @Test
    public fun restauracaoDevolveBancoEFotos() = runBlocking {
        val packageFile = createPackage()

        // Depois do backup o usuário mexe em tudo: apaga foto e exemplar, e troca a imagem.
        database.photoDao().delete("p1")
        database.exemplarDao().deleteById("e1")
        writeImage(NEW_IMAGE)

        val summary = service.restore(packageFile)

        val restored = openDatabase()
        try {
            assertThat(restored.exemplarDao().countAll()).isEqualTo(1)
            assertThat(restored.manufacturerDao().findById("m1")?.name).isEqualTo("Hot Wheels")
            assertThat(restored.photoDao().findById("p1")?.path).isEqualTo(imagePath)
            assertThat(context.filesDir.resolve(imagePath).readBytes()).isEqualTo(ORIGINAL_IMAGE)
        } finally {
            restored.close()
        }
        assertThat(summary.counts["exemplar"]).isEqualTo(1)

        // Cópias de segurança do estado anterior ficam ao lado, para recuperação manual.
        val images = context.filesDir.resolve("images")
        assertThat(images.exists()).isTrue()
        assertThat(startsWithRestoreCopy(context.filesDir)).isTrue()
    }

    private fun createPackage(): File {
        val file = temp.newFile("backup.zip")
        file.outputStream().use { output ->
            runBlocking { service.create(output, "0.1.0 (1)", "Teste") }
        }
        return file
    }

    private fun openDatabase(): AppDatabase = Room
        .databaseBuilder(context, AppDatabase::class.java, DB_NAME)
        .addCallback(AppDatabase.CALLBACK)
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()

    private suspend fun seed() {
        database.manufacturerDao().insert(
            ManufacturerEntity("m1", "Hot Wheels", "EUA", null, 0, 0),
        )
        database.brandDao().insert(BrandEntity("b1", "m1", "Hot Wheels", null))
        database.carModelDao().insert(
            CarModelEntity(
                id = "cm1",
                manufacturerId = "m1",
                name = "Twin Mill",
                scale = "1:64",
                brandId = "b1",
                realVehicleId = null,
                bodyType = null,
                material = null,
                countryOfManufacture = null,
                notes = null,
                archivedAt = null,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        database.variantDao().insert(
            VariantEntity(
                id = "v1",
                modelId = "cm1",
                fingerprint = "fp-1",
                seriesId = null,
                primaryColorId = null,
                wheelsType = null,
                baseType = null,
                decoration = null,
                releaseYear = 2024,
                collectionNumber = null,
                seriesNumber = null,
                edition = null,
                manufacturerCode = null,
                packagingType = null,
                archivedAt = null,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        database.exemplarDao().insert(
            ExemplarEntity(
                id = "e1",
                variantId = "v1",
                internalCode = "HW-0001",
                condition = "SEALED",
                packagingCondition = null,
                locationId = null,
                status = "OWNED",
                notes = null,
                rowVersion = 0,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        database.photoDao().insert(
            PhotoEntity(
                id = "p1",
                exemplarId = "e1",
                angle = "FRONT",
                path = imagePath,
                checksum = "abc",
                isPrimary = true,
                sortOrder = 0,
                width = 100,
                height = 100,
                bytes = ORIGINAL_IMAGE.size.toLong(),
                capturedAt = 0,
            ),
        )
    }

    private fun writeImage(bytes: ByteArray) {
        val file = context.filesDir.resolve(imagePath)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    private fun entryNames(packageFile: File): Set<String> {
        val names = mutableSetOf<String>()
        ZipInputStream(packageFile.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) names += entry.name
            }
        }
        return names
    }

    private fun readEntry(packageFile: File, name: String): ByteArray {
        ZipInputStream(packageFile.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == name) return zip.readBytes()
            }
        }
        error("Entrada $name ausente no pacote.")
    }

    private fun rewrite(
        source: File,
        target: File,
        transform: (name: String, bytes: ByteArray) -> ByteArray,
    ) {
        ZipOutputStream(target.outputStream()).use { output ->
            ZipInputStream(source.inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val bytes = transform(entry.name, zip.readBytes())
                    output.putNextEntry(ZipEntry(entry.name))
                    output.write(bytes)
                    output.closeEntry()
                }
            }
        }
    }

    private fun startsWithRestoreCopy(directory: File): Boolean =
        directory.listFiles()?.any { it.name.startsWith("images.pre-restore-") } == true

    private companion object {
        const val DB_NAME = "backup-service-test.db"
        val ORIGINAL_IMAGE = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val NEW_IMAGE = byteArrayOf(9, 9, 9, 9)
    }
}
