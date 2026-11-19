package br.com.hotwheelscollector.feature.wishlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.domain.model.MoneyCents
import br.com.hotwheelscollector.domain.model.VariantSummary
import br.com.hotwheelscollector.domain.model.WishlistItem
import br.com.hotwheelscollector.domain.repository.VariantRepository
import br.com.hotwheelscollector.domain.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public data class WishlistFormUiState(
    val editing: Boolean = false,
    val title: String = "",
    val variantQuery: String = "",
    val variantId: String? = null,
    val variantLabel: String? = null,
    val variants: List<VariantSummary> = emptyList(),
    val priority: Int = 2,
    val maxPriceText: String = "",
    val foundPriceText: String = "",
    val store: String = "",
    val referenceUrl: String = "",
    val notes: String = "",
    val saving: Boolean = false,
    val message: String? = null,
    val saved: Boolean = false,
)

/**
 * Formulário de desejo (T076).
 *
 * O vínculo com uma variante é opcional: quem ainda não cadastrou o carro descreve o que
 * procura em texto livre. Ter os dois preenchidos não é erro — a variante é o alvo e o
 * texto livre fica como observação curta na lista.
 */
@HiltViewModel
public class WishlistFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wishlistRepository: WishlistRepository,
    variantRepository: VariantRepository,
    private val clock: Clock,
) : ViewModel() {

    private val itemId: String? = savedStateHandle[ARG_ITEM_ID]

    private val _state = MutableStateFlow(WishlistFormUiState(editing = itemId != null))
    public val state: StateFlow<WishlistFormUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            variantRepository.observeSummaries().collect { summaries ->
                _state.update { current ->
                    current.copy(
                        variants = summaries,
                        variantLabel = current.variantId?.let { id ->
                            summaries.firstOrNull { it.variantId == id }?.label()
                                ?: current.variantLabel
                        },
                    )
                }
            }
        }
        if (itemId != null) {
            viewModelScope.launch {
                wishlistRepository.findById(itemId)?.let { item ->
                    _state.update {
                        it.copy(
                            title = item.freeText.orEmpty(),
                            variantId = item.variantId,
                            priority = item.priority,
                            maxPriceText = item.maxPrice?.formatPtBr().orEmpty(),
                            foundPriceText = item.foundPrice?.formatPtBr().orEmpty(),
                            store = item.store.orEmpty(),
                            referenceUrl = item.referenceUrl.orEmpty(),
                            notes = item.notes.orEmpty(),
                        )
                    }
                } ?: _state.update { it.copy(message = NOT_FOUND) }
            }
        }
    }

    public fun onTitleChange(value: String) = _state.update { it.copy(title = value) }

    public fun onVariantQueryChange(value: String) = _state.update {
        it.copy(variantQuery = value)
    }

    public fun onSelectVariant(summary: VariantSummary) = _state.update {
        it.copy(variantId = summary.variantId, variantLabel = summary.label(), variantQuery = "")
    }

    public fun onClearVariant() = _state.update {
        it.copy(variantId = null, variantLabel = null)
    }

    public fun onPriorityChange(priority: Int) = _state.update { it.copy(priority = priority) }

    public fun onMaxPriceChange(value: String) = _state.update { it.copy(maxPriceText = value) }

    public fun onFoundPriceChange(value: String) = _state.update { it.copy(foundPriceText = value) }

    public fun onStoreChange(value: String) = _state.update { it.copy(store = value) }

    public fun onReferenceUrlChange(value: String) = _state.update { it.copy(referenceUrl = value) }

    public fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    public fun onMessageShown() = _state.update { it.copy(message = null) }

    public fun onSave() {
        val current = _state.value
        if (current.saving) return

        val freeText = current.title.trim().takeIf { it.isNotEmpty() }
        if (current.variantId == null && freeText == null) {
            return _state.update { it.copy(message = TITLE_REQUIRED) }
        }
        val maxPrice = if (current.maxPriceText.isBlank()) {
            null
        } else {
            MoneyCents.parsePtBrOrNull(current.maxPriceText)
                ?: return _state.update { it.copy(message = INVALID_PRICE) }
        }
        val foundPrice = if (current.foundPriceText.isBlank()) {
            null
        } else {
            MoneyCents.parsePtBrOrNull(current.foundPriceText)
                ?: return _state.update { it.copy(message = INVALID_PRICE) }
        }
        val url = current.referenceUrl.trim().takeIf { it.isNotEmpty() }
        if (url != null && !url.startsWith("http://") && !url.startsWith("https://")) {
            return _state.update { it.copy(message = INVALID_URL) }
        }

        val now = Instant.now(clock)
        val item = WishlistItem(
            id = itemId.orEmpty(),
            priority = current.priority,
            variantId = current.variantId,
            freeText = freeText,
            maxPrice = maxPrice,
            foundPrice = foundPrice,
            store = current.store.trim().takeIf { it.isNotEmpty() },
            referenceUrl = url,
            notes = current.notes.trim().takeIf { it.isNotEmpty() },
            createdAt = now,
            updatedAt = now,
        )

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = if (itemId == null) {
                wishlistRepository.add(item)
            } else {
                wishlistRepository.update(item)
            }
            result.fold(
                onSuccess = { _state.update { it.copy(saving = false, saved = true) } },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            saving = false,
                            message = error.message ?: GENERIC_ERROR,
                        )
                    }
                },
            )
        }
    }

    private fun VariantSummary.label(): String = buildString {
        append(manufacturerName)
        append(" · ")
        append(modelName)
        releaseYear?.let {
            append(" · ")
            append(it)
        }
    }

    public companion object {
        public const val ARG_ITEM_ID: String = "itemId"
        private const val TITLE_REQUIRED =
            "Escolha uma variante do catálogo ou descreva o que você procura."
        private const val INVALID_PRICE = "Valor inválido. Use, por exemplo, 49,90."
        private const val INVALID_URL = "O link deve começar com http:// ou https://."
        private const val NOT_FOUND = "Desejo não encontrado."
        private const val GENERIC_ERROR = "Não foi possível salvar o desejo."
    }
}
