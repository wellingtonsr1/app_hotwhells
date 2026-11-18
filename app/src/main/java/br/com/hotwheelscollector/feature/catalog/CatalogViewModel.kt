package br.com.hotwheelscollector.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.domain.model.VariantSummary
import br.com.hotwheelscollector.domain.repository.CarModelRepository
import br.com.hotwheelscollector.domain.repository.VariantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Lista de variantes com a contagem de exemplares de cada uma e quantos cadastros ficaram
 * incompletos (modelo sem variante ou variante sem cor/ano). O contador só aparece quando
 * há algo a completar.
 */
public data class CatalogUiState(
    val variants: List<VariantSummary> = emptyList(),
    val loading: Boolean = true,
    val incompleteCount: Int = 0,
) {
    public val isEmpty: Boolean get() = !loading && variants.isEmpty()
    public val hasIncomplete: Boolean get() = incompleteCount > 0
}

@HiltViewModel
public class CatalogViewModel @Inject constructor(
    variantRepository: VariantRepository,
    carModelRepository: CarModelRepository,
) : ViewModel() {

    public val state: StateFlow<CatalogUiState> = combine(
        variantRepository.observeSummaries(),
        carModelRepository.observeIncomplete(),
    ) { variants, incomplete ->
        CatalogUiState(
            variants = variants,
            loading = false,
            incompleteCount = incomplete.size,
        )
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = CatalogUiState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
