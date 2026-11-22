package br.com.diecastcollector.data.repository

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

    private fun createTestImageBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }

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
}
