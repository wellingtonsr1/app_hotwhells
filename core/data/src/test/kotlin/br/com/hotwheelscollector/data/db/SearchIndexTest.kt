package br.com.hotwheelscollector.data.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.hotwheelscollector.data.db.entity.CarModelEntity
import br.com.hotwheelscollector.data.db.entity.CollectionEntity
import br.com.hotwheelscollector.data.db.entity.CollectionItemEntity
import br.com.hotwheelscollector.data.db.entity.ManufacturerEntity
import br.com.hotwheelscollector.data.db.entity.VariantEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sincronização do índice FTS4 (FR-114–FR-115).
 *
 * O banco é aberto sem o callback para simular um acervo que existia antes dos gatilhos:
 * `install` precisa reconstruir o índice quando a contagem diverge. Depois disso, cada
 * escrita passa pelos gatilhos como no app.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class SearchIndexTest {

    private lateinit var database: AppDatabase

    @Before
    public fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        seedCatalog()
        // Acervo anterior aos gatilhos: a instalação precisa popular o índice.
        SearchIndexSync.install(database.openHelper.writableDatabase)
    }

    @After
    public fun tearDown() {
        database.close()
    }

    @Test
    public fun `instalacao reconstroi indice de acervo existente`(): Unit = runBlocking {
        assertThat(database.searchIndexDao().searchVariantIds("twin", 10))
            .containsExactly(VARIANT_ID)
        assertThat(database.searchIndexDao().searchVariantIds("car culture", 10))
            .containsExactly(VARIANT_ID)
    }

    @Test
    public fun `renomear o modelo atualiza o texto indexado`(): Unit = runBlocking {
        database.carModelDao().update(
            model().copy(name = "Bone Shaker"),
        )

        assertThat(database.searchIndexDao().searchVariantIds("twin", 10)).isEmpty()
        assertThat(database.searchIndexDao().searchVariantIds("bone", 10))
            .containsExactly(VARIANT_ID)
    }

    @Test
    public fun `nova variante entra no indice sem reabrir o banco`(): Unit = runBlocking {
        database.carModelDao().insert(
            model().copy(id = "model2", name = "Skyline R34"),
        )

        assertThat(database.searchIndexDao().searchVariantIds("skyline", 10)).isEmpty()

        database.variantDao().insert(
            variant().copy(id = "variant2", modelId = "model2", fingerprint = "fp-2"),
        )

        assertThat(database.searchIndexDao().searchVariantIds("skyline", 10))
            .containsExactly("variant2")
        assertThat(database.searchIndexDao().searchVariantIds("twin", 10))
            .containsExactly(VARIANT_ID)
    }

    @Test
    public fun `colecao nova aparece no texto da variante`(): Unit = runBlocking {
        database.collectionDao().insert(
            CollectionEntity("c2", "Garagem", null, null, null, null, 0L, 0L),
        )

        assertThat(database.searchIndexDao().searchVariantIds("garagem", 10)).isEmpty()

        database.collectionItemDao().insert(
            CollectionItemEntity("ci2", "c2", VARIANT_ID, null, 1, 0, null),
        )

        assertThat(database.searchIndexDao().searchVariantIds("garagem", 10))
            .containsExactly(VARIANT_ID)
    }

    @Test
    public fun `indice apagado e repopulado ao divergir a contagem`(): Unit = runBlocking {
        val db = database.openHelper.writableDatabase
        db.execSQL("DELETE FROM search_index")

        SearchIndexSync.install(db)

        assertThat(database.searchIndexDao().searchVariantIds("twin", 10))
            .containsExactly(VARIANT_ID)
    }

    private suspend fun seedCatalog() {
        database.manufacturerDao().insert(ManufacturerEntity("m1", "Mattel", null, null, 0L, 0L))
        database.collectionDao().insert(
            CollectionEntity("c1", "Coleção A", null, null, null, null, 0L, 0L),
        )
        database.carModelDao().insert(model())
        database.seriesDao().insert(
            br.com.hotwheelscollector.data.db.entity.SeriesEntity(
                "s1", "m1", "Car Culture", null, null, 2024, null, 0L, 0L,
            ),
        )
        database.variantDao().insert(variant())
        database.collectionItemDao().insert(
            CollectionItemEntity("ci1", "c1", VARIANT_ID, null, 1, 0, null),
        )
    }

    private fun model() = CarModelEntity(
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
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun variant() = VariantEntity(
        id = VARIANT_ID,
        modelId = "model1",
        fingerprint = "fp-1",
        seriesId = "s1",
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
        createdAt = 0L,
        updatedAt = 0L,
    )

    private companion object {
        const val VARIANT_ID = "variant1"
    }
}
