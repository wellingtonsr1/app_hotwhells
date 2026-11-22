package br.com.diecastcollector.feature.recognition

import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.domain.model.RecognitionSource
import br.com.diecastcollector.domain.model.SearchFilterOptions
import br.com.diecastcollector.domain.model.SearchFilters
import br.com.diecastcollector.domain.model.SearchResultItem
import br.com.diecastcollector.domain.recognition.RecognitionApplication
import br.com.diecastcollector.domain.recognition.RecognitionField
import br.com.diecastcollector.domain.recognition.RecognitionInput
import br.com.diecastcollector.domain.recognition.RecognitionProposal
import br.com.diecastcollector.domain.recognition.RecognitionProvider
import br.com.diecastcollector.domain.recognition.RecognitionSuggestion
import br.com.diecastcollector.domain.repository.RecognitionRepository
import br.com.diecastcollector.domain.repository.SearchRepository
import br.com.diecastcollector.domain.usecase.FindCatalogMatchesUseCase
import br.com.diecastcollector.domain.usecase.RecordRecognitionResultUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * T128/T129 — ler um código não grava nada; só o aceite explícito registra (SC-010).
 *
 * O teste protege o caminho real do ViewModel: a correspondência vem do catálogo local, o
 * registro leva os campos aceitos e o exemplar correspondente, e uma leitura inconclusiva
 * não inventa dado. A câmera negada não aparece aqui porque o caminho digitado usa a mesma
 * leitura — o que a tela garante é não depender da permissão para chegar ao ViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class RecognitionViewModelTest {

    private val provider = FakeBarcodeProvider()
    private val recognition = FakeRecognitionRepository()
    private val search = FakeSearchRepository()

    @Before
    public fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    public fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): RecognitionViewModel = RecognitionViewModel(
        barcodeProvider = provider,
        findCatalogMatches = FindCatalogMatchesUseCase(search),
        recordRecognitionResult = RecordRecognitionResultUseCase(recognition),
    )

    @Test
    public fun `ler o codigo nao grava nada antes do aceite`(): Unit = runBlocking {
        val viewModel = viewModel()

        viewModel.onTypedCodeChange("HKC25")
        viewModel.readTypedCode()
        awaitState(viewModel) { it.proposal != null }

        assertThat(viewModel.state.value.identified).isTrue()
        assertThat(recognition.recorded).isEmpty()
    }

    @Test
    public fun `aplicar registra so os campos aceitos e vincula a correspondencia`(): Unit = runBlocking {
        val viewModel = viewModel()
        viewModel.onTypedCodeChange("HKC25")
        viewModel.readTypedCode()
        awaitState(viewModel) { it.matches.isNotEmpty() }

        viewModel.apply(setOf(RecognitionField.CODE))
        awaitState(viewModel) { it.recordedId != null }

        val record = recognition.recorded.single()
        assertThat(record.applied.fields.keys).containsExactly(RecognitionField.CODE)
        assertThat(record.exemplarId).isEqualTo("ex-1")
        assertThat(record.proposal.rawValue).isEqualTo("HKC25")
        assertThat(viewModel.state.value.recordedFields)
            .containsExactly(RecognitionField.CODE)
    }

    @Test
    public fun `campo nao aceito nao chega ao registro`(): Unit = runBlocking {
        val viewModel = viewModel()
        viewModel.onTypedCodeChange("HKC25")
        viewModel.readTypedCode()
        awaitState(viewModel) { it.proposal != null }

        viewModel.apply(emptySet())
        awaitState(viewModel) { it.recordedId != null }

        assertThat(recognition.recorded.single().applied.isEmpty).isTrue()
    }

    @Test
    public fun `codigo sem correspondencia registra a leitura sem exemplar`(): Unit = runBlocking {
        search.results = emptyList()
        val viewModel = viewModel()
        viewModel.onTypedCodeChange("DESCONHECIDO")
        viewModel.readTypedCode()
        awaitState(viewModel) { it.proposal != null }

        assertThat(viewModel.state.value.hasMatches).isFalse()

        viewModel.apply(emptySet())
        awaitState(viewModel) { it.recordedId != null }

        val record = recognition.recorded.single()
        assertThat(record.exemplarId).isNull()
        assertThat(record.proposal.rawValue).isEqualTo("DESCONHECIDO")
    }

    @Test
    public fun `leitura inconclusiva nao aplica nenhum campo`(): Unit = runBlocking {
        provider.unidentified = true
        val viewModel = viewModel()
        viewModel.onTypedCodeChange("???")
        viewModel.readTypedCode()
        awaitState(viewModel) { it.proposal != null }

        assertThat(viewModel.state.value.identified).isFalse()

        viewModel.apply(setOf(RecognitionField.MODEL))
        awaitState(viewModel) { it.recordedId != null }

        assertThat(recognition.recorded.single().applied.isEmpty).isTrue()
    }

    @Test
    public fun `falha do provedor vira mensagem sem gravar`(): Unit = runBlocking {
        provider.failure = DomainError.RecognitionUnavailable
        val viewModel = viewModel()
        viewModel.onTypedCodeChange("HKC25")
        viewModel.readTypedCode()
        awaitState(viewModel) { it.failure != null }

        assertThat(viewModel.state.value.busy).isFalse()
        assertThat(recognition.recorded).isEmpty()
    }

    private suspend fun awaitState(
        viewModel: RecognitionViewModel,
        predicate: (RecognitionViewModel.State) -> Boolean,
    ): Unit = withTimeout(2_000) {
        while (!predicate(viewModel.state.value)) {
            kotlinx.coroutines.delay(5)
        }
    }

    private class FakeBarcodeProvider : RecognitionProvider {
        override val source: RecognitionSource = RecognitionSource.BARCODE
        var unidentified: Boolean = false
        var failure: Throwable? = null

        override suspend fun recognize(input: RecognitionInput): RecognitionProposal {
            failure?.let { throw it }
            val raw = when (input) {
                is RecognitionInput.Barcode -> input.value
                is RecognitionInput.Image -> "HKC25"
            }
            return if (unidentified) {
                RecognitionProposal.unidentified(source, "fake", raw)
            } else {
                RecognitionProposal.of(
                    source = source,
                    provider = "fake",
                    rawValue = raw,
                    suggestions = listOf(
                        RecognitionSuggestion(RecognitionField.CODE, raw, 1.0),
                    ),
                )
            }
        }
    }

    private data class Recording(
        val proposal: RecognitionProposal,
        val applied: RecognitionApplication,
        val exemplarId: String?,
    )

    private class FakeRecognitionRepository : RecognitionRepository {
        val recorded: MutableList<Recording> = mutableListOf()

        override suspend fun record(
            proposal: RecognitionProposal,
            applied: RecognitionApplication,
            exemplarId: String?,
            draftId: String?,
        ): String {
            recorded += Recording(proposal, applied, exemplarId)
            return "rec-${recorded.size}"
        }
    }

    private class FakeSearchRepository : SearchRepository {
        var results: List<SearchResultItem> = listOf(
            SearchResultItem(
                exemplarId = "ex-1",
                variantId = "var-1",
                internalCode = "HW-0001",
                modelName = "Nissan Skyline GT-R R34",
                manufacturerName = "Hot Wheels",
                seriesName = "Car Culture",
                collectionName = null,
                primaryColorName = "Azul",
                decoration = null,
                realVehicleName = null,
                manufacturerCode = "HKC25",
                scale = "1:64",
                releaseYear = 2024,
                condition = Condition.SEALED,
                status = ExemplarStatus.OWNED,
                locationName = null,
                priceCents = null,
                acquisitionDate = null,
            ),
        )

        override fun observeResults(filters: SearchFilters): Flow<List<SearchResultItem>> =
            flowOf(results)

        override fun observeOptions(): Flow<SearchFilterOptions> = flowOf(SearchFilterOptions())

        override suspend fun variantIds(query: String, limit: Int): List<String> =
            results.map { it.variantId }

        override suspend fun rebuild(): DomainResult<Unit> = Result.success(Unit)
    }
}
