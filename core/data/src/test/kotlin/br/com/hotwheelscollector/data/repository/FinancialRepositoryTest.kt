package br.com.hotwheelscollector.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.AuditLogWriter
import br.com.hotwheelscollector.data.db.TransactionRunner
import br.com.hotwheelscollector.data.db.entity.CarModelEntity
import br.com.hotwheelscollector.data.db.entity.ManufacturerEntity
import br.com.hotwheelscollector.data.db.entity.VariantEntity
import br.com.hotwheelscollector.domain.model.Acquisition
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.IsoDate
import br.com.hotwheelscollector.domain.model.MoneyCents
import br.com.hotwheelscollector.domain.model.Valuation
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
 * Aquisição e avaliação do exemplar (FR-074–FR-086), no banco real.
 *
 * O que se prova aqui: `save` é upsert por exemplar (não cria uma segunda aquisição), o
 * total investido ignora exemplar arquivado, e a avaliação mais recente é o valor atual
 * sem apagar o histórico.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class FinancialRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var exemplarRepository: ExemplarRepositoryImpl
    private lateinit var acquisitionRepository: AcquisitionRepositoryImpl
    private lateinit var valuationRepository: ValuationRepositoryImpl

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
        valuationRepository = ValuationRepositoryImpl(database, audit, fixedClock)
    }

    @After
    public fun tearDown() {
        database.close()
    }

    @Test
    public fun `save cria aquisicao e a segunda gravacao atualiza a mesma`(): Unit = runBlocking {
        val exemplarId = addExemplar()

        val first = acquisitionRepository.save(
            acquisition(
                exemplarId = exemplarId,
                id = "a1",
                price = 5900,
                shipping = 1200,
                discount = 200,
            ),
        ).getOrThrow()

        assertThat(acquisitionRepository.findByExemplar(exemplarId)).isNotNull()
        assertThat(first.totalCost()).isEqualTo(MoneyCents(6900))

        val second = acquisitionRepository.save(
            acquisition(exemplarId = exemplarId, id = "outro-id", price = 1000),
        ).getOrThrow()

        // O id e a criação pertencem ao registro original; a tela não inventa outra compra.
        assertThat(second.id).isEqualTo("a1")
        assertThat(second.price).isEqualTo(MoneyCents(1000))
        assertThat(acquisitionRepository.findByExemplar(exemplarId)?.price)
            .isEqualTo(MoneyCents(1000))
        assertThat(acquisitionRepository.observeTotalInvested().first()).isEqualTo(1000L)
    }

    @Test
    public fun `total investido ignora exemplar arquivado`(): Unit = runBlocking {
        val keptId = addExemplar()
        val archivedId = addExemplar()
        acquisitionRepository.save(acquisition(keptId, "a1", price = 5000)).getOrThrow()
        acquisitionRepository.save(acquisition(archivedId, "a2", price = 3000)).getOrThrow()

        exemplarRepository.archive(archivedId).getOrThrow()

        assertThat(acquisitionRepository.observeTotalInvested().first()).isEqualTo(5000L)
    }

    @Test
    public fun `avaliacao mais recente e o valor atual sem apagar o historico`(): Unit = runBlocking {
        val exemplarId = addExemplar()

        valuationRepository.add(
            valuation(exemplarId, date = LocalDate.of(2026, 11, 1), value = 10000),
        ).getOrThrow()
        valuationRepository.add(
            valuation(exemplarId, date = LocalDate.of(2026, 11, 10), value = 12000),
        ).getOrThrow()

        val history = valuationRepository.observeByExemplar(exemplarId).first()

        assertThat(history).hasSize(2)
        assertThat(history.first().date).isEqualTo(IsoDate(LocalDate.of(2026, 11, 10)))
        assertThat(history.first().value).isEqualTo(MoneyCents(12000))
        assertThat(valuationRepository.observeCurrentEstimated().first()).isEqualTo(12000L)
    }

    private suspend fun addExemplar(): String {
        if (database.manufacturerDao().findById("m1") == null) seedCatalog()
        return exemplarRepository.add(
            NewExemplar(variantId = VARIANT_ID, condition = Condition.SEALED),
        ).getOrThrow().exemplar.id
    }

    private fun acquisition(
        exemplarId: String,
        id: String,
        price: Long,
        shipping: Long = 0,
        discount: Long = 0,
    ): Acquisition {
        val now = Instant.now(fixedClock)
        return Acquisition(
            id = id,
            exemplarId = exemplarId,
            date = IsoDate(LocalDate.of(2026, 11, 2)),
            price = MoneyCents(price),
            shipping = if (shipping == 0L) null else MoneyCents(shipping),
            discount = if (discount == 0L) null else MoneyCents(discount),
            store = "Loja do shopping",
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun valuation(exemplarId: String, date: LocalDate, value: Long): Valuation = Valuation(
        id = "val-$value",
        exemplarId = exemplarId,
        date = IsoDate(date),
        value = MoneyCents(value),
        source = "Colecionador",
        createdAt = Instant.now(fixedClock),
    )

    private suspend fun seedCatalog() {
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
                id = VARIANT_ID,
                modelId = "model1",
                fingerprint = "fp-twin-mill-red",
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
    }

    private companion object {
        const val VARIANT_ID = "variant1"
    }
}
