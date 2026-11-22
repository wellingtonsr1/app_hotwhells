package br.com.diecastcollector.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.AuditLogWriter
import br.com.diecastcollector.data.db.TransactionRunner
import br.com.diecastcollector.data.db.entity.CarModelEntity
import br.com.diecastcollector.data.db.entity.ExemplarEntity
import br.com.diecastcollector.data.db.entity.ManufacturerEntity
import br.com.diecastcollector.data.db.entity.VariantEntity
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.domain.model.IsoDate
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.model.NegotiationStatus
import br.com.diecastcollector.domain.model.NegotiationType
import br.com.diecastcollector.domain.repository.NewNegotiation
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
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
 * T081 — uma negociação ativa por exemplar (FR-068), conclusão (FR-070–FR-071) e
 * cancelamento (FR-072), com histórico preservado (FR-073).
 *
 * A unicidade é transacional: o schema não tem o índice parcial único de
 * `OPEN`/`RESERVED`, então o teste é a garantia de que o repositório a cumpre.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class NegotiationRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: NegotiationRepositoryImpl

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
        repository = NegotiationRepositoryImpl(
            database = database,
            transactionRunner = transactionRunner,
            auditLogWriter = AuditLogWriter(database.historyDao()),
            clock = clock,
        )
    }

    @After
    public fun tearDown() {
        database.close()
    }

    @Test
    public fun `segunda negociacao ativa e recusada sem tocar no exemplar`(): Unit = runBlocking {
        seed()

        repository.open(NewNegotiation("e1", NegotiationType.SALE)).getOrThrow()
        val second = repository.open(NewNegotiation("e1", NegotiationType.TRADE))

        assertThat(second.isFailure).isTrue()
        assertThat(second.exceptionOrNull()?.message).contains("negociação ativa")
        assertThat(repository.observeByExemplar("e1").first()).hasSize(1)
        assertThat(statusOf("e1")).isEqualTo(ExemplarStatus.OWNED)
    }

    @Test
    public fun `cancelar libera o exemplar para nova negociacao e mantem o evento`(): Unit =
        runBlocking {
            seed()
            val first = repository.open(
                NewNegotiation("e1", NegotiationType.SALE, counterparty = "Ana"),
            ).getOrThrow()

            repository.cancel(first.id).getOrThrow()

            val cancelled = repository.observeByExemplar("e1").first().single()
            assertThat(cancelled.status).isEqualTo(NegotiationStatus.CANCELLED)
            assertThat(cancelled.closedAt).isNotNull()
            assertThat(statusOf("e1")).isEqualTo(ExemplarStatus.OWNED)
            assertThat(repository.observeEventsForExemplar("e1").first().map { it.description })
                .contains("Negociação cancelada")

            val reopened = repository.open(NewNegotiation("e1", NegotiationType.SALE))
            assertThat(reopened.isSuccess).isTrue()
            assertThat(repository.observeByExemplar("e1").first()).hasSize(2)
        }

    @Test
    public fun `venda concluida grava valor data e evento e marca o exemplar`(): Unit =
        runBlocking {
            seed()
            val negotiation = repository.open(
                NewNegotiation(
                    "e1",
                    NegotiationType.SALE,
                    counterparty = "Ana",
                    channel = "Feira",
                ),
            ).getOrThrow()

            repository.closeSale(
                negotiationId = negotiation.id,
                amount = MoneyCents(4_500),
                date = IsoDate.parseIsoOrNull("2026-05-02"),
            ).getOrThrow()

            val closed = repository.observeByExemplar("e1").first().single()
            val expectedMillis = IsoDate.parseIsoOrNull("2026-05-02")!!.value
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            assertThat(closed.status).isEqualTo(NegotiationStatus.CLOSED)
            assertThat(closed.closedAt?.toEpochMilli()).isEqualTo(expectedMillis)
            assertThat(closed.amount).isEqualTo(MoneyCents(4_500))
            assertThat(closed.counterparty).isEqualTo("Ana")
            assertThat(statusOf("e1")).isEqualTo(ExemplarStatus.SOLD)

            val events = repository.observeEventsForExemplar("e1").first()
            assertThat(events.map { it.description }).contains("Venda concluída")
            assertThat(events.mapNotNull { it.amount }).contains(MoneyCents(4_500))
        }

    @Test
    public fun `troca concluida registra a saida e o exemplar recebido`(): Unit = runBlocking {
        seed()
        val negotiation = repository.open(
            NewNegotiation("e1", NegotiationType.TRADE, counterparty = "Bruno"),
        ).getOrThrow()

        repository.closeTrade(
            negotiationId = negotiation.id,
            receivedExemplarId = "e2",
            note = "Camaro azul",
        ).getOrThrow()

        val closed = repository.observeByExemplar("e1").first().single()
        assertThat(closed.status).isEqualTo(NegotiationStatus.CLOSED)
        assertThat(closed.receivedExemplarId).isEqualTo("e2")
        assertThat(statusOf("e1")).isEqualTo(ExemplarStatus.TRADED)
        assertThat(statusOf("e2")).isEqualTo(ExemplarStatus.OWNED)
        assertThat(repository.observeEventsForExemplar("e1").first().map { it.description })
            .contains("Troca concluída · recebido HW-0002 · Camaro azul")
    }

    @Test
    public fun `conclusao recusa estado ja fechado e tipo trocado`(): Unit = runBlocking {
        seed()
        val sale = repository.open(NewNegotiation("e1", NegotiationType.SALE)).getOrThrow()

        val wrongType = repository.closeTrade(sale.id, null, null)
        assertThat(wrongType.isFailure).isTrue()
        assertThat(wrongType.exceptionOrNull()?.message).contains("não uma troca")

        repository.closeSale(sale.id, MoneyCents(1_000), null).getOrThrow()
        val twice = repository.closeSale(sale.id, MoneyCents(2_000), null)
        assertThat(twice.isFailure).isTrue()
        assertThat(twice.exceptionOrNull()?.message).contains("já foi concluída")
    }

    @Test
    public fun `troca recusa receber o proprio exemplar negociado`(): Unit = runBlocking {
        seed()
        val negotiation = repository.open(
            NewNegotiation("e1", NegotiationType.TRADE),
        ).getOrThrow()

        val result = repository.closeTrade(negotiation.id, "e1", null)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("diferente do negociado")
        assertThat(statusOf("e1")).isEqualTo(ExemplarStatus.OWNED)
    }

    private suspend fun statusOf(exemplarId: String): ExemplarStatus =
        ExemplarStatus.valueOf(database.exemplarDao().findById(exemplarId)!!.status)

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
        database.variantDao().insert(
            VariantEntity(
                id = "variant1",
                modelId = "model1",
                fingerprint = "fp-variant1",
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
        listOf("e1", "e2").forEachIndexed { index, id ->
            database.exemplarDao().insert(
                ExemplarEntity(
                    id = id,
                    variantId = "variant1",
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
