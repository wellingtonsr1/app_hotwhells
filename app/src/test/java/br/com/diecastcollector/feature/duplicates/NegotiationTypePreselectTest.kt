package br.com.diecastcollector.feature.duplicates

import androidx.lifecycle.SavedStateHandle
import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.DuplicateAvailability
import br.com.diecastcollector.domain.model.DuplicateOffer
import br.com.diecastcollector.domain.model.NegotiationType
import br.com.diecastcollector.domain.model.Variant
import br.com.diecastcollector.domain.model.VariantSummary
import br.com.diecastcollector.domain.repository.VariantRepository
import br.com.diecastcollector.domain.usecase.CloseSaleUseCase
import br.com.diecastcollector.domain.usecase.CloseTradeUseCase
import br.com.diecastcollector.feature.catalog.FakeDuplicates
import br.com.diecastcollector.feature.catalog.FakeExemplarCatalog
import br.com.diecastcollector.feature.catalog.FakeNegotiations
import br.com.diecastcollector.feature.catalog.NOW
import br.com.diecastcollector.feature.catalog.negotiation
import com.google.common.truth.Truth.assertThat
import java.time.Clock
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
 * Tipo inicial da negociação pela oferta salva (T518, FR-514).
 *
 * Venda e troca explícitas mandam; **Troca e venda**, **Indisponível** e a ausência de
 * oferta continuam em **Venda**; com negociação ativa o campo não muda — a tela está no
 * estado ativo e não oferece uma segunda abertura.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NegotiationTypePreselectTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val clock: Clock = Clock.fixed(NOW, ZoneOffset.UTC)
    private val duplicates = FakeDuplicates()
    private val negotiations = FakeNegotiations()
    private val exemplars = FakeExemplarCatalog()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        exemplars.exemplars.value = listOf(
            br.com.diecastcollector.feature.catalog.ownedExemplar("e1"),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): NegotiationViewModel = NegotiationViewModel(
        savedStateHandle = SavedStateHandle(mapOf(NegotiationViewModel.ARG_EXEMPLAR_ID to "e1")),
        negotiations = negotiations,
        duplicates = duplicates,
        exemplarRepository = exemplars,
        variantRepository = FakeVariants(),
        closeSale = CloseSaleUseCase(negotiations),
        closeTrade = CloseTradeUseCase(negotiations),
        clock = clock,
    )

    private fun offer(availability: DuplicateAvailability) {
        duplicates.offers.value = mapOf(
            "e1" to DuplicateOffer(
                id = "offer-e1",
                exemplarId = "e1",
                availability = availability,
                createdAt = NOW,
                updatedAt = NOW,
            ),
        )
    }

    @Test
    fun `oferta de venda prese-seleciona venda`() = runTest {
        offer(DuplicateAvailability.SALE)

        assertThat(viewModel().state.value.type).isEqualTo(NegotiationType.SALE)
    }

    @Test
    fun `oferta de troca prese-seleciona troca`() = runTest {
        offer(DuplicateAvailability.TRADE)

        assertThat(viewModel().state.value.type).isEqualTo(NegotiationType.TRADE)
    }

    @Test
    fun `oferta de troca e venda mantem venda`() = runTest {
        offer(DuplicateAvailability.BOTH)

        assertThat(viewModel().state.value.type).isEqualTo(NegotiationType.SALE)
    }

    @Test
    fun `oferta indisponivel mantem venda`() = runTest {
        offer(DuplicateAvailability.UNAVAILABLE)

        assertThat(viewModel().state.value.type).isEqualTo(NegotiationType.SALE)
    }

    @Test
    fun `sem oferta o tipo continua venda`() = runTest {
        assertThat(viewModel().state.value.type).isEqualTo(NegotiationType.SALE)
    }

    @Test
    fun `com negociacao ativa o tipo nao e sobrescrito`() = runTest {
        offer(DuplicateAvailability.TRADE)
        negotiations.active.value = mapOf("e1" to negotiation("n1", "e1"))

        val model = viewModel()

        assertThat(model.state.value.type).isEqualTo(NegotiationType.SALE)
        assertThat(model.state.value.active?.id).isEqualTo("n1")
    }

    @Test
    fun `tipo continua editavel depois da prese-lecao`() = runTest {
        offer(DuplicateAvailability.TRADE)
        val model = viewModel()

        model.onTypeChange(NegotiationType.SALE)

        assertThat(model.state.value.type).isEqualTo(NegotiationType.SALE)
    }
}

/** `VariantRepository` mínimo: o teste olha o tipo, não o título da tela. */
private class FakeVariants : VariantRepository {
    override fun observeSummaries(): Flow<List<VariantSummary>> = flowOf(emptyList())

    override fun observeByModel(modelId: String): Flow<List<Variant>> = flowOf(emptyList())

    override suspend fun findById(id: String): Variant? = null

    override suspend fun findActiveByFingerprint(fingerprint: String): Variant? = null

    override suspend fun create(variant: Variant): DomainResult<Variant> =
        Result.failure(DomainError.NotFound)

    override suspend fun update(variant: Variant): DomainResult<Variant> =
        Result.failure(DomainError.NotFound)

    override suspend fun archive(id: String): DomainResult<Unit> =
        Result.failure(DomainError.NotFound)

    override suspend fun mergeVariants(targetId: String, absorbedId: String): DomainResult<Unit> =
        Result.failure(DomainError.NotFound)
}
