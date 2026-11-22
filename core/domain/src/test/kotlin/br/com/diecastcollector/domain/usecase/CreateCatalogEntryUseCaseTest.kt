package br.com.diecastcollector.domain.usecase

import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.CarModel
import br.com.diecastcollector.domain.model.IncompleteEntry
import br.com.diecastcollector.domain.model.Manufacturer
import br.com.diecastcollector.domain.repository.CarModelRepository
import br.com.diecastcollector.domain.repository.ManufacturerRepository
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Testes do cadastro mínimo (US2).
 *
 * O passo inicial resolve **fabricante e modelo** e para aí: variante, exemplar e fotos
 * nascem na tela seguinte. O que este teste protege é o reaproveitamento — digitar o mesmo
 * carro duas vezes não pode criar modelo duplicado — e a validação que roda antes de gravar.
 */
public class CreateCatalogEntryUseCaseTest {

    private lateinit var manufacturers: FakeManufacturerRepository
    private lateinit var models: FakeCarModelRepository
    private lateinit var useCase: CreateCatalogEntryUseCase

    @Before
    fun setUp() {
        manufacturers = FakeManufacturerRepository()
        models = FakeCarModelRepository()
        useCase = CreateCatalogEntryUseCase(manufacturers, models)
    }

    @Test
    fun `cria fabricante e modelo quando nao existem`() = runTest {
        val result = useCase(
            NewCatalogEntry(manufacturerName = "Mattel", modelName = "Twin Mill"),
        )

        assertThat(result.isSuccess).isTrue()
        val model = result.getOrThrow()
        assertThat(model.name).isEqualTo("Twin Mill")
        assertThat(model.scale).isEqualTo("1:64")
        assertThat(manufacturers.items.map { it.name }).containsExactly("Mattel")
        assertThat(models.items).hasSize(1)
    }

    @Test
    fun `reaproveita fabricante e modelo existentes`() = runTest {
        val first = useCase(
            NewCatalogEntry(manufacturerName = "Mattel", modelName = "Twin Mill"),
        ).getOrThrow()

        val second = useCase(
            NewCatalogEntry(manufacturerName = "mattel", modelName = "Twin Mill", scale = "1:64"),
        ).getOrThrow()

        assertThat(second.id).isEqualTo(first.id)
        assertThat(manufacturers.items).hasSize(1)
        assertThat(models.items).hasSize(1)
    }

    @Test
    fun `recusa fabricante em branco`() = runTest {
        val result = useCase(NewCatalogEntry(manufacturerName = "  ", modelName = "Twin Mill"))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(DomainError.Validation::class.java)
    }

    @Test
    fun `recusa escala fora do formato 1 para N`() = runTest {
        val result = useCase(
            NewCatalogEntry(manufacturerName = "Mattel", modelName = "Twin Mill", scale = "64"),
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Escala")
    }

    private class FakeManufacturerRepository : ManufacturerRepository {
        val items = mutableListOf<Manufacturer>()

        override fun observeActive(): Flow<List<Manufacturer>> = flowOf(items)

        override suspend fun findById(id: String): Manufacturer? = items.find { it.id == id }

        override suspend fun findByName(name: String): Manufacturer? =
            items.find { it.name.equals(name.trim(), ignoreCase = true) }

        override suspend fun add(name: String, country: String?): DomainResult<Manufacturer> {
            val manufacturer = Manufacturer(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                country = country,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
            items.add(manufacturer)
            return Result.success(manufacturer)
        }

        override suspend fun rename(id: String, name: String): DomainResult<Unit> =
            Result.success(Unit)

        override suspend fun archive(id: String): DomainResult<Unit> = Result.success(Unit)
    }

    private class FakeCarModelRepository : CarModelRepository {
        val items = mutableListOf<CarModel>()

        override fun observeActive(): Flow<List<CarModel>> = flowOf(items)

        override fun observeByManufacturer(manufacturerId: String): Flow<List<CarModel>> =
            flowOf(items.filter { it.manufacturerId == manufacturerId })

        override suspend fun findById(id: String): CarModel? = items.find { it.id == id }

        override suspend fun findByIdentity(
            manufacturerId: String,
            name: String,
            scale: String,
        ): CarModel? = items.find {
            it.manufacturerId == manufacturerId &&
                it.name.equals(name.trim(), ignoreCase = true) &&
                it.scale == scale
        }

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
                name = name.trim(),
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
