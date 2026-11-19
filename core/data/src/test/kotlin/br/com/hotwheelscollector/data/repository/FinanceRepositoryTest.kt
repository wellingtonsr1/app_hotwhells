package br.com.hotwheelscollector.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.AuditLogWriter
import br.com.hotwheelscollector.data.db.TransactionRunner
import br.com.hotwheelscollector.data.db.entity.CarModelEntity
import br.com.hotwheelscollector.data.db.entity.CollectionEntity
import br.com.hotwheelscollector.data.db.entity.CollectionItemEntity
import br.com.hotwheelscollector.data.db.entity.ManufacturerEntity
import br.com.hotwheelscollector.data.db.entity.SeriesEntity
import br.com.hotwheelscollector.data.db.entity.VariantEntity
import br.com.hotwheelscollector.domain.model.Acquisition
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.FinancePeriod
import br.com.hotwheelscollector.domain.model.IsoDate
import br.com.hotwheelscollector.domain.model.MoneyCents
import br.com.hotwheelscollector.domain.repository.NewExemplar
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Agregações financeiras (FR-077–FR-081) no banco real.
 *
 * Prova o que só a consulta pode errar: custo como `preço + frete − desconto`, exemplar
 * sem preço fora do investido e da média mas contado como excluído, agrupamentos por
 * fabricante/série/coleção e o filtro de período.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class FinanceRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var financeRepository: FinanceRepositoryImpl
    private lateinit var acquisitionRepository: AcquisitionRepositoryImpl
    private lateinit var exemplarRepository: ExemplarRepositoryImpl

    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC)

    @Before
    public fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.CALLBACK)
            .allowMainThreadQueries()
            .build()
        val audit = AuditLogWriter(database.historyDao())
        val transactions = TransactionRunner(database)
        exemplarRepository = ExemplarRepositoryImpl(database, transactions, audit, fixedClock)
        acquisitionRepository = AcquisitionRepositoryImpl(database, transactions, audit, fixedClock)
        financeRepository = FinanceRepositoryImpl(database)
    }

    @After
    public fun tearDown() {
        database.close()
    }

    @Test
    public fun `resumo soma custo real e calcula media so com exemplares avaliados em preco`(): Unit =
        runBlocking {
            seedScenario()

            val summary = financeRepository.observeSummary().first()

            // 10.000 (10.500 + 200 − 700) + 30.000; o exemplar sem preço não entra.
            assertThat(summary.investedCents).isEqualTo(40_000L)
            assertThat(summary.ownedExemplars).isEqualTo(3)
            assertThat(summary.exemplarsWithValue).isEqualTo(2)
            assertThat(summary.exemplarsWithoutValue).isEqualTo(1)
            assertThat(summary.averageCostCents).isEqualTo(20_000L)
            // Só o exemplar 1 tem avaliação; a mais recente vence.
            assertThat(summary.estimatedCents).isEqualTo(15_000L)
            assertThat(summary.exemplarsWithoutValuation).isEqualTo(2)
            assertThat(summary.latestValuationDate).isEqualTo("2026-11-15")
            assertThat(summary.differenceCents).isEqualTo(-25_000L)
        }

    @Test
    public fun `agrupa gastos por fabricante serie e colecao`(): Unit = runBlocking {
        seedScenario()

        val byManufacturer = financeRepository.observeSpendingByManufacturer().first()
        val bySeries = financeRepository.observeSpendingBySeries().first()
        val byCollection = financeRepository.observeSpendingByCollection().first()

        assertThat(byManufacturer).containsExactly(
            br.com.hotwheelscollector.domain.model.SpendingSlice("Mattel", 40_000L),
        )
        assertThat(bySeries).containsExactly(
            br.com.hotwheelscollector.domain.model.SpendingSlice("Car Culture", 40_000L),
        )
        assertThat(byCollection).containsExactly(
            br.com.hotwheelscollector.domain.model.SpendingSlice("Coleção A", 40_000L),
        )
    }

    @Test
    public fun `gastos por mes respeitam intervalo livre`(): Unit = runBlocking {
        seedScenario()

        val all = financeRepository.observeSpendingByMonth().first()
        val november = financeRepository.observeSpendingByMonth(
            FinancePeriod(from = IsoDate(LocalDate.of(2026, 11, 1))),
        ).first()
        val october = financeRepository.observeSpendingByMonth(
            FinancePeriod(to = IsoDate(LocalDate.of(2026, 10, 31))),
        ).first()

        assertThat(all.map { it.yearMonth }).containsExactly("2026-11", "2026-10").inOrder()
        assertThat(all.map { it.cents }).containsExactly(30_000L, 10_000L).inOrder()
        assertThat(november.map { it.cents }).containsExactly(30_000L)
        assertThat(october.map { it.cents }).containsExactly(10_000L)
    }

    @Test
    public fun `exemplar arquivado sai do resumo e dos agrupamentos`(): Unit = runBlocking {
        val ids = seedScenario()

        exemplarRepository.archive(ids.second).getOrThrow()

        val summary = financeRepository.observeSummary().first()
        val byManufacturer = financeRepository.observeSpendingByManufacturer().first()

        assertThat(summary.investedCents).isEqualTo(10_000L)
        assertThat(summary.ownedExemplars).isEqualTo(2)
        assertThat(summary.exemplarsWithValue).isEqualTo(1)
        assertThat(summary.averageCostCents).isEqualTo(10_000L)
        assertThat(byManufacturer.map { it.cents }).containsExactly(10_000L)
    }

    /** Devolve os ids dos exemplares com preço, na ordem em que foram criados. */
    private suspend fun seedScenario(): Pair<String, String> {
        seedCatalog()
        val first = addExemplar()
        val second = addExemplar()
        val withoutPrice = addExemplar()

        acquisitionRepository.save(
            acquisition(
                exemplarId = first,
                id = "a1",
                date = LocalDate.of(2026, 10, 5),
                price = 10_500,
                shipping = 200,
                discount = 700,
            ),
        ).getOrThrow()
        acquisitionRepository.save(
            acquisition(
                exemplarId = second,
                id = "a2",
                date = LocalDate.of(2026, 11, 10),
                price = 30_000,
            ),
        ).getOrThrow()
        acquisitionRepository.save(
            acquisition(
                exemplarId = withoutPrice,
                id = "a3",
                date = LocalDate.of(2026, 11, 12),
                price = null,
                shipping = 900,
            ),
        ).getOrThrow()

        database.valuationDao().insert(
            br.com.hotwheelscollector.data.db.entity.ValuationEntity(
                id = "v1",
                exemplarId = first,
                date = "2026-11-01",
                valueCents = 12_000,
                currency = "BRL",
                source = "Catálogo",
                notes = null,
                createdAt = 0L,
            ),
        )
        database.valuationDao().insert(
            br.com.hotwheelscollector.data.db.entity.ValuationEntity(
                id = "v2",
                exemplarId = first,
                date = "2026-11-15",
                valueCents = 15_000,
                currency = "BRL",
                source = "Colecionador",
                notes = null,
                createdAt = 1L,
            ),
        )

        return first to second
    }

    private suspend fun addExemplar(): String =
        exemplarRepository.add(
            NewExemplar(variantId = VARIANT_ID, condition = Condition.SEALED),
        ).getOrThrow().exemplar.id

    private fun acquisition(
        exemplarId: String,
        id: String,
        date: LocalDate,
        price: Long?,
        shipping: Long = 0,
        discount: Long = 0,
    ): Acquisition {
        val now = Instant.now(fixedClock)
        return Acquisition(
            id = id,
            exemplarId = exemplarId,
            date = IsoDate(date),
            price = price?.let(::MoneyCents),
            shipping = if (shipping == 0L) null else MoneyCents(shipping),
            discount = if (discount == 0L) null else MoneyCents(discount),
            createdAt = now,
            updatedAt = now,
        )
    }

    private suspend fun seedCatalog() {
        val millis = 0L
        database.manufacturerDao().insert(
            ManufacturerEntity("m1", "Mattel", null, null, millis, millis),
        )
        database.seriesDao().insert(
            SeriesEntity(
                id = "s1",
                manufacturerId = "m1",
                name = "Car Culture",
                parentId = null,
                kind = null,
                year = 2024,
                archivedAt = null,
                createdAt = millis,
                updatedAt = millis,
            ),
        )
        database.collectionDao().insert(
            CollectionEntity(
                id = "c1",
                name = "Coleção A",
                description = null,
                parentId = null,
                manufacturerId = null,
                archivedAt = null,
                createdAt = millis,
                updatedAt = millis,
            ),
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
                id = VARIANT_ID,
                modelId = "model1",
                fingerprint = "fp-twin-mill-red",
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
                createdAt = millis,
                updatedAt = millis,
            ),
        )
        database.collectionItemDao().insert(
            CollectionItemEntity(
                id = "ci1",
                collectionId = "c1",
                variantId = VARIANT_ID,
                modelId = null,
                expected = 1,
                nonCollectible = 0,
                notes = null,
            ),
        )
    }

    private companion object {
        const val VARIANT_ID = "variant1"
    }
}
