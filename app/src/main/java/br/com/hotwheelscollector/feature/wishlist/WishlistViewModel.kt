package br.com.hotwheelscollector.feature.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.IsoDate
import br.com.hotwheelscollector.domain.repository.NewAcquisition
import br.com.hotwheelscollector.domain.repository.NewExemplar
import br.com.hotwheelscollector.domain.repository.WishlistEntry
import br.com.hotwheelscollector.domain.repository.WishlistRepository
import br.com.hotwheelscollector.ui.UiMessage
import br.com.hotwheelscollector.ui.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public data class WishlistUiState(
    val items: List<WishlistEntry> = emptyList(),
    val loading: Boolean = true,
    val workingId: String? = null,
    val message: UiMessage? = null,
)

/**
 * Lista de desejos (US6).
 *
 * Converter um desejo vinculado a uma variante cria o exemplar e registra a aquisição
 * com o valor encontrado (ou o valor máximo, quando o encontrado não foi informado) e a
 * loja do desejo, tudo na mesma transação do vínculo. Desejo só com texto livre é marcado
 * como adquirido sem exemplar — não há variante para vincular e inventar uma seria pior
 * do que dizer isso à pessoa.
 */
@HiltViewModel
public class WishlistViewModel @Inject constructor(
    private val wishlistRepository: WishlistRepository,
    private val clock: Clock,
) : ViewModel() {

    private val feedback = MutableStateFlow<Pair<String?, UiMessage?>>(null to null)

    public val state: StateFlow<WishlistUiState> = wishlistRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = null,
        )
        .let { flow ->
            kotlinx.coroutines.flow.combine(flow, feedback) { entries, (workingId, message) ->
                WishlistUiState(
                    items = entries.orEmpty(),
                    loading = entries == null,
                    workingId = workingId,
                    message = message,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = WishlistUiState(),
        )

    public fun onBuy(entry: WishlistEntry) {
        if (feedback.value?.first != null) return
        feedback.value = entry.item.id to null
        viewModelScope.launch {
            val item = entry.item
            val draft = item.variantId?.let { variantId ->
                NewExemplar(
                    variantId = variantId,
                    condition = Condition.SEALED,
                    notes = item.notes,
                    acquisition = NewAcquisition(
                        date = IsoDate.today(clock),
                        price = item.foundPrice ?: item.maxPrice,
                        store = item.store,
                    ),
                )
            }
            val message = wishlistRepository.convertToAcquisition(item.id, draft).fold(
                onSuccess = { outcome ->
                    if (outcome == null) ACQUIRED_WITHOUT_EXEMPLAR else ACQUIRED
                },
                onFailure = { it.toUiMessage() },
            )
            feedback.value = null to message
        }
    }

    public fun onDismiss(id: String) {
        viewModelScope.launch {
            val message = wishlistRepository.dismiss(id).exceptionOrNull()
                ?.toUiMessage(R.string.wishlist_error_save) ?: DISMISSED
            feedback.value = null to message
        }
    }

    public fun onMessageShown() {
        feedback.value = feedback.value?.first to null
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        val ACQUIRED: UiMessage = UiMessage(R.string.wishlist_acquired)
        val ACQUIRED_WITHOUT_EXEMPLAR: UiMessage =
            UiMessage(R.string.wishlist_acquired_without_exemplar)
        val DISMISSED: UiMessage = UiMessage(R.string.wishlist_dismissed)
    }
}
