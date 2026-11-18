package br.com.hotwheelscollector.domain.usecase

import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.CarModel
import br.com.hotwheelscollector.domain.model.IncompleteEntry
import br.com.hotwheelscollector.domain.model.Variant
import br.com.hotwheelscollector.domain.model.VariantSummary
import br.com.hotwheelscollector.domain.repository.CarModelRepository
import br.com.hotwheelscollector.domain.repository.VariantRepository
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/** Testes unitários do CreateVariantUseCase (T042 - US2, FR-035, FR-039, FR-040). */
public class CreateVariantUseCaseTest {

    private lateinit var models: FakeCarModelRepository
    private lateinit var variants: FakeVariantRepository
    private lateinit var useCase: CreateVariantUseCase
    private val clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        models = FakeCarModelRepository()
        variants = FakeVariantRepository()
        useCase = CreateVariantUseCase(models, variants, clock)
    }

    @Test
    fun `cria variante com fingerprint estavel quando nao existe`() = runTest {
        val model = models.add("m1", "Bone Shaker", "1:64").getOrThrow()

        val result = useCase(
            CreateVariantInput(
                modelId = model.id,
                primaryColorId = "color-red",
                wheelsType = "5SP",
                baseType = "Metal",
                releaseYear = 2024,
            )
        )

        assertThat(result.isSuccess).isTrue()
        val outcome = result.getOrThrow()
        assertThat(outcome.alreadyExisted).isFalse()
        assertThat(outcome.variant.fingerprint).matches("^[a-f0-9]{64}$")
        assertThat(outcome.variant.modelId).isEqualTo(model.id)
    }

    @Test
    fun `detecta variante identica e devolve alreadyExisted true para reuso`() = runTest {
        val model = models.add("m1", "Bone Shaker", "1:64").getOrThrow()

        val first = useCase(
            CreateVariantInput(
                modelId = model.id,
                primaryColorId = "color-red",
                wheelsType = "5SP",
                releaseYear = 2024,
            )
        ).getOrThrow()

        // Segunda tentativa com mesmos atributos distintivos (FR-040)
        val second = useCase(
            CreateVariantInput(
                modelId = model.id,
                primaryColorId = "  color-red  ",
                wheelsType = "5sp",
                releaseYear = 2024,
            )
        ).getOrThrow()

        assertThat(second.alreadyExisted).isTrue()
        assertThat(second.variant.id).isEqualTo(first.variant.id)
        assertThat(second.variant.fingerprint).isEqualTo(first.variant.fingerprint)
    }

    @Test
    fun `variantes com atributos diferentes possuem fingerprints distintos`() = runTest {
        val model = models.add("m1", "Bone Shaker", "1:64").getOrThrow()

        val v1 = useCase(
            CreateVariantInput(
                modelId = model.id,
                primaryColorId = "color-red",
                wheelsType = "5SP",
            )
        ).getOrThrow()

        val v2 = useCase(
            CreateVariantInput(
                modelId = model.id,
                primaryColorId = "color-blue",
                wheelsType = "5SP",
            )
        ).getOrThrow()

        assertThat(v1.variant.fingerprint).isNotEqualTo(v2.variant.fingerprint)
        assertThat(v1.alreadyExisted).isFalse()
        assertThat(v2.alreadyExisted).isFalse()
    }

    @Test
    fun `recusa modelo inexistente`() = runTest {
        val result = useCase(
            CreateVariantInput(
                modelId = "inexistente",
                primaryColorId = "color-red",
            )
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Modelo não encontrado")
    }

    private class FakeCarModelRepository : CarModelRepository {
        private val items = mutableListOf<CarModel>()

        override fun observeActive(): Flow<List<CarModel>> = flowOf(items)
        override fun observeByManufacturer(manufacturerId: String): Flow<List<CarModel>> = flowOf(items)
        override suspend fun findById(id: String): CarModel? = items.find { it.id == id }
        override suspend fun findByIdentity(manufacturerId: String, name: String, scale: String): CarModel? =
            items.find { it.name.equals(name, ignoreCase = true) }

        override suspend fun add(
            manufacturerId: String,
            name: String,
            scale: String,
            brandId: String?,
            realVehicleId: String?,
        ): DomainResult<CarModel> {
            val m = CarModel(
                id = UUID.randomUUID().toString(),
                manufacturerId = manufacturerId,
                name = name,
                scale = scale,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
            items.add(m)
            return Result.success(m)
        }

        override suspend fun archive(id: String): DomainResult<Unit> = Result.success(Unit)
        override fun observeWithoutVariant(): Flow<List<IncompleteEntry>> = flowOf(emptyList())
    }

    private class FakeVariantRepository : VariantRepository {
        private val items = mutableListOf<Variant>()

        override fun observeSummaries(): Flow<List<VariantSummary>> = flowOf(emptyList())
        override fun observeByModel(modelId: String): Flow<List<Variant>> =
            flowOf(items.filter { it.modelId == modelId })
        override suspend fun findById(id: String): Variant? = items.find { it.id == id }
        override suspend fun findActiveByFingerprint(fingerprint: String): Variant? =
            items.find { it.fingerprint == fingerprint && it.archivedAt == null }

        override suspend fun create(variant: Variant): DomainResult<Variant> {
            items.add(variant)
            return Result.success(variant)
        }

        override suspend fun archive(id: String): DomainResult<Unit> = Result.success(Unit)
        override suspend fun mergeVariants(targetId: String, absorbedId: String): DomainResult<Unit> = Result.success(Unit)
    }
}
