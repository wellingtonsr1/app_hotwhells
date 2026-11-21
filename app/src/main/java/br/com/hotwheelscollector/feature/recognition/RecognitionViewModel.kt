package br.com.hotwheelscollector.feature.recognition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.data.di.BarcodeRecognition
import br.com.hotwheelscollector.domain.recognition.RecognitionApplication
import br.com.hotwheelscollector.domain.recognition.RecognitionField
import br.com.hotwheelscollector.domain.recognition.RecognitionInput
import br.com.hotwheelscollector.domain.recognition.RecognitionProposal
import br.com.hotwheelscollector.domain.recognition.RecognitionProvider
import br.com.hotwheelscollector.domain.usecase.CatalogMatch
import br.com.hotwheelscollector.domain.usecase.FindCatalogMatchesUseCase
import br.com.hotwheelscollector.domain.usecase.RecordRecognitionResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Fluxo de código de barras (T128/T129, FR-109–FR-113).
 *
 * Ler não grava no catálogo: a leitura produz sugestões e correspondências, e só um aceite
 * explícito da tela grava `recognition_result`. Nenhum exemplar é criado aqui (FR-113).
 * A entrada por texto existe para que a falta de permissão de câmera não impeça o fluxo
 * (FR-108).
 */
@HiltViewModel
public class RecognitionViewModel @Inject constructor(
    @BarcodeRecognition private val barcodeProvider: RecognitionProvider,
    private val findCatalogMatches: FindCatalogMatchesUseCase,
    private val recordRecognitionResult: RecordRecognitionResultUseCase,
) : ViewModel() {

    public data class State(
        val busy: Boolean = false,
        val typedCode: String = "",
        val proposal: RecognitionProposal? = null,
        val matches: List<CatalogMatch> = emptyList(),
        val selectedExemplarId: String? = null,
        val recordedId: String? = null,
        val recordedFields: Set<RecognitionField> = emptySet(),
        val failure: String? = null,
    ) {
        public val rawCode: String? get() = proposal?.rawValue
        public val identified: Boolean get() = proposal?.identified == true
        public val hasMatches: Boolean get() = matches.isNotEmpty()
    }

    private val _state: MutableStateFlow<State> = MutableStateFlow(State())
    public val state: StateFlow<State> = _state.asStateFlow()

    public fun onTypedCodeChange(value: String) {
        _state.update { it.copy(typedCode = value) }
    }

    /** Lê o código digitado — caminho que continua funcionando sem câmera (FR-108). */
    public fun readTypedCode() {
        val code = _state.value.typedCode.trim()
        if (code.isEmpty()) return
        read(RecognitionInput.Barcode(code))
    }

    /** Lê o código da imagem capturada pela câmera (FR-109). */
    public fun onImageCaptured(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        read(RecognitionInput.Image(bytes))
    }

    /** Escolha explícita da correspondência que o usuário está conferindo (FR-110). */
    public fun selectMatch(exemplarId: String) {
        _state.update { it.copy(selectedExemplarId = exemplarId) }
    }

    /**
     * Grava o que o usuário aceitou (FR-104/SC-010).
     *
     * Os valores vêm sempre da proposta, nunca da tela: [RecognitionApplication.accept]
     * ignora campo que não foi sugerido. A correspondência selecionada é apenas o vínculo
     * do registro — o catálogo não é alterado.
     */
    public fun apply(acceptedFields: Set<RecognitionField>) {
        val current = _state.value
        val proposal = current.proposal ?: return
        val application = RecognitionApplication.accept(proposal, acceptedFields)
        viewModelScope.launch {
            val result = runCatching {
                recordRecognitionResult(
                    proposal = proposal,
                    applied = application,
                    exemplarId = current.selectedExemplarId
                        ?: current.matches.firstOrNull()?.exemplarId,
                )
            }
            _state.update { state ->
                result.fold(
                    onSuccess = {
                        state.copy(
                            recordedId = it,
                            recordedFields = application.fields.keys,
                            failure = null,
                        )
                    },
                    onFailure = { state.copy(failure = it.message) },
                )
            }
        }
    }

    public fun clearFailure() {
        _state.update { it.copy(failure = null) }
    }

    private fun read(input: RecognitionInput) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    busy = true,
                    proposal = null,
                    matches = emptyList(),
                    selectedExemplarId = null,
                    recordedId = null,
                    recordedFields = emptySet(),
                    failure = null,
                )
            }
            val result = runCatching {
                val proposal = barcodeProvider.recognize(input)
                proposal to findCatalogMatches(proposal)
            }
            _state.update { state ->
                result.fold(
                    onSuccess = { (proposal, matches) ->
                        state.copy(
                            busy = false,
                            proposal = proposal,
                            matches = matches,
                            selectedExemplarId = matches.firstOrNull()?.exemplarId,
                        )
                    },
                    onFailure = {
                        state.copy(
                            busy = false,
                            failure = it.message ?: "Não foi possível ler o código.",
                        )
                    },
                )
            }
        }
    }
}
