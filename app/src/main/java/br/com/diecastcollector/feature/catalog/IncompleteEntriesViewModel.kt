package br.com.diecastcollector.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.diecastcollector.domain.model.IncompleteEntry
import br.com.diecastcollector.domain.repository.CarModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Cadastros incompletos: modelo sem variante e variante sem cor principal ou sem ano. */
public data class IncompleteEntriesUiState(
    val entries: List<IncompleteEntry> = emptyList(),
    val loading: Boolean = true,
) {
    public val isEmpty: Boolean get() = !loading && entries.isEmpty()
}

@HiltViewModel
public class IncompleteEntriesViewModel @Inject constructor(
    carModelRepository: CarModelRepository,
) : ViewModel() {

    public val state: StateFlow<IncompleteEntriesUiState> =
        carModelRepository.observeIncomplete()
            .map { IncompleteEntriesUiState(entries = it, loading = false) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = IncompleteEntriesUiState(),
            )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
