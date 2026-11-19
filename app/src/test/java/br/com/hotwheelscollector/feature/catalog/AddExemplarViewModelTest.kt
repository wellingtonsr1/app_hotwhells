package br.com.hotwheelscollector.feature.catalog

import androidx.lifecycle.SavedStateHandle
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.Exemplar
import br.com.hotwheelscollector.domain.model.ExemplarStatus
import br.com.hotwheelscollector.domain.model.Location
import br.com.hotwheelscollector.domain.model.Movement
import br.com.hotwheelscollector.domain.model.PackagingCondition
import br.com.hotwheelscollector.domain.repository.AddExemplarOutcome
import br.com.hotwheelscollector.domain.repository.ExemplarDependents
import br.com.hotwheelscollector.domain.repository.ExemplarRepository
import br.com.hotwheelscollector.domain.repository.LocationRepository
import br.com.hotwheelscollector.domain.repository.NewExemplar
import br.com.hotwheelscollector.domain.usecase.AddExemplarUseCase
import br.com.hotwheelscollector.navigation.RouteArgs
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Testes do estado da tela de cadastro de exemplar.
 *
 * O caso de uso é o **real**: o que se testa é a combinação ViewModel + validações de
 * domínio, com o banco substituído por fakes. Assim, regra recusada no domínio não
 * vira mensagem errada na tela, e texto inválido nem chega ao repositório.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddExemplarViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC)
    private val repository = FakeAddExemplarRepository()
    private val locationRepository = FakeLocationRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(variantId: String = "v1"): AddExemplarViewModel = AddExemplarViewModel(
        savedStateHandle = SavedStateHandle(mapOf(RouteArgs.VARIANT_ID to variantId)),
        addExemplar = AddExemplarUseCase(repository, fixedClock),
        exemplarRepository = repository,
        locationRepository = locationRepository,
        clock = fixedClock,
    )

    @Test
    fun `variantId vem do SavedStateHandle`() = runTest {
        val model = viewModel(variantId = "variante-42")

        assertThat(model.state.value.variantId).isEqualTo("variante-42")
    }

    @Test
    fun `avisa quando a variante ja tem exemplares`() = runTest {
        repository.existingCount = 2

        val model = viewModel()

        assertThat(model.state.value.duplicateNotice).contains("2 exemplar(es)")
    }

    @Test
    fun `nao avisa repeticao quando a variante esta vazia`() = runTest {
        repository.existingCount = 0

        val model = viewModel()

        assertThat(model.state.value.duplicateNotice).isNull()
    }

    @Test
    fun `salvar com valores validos informa o codigo do exemplar`() = runTest {
        val model = viewModel()
        model.onPriceChange("59,90")
        model.onNotesChange("Comprado na feira")

        model.onSave()

        val state = model.state.value
        assertThat(state.savedExemplarId).isNotNull()
        assertThat(state.message).contains("HW-000001")
        assertThat(repository.lastDraft?.acquisition?.price?.cents).isEqualTo(5990L)
        assertThat(repository.lastDraft?.notes).isEqualTo("Comprado na feira")
    }

    @Test
    fun `segundo exemplar usa mensagem ordinal sem bloquear`() = runTest {
        repository.nextCount = 2
        val model = viewModel()

        model.onSave()

        assertThat(model.state.value.message).isEqualTo("2º exemplar desta variante.")
        assertThat(model.state.value.savedExemplarId).isNotNull()
    }

    @Test
    fun `valor invalido nao chega ao repositorio`() = runTest {
        val model = viewModel()
        model.onPriceChange("abc")

        model.onSave()

        assertThat(model.state.value.message).contains("Valor inválido")
        assertThat(repository.addCalls).isEqualTo(0)
    }

    @Test
    fun `data invalida e recusada antes de salvar`() = runTest {
        val model = viewModel()
        model.onDateChange("31/02/2026")

        model.onSave()

        assertThat(model.state.value.message).contains("Data inválida")
        assertThat(repository.addCalls).isEqualTo(0)
    }

    @Test
    fun `data futura e recusada pelo dominio`() = runTest {
        val model = viewModel()
        model.onDateChange("20/11/2026")
        // Sem valor ou loja não há aquisição, e aí a data nem é considerada.
        model.onPriceChange("50")

        model.onSave()

        assertThat(model.state.value.message).contains("futuro")
        assertThat(repository.addCalls).isEqualTo(0)
    }

    @Test
    fun `desconto maior que preco mais frete e recusado`() = runTest {
        val model = viewModel()
        model.onPriceChange("50")
        model.onShippingChange("10")
        model.onDiscountChange("90")

        model.onSave()

        assertThat(model.state.value.message).contains("desconto")
        assertThat(repository.addCalls).isEqualTo(0)
    }

    @Test
    fun `falha de armazenamento vira mensagem amigavel`() = runTest {
        repository.failure = DomainError.Storage("sem espaço")
        val model = viewModel()

        model.onSave()

        assertThat(model.state.value.message).contains("sem espaço")
        assertThat(model.state.value.savedExemplarId).isNull()
    }

    @Test
    fun `trocar condicao atualiza o estado`() = runTest {
        val model = viewModel()

        model.onConditionChange(Condition.DAMAGED)

        assertThat(model.state.value.condition).isEqualTo(Condition.DAMAGED)
    }

    @Test
    fun `mensagem consumida nao reaparece`() = runTest {
        val model = viewModel()
        model.onSave()

        model.onMessageShown()

        assertThat(model.state.value.message).isNull()
    }

    @Test
    fun `localizacoes cadastradas chegam ao estado`() = runTest {
        locationRepository.locations = listOf(
            Location(
                id = "loc1",
                type = br.com.hotwheelscollector.domain.model.LocationType.RACK,
                name = "Estante da sala",
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        val model = viewModel()

        // O seletor só oferece o que veio do repositório — nada de texto livre.
        assertThat(model.state.value.locations.map { it.id }).containsExactly("loc1")
    }
}

private class FakeAddExemplarRepository : ExemplarRepository {

    var existingCount: Int = 0
    var nextCount: Int = 1
    var addCalls: Int = 0
    var lastDraft: NewExemplar? = null
    var failure: DomainError? = null

    override fun observeOwned(): Flow<List<Exemplar>> = flowOf(emptyList())

    override fun observeByVariant(variantId: String): Flow<List<Exemplar>> = flowOf(emptyList())

    override suspend fun findById(id: String): Exemplar? = null

    override suspend fun add(draft: NewExemplar): DomainResult<AddExemplarOutcome> {
        addCalls++
        lastDraft = draft
        failure?.let { return Result.failure(it) }
        val now = Instant.parse("2026-11-16T12:00:00Z")
        return Result.success(
            AddExemplarOutcome(
                exemplar = Exemplar(
                    id = "exemplar-1",
                    variantId = draft.variantId,
                    internalCode = "HW-%06d".format(nextCount),
                    condition = draft.condition,
                    status = ExemplarStatus.OWNED,
                    packagingCondition = draft.packagingCondition,
                    locationId = draft.locationId,
                    notes = draft.notes,
                    createdAt = now,
                    updatedAt = now,
                ),
                sameVariantCount = nextCount,
            ),
        )
    }

    override suspend fun updateDetails(
        id: String,
        condition: Condition,
        packagingCondition: PackagingCondition?,
        notes: String?,
        expectedRowVersion: Long,
    ): DomainResult<Exemplar> = Result.failure(DomainError.NotFound)

    override suspend fun move(id: String, toLocationId: String?, note: String?): DomainResult<Unit> =
        Result.success(Unit)

    override suspend fun countByVariant(variantId: String): Int = existingCount

    override suspend fun archive(id: String): DomainResult<Unit> = Result.success(Unit)

    override suspend fun dependents(id: String): ExemplarDependents = ExemplarDependents()

    override suspend fun delete(id: String): DomainResult<Unit> = Result.success(Unit)
}

private class FakeLocationRepository : LocationRepository {

    var locations: List<Location> = emptyList()

    override fun observeActive(): Flow<List<Location>> = flowOf(locations)

    override fun observeTree(): Flow<List<br.com.hotwheelscollector.domain.model.LocationNode>> =
        flowOf(emptyList())

    override suspend fun children(parentId: String): List<Location> = emptyList()

    override suspend fun findById(id: String): Location? = locations.firstOrNull { it.id == id }

    override suspend fun add(
        type: br.com.hotwheelscollector.domain.model.LocationType,
        name: String,
        parentId: String?,
    ): DomainResult<Location> = Result.failure(DomainError.NotFound)

    override suspend fun rename(id: String, name: String): DomainResult<Unit> =
        Result.success(Unit)

    override suspend fun archive(
        id: String,
        policy: br.com.hotwheelscollector.domain.model.LocationArchivePolicy,
    ): DomainResult<Int> = Result.success(0)

    override suspend fun movements(exemplarId: String): Flow<List<Movement>> = flowOf(emptyList())

    override fun observeContents(
        locationId: String,
    ): Flow<List<br.com.hotwheelscollector.domain.model.InventoryItem>> = flowOf(emptyList())
}
