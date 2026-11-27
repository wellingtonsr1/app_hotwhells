package br.com.diecastcollector.feature.duplicates

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.DuplicateAvailability
import br.com.diecastcollector.domain.model.Exemplar
import br.com.diecastcollector.domain.model.IsoDate
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.model.Negotiation
import br.com.diecastcollector.domain.model.NegotiationEvent
import br.com.diecastcollector.domain.model.NegotiationStatus
import br.com.diecastcollector.domain.model.NegotiationType
import br.com.diecastcollector.domain.repository.DuplicateRepository
import br.com.diecastcollector.domain.repository.ExemplarRepository
import br.com.diecastcollector.domain.repository.NegotiationRepository
import br.com.diecastcollector.domain.repository.NewNegotiation
import br.com.diecastcollector.domain.repository.VariantRepository
import br.com.diecastcollector.domain.usecase.CloseSaleUseCase
import br.com.diecastcollector.domain.usecase.CloseTradeUseCase
import br.com.diecastcollector.ui.UiMessage
import br.com.diecastcollector.ui.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public data class NegotiationUiState(
    val internalCode: String? = null,
    val title: String = "",
    val today: String = "",
    val negotiations: List<Negotiation> = emptyList(),
    val events: List<NegotiationEvent> = emptyList(),
    val ownedExemplars: List<Exemplar> = emptyList(),
    val loading: Boolean = true,
    val working: Boolean = false,
    val type: NegotiationType = NegotiationType.SALE,
    val counterparty: String = "",
    val channel: String = "",
    val amountText: String = "",
    val notes: String = "",
    val reserve: Boolean = true,
    val message: UiMessage? = null,
) {
    public val active: Negotiation? = negotiations.firstOrNull {
        it.status == NegotiationStatus.OPEN || it.status == NegotiationStatus.RESERVED
    }
}

/**
 * Negociação de um exemplar (US7, FR-068–FR-073).
 *
 * A tela abre a negociação, conclui venda/troca e cancela. O histórico vem de outra
 * consulta e não depende do estado ativo: concluir ou cancelar não apaga nada.
 */
@HiltViewModel
public class NegotiationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val negotiations: NegotiationRepository,
    private val duplicates: DuplicateRepository,
    exemplarRepository: ExemplarRepository,
    variantRepository: VariantRepository,
    private val closeSale: CloseSaleUseCase,
    private val closeTrade: CloseTradeUseCase,
    clock: Clock,
) : ViewModel() {

    private val exemplarId: String =
        savedStateHandle.get<String>(ARG_EXEMPLAR_ID).orEmpty()

    private val _state = MutableStateFlow(
        NegotiationUiState(today = IsoDate.today(clock).formatPtBr()),
    )
    public val state: StateFlow<NegotiationUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preselectType()
        }
        viewModelScope.launch {
            val exemplar = exemplarRepository.findById(exemplarId)
            _state.update {
                it.copy(internalCode = exemplar?.internalCode, loading = exemplar == null)
            }
            val variantId = exemplar?.variantId ?: return@launch
            variantRepository.observeSummaries().collect { summaries ->
                val summary = summaries.firstOrNull { it.variantId == variantId } ?: return@collect
                _state.update { current ->
                    current.copy(
                        title = listOfNotNull(
                            summary.manufacturerName,
                            summary.modelName,
                            summary.colorName,
                        ).joinToString(" · "),
                    )
                }
            }
        }
        viewModelScope.launch {
            exemplarRepository.observeOwned().collect { owned ->
                _state.update {
                    it.copy(ownedExemplars = owned.filter { item -> item.id != exemplarId })
                }
            }
        }
        viewModelScope.launch {
            negotiations.observeByExemplar(exemplarId).collect { list ->
                _state.update { it.copy(negotiations = list) }
            }
        }
        viewModelScope.launch {
            negotiations.observeEventsForExemplar(exemplarId).collect { list ->
                _state.update { it.copy(events = list) }
            }
        }
    }

    /**
     * Tipo inicial pela oferta salva (T518, FR-514).
     *
     * `SALE` e `TRADE` explícitos mandam; **Troca e venda**, **Indisponível** e ausência de
     * oferta continuam em **Venda**, que é o caminho mais comum. Com negociação ativa nada
     * muda: o tipo não é oferecido de novo e a tela mostra o estado ativo.
     */
    private suspend fun preselectType() {
        val hasNegotiation = negotiations.observeByExemplar(exemplarId).first().any {
            it.status == NegotiationStatus.OPEN || it.status == NegotiationStatus.RESERVED
        }
        if (hasNegotiation) return
        val availability = duplicates.offerFor(exemplarId)?.availability
        val type = when (availability) {
            DuplicateAvailability.TRADE -> NegotiationType.TRADE
            else -> NegotiationType.SALE
        }
        _state.update { current -> if (current.active != null) current else current.copy(type = type) }
    }

    public fun onTypeChange(type: NegotiationType) = _state.update { it.copy(type = type) }

    public fun onCounterpartyChange(value: String) = _state.update {
        it.copy(counterparty = value)
    }

    public fun onChannelChange(value: String) = _state.update { it.copy(channel = value) }

    public fun onAmountChange(value: String) = _state.update { it.copy(amountText = value) }

    public fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    public fun onReserveChange(value: Boolean) = _state.update { it.copy(reserve = value) }

    public fun onMessageShown() = _state.update { it.copy(message = null) }

    public fun onOpen() {
        val current = _state.value
        if (current.working || current.active != null) return
        val amount = if (current.amountText.isBlank()) {
            null
        } else {
            MoneyCents.parsePtBrOrNull(current.amountText)
                ?: return _state.update {
                    it.copy(message = UiMessage(R.string.error_invalid_money))
                }
        }

        runOperation(
            onSuccess = {
                _state.update {
                    it.copy(
                        counterparty = "",
                        channel = "",
                        amountText = "",
                        notes = "",
                        message = UiMessage(R.string.negotiation_opened),
                    )
                }
            },
        ) {
            negotiations.open(
                NewNegotiation(
                    exemplarId = exemplarId,
                    type = current.type,
                    counterparty = current.counterparty,
                    channel = current.channel,
                    amount = amount,
                    notes = current.notes,
                    reserve = current.reserve,
                ),
            ).map { Unit }
        }
    }

    public fun onCloseSale(amountText: String, dateText: String) {
        val active = _state.value.active ?: return
        val amount = MoneyCents.parsePtBrOrNull(amountText)
            ?: return _state.update {
                it.copy(message = UiMessage(R.string.negotiation_value_required))
            }
        val date = if (dateText.isBlank()) {
            null
        } else {
            IsoDate.parsePtBrOrNull(dateText)
                ?: return _state.update {
                    it.copy(message = UiMessage(R.string.error_invalid_date))
                }
        }

        runOperation(
            onSuccess = {
                _state.update { it.copy(message = UiMessage(R.string.negotiation_sale_closed)) }
            },
        ) {
            closeSale.close(active.id, amount, date).map { Unit }
        }
    }

    public fun onCloseTrade(receivedExemplarId: String?, note: String) {
        val active = _state.value.active ?: return
        runOperation(
            onSuccess = {
                _state.update { it.copy(message = UiMessage(R.string.negotiation_trade_closed)) }
            },
        ) {
            closeTrade.close(active.id, receivedExemplarId, note).map { Unit }
        }
    }

    public fun onCancel(negotiationId: String) {
        runOperation(
            onSuccess = {
                _state.update { it.copy(message = UiMessage(R.string.negotiation_cancelled)) }
            },
        ) {
            negotiations.cancel(negotiationId).map { Unit }
        }
    }

    /** Executa uma operação com o indicador de trabalho e a mensagem de erro padronizados. */
    private fun runOperation(
        onSuccess: () -> Unit,
        operation: suspend () -> Result<Unit>,
    ) {
        if (_state.value.working) return
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            operation().fold(
                onSuccess = {
                    _state.update { it.copy(working = false) }
                    onSuccess()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            working = false,
                            message = error.toUiMessage(),
                        )
                    }
                },
            )
        }
    }

    public companion object {
        public const val ARG_EXEMPLAR_ID: String = "exemplarId"
    }
}
