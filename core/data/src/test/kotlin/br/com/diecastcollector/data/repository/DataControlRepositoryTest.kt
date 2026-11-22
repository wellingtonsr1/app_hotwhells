package br.com.diecastcollector.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.entity.CarModelEntity
import br.com.diecastcollector.data.db.entity.ExemplarEntity
import br.com.diecastcollector.data.db.entity.LocationEntity
import br.com.diecastcollector.data.db.entity.ManufacturerEntity
import br.com.diecastcollector.data.db.entity.PhotoEntity
import br.com.diecastcollector.data.db.entity.VariantEntity
import br.com.diecastcollector.data.storage.FileImageStore
import br.com.diecastcollector.domain.model.LocationType
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exclusão total dos dados (T146, FR-157, Art. III.7).
 *
 * O que o teste protege: a contagem mostra o que está gravado, a exclusão apaga **tudo**
 * — inclusive as fotos do disco — e o banco continua utilizável depois, sem índice parcial
 * ou tabela pela metade. Também prova que nenhum resíduo fica apontando para foto.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class DataControlRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: DataControlRepositoryImpl
    private lateinit var context: Context
    private lateinit var imagesDir: File

    @Before
    public fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        imagesDir = File(context.filesDir, "images").apply { deleteRecursively() }
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.CALLBACK)
            .allowMainThreadQueries()
            .build()
        repository = DataControlRepositoryImpl(
            database = database,
            imageStore = FileImageStore(context),
        )
    }

    @After
    public fun tearDown() {
        database.close()
        imagesDir.deleteRecursively()
    }

    @Test
    public fun `contagem reflete o acervo e zera depois da exclusao`(): Unit = runBlocking {
        seed()

        val before = repository.observeCounts().first()
        assertThat(before.exemplars).isEqualTo(2)
        assertThat(before.models).isEqualTo(1)
        assertThat(before.photos).isEqualTo(1)
        assertThat(before.locations).isEqualTo(1)
        assertThat(before.isEmpty).isFalse()

        val result = repository.deleteAll()

        assertThat(result.isSuccess).isTrue()
        val after = repository.observeCounts().first()
        assertThat(after.isEmpty).isTrue()
        assertThat(after.exemplars).isEqualTo(0)
        assertThat(after.photos).isEqualTo(0)
        // A lista do catálogo é derivada: sem variante, não sobra cartão.
        assertThat(database.variantDao().observeSummaries().first()).isEmpty()
        // Nada de linha de foto apontando para arquivo inexistente.
        assertThat(database.photoDao().countByExemplar("e1")).isEqualTo(0)
    }

    @Test
    public fun `exclusao remove as fotos do disco`(): Unit = runBlocking {
        seed()
        val photo = File(imagesDir, "e1/front.jpg").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }
        assertThat(photo.exists()).isTrue()

        repository.deleteAll().getOrThrow()

        assertThat(imagesDir.exists()).isFalse()
    }

    @Test
    public fun `banco continua utilizavel depois de excluir e aceita o mesmo fingerprint`(): Unit =
        runBlocking {
            seed()
            repository.deleteAll().getOrThrow()

            // O índice único parcial de fingerprint segue instalado, mas vazio: gravar de
            // novo o mesmo valor não colide com o que foi apagado.
            database.manufacturerDao().insert(
                ManufacturerEntity("m1", "Mattel", null, null, 1L, 1L),
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
                    createdAt = 1L,
                    updatedAt = 1L,
                ),
            )
            database.variantDao().insert(
                VariantEntity(
                    id = "variant2",
                    modelId = "model1",
                    fingerprint = FINGERPRINT,
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
                    createdAt = 1L,
                    updatedAt = 1L,
                ),
            )

            assertThat(repository.observeCounts().first().models).isEqualTo(1)
        }

    private suspend fun seed() {
        val millis = 0L
        database.manufacturerDao().insert(
            ManufacturerEntity("m1", "Mattel", null, null, millis, millis),
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
                createdAt = millis,
                updatedAt = millis,
            ),
        )
        database.variantDao().insert(
            VariantEntity(
                id = "variant1",
                modelId = "model1",
                fingerprint = FINGERPRINT,
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
                createdAt = millis,
                updatedAt = millis,
            ),
        )
        database.locationDao().insert(
            LocationEntity(
                id = "loc1",
                type = LocationType.RACK.name,
                name = "Estante da sala",
                parentId = null,
                archivedAt = null,
                createdAt = millis,
                updatedAt = millis,
            ),
        )
        listOf("e1", "e2").forEachIndexed { index, id ->
            database.exemplarDao().insert(
                ExemplarEntity(
                    id = id,
                    variantId = "variant1",
                    internalCode = "HW-00000$index",
                    condition = "SEALED",
                    packagingCondition = null,
                    locationId = "loc1",
                    status = "OWNED",
                    notes = null,
                    createdAt = millis,
                    updatedAt = millis,
                ),
            )
        }
        database.photoDao().insert(
            PhotoEntity(
                id = "p1",
                exemplarId = "e1",
                angle = "FRONT",
                path = "images/e1/front.jpg",
                checksum = "abc",
                isPrimary = true,
                sortOrder = 0,
                width = 1600,
                height = 1200,
                bytes = 3L,
                capturedAt = millis,
            ),
        )
    }

    private companion object {
        const val FINGERPRINT = "fp-twin-mill-red"
    }
}
