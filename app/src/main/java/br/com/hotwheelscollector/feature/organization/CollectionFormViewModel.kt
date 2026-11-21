package br.com.hotwheelscollector.feature.organization

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.repository.CollectionRepository
import br.com.hotwheelscollector.navigation.RouteArgs
import br.com.hotwheelscollector.ui.UiMessage
import br.com.hotwheelscollector.ui.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public data class ParentOption(val id: String, val name: String)

public data class CollectionFormUiState(
    val name: String = "",
    val description: String = "",
    val parentId: String? = null,
    val parents: List<ParentOption> = emptyList(),
    val editing: Boolean = false,
    val saving: Boolean = false,
    val message: UiMessage? = null,
    val saved: Boolean = false,
)

/**
 * Criação e edição de coleção (T070).
 *
 * A proteção contra ciclo não é repetida aqui: quem valida a hierarquia é o repositório,
 * que é o único que enxerga a cadeia inteira. A tela só mostra o erro.
 */
@HiltViewModel
public class CollectionFormViewModel @Inject constructor(
    private val collections: CollectionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val collectionId: String? = savedStateHandle
        .get<String>(RouteArgs.COLLECTION_ID)
        ?.takeIf { it.isNotBlank() }

    private val draft = MutableStateFlow(Draft())
    private val status = MutableStateFlow(Status())

    public val state: StateFlow<CollectionFormUiState> = combine(
        draft,
        collections.observeAll(),
        status,
    ) { current, all, feedback ->
        CollectionFormUiState(
            name = current.name,
            description = current.description,
            parentId = current.parentId,
            parents = all
                .filter { it.id != collectionId }
                .map { ParentOption(it.id, it.name) },
            editing = collectionId != null,
            saving = feedback.saving,
            message = feedback.message,
            saved = feedback.saved,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = CollectionFormUiState(editing = collectionId != null),
    )

    init {
        collectionId?.let { id ->
            viewModelScope.launch {
                collections.findById(id)?.let { collection ->
                    draft.value = Draft(
                        name = collection.name,
                        description = collection.description.orEmpty(),
                        parentId = collection.parentId,
                    )
                }
            }
        }
    }

    public fun onNameChange(value: String) {
        draft.update { it.copy(name = value) }
    }

    public fun onDescriptionChange(value: String) {
        draft.update { it.copy(description = value) }
    }

    public fun onParentChange(parentId: String?) {
        draft.update { it.copy(parentId = parentId) }
    }

    public fun onSave() {
        if (status.value.saving) return
        val current = draft.value
        status.value = Status(saving = true)
        viewModelScope.launch {
            val result = if (collectionId == null) {
                collections.create(
                    name = current.name,
                    description = current.description,
                    parentId = current.parentId,
                ).map { }
            } else {
                collections.update(
                    id = collectionId,
                    name = current.name,
                    description = current.description,
                    parentId = current.parentId,
                )
            }
            status.value = result.fold(
                onSuccess = { Status(saved = true) },
                onFailure = {
                    Status(message = it.toUiMessage(R.string.error_generic_save))
                },
            )
        }
    }

    public fun onMessageShown() {
        status.update { it.copy(message = null) }
    }

    private data class Draft(
        val name: String = "",
        val description: String = "",
        val parentId: String? = null,
    )

    private data class Status(
        val saving: Boolean = false,
        val message: UiMessage? = null,
        val saved: Boolean = false,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
