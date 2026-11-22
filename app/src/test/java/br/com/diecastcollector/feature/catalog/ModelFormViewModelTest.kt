package br.com.diecastcollector.feature.catalog

import androidx.lifecycle.SavedStateHandle
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.CarModel
import br.com.diecastcollector.domain.model.IncompleteEntry
import br.com.diecastcollector.domain.model.RealVehicle
import br.com.diecastcollector.domain.repository.CarModelRepository
import br.com.diecastcollector.domain.repository.RealVehicleRepository
import br.com.diecastcollector.navigation.RouteArgs
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Modelo e veículo real (T043). O que o teste protege: um veículo digitado nos campos é
 * criado **e vinculado** ao modelo no mesmo salvamento; nada é gravado se o nome faltar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class ModelFormViewModelTest {

    private val now = Instant.parse("2026-11-20T12:00:00Z")
    private val carModels = FakeCarModels(
        CarModel(
            id = "m1",
            manufacturerId = "mf1",
            name = "Twin Mill",
            scale = "1:64",
            createdAt = now,
            updatedAt = now,
        ),
    )
    private val vehicles = FakeRealVehicles()

    @Before
    public fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    public fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ModelFormViewModel(
        savedStateHandle = SavedStateHandle(mapOf(RouteArgs.MODEL_ID to "m1")),
        carModelRepository = carModels,
        realVehicleRepository = vehicles,
    )

    @Test
    public fun carregaOsCamposDoModelo() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertThat(viewModel.state.value.loading).isFalse()
        assertThat(viewModel.state.value.modelName).isEqualTo("Twin Mill")
        assertThat(viewModel.state.value.scale).isEqualTo("1:64")
    }

    @Test
    public fun salvaComVeiculoRealNovoVinculado() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.setBodyType("Cupê")
        viewModel.setVehicleMake("Plymouth")
        viewModel.setVehicleModel("Road Runner")
        viewModel.setVehicleYear("1970")
        viewModel.save()
        advanceUntilIdle()

        assertThat(vehicles.added?.model).isEqualTo("Road Runner")
        assertThat(carModels.lastRealVehicleId).isEqualTo("rv1")
        assertThat(carModels.lastBodyType).isEqualTo("Cupê")
        assertThat(viewModel.state.value.saved).isTrue()
    }

    @Test
    public fun nomeEmBrancoNaoGravaNada() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.setName("   ")
        viewModel.save()
        advanceUntilIdle()

        assertThat(viewModel.state.value.error).isEqualTo(ModelFormError.REQUIRED_NAME)
        assertThat(viewModel.state.value.saved).isFalse()
        assertThat(vehicles.added).isNull()
    }
}

private class FakeRealVehicles : RealVehicleRepository {
    var added: RealVehicle? = null

    override fun observeAll(): Flow<List<RealVehicle>> = flowOf(emptyList())

    override suspend fun add(
        make: String,
        model: String,
        year: Int?,
        country: String?,
    ): DomainResult<RealVehicle> {
        val vehicle = RealVehicle("rv1", make, model, year, country)
        added = vehicle
        return Result.success(vehicle)
    }
}

private class FakeCarModels(initial: CarModel) : CarModelRepository {
    private var model: CarModel = initial
    var lastRealVehicleId: String? = null
    var lastBodyType: String? = null

    override fun observeActive(): Flow<List<CarModel>> = flowOf(listOf(model))
    override fun observeByManufacturer(manufacturerId: String): Flow<List<CarModel>> =
        flowOf(emptyList())

    override suspend fun findById(id: String): CarModel? = model.takeIf { it.id == id }

    override suspend fun findByIdentity(
        manufacturerId: String,
        name: String,
        scale: String,
    ): CarModel? = null

    override suspend fun add(
        manufacturerId: String,
        name: String,
        scale: String,
        brandId: String?,
        realVehicleId: String?,
    ): DomainResult<CarModel> = Result.success(model)

    override suspend fun update(
        id: String,
        name: String,
        scale: String,
        bodyType: String?,
        material: String?,
        countryOfManufacture: String?,
        notes: String?,
        realVehicleId: String?,
    ): DomainResult<CarModel> {
        lastRealVehicleId = realVehicleId
        lastBodyType = bodyType
        model = model.copy(
            name = name,
            scale = scale,
            bodyType = bodyType,
            material = material,
            countryOfManufacture = countryOfManufacture,
            notes = notes,
            realVehicleId = realVehicleId,
            updatedAt = model.updatedAt,
        )
        return Result.success(model)
    }

    override suspend fun archive(id: String): DomainResult<Unit> = Result.success(Unit)

    override fun observeIncomplete(): Flow<List<IncompleteEntry>> = flowOf(emptyList())
}
