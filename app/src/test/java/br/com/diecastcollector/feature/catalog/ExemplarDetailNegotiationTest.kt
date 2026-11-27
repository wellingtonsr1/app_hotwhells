package br.com.diecastcollector.feature.catalog

import androidx.lifecycle.SavedStateHandle
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.DuplicateAvailability
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.navigation.RouteArgs
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Disponibilidade e aviso a partir da ficha do exemplar (T513, FR-506–FR-515).
 *
 * O teste cobre o que a folha da ficha promete: nada é gravado antes do **Continuar**, o
 * cancelamento mantém a folha, exemplar fora do acervo é recusado com explicação, a falha
 * de gravação não fecha a folha e reabrir mostra condição, valor e observações salvos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExemplarDetailNegotiationTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val clock: Clock = Clock.fixed(NOW, java.time.ZoneOffset.UTC)
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

    private fun viewModel(): ExemplarDetailViewModel = ExemplarDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(RouteArgs.EXEMPLAR_ID to "e1")),
        exemplarRepository = exemplars,
        locationRepository = FakeLocations(),
        acquisitionRepository = FakeAcquisitions(),
        valuationRepository = FakeValuations(),
        photoRepository = FakePhotos(),
        duplicates = duplicates,
        negotiations = negotiations,
        clock = clock,
    )

    @Test
    fun `unica unidade avisa antes de gravar`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        val model = viewModel()

        model.onOpenOfferSheet()
        model.onSaveOffer(DuplicateAvailability.SALE, Condition.SEALED, "45,00", "Na caixa")

        assertThat(model.state.value.sheetOpen).isTrue()
        assertThat(model.state.value.uniqueWarning).isTrue()
        assertThat(model.state.value.pendingOffer?.askingPrice).isEqualTo(MoneyCents(4_500))
        assertThat(duplicates.saved).isEmpty()
    }

    @Test
    fun `continuar grava e fecha a folha`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        val model = viewModel()

        model.onOpenOfferSheet()
        model.onSaveOffer(DuplicateAvailability.TRADE, Condition.LOOSE, "80,00", "Só troca")
        model.onContinueUniqueWarning()

        val offer = model.state.value.offer
        assertThat(offer?.availability).isEqualTo(DuplicateAvailability.TRADE)
        assertThat(offer?.condition).isEqualTo(Condition.LOOSE)
        assertThat(offer?.askingPrice).isEqualTo(MoneyCents(8_000))
        assertThat(offer?.notes).isEqualTo("Só troca")
        assertThat(model.state.value.sheetOpen).isFalse()
        assertThat(model.state.value.message?.resId).isEqualTo(R.string.availability_updated)
    }

    @Test
    fun `cancelar o aviso mantem a folha e nao grava`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        val model = viewModel()

        model.onOpenOfferSheet()
        model.onSaveOffer(DuplicateAvailability.SALE, null, "", "")
        model.onCancelUniqueWarning()

        assertThat(model.state.value.uniqueWarning).isFalse()
        assertThat(model.state.value.sheetOpen).isTrue()
        assertThat(duplicates.saved).isEmpty()
        assertThat(model.state.value.offer).isNull()
    }

    @Test
    fun `variante com duas unidades grava direto`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"), ownedExemplar("e2"))
        val model = viewModel()

        model.onOpenOfferSheet()
        model.onSaveOffer(DuplicateAvailability.BOTH, null, "", "")

        assertThat(model.state.value.uniqueWarning).isFalse()
        assertThat(duplicates.saved).containsExactly("e1" to DuplicateAvailability.BOTH)
        assertThat(model.state.value.sheetOpen).isFalse()
    }

    @Test
    fun `exemplar fora do acervo nao abre a folha e explica`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1").copy(status = ExemplarStatus.SOLD))
        val model = viewModel()

        model.onOpenOfferSheet()

        assertThat(model.state.value.sheetOpen).isFalse()
        assertThat(model.state.value.message?.resId)
            .isEqualTo(R.string.exemplar_negotiation_blocked)
    }

    @Test
    fun `falha ao gravar mantem a folha aberta`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"), ownedExemplar("e2"))
        duplicates.failure = DomainError.Storage("sem espaço")
        val model = viewModel()

        model.onOpenOfferSheet()
        model.onSaveOffer(DuplicateAvailability.SALE, null, "10,00", "")

        assertThat(model.state.value.sheetOpen).isTrue()
        assertThat(model.state.value.message?.resId).isEqualTo(R.string.error_storage)
        assertThat(model.state.value.message?.args?.joinToString()).contains("sem espaço")
    }

    @Test
    fun `valor invalido nao grava nem abre o aviso`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        val model = viewModel()

        model.onOpenOfferSheet()
        model.onSaveOffer(DuplicateAvailability.SALE, null, "abc", "")

        assertThat(model.state.value.message?.resId).isEqualTo(R.string.error_invalid_money)
        assertThat(model.state.value.uniqueWarning).isFalse()
        assertThat(duplicates.saved).isEmpty()
    }

    @Test
    fun `com negociacao ativa a edicao da oferta nao repete o aviso`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        negotiations.active.value = mapOf("e1" to negotiation("n1", "e1"))
        val model = viewModel()

        model.onOpenOfferSheet()
        model.onSaveOffer(DuplicateAvailability.SALE, null, "", "")

        assertThat(model.state.value.activeNegotiation?.id).isEqualTo("n1")
        assertThat(model.state.value.uniqueWarning).isFalse()
        assertThat(duplicates.saved).hasSize(1)
    }

    // ---- Negociação (T516, US2) ------------------------------------------------------

    @Test
    fun `negociar a unica unidade pede o aviso e nao navega`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        val model = viewModel()

        model.onNegotiate()

        assertThat(model.state.value.uniqueWarning).isTrue()
        assertThat(model.state.value.pendingNegotiation).isTrue()
        assertThat(model.state.value.negotiationRequested).isFalse()
    }

    @Test
    fun `continuar o aviso de negociacao pede a navegacao`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        val model = viewModel()

        model.onNegotiate()
        model.onContinueUniqueWarning()

        assertThat(model.state.value.negotiationRequested).isTrue()
        model.onNegotiationOpened()
        assertThat(model.state.value.negotiationRequested).isFalse()
    }

    @Test
    fun `cancelar o aviso de negociacao nao navega`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        val model = viewModel()

        model.onNegotiate()
        model.onCancelUniqueWarning()

        assertThat(model.state.value.uniqueWarning).isFalse()
        assertThat(model.state.value.negotiationRequested).isFalse()
    }

    @Test
    fun `negociar com duas unidades navega direto`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"), ownedExemplar("e2"))
        val model = viewModel()

        model.onNegotiate()

        assertThat(model.state.value.uniqueWarning).isFalse()
        assertThat(model.state.value.negotiationRequested).isTrue()
    }

    @Test
    fun `com negociacao ativa navega sem aviso`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1"))
        negotiations.active.value = mapOf("e1" to negotiation("n1", "e1"))
        val model = viewModel()

        model.onNegotiate()

        assertThat(model.state.value.uniqueWarning).isFalse()
        assertThat(model.state.value.negotiationRequested).isTrue()
    }

    @Test
    fun `exemplar fora do acervo nao negocia`() = runTest {
        exemplars.exemplars.value = listOf(ownedExemplar("e1").copy(status = ExemplarStatus.SOLD))
        val model = viewModel()

        model.onNegotiate()

        assertThat(model.state.value.negotiationRequested).isFalse()
        assertThat(model.state.value.message?.resId)
            .isEqualTo(R.string.exemplar_negotiation_blocked)
    }
}
