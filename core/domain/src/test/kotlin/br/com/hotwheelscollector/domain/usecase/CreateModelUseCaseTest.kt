package br.com.hotwheelscollector.domain.usecase

import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.CarModel
import br.com.hotwheelscollector.domain.model.IncompleteEntry
import br.com.hotwheelscollector.domain.model.Manufacturer
import br.com.hotwheelscollector.domain.repository.CarModelRepository
import br.com.hotwheelscollector.domain.repository.ManufacturerRepository
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/** Testes unitários do CreateModelUseCase (T041 - US2, FR-025, FR-033). */
public class CreateModelUseCaseTest {

    private lateinit var manufacturers: FakeManufacturerRepository
    private lateinit var models: FakeCarModelRepository
    private lateinit var useCase: CreateModelUseCase

    @Before
    fun setUp() {
        manufacturers = FakeManufacturerRepository()
        models = FakeCarModelRepository()
        useCase = CreateModelUseCase(manufacturers, models)
    }

    @Test
    fun `cria modelo novo com sucesso quando nao existe`() = runTest {
        val manufacturer = manufacturers.add("Mattel", "EUA").getOrThrow()

        val result = useCase(
            CreateModelInput(
                manufacturerId = manufacturer.id,
                name = "Twin Mill",
                scale = "1:64",
            )
        )

        assertThat(result.isSuccess).isTrue()
        val outcome = result.getOrThrow()
        assertThat(outcome.alreadyExisted).isFalse()
        assertThat(outcome.model.name).isEqualTo("Twin Mill")
        assertThat(outcome.model.scale).isEqualTo("1:64")
    }

    @Test
    fun `detecta modelo existente com mesmo nome e escala e devolve alreadyExisted true`() = runTest {
        val manufacturer = manufacturers.add("Mattel", "EUA").getOrThrow()
        val existing = models.add(manufacturer.id, "Twin Mill", "1:64").getOrThrow()

        val result = useCase(
            CreateModelInput(
                manufacturerId = manufacturer.id,
                name = "Twin Mill",
                scale = "1:64",
                forceCreate = false,
            )
        )

        assertThat(result.isSuccess).isTrue()
        val outcome = result.getOrThrow()
        assertThat(outcome.alreadyExisted).isTrue()
        assertThat(outcome.model.id).isEqualTo(existing.id)
    }

    @Test
    fun `recusa escala invalida`() = runTest {
        val manufacturer = manufacturers.add("Mattel", "EUA").getOrThrow()

        val result = useCase(
            CreateModelInput(
                manufacturerId = manufacturer.id,
                name = "Twin Mill",
                scale = "64",
            )
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(DomainError.Validation::class.java)
    }

    @Test
    fun `recusa fabricante inexistente`() = runTest {
        val result = useCase(
            CreateModelInput(
                manufacturerId = "inexistente",
                name = "Twin Mill",
                scale = "1:64",
            )
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Fabricante não encontrado")
    }

    private class FakeManufacturerRepository : ManufacturerRepository {
        private val items = mutableListOf<Manufacturer>()

        override fun observeActive(): Flow<List<Manufacturer>> = flowOf(items)
        override suspend fun findById(id: String): Manufacturer? = items.find { it.id == id }
        override suspend fun findByName(name: String): Manufacturer? = items.find { it.name.equals(name, ignoreCase = true) }
        override suspend fun add(name: String, country: String?): DomainResult<Manufacturer> {
            val m = Manufacturer(
                id = UUID.randomUUID().toString(),
                name = name,
                country = country,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
            items.add(m)
            return Result.success(m)
        }
        override suspend fun rename(id: String, name: String): DomainResult<Unit> = Result.success(Unit)
        override suspend fun archive(id: String): DomainResult<Unit> = Result.success(Unit)
    }

    private class FakeCarModelRepository : CarModelRepository {
        private val items = mutableListOf<CarModel>()

        override fun observeActive(): Flow<List<CarModel>> = flowOf(items)
        override fun observeByManufacturer(manufacturerId: String): Flow<List<CarModel>> =
            flowOf(items.filter { it.manufacturerId == manufacturerId })
        override suspend fun findById(id: String): CarModel? = items.find { it.id == id }
        override suspend fun findByIdentity(manufacturerId: String, name: String, scale: String): CarModel? =
            items.find { it.manufacturerId == manufacturerId && it.name.equals(name, ignoreCase = true) && it.scale == scale }

        override suspend fun add(
            manufacturerId: String,
            name: String,
            scale: String,
            brandId: String?,
            realVehicleId: String?,
        ): DomainResult<CarModel> {
            val model = CarModel(
                id = UUID.randomUUID().toString(),
                manufacturerId = manufacturerId,
                name = name,
                scale = scale,
                brandId = brandId,
                realVehicleId = realVehicleId,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
            items.add(model)
            return Result.success(model)
        }

        override suspend fun archive(id: String): DomainResult<Unit> = Result.success(Unit)

        override suspend fun update(
            id: String,
            name: String,
            scale: String,
            bodyType: String?,
            material: String?,
            countryOfManufacture: String?,
            notes: String?,
            realVehicleId: String?,
        ): DomainResult<CarModel> = Result.failure(DomainError.NotFound)

        override fun observeIncomplete(): Flow<List<IncompleteEntry>> = flowOf(emptyList())
    }
}
