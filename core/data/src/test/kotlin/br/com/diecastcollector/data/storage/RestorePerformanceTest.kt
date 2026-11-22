package br.com.diecastcollector.data.storage

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.entity.CarModelEntity
import br.com.diecastcollector.data.db.entity.ExemplarEntity
import br.com.diecastcollector.data.db.entity.ManufacturerEntity
import br.com.diecastcollector.data.db.entity.PhotoEntity
import br.com.diecastcollector.data.db.entity.VariantEntity
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Restauração em escala (T153, SC-013).
 *
 * O que o teste exige: **1.000 exemplares e 2.000 fotos** restaurados em **menos de 5
 * minutos**, com os vínculos de imagem preservados — cada `photo.path` aponta para um
 * arquivo existente e o checksum gravado no banco continua igual ao do arquivo.
 *
 * O pacote é criado pelo próprio `FileBackupService` (banco, fotos, JSON e CSV), como o
 * usuário faria; a restauração substitui o banco real do teste e as fotos em `filesDir`.
 * Não há atalho: a validação de integridade, contagens, hashes e chaves estrangeiras roda
 * no caminho de produção.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class RestorePerformanceTest {

    @get:Rule
    public val temp: TemporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var service: FileBackupService

    /** Tamanho do último pacote criado, para a mensagem de evidência. */
    private var packageBytes: Long = 0L

    @Before
    public fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DB_NAME)
        imagesDir().deleteRecursively()

        database = openDatabase()
        service = FileBackupService(
            context = context,
            database = database,
            clock = Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC),
        )

        runBlocking { seed() }
    }

    @After
    public fun tearDown() {
        runCatching { database.close() }
        context.deleteDatabase(DB_NAME)
        imagesDir().deleteRecursively()
        context.filesDir.listFiles { file -> file.name.startsWith("images.pre-restore-") }
            ?.forEach { it.deleteRecursively() }
    }

    @Test
    public fun restauraMilExemplaresEDoisMilFotosEmMenosDeCincoMinutos() = runBlocking {
        val packageFile = createPackage()

        // O usuário limpa o acervo antes de restaurar: nada do que estava aqui pode
        // mascarar o que veio do pacote.
        database.clearAllTables()
        imagesDir().deleteRecursively()
        assertThat(database.exemplarDao().countAll()).isEqualTo(0)

        val startedAt = System.nanoTime()
        val summary = service.restore(packageFile)
        val restoreMillis = (System.nanoTime() - startedAt) / 1_000_000

        println(
            "T153 — pacote de ${packageBytes / 1024} KB; " +
                "restauração de $EXEMPLARS exemplares e $PHOTOS fotos em ${restoreMillis}ms",
        )

        assertThat(summary.counts["exemplar"]).isEqualTo(EXEMPLARS)
        assertThat(summary.imageCount).isEqualTo(PHOTOS)

        val restored = openDatabase()
        try {
            assertThat(restored.exemplarDao().countAll()).isEqualTo(EXEMPLARS)

            val photos = readRestoredPhotos(restored)
            assertThat(photos).hasSize(PHOTOS)

            val missing = photos.filterNot { (path, _) -> context.filesDir.resolve(path).isFile }
            assertThat(missing.map { it.first }).isEmpty()

            val corrupted = photos.filter { (path, checksum) ->
                sha256(context.filesDir.resolve(path)) != checksum
            }
            assertThat(corrupted.map { it.first }).isEmpty()

            val brokenLinks = restored.openHelper.readableDatabase
                .query("PRAGMA foreign_key_check")
                .use { it.count }
            assertThat(brokenLinks).isEqualTo(0)

            // Amostra do conteúdo: o primeiro e o último exemplar continuam identificáveis.
            assertThat(restored.exemplarDao().findByInternalCode(code(0))?.id).isEqualTo("e0")
            assertThat(restored.exemplarDao().findByInternalCode(code(EXEMPLARS - 1))?.id)
                .isEqualTo("e${EXEMPLARS - 1}")
        } finally {
            restored.close()
        }

        assertThat(restoreMillis).isLessThan(FIVE_MINUTES_MS)
    }

    private fun createPackage(): File {
        val file = temp.newFile("backup-perf.zip")
        file.outputStream().use { output ->
            runBlocking { service.create(output, "0.1.0 (1)", "Teste de escala") }
        }
        packageBytes = file.length()
        return file
    }

    private fun openDatabase(): AppDatabase = Room
        .databaseBuilder(context, AppDatabase::class.java, DB_NAME)
        .addCallback(AppDatabase.CALLBACK)
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .allowMainThreadQueries()
        .build()

    private suspend fun seed() {
        database.withTransaction {
            database.manufacturerDao().insert(
                ManufacturerEntity("m1", "Mattel", "EUA", null, 0, 0),
            )
            database.carModelDao().insert(
                CarModelEntity(
                    id = "model1",
                    manufacturerId = "m1",
                    name = "Twin Mill",
                    scale = "1:64",
                    brandId = null,
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
                    modelId = "model1",
                    fingerprint = "fp-twin-mill",
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

            repeat(EXEMPLARS) { index ->
                val exemplarId = "e$index"
                database.exemplarDao().insert(
                    ExemplarEntity(
                        id = exemplarId,
                        variantId = "v1",
                        internalCode = code(index),
                        condition = "SEALED",
                        packagingCondition = null,
                        locationId = null,
                        status = "OWNED",
                        notes = null,
                        createdAt = 0,
                        updatedAt = 0,
                    ),
                )

                // Duas fotos por exemplar: 2.000 arquivos reais em disco, como no acervo.
                ANGLES.forEach { angle ->
                    val bytes = imageBytes(index, angle)
                    val relativePath = "images/$exemplarId/$angle.jpg"
                    val file = context.filesDir.resolve(relativePath).apply {
                        parentFile?.mkdirs()
                        writeBytes(bytes)
                    }
                    assertThat(file.length()).isEqualTo(bytes.size.toLong())

                    database.photoDao().insert(
                        PhotoEntity(
                            id = "p-$exemplarId-$angle",
                            exemplarId = exemplarId,
                            angle = angle.uppercase(),
                            path = relativePath,
                            checksum = sha256(bytes),
                            isPrimary = angle == "front",
                            sortOrder = if (angle == "front") 0 else 1,
                            width = 1600,
                            height = 1200,
                            bytes = bytes.size.toLong(),
                            capturedAt = 0,
                        ),
                    )
                }
            }
        }
    }

    private fun readRestoredPhotos(database: AppDatabase): List<Pair<String, String>> =
        database.openHelper.readableDatabase
            .query("SELECT path, checksum FROM photo")
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getString(1))
                }
            }

    private fun imagesDir(): File = context.filesDir.resolve("images")

    private fun imageBytes(index: Int, angle: String): ByteArray {
        val seed = "$index-$angle".toByteArray()
        return ByteArray(PHOTO_BYTES) { position -> seed[position % seed.size] }
    }

    private fun sha256(file: File): String = sha256(file.readBytes())

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun code(index: Int): String = "HW-%06d".format(index)

    private companion object {
        const val DB_NAME = "restore-performance-test.db"
        const val EXEMPLARS = 1_000
        const val PHOTOS = 2_000
        const val PHOTO_BYTES = 512
        const val FIVE_MINUTES_MS = 5 * 60 * 1000L
        val ANGLES = listOf("front", "back")
    }
}
