package br.com.diecastcollector.feature.catalog

import androidx.lifecycle.SavedStateHandle
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.DuplicateAvailability
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.navigation.RouteArgs
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Disponibilidade a partir da lista de exemplares (T511, FR-501, FR-507, FR-515a).
 *
 * A lista é a massa real da tela: o tamanho do `exemplars` é a contagem da variante, que é o
 * dado que decide o aviso. O teste cobra o que o usuário vê: aviso com 1 unidade, silêncio
 * com 2+, silêncio em **Indisponível**, nada gravado ao cancelar e a oferta salva voltando
 * preenchida na reabertura (FR-509).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExemplarListNegotiationTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val exemplars = FakeExemplarCatalog()
    private val duplicates = FakeDuplicates()
    private val negotiations = FakeNegotiations()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): ExemplarListViewModel = ExemplarListViewModel(
        savedStateHandle = SavedStateHandle(mapOf(RouteArgs.VARIANT_ID to "v1")),
        exemplarRepository = exemplars,
        locationRepository = FakeLocations(),
        photoRepository = FakePhotos(),
        duplicates = duplicates,
        negotiations = negotiations,
    )

    @Test
    fun `unica unidade avisa antes de gravar`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        val model = viewModel()
        collect(model)

        model.onOpenOfferSheet("e1")
        model.onSaveOffer(DuplicateAvailability.SALE, Condition.SEALED, "45,00", "Na caixa")

        assertThat(model.state.value.uniqueWarning).isTrue()
        assertThat(model.state.value.pendingOffer?.askingPrice).isEqualTo(MoneyCents(4_500))
        assertThat(duplicates.saved).isEmpty()
    }

    @Test
    fun `continuar o aviso grava venda com condicao valor e observacoes`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        val model = viewModel()
        collect(model)

        model.onOpenOfferSheet("e1")
        model.onSaveOffer(DuplicateAvailability.SALE, Condition.LOOSE, "45,00", "Na caixa")
        model.onContinueUniqueWarning()

        val offer = model.state.value.offers.getValue("e1")
        assertThat(offer.availability).isEqualTo(DuplicateAvailability.SALE)
        assertThat(offer.condition).isEqualTo(Condition.LOOSE)
        assertThat(offer.askingPrice).isEqualTo(MoneyCents(4_500))
        assertThat(offer.notes).isEqualTo("Na caixa")
        assertThat(model.state.value.sheetOpen).isFalse()
        assertThat(model.state.value.message?.resId).isEqualTo(R.string.availability_updated)
    }

    @Test
    fun `variante com dois exemplares nao avisa`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"), ownedExemplar("e2"))
        val model = viewModel()
        collect(model)

        model.onOpenOfferSheet("e1")
        model.onSaveOffer(DuplicateAvailability.TRADE, null, "", "")

        assertThat(model.state.value.uniqueWarning).isFalse()
        assertThat(duplicates.saved).containsExactly("e1" to DuplicateAvailability.TRADE)
    }

    @Test
    fun `indisponivel nao avisa nem com uma unidade`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        val model = viewModel()
        collect(model)

        model.onOpenOfferSheet("e1")
        model.onSaveOffer(DuplicateAvailability.UNAVAILABLE, null, "", "")

        assertThat(model.state.value.uniqueWarning).isFalse()
        assertThat(duplicates.saved).containsExactly("e1" to DuplicateAvailability.UNAVAILABLE)
    }

    @Test
    fun `cancelar o aviso nao grava e mantem a folha`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        val model = viewModel()
        collect(model)

        model.onOpenOfferSheet("e1")
        model.onSaveOffer(DuplicateAvailability.SALE, null, "", "")
        model.onCancelUniqueWarning()

        assertThat(model.state.value.uniqueWarning).isFalse()
        assertThat(model.state.value.sheetOpen).isTrue()
        assertThat(model.state.value.offers).isEmpty()
        assertThat(duplicates.saved).isEmpty()
    }

    @Test
    fun `oferta salva reabre com condicao valor e observacoes`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"), ownedExemplar("e2"))
        val model = viewModel()
        collect(model)

        model.onOpenOfferSheet("e1")
        model.onSaveOffer(DuplicateAvailability.BOTH, Condition.DAMAGED, "12,50", "Aceito troca")
        model.onOpenOfferSheet("e1")

        val offer = model.state.value.offers.getValue("e1")
        assertThat(offer.condition).isEqualTo(Condition.DAMAGED)
        assertThat(offer.askingPrice).isEqualTo(MoneyCents(1_250))
        assertThat(offer.notes).isEqualTo("Aceito troca")
        assertThat(model.state.value.selectedId).isEqualTo("e1")
    }

    @Test
    fun `valor invalido nao chega ao repositorio`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        val model = viewModel()
        collect(model)

        model.onOpenOfferSheet("e1")
        model.onSaveOffer(DuplicateAvailability.SALE, null, "abc", "")

        assertThat(model.state.value.message?.resId).isEqualTo(R.string.error_invalid_money)
        assertThat(model.state.value.uniqueWarning).isFalse()
        assertThat(duplicates.saved).isEmpty()
    }

    @Test
    fun `negociacao ativa aparece no estado e dispensa o aviso ao editar`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        negotiations.active.value = mapOf(
            "e1" to negotiation("n1", "e1"),
        )
        val model = viewModel()
        collect(model)

        model.onOpenOfferSheet("e1")
        model.onSaveOffer(DuplicateAvailability.SALE, null, "", "")

        assertThat(model.state.value.hasActiveNegotiation("e1")).isTrue()
        assertThat(model.state.value.uniqueWarning).isFalse()
        assertThat(duplicates.saved).hasSize(1)
    }

    @Test
    fun `mensagem consumida nao reaparece`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"), ownedExemplar("e2"))
        val model = viewModel()
        collect(model)

        model.onOpenOfferSheet("e1")
        model.onSaveOffer(DuplicateAvailability.SALE, null, "", "")
        model.onMessageShown()

        assertThat(model.state.value.message).isNull()
    }

    // ---- Negociação (T516, US2) ------------------------------------------------------

    @Test
    fun `negociar a unica unidade pede o aviso e nao abre`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        val model = viewModel()
        collect(model)

        model.onNegotiate("e1")

        assertThat(model.state.value.uniqueWarning).isTrue()
        assertThat(model.state.value.pendingNegotiationId).isEqualTo("e1")
        assertThat(model.state.value.openNegotiationId).isNull()
    }

    @Test
    fun `continuar o aviso de negociacao abre a tela`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        val model = viewModel()
        collect(model)

        model.onNegotiate("e1")
        model.onContinueUniqueWarning()

        assertThat(model.state.value.openNegotiationId).isEqualTo("e1")
        model.onNegotiationOpened()
        assertThat(model.state.value.openNegotiationId).isNull()
    }

    @Test
    fun `cancelar o aviso de negociacao nao abre nem grava`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        val model = viewModel()
        collect(model)

        model.onNegotiate("e1")
        model.onCancelUniqueWarning()

        assertThat(model.state.value.uniqueWarning).isFalse()
        assertThat(model.state.value.openNegotiationId).isNull()
        assertThat(duplicates.saved).isEmpty()
    }

    @Test
    fun `negociar com duas unidades abre direto`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"), ownedExemplar("e2"))
        val model = viewModel()
        collect(model)

        model.onNegotiate("e2")

        assertThat(model.state.value.uniqueWarning).isFalse()
        assertThat(model.state.value.openNegotiationId).isEqualTo("e2")
    }

    @Test
    fun `com negociacao ativa abre sem aviso`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        negotiations.active.value = mapOf("e1" to negotiation("n1", "e1"))
        val model = viewModel()
        collect(model)

        model.onNegotiate("e1")

        assertThat(model.state.value.uniqueWarning).isFalse()
        assertThat(model.state.value.openNegotiationId).isEqualTo("e1")
    }

    /** O `stateIn` só coleta com assinante; o teste precisa de um para ver o estado atual. */
    private fun TestScope.collect(model: ExemplarListViewModel) {
        backgroundScope.launch(dispatcher) { model.state.collect {} }
    }
}
