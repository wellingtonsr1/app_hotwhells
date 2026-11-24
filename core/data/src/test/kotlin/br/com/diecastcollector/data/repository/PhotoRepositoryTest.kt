package br.com.diecastcollector.data.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.AuditLogWriter
import br.com.diecastcollector.data.db.TransactionRunner
import br.com.diecastcollector.data.storage.FileImageStore
import br.com.diecastcollector.domain.model.PhotoAngle
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Testes de integração do PhotoRepository (T045, T046 - US2, FR-020, FR-021). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class PhotoRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var imageStore: FileImageStore
    private lateinit var repository: PhotoRepositoryImpl
    private val clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).addCallback(AppDatabase.CALLBACK).allowMainThreadQueries().build()

        runBlocking {
            val manufacturer = br.com.diecastcollector.data.db.entity.ManufacturerEntity(
                "mf1", "Mattel", null, null, 1L, 1L
            )
            database.manufacturerDao().insert(manufacturer)
            val model = br.com.diecastcollector.data.db.entity.CarModelEntity(
                "m1", "mf1", "Twin Mill", "1:64", null, null, null, null, null, null, null, 1L, 1L
            )
            database.carModelDao().insert(model)
            val variant = br.com.diecastcollector.data.db.entity.VariantEntity(
                "v1", "m1", "fp1", null, null, null, null, null, null, null, null, null, null, null, null, 1L, 1L
            )
            database.variantDao().insert(variant)
            val exemplar = br.com.diecastcollector.data.db.entity.ExemplarEntity(
                "ex1", "v1", "HW-000001", "SEALED", null, null, "OWNED", null, 0L, 1L, 1L
            )
            database.exemplarDao().insert(exemplar)
        }

        val runner = TransactionRunner(database)
        val auditLogWriter = AuditLogWriter(database.historyDao())
        imageStore = FileImageStore(ApplicationProvider.getApplicationContext())
        repository = PhotoRepositoryImpl(database, imageStore, runner, auditLogWriter, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createTestImageBytes(width: Int = 100, height: Int = 100): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    private fun filesDir(): File = ApplicationProvider.getApplicationContext<Context>().filesDir

    private fun photoFiles(): List<File> =
        File(filesDir(), "images/ex1").listFiles()
            ?.filter { it.extension == "jpg" }
            .orEmpty()

    @Test
    fun `adicionar primeira foto define como isPrimary true automaticamente`() = runBlocking {
        val bytes = createTestImageBytes()
        val result = repository.add(exemplarId = "ex1", angle = PhotoAngle.FRONT, source = bytes)

        assertThat(result.isSuccess).isTrue()
        val photo = result.getOrThrow()
        assertThat(photo.isPrimary).isTrue()
        assertThat(photo.angle).isEqualTo(PhotoAngle.FRONT)
        assertThat(photo.sortOrder).isEqualTo(0)
        assertThat(photo.checksum).isNotEmpty()
    }

    @Test
    fun `adicionar segunda foto define isPrimary false`() = runBlocking {
        val bytes = createTestImageBytes()
        val first = repository.add(exemplarId = "ex1", angle = PhotoAngle.FRONT, source = bytes).getOrThrow()
        val second = repository.add(exemplarId = "ex1", angle = PhotoAngle.BACK, source = bytes).getOrThrow()

        assertThat(first.isPrimary).isTrue()
        assertThat(second.isPrimary).isFalse()
        assertThat(second.sortOrder).isEqualTo(1)

        val list = repository.observeByExemplar("ex1").first()
        assertThat(list).hasSize(2)
    }

    @Test
    fun `setPrimary altera a foto principal`() = runBlocking {
        val bytes = createTestImageBytes()
        val p1 = repository.add(exemplarId = "ex1", angle = PhotoAngle.FRONT, source = bytes).getOrThrow()
        val p2 = repository.add(exemplarId = "ex1", angle = PhotoAngle.BACK, source = bytes).getOrThrow()

        val setRes = repository.setPrimary(exemplarId = "ex1", photoId = p2.id)
        assertThat(setRes.isSuccess).isTrue()

        val list = repository.observeByExemplar("ex1").first()
        val updatedP1 = list.find { it.id == p1.id }
        val updatedP2 = list.find { it.id == p2.id }

        assertThat(updatedP1?.isPrimary).isFalse()
        assertThat(updatedP2?.isPrimary).isTrue()
    }

    @Test
    fun `mapa por variante traz so a foto de capa de cada exemplar`() = runBlocking {
        val bytes = createTestImageBytes()
        repository.add(exemplarId = "ex1", angle = PhotoAngle.FRONT, source = bytes).getOrThrow()
        val back = repository.add(exemplarId = "ex1", angle = PhotoAngle.BACK, source = bytes).getOrThrow()
        repository.setPrimary(exemplarId = "ex1", photoId = back.id).getOrThrow()

        val paths = repository.observePrimaryPathsByVariant("v1").first()
        assertThat(paths).containsExactly("ex1", back.path)
        Unit
    }

    @Test
    fun `remove apaga registro do banco e arquivo do disco`() = runBlocking {
        val bytes = createTestImageBytes()
        val photo = repository.add(exemplarId = "ex1", angle = PhotoAngle.TOP, source = bytes).getOrThrow()
        assertThat(imageStore.exists(photo.path)).isTrue()

        val removeRes = repository.remove(photo.id)
        assertThat(removeRes.isSuccess).isTrue()

        val list = repository.observeByExemplar("ex1").first()
        assertThat(list).isEmpty()
        assertThat(imageStore.exists(photo.path)).isFalse()
    }

    // ---- Edição não destrutiva (FR-214–FR-219) -------------------------------------

    @Test
    fun `saveEdit preserva original e mantem categoria principal e vinculo`() = runBlocking {
        val original = repository
            .add(exemplarId = "ex1", angle = PhotoAngle.FRONT, source = createTestImageBytes())
            .getOrThrow()
        val editedBytes = createTestImageBytes(width = 60, height = 90)

        val edited = repository
            .saveEdit(original.id, editedBytes, "v1;aspect=SQUARE")
            .getOrThrow()

        assertThat(edited.isEdited).isTrue()
        assertThat(edited.path).isNotEqualTo(original.path)
        assertThat(edited.originalPath).isEqualTo(original.path)
        assertThat(edited.exemplarId).isEqualTo("ex1")
        assertThat(edited.angle).isEqualTo(PhotoAngle.FRONT)
        assertThat(edited.isPrimary).isTrue()
        assertThat(edited.sortOrder).isEqualTo(0)
        assertThat(edited.editParams).isEqualTo("v1;aspect=SQUARE")
        assertThat(edited.editedAt).isNotNull()
        assertThat(edited.width).isEqualTo(60)
        assertThat(edited.height).isEqualTo(90)

        // Original intocado e a foto continua sendo uma só.
        assertThat(imageStore.exists(original.path)).isTrue()
        assertThat(imageStore.inspect(original.path)!!.checksum).isEqualTo(original.checksum)
        assertThat(repository.observeByExemplar("ex1").first()).hasSize(1)
        assertThat(photoFiles()).hasSize(2)
    }

    @Test
    fun `segunda edicao substitui a derivada sem recomprimir o original`() = runBlocking {
        val original = repository
            .add(exemplarId = "ex1", angle = PhotoAngle.FRONT, source = createTestImageBytes())
            .getOrThrow()
        val first = repository
            .saveEdit(original.id, createTestImageBytes(80, 80), "v1;brightness=0.2")
            .getOrThrow()
        val firstDerivative = first.path

        val second = repository
            .saveEdit(original.id, createTestImageBytes(70, 70), "v1;contrast=0.4")
            .getOrThrow()

        assertThat(second.originalPath).isEqualTo(original.path)
        assertThat(second.path).isNotEqualTo(firstDerivative)
        assertThat(imageStore.exists(firstDerivative)).isFalse()
        assertThat(imageStore.exists(original.path)).isTrue()
        // No máximo dois arquivos: o original e a derivada ativa (FR-215).
        assertThat(photoFiles()).hasSize(2)
    }

    @Test
    fun `restoreOriginal volta ao arquivo original e apaga a derivada`() = runBlocking {
        val original = repository
            .add(exemplarId = "ex1", angle = PhotoAngle.BACK, source = createTestImageBytes())
            .getOrThrow()
        val edited = repository
            .saveEdit(original.id, createTestImageBytes(50, 50), "v1;rotation=1")
            .getOrThrow()
        val derivative = edited.path

        val restored = repository.restoreOriginal(original.id).getOrThrow()

        assertThat(restored.path).isEqualTo(original.path)
        assertThat(restored.originalPath).isNull()
        assertThat(restored.isEdited).isFalse()
        assertThat(restored.editParams).isNull()
        assertThat(restored.editedAt).isNull()
        assertThat(restored.checksum).isEqualTo(original.checksum)
        assertThat(restored.width).isEqualTo(original.width)
        assertThat(imageStore.exists(derivative)).isFalse()
        assertThat(imageStore.exists(original.path)).isTrue()
        assertThat(repository.observeByExemplar("ex1").first()).hasSize(1)
    }

    @Test
    fun `restoreOriginal sem edicao falha sem alterar nada`() = runBlocking {
        val photo = repository
            .add(exemplarId = "ex1", angle = PhotoAngle.LEFT, source = createTestImageBytes())
            .getOrThrow()

        val result = repository.restoreOriginal(photo.id)

        assertThat(result.isFailure).isTrue()
        val current = repository.observeByExemplar("ex1").first().single()
        assertThat(current.path).isEqualTo(photo.path)
        assertThat(current.checksum).isEqualTo(photo.checksum)
        assertThat(imageStore.exists(photo.path)).isTrue()
    }

    @Test
    fun `remove apaga tambem o original preservado`() = runBlocking {
        val original = repository
            .add(exemplarId = "ex1", angle = PhotoAngle.RIGHT, source = createTestImageBytes())
            .getOrThrow()
        val edited = repository
            .saveEdit(original.id, createTestImageBytes(60, 60), "v1;sharpness=0.3")
            .getOrThrow()

        repository.remove(original.id).getOrThrow()

        assertThat(imageStore.exists(edited.path)).isFalse()
        assertThat(imageStore.exists(original.path)).isFalse()
        assertThat(repository.observeByExemplar("ex1").first()).isEmpty()
    }

    @Test
    fun `saveEdit sem original no disco falha e mantem o registro`() = runBlocking {
        val original = repository
            .add(exemplarId = "ex1", angle = PhotoAngle.FRONT, source = createTestImageBytes())
            .getOrThrow()
        File(filesDir(), original.path).delete()

        val result = repository.saveEdit(original.id, createTestImageBytes(40, 40), "v1")

        assertThat(result.isFailure).isTrue()
        val current = repository.observeByExemplar("ex1").first().single()
        assertThat(current.path).isEqualTo(original.path)
        assertThat(current.isEdited).isFalse()
    }
}
