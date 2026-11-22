package br.com.diecastcollector.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.AuditLogWriter
import br.com.diecastcollector.data.db.TransactionRunner
import br.com.diecastcollector.data.db.entity.ExemplarEntity
import br.com.diecastcollector.data.db.entity.PhotoEntity
import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.model.Variant
import br.com.diecastcollector.domain.usecase.CreateVariantUseCase
import br.com.diecastcollector.domain.usecase.UpdateVariantInput
import br.com.diecastcollector.domain.usecase.UpdateVariantUseCase
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
 * Edição de variante incompleta (FR-029).
 *
 * O que precisa ficar provado aqui: completar cor e ano **não** cria variante nova, não
 * mexe nos exemplares e recalcula o fingerprint; e uma edição que tornaria a variante
 * idêntica a outra ativa é recusada em vez de gravar duplicata (Art. I.3).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class VariantRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var variants: VariantRepositoryImpl
    private lateinit var models: CarModelRepositoryImpl
    private lateinit var manufacturers: ManufacturerRepositoryImpl
    private lateinit var colors: ColorRepositoryImpl
    private lateinit var updateVariant: UpdateVariantUseCase
    private val clock = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC)
    private val now: Instant = Instant.parse("2026-11-16T12:00:00Z")

    @Before
    public fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).addCallback(AppDatabase.CALLBACK).allowMainThreadQueries().build()

        variants = VariantRepositoryImpl(
            database = database,
            transactionRunner = TransactionRunner(database),
            auditLogWriter = AuditLogWriter(database.historyDao()),
            clock = clock,
        )
        models = CarModelRepositoryImpl(database, clock)
        manufacturers = ManufacturerRepositoryImpl(database, clock)
        colors = ColorRepositoryImpl(database)
        updateVariant = UpdateVariantUseCase(variants, clock)
    }

    @After
    public fun tearDown() {
        database.close()
    }

    @Test
    public fun `completar cor e ano preserva id, criacao e tira a variante dos incompletos`() =
        runBlocking {
            val model = newModel()
            val red = colors.findOrCreate("Vermelho", null).getOrThrow()
            val incomplete = newVariant(model.id, "fp-incompleta")
            variants.create(incomplete).getOrThrow()

            assertThat(models.observeIncomplete().first()).hasSize(1)

            val updated = updateVariant(
                incomplete.id,
                UpdateVariantInput(primaryColorId = red.id, releaseYear = 2024),
            ).getOrThrow()

            assertThat(updated.id).isEqualTo(incomplete.id)
            assertThat(updated.createdAt).isEqualTo(now)
            assertThat(updated.updatedAt).isEqualTo(Instant.parse("2026-11-20T10:00:00Z"))
            assertThat(updated.primaryColorId).isEqualTo(red.id)
            assertThat(updated.releaseYear).isEqualTo(2024)
            assertThat(updated.fingerprint).isNotEqualTo(incomplete.fingerprint)

            val saved = variants.findById(incomplete.id)!!
            assertThat(saved.primaryColorId).isEqualTo(red.id)
            assertThat(saved.releaseYear).isEqualTo(2024)
            // A lista é derivada: completou, sai sozinha.
            assertThat(models.observeIncomplete().first()).isEmpty()
        }

    @Test
    public fun `edicao nao mexe nos exemplares da variante`() = runBlocking {
        val model = newModel()
        val variant = newVariant(model.id, "fp-exemplar")
        variants.create(variant).getOrThrow()
        val exemplars = database.exemplarDao()
        exemplars.insert(
            ExemplarEntity(
                id = "exemplar-1",
                variantId = variant.id,
                internalCode = "HW-000001",
                condition = "SEALED",
                packagingCondition = null,
                locationId = null,
                status = "OWNED",
                notes = null,
                createdAt = now.toEpochMilli(),
                updatedAt = now.toEpochMilli(),
            ),
        )
        val red = colors.findOrCreate("Vermelho", null).getOrThrow()

        updateVariant(
            variant.id,
            UpdateVariantInput(primaryColorId = red.id, releaseYear = 2023),
        ).getOrThrow()

        assertThat(exemplars.countByVariant(variant.id)).isEqualTo(1)
        assertThat(exemplars.findById("exemplar-1")?.variantId).isEqualTo(variant.id)
    }

    @Test
    public fun `uniao move exemplares e arquiva a variante absorvida`() = runBlocking {
        val model = newModel()
        val target = newVariant(model.id, "fp-target")
        val absorbed = newVariant(model.id, "fp-absorbed")
        variants.create(target).getOrThrow()
        variants.create(absorbed).getOrThrow()
        val exemplars = database.exemplarDao()
        exemplars.insert(
            ExemplarEntity(
                id = "exemplar-1",
                variantId = absorbed.id,
                internalCode = "HW-000001",
                condition = "SEALED",
                packagingCondition = null,
                locationId = null,
                status = "OWNED",
                notes = null,
                createdAt = now.toEpochMilli(),
                updatedAt = now.toEpochMilli(),
            ),
        )

        variants.mergeVariants(target.id, absorbed.id).getOrThrow()

        assertThat(exemplars.countByVariant(target.id)).isEqualTo(1)
        assertThat(exemplars.countByVariant(absorbed.id)).isEqualTo(0)
        assertThat(variants.findById(absorbed.id)?.archivedAt).isNotNull()
        assertThat(database.historyDao().observeFor("variant", target.id).first())
            .isNotEmpty()
    }

    @Test
    public fun `edicao recusa variante identica a outra ativa`() = runBlocking {
        val model = newModel()
        val red = colors.findOrCreate("Vermelho", null).getOrThrow()
        val first = newVariant(
            model.id,
            CreateVariantUseCase.calculateFingerprint(
                modelId = model.id,
                seriesId = null,
                primaryColorId = red.id,
                secondaryColorIds = emptyList(),
                wheelsType = null,
                baseType = null,
                decoration = null,
                releaseYear = 2024,
                edition = null,
                manufacturerCode = null,
            ),
        ).copy(primaryColorId = red.id, releaseYear = 2024)
        variants.create(first).getOrThrow()
        val second = newVariant(model.id, "fp-segunda")
        variants.create(second).getOrThrow()

        // Completar a segunda com os mesmos atributos da primeira tornaria as duas idênticas.
        val result = updateVariant(
            second.id,
            UpdateVariantInput(primaryColorId = red.id, releaseYear = 2024),
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(DomainError.Duplicate::class.java)
        val untouched = variants.findById(second.id)!!
        assertThat(untouched.primaryColorId).isNull()
        assertThat(untouched.releaseYear).isNull()
    }

    @Test
    public fun `edicao recusa ano invalido e variante inexistente`() = runBlocking {
        val model = newModel()
        val variant = newVariant(model.id, "fp-ano")
        variants.create(variant).getOrThrow()

        val invalidYear = updateVariant(variant.id, UpdateVariantInput(releaseYear = 1800))
        assertThat(invalidYear.exceptionOrNull()).isInstanceOf(DomainError.Validation::class.java)

        val missing = updateVariant("nao-existe", UpdateVariantInput(releaseYear = 2024))
        assertThat(missing.exceptionOrNull()).isInstanceOf(DomainError.NotFound::class.java)
    }

    @Test
    public fun `edicao grava cores secundarias como conjunto`() = runBlocking {
        val model = newModel()
        val red = colors.findOrCreate("Vermelho", null).getOrThrow()
        val blue = colors.findOrCreate("Azul", null).getOrThrow()
        val variant = newVariant(model.id, "fp-cores")
        variants.create(variant).getOrThrow()

        updateVariant(
            variant.id,
            UpdateVariantInput(secondaryColorIds = listOf(red.id, blue.id)),
        ).getOrThrow()
        assertThat(variants.findById(variant.id)!!.secondaryColorIds)
            .containsExactly(red.id, blue.id)

        // Regravar substitui: remover uma cor não deixa a antiga para trás.
        updateVariant(variant.id, UpdateVariantInput(secondaryColorIds = listOf(blue.id)))
            .getOrThrow()
        assertThat(variants.findById(variant.id)!!.secondaryColorIds).containsExactly(blue.id)
        Unit
    }

    @Test
    public fun `resumo do catalogo traz a foto principal do exemplar`() = runBlocking {
        val model = newModel()
        val variant = newVariant(model.id, "fp-foto")
        variants.create(variant).getOrThrow()
        database.exemplarDao().insert(
            ExemplarEntity(
                "ex-foto",
                variant.id,
                "HW-000009",
                "SEALED",
                null,
                null,
                "OWNED",
                null,
                0L,
                1L,
                1L,
            ),
        )
        database.photoDao().insert(
            PhotoEntity(
                id = "p1",
                exemplarId = "ex-foto",
                angle = "FRONT",
                path = "images/ex-foto/front.jpg",
                checksum = "c1",
                isPrimary = false,
                sortOrder = 0,
                width = null,
                height = null,
                bytes = null,
                capturedAt = null,
            ),
        )
        // A foto marcada como principal vence mesmo com sort_order maior.
        database.photoDao().insert(
            PhotoEntity(
                id = "p2",
                exemplarId = "ex-foto",
                angle = "BACK",
                path = "images/ex-foto/back.jpg",
                checksum = "c2",
                isPrimary = true,
                sortOrder = 1,
                width = null,
                height = null,
                bytes = null,
                capturedAt = null,
            ),
        )

        val summary = variants.observeSummaries().first().single { it.variantId == variant.id }
        assertThat(summary.primaryPhotoPath).isEqualTo("images/ex-foto/back.jpg")
    }

    private suspend fun newModel(): br.com.diecastcollector.domain.model.CarModel {
        val manufacturer = manufacturers.add("Mattel", "EUA").getOrThrow()
        return models.add(manufacturer.id, "Twin Mill", "1:64").getOrThrow()
    }

    private fun newVariant(modelId: String, fingerprint: String): Variant = Variant(
        id = "variant-$fingerprint",
        modelId = modelId,
        fingerprint = fingerprint,
        createdAt = now,
        updatedAt = now,
    )
}
