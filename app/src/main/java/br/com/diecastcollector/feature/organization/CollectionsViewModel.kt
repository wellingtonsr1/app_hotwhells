package br.com.diecastcollector.feature.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.diecastcollector.domain.model.CollectionProgress
import br.com.diecastcollector.domain.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

public data class CollectionsUiState(
    val items: List<CollectionProgress> = emptyList(),
    val loading: Boolean = true,
) {
    public val isEmpty: Boolean get() = !loading && items.isEmpty()
}

/** Lista de coleções com o progresso já calculado pelo repositório (T070). */
@HiltViewModel
public class CollectionsViewModel @Inject constructor(
    collections: CollectionRepository,
) : ViewModel() {

    public val state: StateFlow<CollectionsUiState> = collections.observeAll()
        .map { CollectionsUiState(items = it, loading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = CollectionsUiState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
