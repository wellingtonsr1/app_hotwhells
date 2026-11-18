package br.com.hotwheelscollector.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.AuditLogWriter
import br.com.hotwheelscollector.data.db.TransactionRunner
import br.com.hotwheelscollector.data.db.entity.CarModelEntity
import br.com.hotwheelscollector.data.db.entity.LocationEntity
import br.com.hotwheelscollector.data.db.entity.ManufacturerEntity
import br.com.hotwheelscollector.data.db.entity.VariantEntity
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.ExemplarStatus
import br.com.hotwheelscollector.domain.model.IsoDate
import br.com.hotwheelscollector.domain.model.LocationType
import br.com.hotwheelscollector.domain.model.MoneyCents
import br.com.hotwheelscollector.domain.model.PackagingCondition
import br.com.hotwheelscollector.domain.repository.NewAcquisition
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
 * Integração do cadastro de exemplar (T030).
 *
 * Verifica a regra 1 de `data-model.md` no banco de verdade: exemplar, aquisição,
 * movimentação inicial e trilha de auditoria nascem juntos. Também confere que uma
 * falha no meio não deixa metade do cadastro gravada.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class ExemplarRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ExemplarRepositoryImpl

    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC)

    @Before
    public fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.CALLBACK)
            .allowMainThreadQueries()
            .build()
        repository = ExemplarRepositoryImpl(
            database = database,
            transactionRunner = TransactionRunner(database),
            auditLogWriter = AuditLogWriter(database.historyDao()),
            clock = fixedClock,
        )
    }

    @After
    public fun tearDown() {
        database.close()
    }

    @Test
    public fun `cadastro grava exemplar aquisicao movimentacao e auditoria juntos`(): Unit = runBlocking {
        seedCatalog()

        val result = repository.add(
            NewExemplar(
                variantId = VARIANT_ID,
                condition = Condition.SEALED,
                locationId = LOCATION_ID,
                notes = "Caixa com marca de estoque",
                acquisition = NewAcquisition(
                    date = IsoDate(LocalDate.of(2026, 11, 10)),
                    price = MoneyCents(5900),
                    shipping = MoneyCents(1200),
                ),
            ),
        )

        assertThat(result.isSuccess).isTrue()
        val outcome = result.getOrThrow()
        assertThat(outcome.sameVariantCount).isEqualTo(1)
        assertThat(outcome.exemplar.internalCode).isEqualTo("HW-000001")

        val exemplarId = outcome.exemplar.id
        assertThat(database.exemplarDao().findById(exemplarId)).isNotNull()
        assertThat(database.acquisitionDao().findByExemplar(exemplarId)?.priceCents).isEqualTo(5900L)

        val movements = database.movementDao().observeByExemplar(exemplarId).first()
        assertThat(movements).hasSize(1)
        assertThat(movements.first().toLocationId).isEqualTo(LOCATION_ID)

        val history = database.historyDao().observeFor("exemplar", exemplarId).first()
        assertThat(history.map { it.field }).contains("created")
    }

    @Test
    public fun `segundo exemplar da variante conta dois`(): Unit = runBlocking {
        seedCatalog()

        repository.add(NewExemplar(variantId = VARIANT_ID, condition = Condition.SEALED))
        val second = repository.add(NewExemplar(variantId = VARIANT_ID, condition = Condition.LOOSE))

        assertThat(second.getOrThrow().sameVariantCount).isEqualTo(2)
        assertThat(second.getOrThrow().exemplar.internalCode).isEqualTo("HW-000002")
    }

    @Test
    public fun `exemplar de variante inexistente falha sem gravar nada`(): Unit = runBlocking {
        val before = database.exemplarDao().countAll()

        val result = repository.add(
            NewExemplar(variantId = "variante-inexistente", condition = Condition.SEALED),
        )

        assertThat(result.isFailure).isTrue()
        assertThat(database.exemplarDao().countAll()).isEqualTo(before)
        assertThat(database.historyDao().observeFor("exemplar", "qualquer").first()).isEmpty()
    }

    @Test
    public fun `mover exemplar grava localizacao e movimentacao`(): Unit = runBlocking {
        seedCatalog()
        val outcome = repository.add(
            NewExemplar(variantId = VARIANT_ID, condition = Condition.SEALED),
        ).getOrThrow()

        val moved = repository.move(outcome.exemplar.id, LOCATION_ID, "Guardado na estante")

        assertThat(moved.isSuccess).isTrue()
        assertThat(database.exemplarDao().findById(outcome.exemplar.id)?.locationId)
            .isEqualTo(LOCATION_ID)
        val movements = database.movementDao().observeByExemplar(outcome.exemplar.id).first()
        assertThat(movements).hasSize(1)
        assertThat(movements.first().note).isEqualTo("Guardado na estante")
    }

    @Test
    public fun `escrita com versao desatualizada e recusada`(): Unit = runBlocking {
        seedCatalog()
        val outcome = repository.add(
            NewExemplar(variantId = VARIANT_ID, condition = Condition.SEALED),
        ).getOrThrow()

        val stale = repository.updateDetails(
            id = outcome.exemplar.id,
            condition = Condition.LOOSE,
            packagingCondition = null,
            notes = null,
            expectedRowVersion = 99L,
        )

        assertThat(stale.isFailure).isTrue()
        assertThat(stale.exceptionOrNull()).isEqualTo(DomainError.StaleWrite)
    }

    @Test
    public fun `editar detalhes grava condicao embalagem e observacoes com auditoria`(): Unit = runBlocking {
        seedCatalog()
        val outcome = repository.add(
            NewExemplar(variantId = VARIANT_ID, condition = Condition.SEALED),
        ).getOrThrow()

        val result = repository.updateDetails(
            id = outcome.exemplar.id,
            condition = Condition.LOOSE,
            packagingCondition = PackagingCondition.OPENED,
            notes = "Solto, sem caixa",
            expectedRowVersion = outcome.exemplar.rowVersion,
        )

        assertThat(result.isSuccess).isTrue()
        val updated = result.getOrThrow()
        assertThat(updated.condition).isEqualTo(Condition.LOOSE)
        assertThat(updated.packagingCondition).isEqualTo(PackagingCondition.OPENED)
        assertThat(updated.notes).isEqualTo("Solto, sem caixa")
        assertThat(updated.rowVersion).isEqualTo(outcome.exemplar.rowVersion + 1)

        val audit = database.historyDao().observeFor("exemplar", updated.id).first().map { it.field }
        assertThat(audit).containsAtLeast("condition", "packaging_condition", "notes")
    }

    @Test
    public fun `arquivar retira do acervo e preserva vinculos e historico`(): Unit = runBlocking {
        seedCatalog()
        val outcome = repository.add(
            NewExemplar(
                variantId = VARIANT_ID,
                condition = Condition.SEALED,
                locationId = LOCATION_ID,
                acquisition = NewAcquisition(
                    date = IsoDate(LocalDate.of(2026, 11, 10)),
                    price = MoneyCents(5900),
                ),
            ),
        ).getOrThrow()
        val exemplarId = outcome.exemplar.id

        val archived = repository.archive(exemplarId)

        assertThat(archived.isSuccess).isTrue()
        assertThat(database.exemplarDao().findById(exemplarId)?.status)
            .isEqualTo(ExemplarStatus.ARCHIVED.name)
        // Nada é apagado: aquisição, movimentação e auditoria continuam no banco (Art. I.7).
        assertThat(database.acquisitionDao().findByExemplar(exemplarId)).isNotNull()
        assertThat(database.movementDao().observeByExemplar(exemplarId).first()).hasSize(1)
        assertThat(database.historyDao().observeFor("exemplar", exemplarId).first().map { it.field })
            .contains("status")

        // Some do acervo e das contagens (FR-044).
        assertThat(repository.observeOwned().first()).isEmpty()
        assertThat(repository.countByVariant(VARIANT_ID)).isEqualTo(0)
        assertThat(database.variantDao().observeSummaries().first().first().exemplarCount).isEqualTo(0)
    }

    @Test
    public fun `exclusao e bloqueada quando existem vinculos`(): Unit = runBlocking {
        seedCatalog()
        val outcome = repository.add(
            NewExemplar(
                variantId = VARIANT_ID,
                condition = Condition.SEALED,
                acquisition = NewAcquisition(
                    date = IsoDate(LocalDate.of(2026, 11, 10)),
                    price = MoneyCents(5900),
                ),
            ),
        ).getOrThrow()

        val dependents = repository.dependents(outcome.exemplar.id)
        val deleted = repository.delete(outcome.exemplar.id)

        assertThat(dependents.hasAny).isTrue()
        assertThat(dependents.acquisitions).isEqualTo(1)
        assertThat(deleted.isFailure).isTrue()
        assertThat(deleted.exceptionOrNull()).isInstanceOf(DomainError.Blocked::class.java)
        assertThat((deleted.exceptionOrNull() as DomainError.Blocked).reason)
            .contains("aquisição")
        assertThat(database.exemplarDao().findById(outcome.exemplar.id)).isNotNull()
    }

    @Test
    public fun `exclusao sem vinculos remove o exemplar`(): Unit = runBlocking {
        seedCatalog()
        val outcome = repository.add(
            NewExemplar(variantId = VARIANT_ID, condition = Condition.SEALED),
        ).getOrThrow()

        assertThat(repository.dependents(outcome.exemplar.id).hasAny).isFalse()
        val deleted = repository.delete(outcome.exemplar.id)

        assertThat(deleted.isSuccess).isTrue()
        assertThat(database.exemplarDao().findById(outcome.exemplar.id)).isNull()
        assertThat(database.exemplarDao().countAll()).isEqualTo(0)
    }

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
        database.locationDao().insert(
            LocationEntity(
                id = LOCATION_ID,
                type = LocationType.RACK.name,
                name = "Estante da sala",
                parentId = null,
                archivedAt = null,
                createdAt = millis,
                updatedAt = millis,
            ),
        )
    }

    private companion object {
        const val VARIANT_ID = "variant1"
        const val LOCATION_ID = "loc1"
    }
}
