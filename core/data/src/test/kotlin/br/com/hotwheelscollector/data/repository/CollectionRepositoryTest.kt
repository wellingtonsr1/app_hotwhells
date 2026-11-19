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
 * Coleções no banco real (FR-049–FR-056): progresso, faltantes, não colecionável,
 * hierarquia sem ciclo e recusa de item repetido.
 *
 * O seed usa exemplares `OWNED` e `SOLD`: se a consulta contar vendido, o teste acusa —
 * vender não apaga o exemplar do banco, mas tira a unidade do acervo (FR-044/FR-070).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class CollectionRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: CollectionRepositoryImpl
    private val clock = Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneOffset.UTC)

    @Before
    public fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.CALLBACK)
            .allowMainThreadQueries()
            .build()
        repository = CollectionRepositoryImpl(
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
    public fun `progresso conta so exemplar possuido e lista o que falta`(): Unit = runBlocking {
        seedCatalog()
        val collection = repository.create("Car Culture").getOrThrow()
        repository.addVariant(collection.id, "v1").getOrThrow()
        repository.addVariant(collection.id, "v2").getOrThrow()
        val extra = repository.addVariant(collection.id, "v3").getOrThrow()
        repository.setNonCollectible(extra.id, true).getOrThrow()

        // v1 tem um exemplar possuído e um vendido; v2 não tem nenhum.
        val progress = repository.observeAll().first().single()

        assertThat(progress.name).isEqualTo("Car Culture")
        assertThat(progress.known).isEqualTo(2)
        assertThat(progress.owned).isEqualTo(1)
        assertThat(progress.percent).isEqualTo(50)
        assertThat(progress.itemCount).isEqualTo(3)
        assertThat(progress.missing.map { it.variantId }).containsExactly("v2")
    }

    @Test
    public fun `item de modelo conta exemplares de qualquer variante do modelo`(): Unit = runBlocking {
        seedCatalog()
        database.exemplarDao().insert(exemplar("e3", "v2", "HW-3", "OWNED"))
        database.exemplarDao().insert(exemplar("e4", "v3", "HW-4", "ARCHIVED"))
        val collection = repository.create("Série inteira").getOrThrow()
        repository.addModel(collection.id, "m1", expected = 2).getOrThrow()

        val progress = repository.observeAll().first().single()

        // m1 tem duas variantes, cada uma com um exemplar possuído: 2 de 2.
        assertThat(progress.known).isEqualTo(2)
        assertThat(progress.owned).isEqualTo(2)
        assertThat(progress.isComplete).isTrue()
        assertThat(progress.missing).isEmpty()
    }

    @Test
    public fun `marcar nao colecionavel tira o item do denominador e pode ser desfeito`(): Unit = runBlocking {
        seedCatalog()
        val collection = repository.create("Car Culture").getOrThrow()
        repository.addVariant(collection.id, "v1").getOrThrow()
        val missing = repository.addVariant(collection.id, "v2").getOrThrow()

        repository.setNonCollectible(missing.id, true).getOrThrow()
        assertThat(repository.observeAll().first().single().known).isEqualTo(1)

        repository.setNonCollectible(missing.id, false).getOrThrow()
        val back = repository.observeAll().first().single()
        assertThat(back.known).isEqualTo(2)
        assertThat(back.missing.map { it.variantId }).containsExactly("v2")
    }

    @Test
    public fun `mesma variante duas vezes na colecao e recusada`(): Unit = runBlocking {
        seedCatalog()
        val collection = repository.create("Car Culture").getOrThrow()
        repository.addVariant(collection.id, "v1").getOrThrow()

        val duplicate = repository.addVariant(collection.id, "v1")

        assertThat(duplicate.isFailure).isTrue()
        assertThat(duplicate.exceptionOrNull()).isInstanceOf(DomainError.Duplicate::class.java)
        assertThat(repository.observeItems(collection.id).first()).hasSize(1)
    }

    @Test
    public fun `hierarquia recusa ciclo direto e ciclo em dois niveis`(): Unit = runBlocking {
        val parent = repository.create("Raiz").getOrThrow()
        val child = repository.create("Filha", parentId = parent.id).getOrThrow()

        val selfCycle = repository.update(
            id = parent.id,
            name = "Raiz",
            parentId = parent.id,
        )
        assertThat(selfCycle.isFailure).isTrue()
        assertThat(selfCycle.exceptionOrNull()).isInstanceOf(DomainError.Validation::class.java)

        // Mover a raiz para dentro da própria filha fecharia o ciclo.
        val indirect = repository.update(id = parent.id, name = "Raiz", parentId = child.id)
        assertThat(indirect.isFailure).isTrue()
        assertThat(indirect.exceptionOrNull()?.message).contains("subcoleção")
        assertThat(repository.findById(parent.id)?.parentId).isNull()
    }

    @Test
    public fun `arquivar colecao com subcolecao ativa e bloqueado`(): Unit = runBlocking {
        val parent = repository.create("Raiz").getOrThrow()
        repository.create("Filha", parentId = parent.id).getOrThrow()

        val result = repository.archive(parent.id)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(DomainError.Blocked::class.java)
        assertThat(repository.observeAll().first()).hasSize(2)
    }

    @Test
    public fun `remover item tira da colecao sem apagar a variante`(): Unit = runBlocking {
        seedCatalog()
        val collection = repository.create("Car Culture").getOrThrow()
        val item = repository.addVariant(collection.id, "v1").getOrThrow()

        repository.removeItem(item.id).getOrThrow()

        assertThat(repository.observeItems(collection.id).first()).isEmpty()
        assertThat(database.variantDao().findById("v1")).isNotNull()
    }

    /** Fabricante, modelo e duas variantes com exemplar possuído; `v3` fica sem exemplar. */
    private suspend fun seedCatalog() {
        val t = 0L
        database.manufacturerDao().insert(ManufacturerEntity("m1", "Mattel", null, null, t, t))
        database.carModelDao().insert(
            CarModelEntity(
                id = "m1",
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
        database.variantDao().insert(variant("v1"))
        database.variantDao().insert(variant("v2"))
        database.variantDao().insert(variant("v3"))

        database.exemplarDao().insert(exemplar("e1", "v1", "HW-1", "OWNED"))
        database.exemplarDao().insert(exemplar("e2", "v1", "HW-2", "SOLD"))
    }

    private fun variant(id: String): VariantEntity = VariantEntity(
        id = id,
        modelId = "m1",
        fingerprint = "fp-$id",
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
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun exemplar(
        id: String,
        variantId: String,
        internalCode: String,
        status: String,
    ): ExemplarEntity = ExemplarEntity(
        id = id,
        variantId = variantId,
        internalCode = internalCode,
        condition = "SEALED",
        packagingCondition = null,
        locationId = null,
        status = status,
        notes = null,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
