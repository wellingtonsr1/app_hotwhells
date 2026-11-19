package br.com.hotwheelscollector.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.AuditLogWriter
import br.com.hotwheelscollector.data.db.TransactionRunner
import br.com.hotwheelscollector.data.db.entity.CarModelEntity
import br.com.hotwheelscollector.data.db.entity.ExemplarEntity
import br.com.hotwheelscollector.data.db.entity.ManufacturerEntity
import br.com.hotwheelscollector.data.db.entity.VariantEntity
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.model.ExemplarStatus
import br.com.hotwheelscollector.domain.model.LocationArchivePolicy
import br.com.hotwheelscollector.domain.model.LocationType
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
 * Localizações e inventário no banco real (FR-088–FR-095).
 *
 * O seed tem hierarquia de dois níveis e exemplar na prateleira: o inventário da estante
 * precisa trazer o que está na prateleira, e a exclusão da estante com sublocalização ativa
 * precisa ser recusada em vez de cascatear.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class LocationRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: LocationRepositoryImpl
    private val clock = Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneOffset.UTC)

    @Before
    public fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.CALLBACK)
            .allowMainThreadQueries()
            .build()
        repository = LocationRepositoryImpl(
            database = database,
            transactionRunner = TransactionRunner(database),
            auditLogWriter = AuditLogWriter(database.historyDao()),
            clock = clock,
        )
    }

    @After
    public fun tearDown() {
        database.close()
    }

    @Test
    public fun `arvore mostra hierarquia e conta exemplares por nivel`(): Unit = runBlocking {
        seed()

        val tree = repository.observeTree().first()

        assertThat(tree).hasSize(1)
        val rack = tree.single()
        assertThat(rack.location.name).isEqualTo("Estante A")
        assertThat(rack.exemplarCount).isEqualTo(0)
        assertThat(rack.children.single().location.name).isEqualTo("Prateleira 3")
        assertThat(rack.children.single().exemplarCount).isEqualTo(1)
        assertThat(rack.subtreeExemplarCount).isEqualTo(1)
    }

    @Test
    public fun `inventario da localizacao inclui sublocalizacoes`(): Unit = runBlocking {
        seed()

        val contents = repository.observeContents("rack").first()

        assertThat(contents).hasSize(1)
        assertThat(contents.single().exemplarId).isEqualTo("e1")
        assertThat(contents.single().title).isEqualTo("Mattel · Twin Mill")
        assertThat(contents.single().locationName).isEqualTo("Prateleira 3")
        assertThat(contents.single().condition.name).isEqualTo("SEALED")
    }

    @Test
    public fun `excluir com sublocalizacao ativa e bloqueado`(): Unit = runBlocking {
        seed()

        val result = repository.archive("rack", LocationArchivePolicy.None)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(DomainError.Blocked::class.java)
        assertThat(repository.findById("rack")).isNotNull()
    }

    @Test
    public fun `excluir com exemplares e sem politica e bloqueado`(): Unit = runBlocking {
        seed()

        val result = repository.archive("shelf", LocationArchivePolicy.None)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("1 exemplar")
        assertThat(repository.findById("shelf")).isNotNull()
    }

    @Test
    public fun `mover exemplares na exclusao muda a localizacao e registra movimentacao`(): Unit = runBlocking {
        seed()
        val box = repository.add(LocationType.CRATE, "Caixa 12").getOrThrow()

        val handled = repository.archive(
            id = "shelf",
            policy = LocationArchivePolicy.MoveTo(box.id),
        ).getOrThrow()

        assertThat(handled).isEqualTo(1)
        assertThat(database.exemplarDao().findById("e1")?.locationId).isEqualTo(box.id)
        assertThat(database.movementDao().observeByExemplar("e1").first()).hasSize(1)
        // A linha continua no banco para preservar histórico; arquivada é o que a tira da árvore.
        assertThat(repository.findById("shelf")?.archivedAt).isNotNull()
        assertThat(repository.observeTree().first().flatMap { it.children }.map { it.id })
            .doesNotContain("shelf")
        assertThat(repository.observeContents(box.id).first()).hasSize(1)
    }

    @Test
    public fun `arquivar exemplares na exclusao tira do acervo e nao apaga`(): Unit = runBlocking {
        seed()

        repository.archive("shelf", LocationArchivePolicy.ArchiveExemplars).getOrThrow()

        val exemplar = database.exemplarDao().findById("e1")
        assertThat(exemplar?.status).isEqualTo(ExemplarStatus.ARCHIVED.name)
        assertThat(repository.observeContents("rack").first()).isEmpty()
        assertThat(database.movementDao().observeByExemplar("e1").first()).hasSize(1)
    }

    @Test
    public fun `exemplar arquivado nao entra na conta nem e movimentado`(): Unit = runBlocking {
        seed()
        // Já arquivado: continua no banco e aponta para a prateleira, mas não é acervo.
        insertExemplar("e2", "shelf", ExemplarStatus.ARCHIVED, "HW-2")
        val box = repository.add(LocationType.CRATE, "Caixa 12").getOrThrow()

        val handled = repository.archive(
            id = "shelf",
            policy = LocationArchivePolicy.MoveTo(box.id),
        ).getOrThrow()

        assertThat(handled).isEqualTo(1)
        assertThat(database.exemplarDao().findById("e1")?.locationId).isEqualTo(box.id)
        // O arquivado fica onde estava: movimentar histórico não é o que a exclusão pediu.
        assertThat(database.exemplarDao().findById("e2")?.locationId).isEqualTo("shelf")
        assertThat(database.movementDao().observeByExemplar("e2").first()).isEmpty()
    }

    @Test
    public fun `exemplar vendido nao bloqueia exclusao sem politica`(): Unit = runBlocking {
        seed()
        val display = repository.add(LocationType.DISPLAY, "Vitrine").getOrThrow()
        insertExemplar("e3", display.id, ExemplarStatus.SOLD, "HW-3")

        val handled = repository.archive(display.id, LocationArchivePolicy.None).getOrThrow()

        assertThat(handled).isEqualTo(0)
        assertThat(repository.findById(display.id)?.archivedAt).isNotNull()
        // Vendido é histórico: fica apontando para a localização, que só sai da árvore.
        assertThat(database.exemplarDao().findById("e3")?.locationId).isEqualTo(display.id)
    }

    @Test
    public fun `localizacao vazia pode ser excluida sem politica`(): Unit = runBlocking {
        val box = repository.add(LocationType.CRATE, "Caixa vazia").getOrThrow()

        val handled = repository.archive(box.id, LocationArchivePolicy.None).getOrThrow()

        assertThat(handled).isEqualTo(0)
        assertThat(repository.findById(box.id)?.archivedAt).isNotNull()
        assertThat(repository.observeTree().first()).isEmpty()
    }

    /** Estante A > Prateleira 3, com um exemplar na prateleira. */
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
                id = "v1",
                modelId = "model1",
                fingerprint = "fp-v1",
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
                updatedAt = t,
            ),
        )
        database.locationDao().insert(
            br.com.hotwheelscollector.data.db.entity.LocationEntity(
                id = "rack",
                type = LocationType.RACK.name,
                name = "Estante A",
                parentId = null,
                archivedAt = null,
                createdAt = t,
                updatedAt = t,
            ),
        )
        database.locationDao().insert(
            br.com.hotwheelscollector.data.db.entity.LocationEntity(
                id = "shelf",
                type = LocationType.SHELF.name,
                name = "Prateleira 3",
                parentId = "rack",
                archivedAt = null,
                createdAt = t,
                updatedAt = t,
            ),
        )
        insertExemplar("e1", "shelf", ExemplarStatus.OWNED, "HW-1")
    }

    private suspend fun insertExemplar(
        id: String,
        locationId: String,
        status: ExemplarStatus,
        code: String,
    ) {
        database.exemplarDao().insert(
            ExemplarEntity(
                id = id,
                variantId = "v1",
                internalCode = code,
                condition = "SEALED",
                packagingCondition = null,
                locationId = locationId,
                status = status.name,
                notes = null,
                createdAt = 0L,
                updatedAt = 0L,
            ),
        )
    }
}
