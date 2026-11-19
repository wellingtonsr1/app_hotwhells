package br.com.hotwheelscollector.feature.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.IsoDate
import br.com.hotwheelscollector.domain.repository.ExemplarRepository
import br.com.hotwheelscollector.domain.repository.NewAcquisition
import br.com.hotwheelscollector.domain.repository.NewExemplar
import br.com.hotwheelscollector.domain.repository.WishlistEntry
import br.com.hotwheelscollector.domain.repository.WishlistRepository
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
    val message: String? = null,
)

/**
 * Lista de desejos (US6).
 *
 * Converter um desejo vinculado a uma variante cria o exemplar e registra a aquisição
 * com o valor encontrado (ou o valor máximo, quando o encontrado não foi informado) e a
 * loja do desejo. Desejo só com texto livre é marcado como adquirido sem exemplar —
 * não há variante para vincular e inventar uma seria pior do que dizer isso à pessoa.
 */
@HiltViewModel
public class WishlistViewModel @Inject constructor(
    private val wishlistRepository: WishlistRepository,
    private val exemplarRepository: ExemplarRepository,
    private val clock: Clock,
) : ViewModel() {

    private val feedback = MutableStateFlow<Pair<String?, String?>>(null to null)

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
            val variantId = item.variantId
            val message = if (variantId == null) {
                wishlistRepository.markAcquired(item.id, null).fold(
                    onSuccess = { ACQUIRED_WITHOUT_EXEMPLAR },
                    onFailure = { it.message ?: GENERIC_ERROR },
                )
            } else {
                exemplarRepository.add(
                    NewExemplar(
                        variantId = variantId,
                        condition = Condition.SEALED,
                        notes = item.notes,
                        acquisition = NewAcquisition(
                            date = IsoDate.today(clock),
                            price = item.foundPrice ?: item.maxPrice,
                            store = item.store,
                        ),
                    ),
                ).fold(
                    onSuccess = { outcome ->
                        wishlistRepository.markAcquired(item.id, outcome.exemplar.id).fold(
                            onSuccess = { ACQUIRED },
                            onFailure = { it.message ?: GENERIC_ERROR },
                        )
                    },
                    onFailure = { it.message ?: GENERIC_ERROR },
                )
            }
            feedback.value = null to message
        }
    }

    public fun onDismiss(id: String) {
        viewModelScope.launch {
            val message = wishlistRepository.dismiss(id).exceptionOrNull()
                ?.message ?: DISMISSED
            feedback.value = null to message
        }
    }

    public fun onMessageShown() {
        feedback.value = feedback.value?.first to null
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val ACQUIRED = "Desejo convertido em exemplar e aquisição."
        const val ACQUIRED_WITHOUT_EXEMPLAR =
            "Desejo marcado como adquirido. Ele não aponta para uma variante, então nenhum exemplar foi criado."
        const val DISMISSED = "Desejo removido da lista em aberto."
        const val GENERIC_ERROR = "Não foi possível concluir a operação."
    }
}
