package br.com.hotwheelscollector.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.domain.model.VariantSummary
import br.com.hotwheelscollector.domain.repository.VariantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Lista de variantes com a contagem de exemplares que já existe de cada uma. */
public data class CatalogUiState(
    val variants: List<VariantSummary> = emptyList(),
    val loading: Boolean = true,
) {
    public val isEmpty: Boolean get() = !loading && variants.isEmpty()
}

@HiltViewModel
public class CatalogViewModel @Inject constructor(
    variantRepository: VariantRepository,
) : ViewModel() {

    public val state: StateFlow<CatalogUiState> = variantRepository.observeSummaries()
        .map { CatalogUiState(variants = it, loading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = CatalogUiState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
