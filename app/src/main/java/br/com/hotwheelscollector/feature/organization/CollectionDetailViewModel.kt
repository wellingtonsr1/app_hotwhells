package br.com.hotwheelscollector.feature.organization

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.CollectionItemStatus
import br.com.hotwheelscollector.domain.model.CollectionProgress
import br.com.hotwheelscollector.domain.repository.CarModelRepository
import br.com.hotwheelscollector.domain.repository.CollectionRepository
import br.com.hotwheelscollector.domain.repository.ManufacturerRepository
import br.com.hotwheelscollector.domain.repository.VariantRepository
import br.com.hotwheelscollector.navigation.RouteArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public enum class CollectionOptionKind { VARIANT, MODEL }

/** Opção do seletor de itens: variante (a unidade que o colecionador distingue) ou modelo. */
public data class CollectionOption(
    val id: String,
    val label: String,
    val kind: CollectionOptionKind,
)

public data class CollectionDetailUiState(
    val collectionId: String = "",
    val loading: Boolean = true,
    val name: String = "",
    val progress: CollectionProgress? = null,
    val items: List<CollectionItemStatus> = emptyList(),
    val missing: List<CollectionItemStatus> = emptyList(),
    val options: List<CollectionOption> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
    val archived: Boolean = false,
) {
    public val notFound: Boolean get() = !loading && progress == null
}

/**
 * Detalhe da coleção (T071/T072).
 *
 * O progresso e os faltantes vêm do repositório, que usa a mesma regra do painel; a tela
 * não recalcula nada. A lista de opções junta variantes e modelos porque FR-051 permite
 * os dois níveis.
 */
@HiltViewModel
public class CollectionDetailViewModel @Inject constructor(
    private val collections: CollectionRepository,
    variants: VariantRepository,
    models: CarModelRepository,
    manufacturers: ManufacturerRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val collectionId: String = savedStateHandle
        .get<String>(RouteArgs.COLLECTION_ID)
        .orEmpty()

    private val feedback = MutableStateFlow(Feedback())

    private val options = combine(
        variants.observeSummaries(),
        models.observeActive(),
        manufacturers.observeActive(),
    ) { variantRows, modelRows, manufacturerRows ->
        val manufacturerNames = manufacturerRows.associate { it.id to it.name }
        val variantOptions = variantRows.map { summary ->
            CollectionOption(
                id = summary.variantId,
                label = listOfNotNull(
                    summary.manufacturerName,
                    summary.modelName,
                    summary.colorName,
                ).joinToString(" · "),
                kind = CollectionOptionKind.VARIANT,
            )
        }
        val modelOptions = modelRows.map { model ->
            CollectionOption(
                id = model.id,
                label = listOfNotNull(
                    manufacturerNames[model.manufacturerId],
                    model.name,
                    model.scale,
                ).joinToString(" · "),
                kind = CollectionOptionKind.MODEL,
            )
        }
        (variantOptions + modelOptions).sortedBy { it.label.lowercase() }
    }

    public val state: StateFlow<CollectionDetailUiState> = combine(
        collections.observeAll(),
        collections.observeItems(collectionId),
        options,
        feedback,
    ) { all, items, available, status ->
        val progress = all.firstOrNull { it.id == collectionId }
        CollectionDetailUiState(
            collectionId = collectionId,
            loading = false,
            name = progress?.name.orEmpty(),
            progress = progress,
            items = items,
            missing = items.filter { it.isMissing },
            options = available,
            busy = status.busy,
            message = status.message,
            archived = status.archived,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = CollectionDetailUiState(collectionId = collectionId),
    )

    public fun onAdd(option: CollectionOption) {
        when (option.kind) {
            CollectionOptionKind.VARIANT -> run(
                success = ADDED,
            ) { collections.addVariant(collectionId, option.id) }
            CollectionOptionKind.MODEL -> run(
                success = ADDED,
            ) { collections.addModel(collectionId, option.id) }
        }
    }

    public fun onToggleNonCollectible(item: CollectionItemStatus) {
        run(success = null) {
            collections.setNonCollectible(item.itemId, !item.nonCollectible)
        }
    }

    public fun onRemoveItem(item: CollectionItemStatus) {
        run(success = null) { collections.removeItem(item.itemId) }
    }

    public fun onArchive() {
        if (feedback.value.busy) return
        feedback.value = feedback.value.copy(busy = true, message = null)
        viewModelScope.launch {
            collections.archive(collectionId).fold(
                onSuccess = {
                    feedback.value = Feedback(archived = true)
                },
                onFailure = { error ->
                    feedback.value = Feedback(message = error.message ?: GENERIC_ERROR)
                },
            )
        }
    }

    public fun onMessageShown() {
        feedback.update { it.copy(message = null) }
    }

    private fun run(success: String?, block: suspend () -> DomainResult<*>) {
        if (feedback.value.busy) return
        feedback.value = feedback.value.copy(busy = true, message = null)
        viewModelScope.launch {
            feedback.value = block().fold(
                onSuccess = {
                    Feedback(message = success)
                },
                onFailure = { error ->
                    Feedback(message = error.message ?: GENERIC_ERROR)
                },
            )
        }
    }

    private data class Feedback(
        val busy: Boolean = false,
        val message: String? = null,
        val archived: Boolean = false,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val ADDED = "Item adicionado à coleção."
        const val GENERIC_ERROR = "Não foi possível concluir a operação."
    }
}
