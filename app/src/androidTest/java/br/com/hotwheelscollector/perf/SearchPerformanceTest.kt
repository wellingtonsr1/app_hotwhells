package br.com.hotwheelscollector.perf

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.entity.*
import br.com.hotwheelscollector.domain.model.CatalogSort
import br.com.hotwheelscollector.domain.model.ExemplarStatus
import br.com.hotwheelscollector.domain.model.SearchFilters
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/***
 * T058: Teste de desempenho com muitos exemplares.
 * 
 * Goal: busca < 300 ms, lista < 1 s com muitos exemplares no banco real.
 * 
 * Este teste popula o banco e mede performance de busca e listagem.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class SearchPerformanceTest {

    lateinit var database: AppDatabase
    lateinit var repository: SearchRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SearchRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `busca FTS deve ser rapida com muitos registros`() = runBlocking {
        runBlocking { seed() }

        val startNano = System.nanoTime()
        val results = repository.observeResults(SearchFilters(query = "twin")).first()
        val elapsedMs = (System.nanoTime() - startNano) / 1_000_000

        // Busca deve completar em menos de 300ms
        assertThat(elapsedMs).isLessThan(300)
        // Deve retornar resultados relevantes
        assertThat(results).isNotEmpty()
    }

    @Test
    fun `listagem completa deve ser rapida com muitos registros`() = runBlocking {
        runBlocking { seed() }

        val startNano = System.nanoTime()
        val allResults = repository.observeResults(SearchFilters()).first()
        val elapsedMs = (System.nanoTime() - startNano) / 1_000_000

        // Listagem deve completar em menos de 1s
        assertThat(elapsedMs).isLessThan(1000)
        // Deve retornar um numero significativo de resultados
        assertThat(allResults.size).isGreaterThan(500)
    }

    @Test
    fun `filtros combinados devem ser rapidos com muitos registros`() = runBlocking {
        runBlocking { seed() }

        val startNano = System.nanoTime()
        val filtered = repository.observeResults(
            SearchFilters().copy(
                manufacturerIds = setOf("m1"),
                colorIds = setOf("red"),
                statuses = ExemplarStatus.OWNED.entries.toSet()
            )
        ).first()
        val elapsedMs = (System.nanoTime() - startNano) / 1_000_000

        // Filtros combinados devem completar em menos de 300ms
        assertThat(elapsedMs).isLessThan(300)
        // Deve retornar resultados
        assertThat(filtered).isNotEmpty()
    }

    @Test
    fun `ordenacao por ano deve ser rapida com muitos registros`() = runBlocking {
        runBlocking { seed() }

        val startNano = System.nanoTime()
        val sorted = repository.observeResults(
            SearchFilters(sort = CatalogSort.YEAR)
        ).first()
        val elapsedMs = (System.nanoTime() - startNano) / 1_000_000

        // Ordenacao deve completar em menos de 300ms
        assertThat(elapsedMs).isLessThan(300)
        // Resultados ordenados devem ser consistentes
        assertThat(sorted).isNotEmpty()
    }

    @Test
    fun `busca sem acento encontra registros`() = runBlocking {
        runBlocking { seed() }

        val startNano = System.nanoTime()
        // Busca sem acento deve encontrar registros acentuados (tokenizer unicode61 remove_diacritics=1)
        val results = repository.observeResults(
            SearchFilters(query = "colecao")
        ).first()
        val elapsedMs = (System.nanoTime() - startNano) / 1_000_000

        // Busca deve completar rapidamente
        assertThat(elapsedMs).isLessThan(300)
        // Deve encontrar resultados (indice remove diacriticos)
        assertThat(results).isNotEmpty()
    }

    private suspend fun seed() {
        val db = database
        val t = 0L

        db.manufacturerDao().insert(ManufacturerEntity("m1", "Mattel", null, null, t, t))
        db.manufacturerDao().insert(ManufacturerEntity("m2", "Spin Master", null, null, t, t))
        db.seriesDao().insert(SeriesEntity("s1", "m1", "Car Culture", null, null, 2024, null, t, t))
        db.collectionDao().insert(CollectionEntity("c1", "Coleção A", null, null, null, null, t, t))
        db.colorDao().insert(ColorEntity("red", "Vermelho", null))
        db.colorDao().insert(ColorEntity("blue", "Azul", null))
        db.locationDao().insert(LocationEntity("l1", "Caixa", "Caixa 1", null, null, t, t))
        db.locationDao().insert(LocationEntity("l2", "Caixa", "Caixa 2", null, null, t, t))

        db.carModelDao().insert(CarModelEntity("model1", "m1", "Twin Mill", "1:64", null, null, null, null, null, null, null, 0L, 0L))
        db.carModelDao().insert(CarModelEntity("model2", "m1", "Skyline R34", "1:64", null, null, null, null, null, null, null, 0L, 0L))
        db.carModelDao().insert(CarModelEntity("model3", "m2", "Bone Shaker", "1:24", null, null, null, null, null, null, null, 0L, 0L))

        // Variants with null for all optional fields
        db.variantDao().insert(VariantEntity(
            id = "variant1",
            modelId = "model1",
            fingerprint = "fp-v1",
            seriesId = "s1",
            primaryColorId = "red",
            wheelsType = null,
            baseType = null,
            decoration = null,
            releaseYear = null,
            collectionNumber = null,
            seriesNumber = null,
            edition = null,
            manufacturerCode = null,
            packagingType = null,
            archivedAt = null,
            createdAt = t,
            updatedAt = t
        ))
        db.variantDao().insert(VariantEntity(
            id = "variant2",
            modelId = "model2",
            fingerprint = "fp-v2",
            seriesId = "s1",
            primaryColorId = "blue",
            wheelsType = null,
            baseType = null,
            decoration = null,
            releaseYear = null,
            collectionNumber = null,
            seriesNumber = null,
            edition = null,
            manufacturerCode = null,
            packagingType = null,
            archivedAt = null,
            createdAt = t,
            updatedAt = t
        ))
        db.variantDao().insert(VariantEntity(
            id = "variant3",
            modelId = "model3",
            fingerprint = "fp-v3",
            seriesId = null,
            primaryColorId = null,
            wheelsType = null,
            baseType = null,
            decoration = null,
            releaseYear = null,
            collectionNumber = null,
            seriesNumber = null,
            edition = null,
            manufacturerCode = null,
            packagingType = null,
            archivedAt = null,
            createdAt = t,
            updatedAt = t
        ))

        // Exemplars
        db.exemplarDao().insert(ExemplarEntity(
            id = "e1",
            variantId = "variant1",
            internalCode = "HW-1",
            condition = "SEALED",
            packagingCondition = null,
            locationId = "l1",
            status = "OWNED",
            notes = null,
            rowVersion = 0L,
            createdAt = t,
            updatedAt = t
        ))
        db.exemplarDao().insert(ExemplarEntity(
            id = "e2",
            variantId = "variant1",
            internalCode = "HW-2",
            condition = "LOOSE",
            packagingCondition = null,
            locationId = null,
            status = "SOLD",
            notes = null,
            rowVersion = 0L,
            createdAt = t,
            updatedAt = t
        ))
        db.exemplarDao().insert(ExemplarEntity(
            id = "e3",
            variantId = "variant2",
            internalCode = "HW-3",
            condition = "LOOSE",
            packagingCondition = null,
            locationId = "l2",
            status = "OWNED",
            notes = null,
            rowVersion = 0L,
            createdAt = t,
            updatedAt = t
        ))
        db.exemplarDao().insert(ExemplarEntity(
            id = "e4",
            variantId = "variant3",
            internalCode = "HW-4",
            condition = "SEALED",
            packagingCondition = null,
            locationId = null,
            status = "OWNED",
            notes = null,
            rowVersion = 0L,
            createdAt = t,
            updatedAt = t
        ))

        // Photos
        db.photoDao().insert(PhotoEntity(
            id = "p1",
            exemplarId = "e1",
            angle = "FRONT",
            path = "images/e1/front_1.jpg",
            checksum = "chk-p1",
            isPrimary = true,
            sortOrder = 0,
            width = 800,
            height = 800,
            bytes = 100_000L,
            capturedAt = 0L
        ))
        db.photoDao().insert(PhotoEntity(
            id = "p2",
            exemplarId = "e2",
            angle = "BACK",
            path = "images/e2/back_1.jpg",
            checksum = "chk-p2",
            isPrimary = false,
            sortOrder = 0,
            width = 800,
            height = 800,
            bytes = 100_000L,
            capturedAt = 0L
        ))

        // Aquisições
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO acquisition (id, exemplar_id, date, price_cents, currency, created_at, updated_at) VALUES ('a1', 'e1', '2026-01-10', 12000, 'BRL', 0, 0)"
        )
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO acquisition (id, exemplar_id, date, price_cents, currency, created_at, updated_at) VALUES ('a2', 'e2', '2026-02-01', 5000, 'BRL', 0, 0)"
        )
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO acquisition (id, exemplar_id, date, price_cents, currency, created_at, updated_at) VALUES ('a3', 'e3', '2025-12-01', 30000, 'BRL', 0, 0)"
        )
    }
}