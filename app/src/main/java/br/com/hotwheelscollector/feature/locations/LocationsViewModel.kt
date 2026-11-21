package br.com.hotwheelscollector.feature.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.domain.model.LocationArchivePolicy
import br.com.hotwheelscollector.domain.model.LocationNode
import br.com.hotwheelscollector.domain.model.LocationType
import br.com.hotwheelscollector.domain.repository.LocationRepository
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

/** Linha plana da árvore, com profundidade para a tela indentar. */
public data class LocationRowModel(
    val id: String,
    val name: String,
    val type: LocationType,
    val depth: Int,
    val exemplarCount: Int,
    val subtreeCount: Int,
    val hasChildren: Boolean,
    val childCount: Int = 0,
)

/**
 * Resultado de um comando, sem texto: o que dizer e com qual número a tela resolve no
 * `strings.xml`, onde os plurais ficam em pt-BR (`exemplar`/`exemplares`).
 */
public sealed interface LocationsMessage {
    public data class MovedExemplars(val count: Int) : LocationsMessage
    public data class ArchivedExemplars(val count: Int) : LocationsMessage
    public data object ArchivedLocation : LocationsMessage
    public data class Failure(val message: UiMessage) : LocationsMessage
}

public data class LocationsUiState(
    val rows: List<LocationRowModel> = emptyList(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val message: LocationsMessage? = null,
) {
    public val isEmpty: Boolean get() = !loading && rows.isEmpty()

    /** Candidatos a destino de movimentação, excluindo a própria e as descendentes. */
    public fun moveTargets(locationId: String): List<LocationRowModel> {
        val target = rows.firstOrNull { it.id == locationId } ?: return emptyList()
        val descendants = descendantsOf(target)
        return rows.filter { it.id != target.id && it.id !in descendants }
    }

    public fun descendantsOf(location: LocationRowModel): Set<String> {
        val start = rows.indexOfFirst { it.id == location.id }
        if (start < 0) return emptySet()
        val result = mutableSetOf<String>()
        for (index in start + 1 until rows.size) {
            val row = rows[index]
            if (row.depth <= location.depth) break
            result += row.id
        }
        return result
    }
}

/**
 * Árvore de localizações (T102).
 *
 * A árvore chega montada do repositório; aqui só é achatada com profundidade. A exclusão
 * não decide nada sozinha: a tela manda a política e o repositório recusa quando há
 * sublocalização ou exemplar sem destino (FR-093).
 */
@HiltViewModel
public class LocationsViewModel @Inject constructor(
    private val locations: LocationRepository,
) : ViewModel() {

    private val feedback = MutableStateFlow(Feedback())

    public val state: StateFlow<LocationsUiState> = combine(
        locations.observeTree(),
        feedback,
    ) { tree, status ->
        LocationsUiState(
            rows = tree.flatten(),
            loading = false,
            busy = status.busy,
            message = status.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = LocationsUiState(),
    )

    public fun onAdd(type: LocationType, name: String, parentId: String?) {
        run { locations.add(type, name, parentId) }
    }

    public fun onRename(id: String, name: String) {
        run { locations.rename(id, name) }
    }

    /**
     * A contagem da mensagem vem do repositório, não da tela: o que a linha mostra é o
     * acervo da subárvore, e o repositório trata exatamente os exemplares possuídos ali.
     * Usar o número da tela faria a mensagem anunciar quantos itens ela achava que havia.
     */
    public fun onArchive(id: String, policy: LocationArchivePolicy) {
        run(success = { handled -> archiveMessage(policy, handled) }) {
            locations.archive(id, policy)
        }
    }

    public fun onMessageShown() {
        feedback.update { it.copy(message = null) }
    }

    /** Um caminho só para os comandos: trava enquanto ocupa, traduz falha e sucesso. */
    private fun <T> run(
        success: (T) -> LocationsMessage? = { null },
        block: suspend () -> Result<T>,
    ) {
        if (feedback.value.busy) return
        feedback.value = Feedback(busy = true)
        viewModelScope.launch {
            feedback.value = block().fold(
                onSuccess = { Feedback(message = success(it)) },
                onFailure = {
                    Feedback(message = LocationsMessage.Failure(it.toUiMessage()))
                },
            )
        }
    }

    private fun archiveMessage(policy: LocationArchivePolicy, handled: Int): LocationsMessage =
        when (policy) {
            is LocationArchivePolicy.MoveTo -> LocationsMessage.MovedExemplars(handled)
            LocationArchivePolicy.ArchiveExemplars ->
                LocationsMessage.ArchivedExemplars(handled)
            LocationArchivePolicy.None -> LocationsMessage.ArchivedLocation
        }

    private data class Feedback(
        val busy: Boolean = false,
        val message: LocationsMessage? = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private fun List<LocationNode>.flatten(
    depth: Int = 0,
    out: MutableList<LocationRowModel> = mutableListOf(),
): List<LocationRowModel> {
    forEach { node ->
        out += LocationRowModel(
            id = node.id,
            name = node.location.name,
            type = node.location.type,
            depth = depth,
            exemplarCount = node.exemplarCount,
            subtreeCount = node.subtreeExemplarCount,
            hasChildren = node.children.isNotEmpty(),
            childCount = node.children.size,
        )
        node.children.flatten(depth + 1, out)
    }
    return out
}
