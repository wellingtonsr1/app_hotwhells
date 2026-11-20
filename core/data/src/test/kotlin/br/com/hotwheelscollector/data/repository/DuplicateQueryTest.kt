package br.com.hotwheelscollector.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.TransactionRunner
import br.com.hotwheelscollector.data.db.entity.CarModelEntity
import br.com.hotwheelscollector.data.db.entity.ExemplarEntity
import br.com.hotwheelscollector.data.db.entity.ManufacturerEntity
import br.com.hotwheelscollector.data.db.entity.VariantEntity
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.DuplicateAvailability
import br.com.hotwheelscollector.domain.model.MoneyCents
import br.com.hotwheelscollector.domain.model.NegotiationType
import br.com.hotwheelscollector.domain.repository.NewNegotiation
import com.google.common.truth.Truth.assertThat
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

/**
 * T080 — duplicados agrupados por `variant_id` (FR-065, SC-005).
 *
 * O caso que dá nome ao teste: três exemplares da variante A e um da variante B do mesmo
 * modelo. Só os três da variante A são duplicatas; a variante B não entra na lista.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class DuplicateQueryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: DuplicateRepositoryImpl
    private lateinit var negotiations: NegotiationRepositoryImpl

    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-05-01T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @Before
    public fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.CALLBACK)
            .allowMainThreadQueries()
            .build()
        val transactionRunner = TransactionRunner(database)
        repository = DuplicateRepositoryImpl(database, transactionRunner, clock)
        negotiations = NegotiationRepositoryImpl(
            database = database,
            transactionRunner = transactionRunner,
            auditLogWriter = br.com.hotwheelscollector.data.db.AuditLogWriter(database.historyDao()),
            clock = clock,
        )
    }

    @After
    public fun tearDown() {
        database.close()
    }

    @Test
    public fun `variantes distintas do mesmo modelo nao viram um grupo so`(): Unit = runBlocking {
        seed()

        val groups = repository.observeGroups().first()

        assertThat(groups).hasSize(1)
        assertThat(groups.single().variantId).isEqualTo("variant-a")
        assertThat(groups.single().count).isEqualTo(3)
        assertThat(groups.single().exemplars.map { it.exemplar.id })
            .containsExactly("e1", "e2", "e3")
    }

    @Test
    public fun `grupo carrega a disponibilidade de cada exemplar`(): Unit = runBlocking {
        seed()

        repository.setOffer(
            exemplarId = "e2",
            availability = DuplicateAvailability.BOTH,
            condition = Condition.LOOSE,
            askingPrice = MoneyCents(4_500),
            notes = "Aceito troca por Ferrari",
        )

        val group = repository.observeGroups().first().single()
        val offered = group.exemplars.single { it.exemplar.id == "e2" }
        assertThat(offered.offer).isNotNull()
        assertThat(offered.offer?.availability).isEqualTo(DuplicateAvailability.BOTH)
        assertThat(offered.offer?.condition).isEqualTo(Condition.LOOSE)
        assertThat(offered.offer?.askingPrice).isEqualTo(MoneyCents(4_500))
        assertThat(group.exemplars.single { it.exemplar.id == "e1" }.offer).isNull()
    }

    @Test
    public fun `marcar de novo atualiza a mesma oferta`(): Unit = runBlocking {
        seed()

        repository.setOffer("e1", DuplicateAvailability.SALE, null, null, null)
        repository.setOffer(
            "e1",
            DuplicateAvailability.UNAVAILABLE,
            Condition.SEALED,
            MoneyCents(9_900),
            "Reservado para a prateleira",
        )

        val offer = repository.offerFor("e1")!!
        assertThat(offer.availability).isEqualTo(DuplicateAvailability.UNAVAILABLE)
        assertThat(offer.askingPrice).isEqualTo(MoneyCents(9_900))
        assertThat(
            database.duplicateDao().findOffer("e1")?.id,
        ).isEqualTo(offer.id)
    }

    @Test
    public fun `grupo mostra a negociacao ativa do exemplar`(): Unit = runBlocking {
        seed()

        negotiations.open(
            NewNegotiation(
                exemplarId = "e3",
                type = NegotiationType.SALE,
                counterparty = "Ana",
            ),
        ).getOrThrow()

        val group = repository.observeGroups().first().single()
        val negotiated = group.exemplars.single { it.exemplar.id == "e3" }
        assertThat(negotiated.activeNegotiation).isNotNull()
        assertThat(negotiated.activeNegotiation?.counterparty).isEqualTo("Ana")
        assertThat(group.exemplars.single { it.exemplar.id == "e1" }.activeNegotiation).isNull()
    }

    private suspend fun seed() {
        val t = 0L
        database.manufacturerDao().insert(ManufacturerEntity("m1", "Mattel", null, null, t, t))
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
                createdAt = t,
                updatedAt = t,
            ),
        )
        listOf("variant-a" to "Vermelho", "variant-b" to "Azul").forEach { (id, _) ->
            database.variantDao().insert(
                VariantEntity(
                    id = id,
                    modelId = "model1",
                    fingerprint = "fp-$id",
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
                    createdAt = t,
                    updatedAt = t,
                ),
            )
        }
        listOf(
            "e1" to "variant-a",
            "e2" to "variant-a",
            "e3" to "variant-a",
            "e4" to "variant-b",
        ).forEachIndexed { index, (id, variantId) ->
            database.exemplarDao().insert(
                ExemplarEntity(
                    id = id,
                    variantId = variantId,
                    internalCode = "HW-%04d".format(index + 1),
                    condition = Condition.LOOSE.name,
                    packagingCondition = null,
                    locationId = null,
                    status = "OWNED",
                    notes = null,
                    rowVersion = 0L,
                    createdAt = index.toLong(),
                    updatedAt = index.toLong(),
                ),
            )
        }
    }
}
