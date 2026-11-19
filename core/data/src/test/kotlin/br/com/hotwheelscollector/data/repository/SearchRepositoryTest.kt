package br.com.hotwheelscollector.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.entity.CarModelEntity
import br.com.hotwheelscollector.data.db.entity.CollectionEntity
import br.com.hotwheelscollector.data.db.entity.CollectionItemEntity
import br.com.hotwheelscollector.data.db.entity.ColorEntity
import br.com.hotwheelscollector.data.db.entity.ExemplarEntity
import br.com.hotwheelscollector.data.db.entity.LocationEntity
import br.com.hotwheelscollector.data.db.entity.ManufacturerEntity
import br.com.hotwheelscollector.data.db.entity.PhotoEntity
import br.com.hotwheelscollector.data.db.entity.SeriesEntity
import br.com.hotwheelscollector.data.db.entity.VariantEntity
import br.com.hotwheelscollector.domain.model.CatalogSort
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.ExemplarStatus
import br.com.hotwheelscollector.domain.model.SearchFilters
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Busca e filtros do acervo (FR-114–FR-122) no banco real, com o índice FTS instalado pelo
 * callback de abertura.
 *
 * O seed cobre o que costuma passar despercebido: vendido continua no banco mas sai do
 * padrão, duas variantes do mesmo modelo, acervo com e sem preço de aquisição, e nomes
 * acentuados que precisam ser encontrados sem acento.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class SearchRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: SearchRepositoryImpl

    @Before
    public fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.CALLBACK)
            .allowMainThreadQueries()
            .build()
        repository = SearchRepositoryImpl(database)
    }

    @After
    public fun tearDown() {
        database.close()
    }

    @Test
    public fun `padrao mostra so o acervo ativo e ordena por nome`(): Unit = runBlocking {
        seed()

        val results = repository.observeResults(SearchFilters()).first()

        assertThat(results.map { it.exemplarId }).containsExactly("e4", "e3", "e1").inOrder()
        assertThat(results.map { it.modelName })
            .containsExactly("Bone Shaker", "Skyline R34", "Twin Mill").inOrder()
        assertThat(results.map { it.status }).containsExactly(
            ExemplarStatus.OWNED,
            ExemplarStatus.OWNED,
            ExemplarStatus.OWNED,
        )
    }

    @Test
    public fun `busca textual tolera prefixo ordem livre e acento`(): Unit = runBlocking {
        seed()
        val all = SearchFilters(statuses = ExemplarStatus.entries.toSet())

        assertThat(repository.observeResults(all.copy(query = "twin")).first().map { it.exemplarId })
            .containsExactly("e1", "e2")

        assertThat(repository.observeResults(all.copy(query = "r34 sky")).first().map { it.modelName })
            .containsExactly("Skyline R34")

        // O índice remove diacríticos: "colecao" encontra "Coleção A".
        assertThat(repository.observeResults(all.copy(query = "colecao")).first().map { it.exemplarId })
            .containsExactly("e1", "e2")
    }

    @Test
    public fun `status escolhido troca o recorte do acervo`(): Unit = runBlocking {
        seed()

        val sold = repository.observeResults(
            SearchFilters(statuses = setOf(ExemplarStatus.SOLD)),
        ).first()

        assertThat(sold.map { it.exemplarId }).containsExactly("e2")
        assertThat(sold.single().condition).isEqualTo(Condition.LOOSE)
    }

    @Test
    public fun `filtros de categorias se somam e dentro da categoria se alternam`(): Unit = runBlocking {
        seed()

        // Fabricante + cor são categorias diferentes: interseção.
        val mattelBlue = repository.observeResults(
            SearchFilters(manufacturerIds = setOf("m1"), colorIds = setOf("blue")),
        ).first()
        assertThat(mattelBlue.map { it.exemplarId }).containsExactly("e3")

        // Duas cores na mesma categoria: união.
        val redOrBlue = repository.observeResults(
            SearchFilters(colorIds = setOf("red", "blue")),
        ).first()
        assertThat(redOrBlue.map { it.exemplarId }).containsExactly("e4", "e3", "e1").inOrder()

        // Condição e localização também entram na interseção.
        val sealedInBox = repository.observeResults(
            SearchFilters(
                conditions = setOf(Condition.SEALED),
                locationIds = setOf("l1"),
            ),
        ).first()
        assertThat(sealedInBox.map { it.exemplarId }).containsExactly("e1")
        assertThat(sealedInBox.single().locationName).isEqualTo("Caixa 1")
    }

    @Test
    public fun `filtros de serie colecao ano escala e modelo`(): Unit = runBlocking {
        seed()

        assertThat(
            repository.observeResults(SearchFilters(seriesIds = setOf("s1"))).first(),
        ).hasSize(2)
        assertThat(
            repository.observeResults(SearchFilters(collectionIds = setOf("c1"))).first()
                .map { it.exemplarId },
        ).containsExactly("e1")
        assertThat(
            repository.observeResults(SearchFilters(years = setOf(2019))).first()
                .map { it.exemplarId },
        ).containsExactly("e4")
        assertThat(
            repository.observeResults(SearchFilters(scales = setOf("1:24"))).first()
                .map { it.exemplarId },
        ).containsExactly("e4")
        assertThat(
            repository.observeResults(SearchFilters(modelIds = setOf("model2"))).first()
                .map { it.exemplarId },
        ).containsExactly("e3")
    }

    @Test
    public fun `faixa de preco usa o preco de aquisicao e exclui sem preco`(): Unit = runBlocking {
        seed()

        val middle = repository.observeResults(
            SearchFilters(priceMinCents = 10_000, priceMaxCents = 20_000),
        ).first()
        assertThat(middle.map { it.exemplarId }).containsExactly("e1")
        assertThat(middle.single().priceCents).isEqualTo(12_000L)

        val from = repository.observeResults(
            SearchFilters(priceMinCents = 20_000),
        ).first()
        assertThat(from.map { it.exemplarId }).containsExactly("e3")

        // O exemplar sem aquisição não tem preço para comparar e fica fora da faixa.
        assertThat(from.none { it.exemplarId == "e4" }).isTrue()
    }

    @Test
    public fun `ordenacoes por ano aquisicao e valor`(): Unit = runBlocking {
        seed()

        val byYear = repository.observeResults(
            SearchFilters(sort = CatalogSort.YEAR),
        ).first()
        // Ano decrescente e sem ano por último.
        assertThat(byYear.map { it.exemplarId }).containsExactly("e1", "e3", "e4").inOrder()

        val byAcquisition = repository.observeResults(
            SearchFilters(sort = CatalogSort.ACQUISITION_DATE),
        ).first()
        // Sem data de compra, e4 fica por último; e1 é mais recente que e3.
        assertThat(byAcquisition.map { it.exemplarId }).containsExactly("e1", "e3", "e4").inOrder()

        val byValue = repository.observeResults(
            SearchFilters(sort = CatalogSort.VALUE),
        ).first()
        assertThat(byValue.map { it.exemplarId }).containsExactly("e3", "e1", "e4").inOrder()
    }

    @Test
    public fun `galeria recebe a foto principal ou a primeira disponivel`(): Unit = runBlocking {
        seed()

        val owned = repository.observeResults(SearchFilters()).first()
        val twin = owned.single { it.exemplarId == "e1" }
        assertThat(twin.primaryPhotoPath).isEqualTo("images/e1/front_1.jpg")
        // Sem foto cadastrada, a galeria mostra o marcador em vez de sumir com o item.
        assertThat(owned.single { it.exemplarId == "e3" }.primaryPhotoPath).isNull()

        // O vendido não tem foto principal, mas tem uma foto: a galeria usa a primeira.
        val sold = repository.observeResults(
            SearchFilters(statuses = setOf(ExemplarStatus.SOLD)),
        ).first().single()
        assertThat(sold.primaryPhotoPath).isEqualTo("images/e2/back_1.jpg")
    }

    @Test
    public fun `opcoes trazem rotulos e ids do catalogo`(): Unit = runBlocking {
        seed()

        val options = repository.observeOptions().first()

        assertThat(options.manufacturers.map { it.label })
            .containsExactly("Mattel", "Spin Master").inOrder()
        assertThat(options.collections.map { it.label }).containsExactly("Coleção A")
        assertThat(options.scales).containsExactly("1:24", "1:64").inOrder()
        assertThat(options.years).containsExactly(2024, 2020, 2019).inOrder()
        assertThat(options.colors.map { it.label }).containsExactly("Azul", "Vermelho").inOrder()
        assertThat(options.locations.map { it.label }).containsExactly("Caixa 1", "Caixa 2").inOrder()
    }

    @Test
    public fun `variantIds alimenta o filtro por variante da busca`(): Unit = runBlocking {
        seed()

        assertThat(repository.variantIds("twin", 10)).containsExactly("variant1")
        assertThat(repository.variantIds("   ", 10)).isEmpty()
    }

    private suspend fun seed() {
        val db = database
        val t = 0L
        db.manufacturerDao().insert(ManufacturerEntity("m1", "Mattel", null, null, t, t))
        db.manufacturerDao().insert(ManufacturerEntity("m2", "Spin Master", null, null, t, t))
        db.seriesDao().insert(
            SeriesEntity("s1", "m1", "Car Culture", null, null, 2024, null, t, t),
        )
        db.collectionDao().insert(
            CollectionEntity("c1", "Coleção A", null, null, null, null, t, t),
        )
        db.colorDao().insert(ColorEntity("red", "Vermelho", null))
        db.colorDao().insert(ColorEntity("blue", "Azul", null))
        db.locationDao().insert(LocationEntity("l1", "Caixa", "Caixa 1", null, null, t, t))
        db.locationDao().insert(LocationEntity("l2", "Caixa", "Caixa 2", null, null, t, t))

        db.carModelDao().insert(model("model1", "m1", "Twin Mill", "1:64"))
        db.carModelDao().insert(model("model2", "m1", "Skyline R34", "1:64"))
        db.carModelDao().insert(model("model3", "m2", "Bone Shaker", "1:24"))

        db.variantDao().insert(variant("variant1", "model1", "s1", "red", 2024))
        db.variantDao().insert(variant("variant2", "model2", "s1", "blue", 2020))
        db.variantDao().insert(variant("variant3", "model3", null, "blue", 2019))
        db.collectionItemDao().insert(
            CollectionItemEntity("ci1", "c1", "variant1", null, 1, 0, null),
        )

        // Os gatilhos instalados no onOpen indexam as variantes assim que elas entram.
        db.exemplarDao().insert(exemplar("e1", "variant1", "HW-1", "SEALED", "OWNED", "l1"))
        db.exemplarDao().insert(exemplar("e2", "variant1", "HW-2", "LOOSE", "SOLD", null))
        db.exemplarDao().insert(exemplar("e3", "variant2", "HW-3", "LOOSE", "OWNED", "l2"))
        db.exemplarDao().insert(exemplar("e4", "variant3", "HW-4", "SEALED", "OWNED", null))

        db.photoDao().insert(photo("p1", "e1", "FRONT", "images/e1/front_1.jpg", primary = true))
        db.photoDao().insert(photo("p2", "e2", "BACK", "images/e2/back_1.jpg", primary = false))

        acquisition("a1", "e1", "2026-01-10", 12_000)
        acquisition("a2", "e2", "2026-02-01", 5_000)
        acquisition("a3", "e3", "2025-12-01", 30_000)
    }

    private fun acquisition(id: String, exemplarId: String, date: String, price: Long) {
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO acquisition (id, exemplar_id, date, price_cents, currency, " +
                "created_at, updated_at) VALUES ('$id', '$exemplarId', '$date', $price, 'BRL', 0, 0)",
        )
    }

    private fun model(id: String, manufacturerId: String, name: String, scale: String) =
        CarModelEntity(
            id = id,
            manufacturerId = manufacturerId,
            name = name,
            scale = scale,
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

    private fun variant(
        id: String,
        modelId: String,
        seriesId: String?,
        colorId: String,
        year: Int,
    ) = VariantEntity(
        id = id,
        modelId = modelId,
        fingerprint = "fp-$id",
        seriesId = seriesId,
        primaryColorId = colorId,
        wheelsType = null,
        baseType = null,
        decoration = null,
        releaseYear = year,
        collectionNumber = null,
        seriesNumber = null,
        edition = null,
        manufacturerCode = null,
        packagingType = null,
        archivedAt = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun photo(
        id: String,
        exemplarId: String,
        angle: String,
        path: String,
        primary: Boolean,
    ) = PhotoEntity(
        id = id,
        exemplarId = exemplarId,
        angle = angle,
        path = path,
        checksum = "checksum-$id",
        isPrimary = primary,
        sortOrder = 0,
        width = 800,
        height = 800,
        bytes = 100_000,
        capturedAt = null,
    )

    private fun exemplar(
        id: String,
        variantId: String,
        internalCode: String,
        condition: String,
        status: String,
        locationId: String?,
    ) = ExemplarEntity(
        id = id,
        variantId = variantId,
        internalCode = internalCode,
        condition = condition,
        packagingCondition = null,
        locationId = locationId,
        status = status,
        notes = null,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
