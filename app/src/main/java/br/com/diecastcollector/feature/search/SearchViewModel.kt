package br.com.diecastcollector.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.CatalogSort
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.model.SearchFilterOptions
import br.com.diecastcollector.domain.model.SearchFilters
import br.com.diecastcollector.domain.model.SearchResultItem
import br.com.diecastcollector.domain.repository.SearchRepository
import br.com.diecastcollector.ui.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

public data class SearchUiState(
    val query: String = "",
    val filters: SearchFilters = SearchFilters(),
    val options: SearchFilterOptions = SearchFilterOptions(),
    val results: List<SearchResultItem> = emptyList(),
    val viewMode: CatalogViewMode = CatalogViewMode.GRID,
    val filtersOpen: Boolean = false,
    val priceMinText: String = "",
    val priceMaxText: String = "",
    val priceError: UiMessage? = null,
    val loading: Boolean = true,
) {
    public val count: Int get() = results.size
    public val isEmpty: Boolean get() = !loading && results.isEmpty()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
public class SearchViewModel @Inject constructor(
    searchRepository: SearchRepository,
) : ViewModel() {

    private data class FormState(
        val viewMode: CatalogViewMode = CatalogViewMode.GRID,
        val filtersOpen: Boolean = false,
        val priceMinText: String = "",
        val priceMaxText: String = "",
        val priceError: UiMessage? = null,
    )

    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(SearchFilters())
    private val form = MutableStateFlow(FormState())

    private val effectiveFilters = combine(query.debounce(DEBOUNCE_MS), filters) { text, current ->
        current.copy(query = text)
    }.distinctUntilChanged()

    private val results = effectiveFilters.flatMapLatest(searchRepository::observeResults)

    public val state: StateFlow<SearchUiState> = combine(
        results,
        searchRepository.observeOptions(),
        query,
        filters,
        form,
    ) { items, options, text, activeFilters, formState ->
        SearchUiState(
            query = text,
            filters = activeFilters,
            options = options,
            results = items,
            viewMode = formState.viewMode,
            filtersOpen = formState.filtersOpen,
            priceMinText = formState.priceMinText,
            priceMaxText = formState.priceMaxText,
            priceError = formState.priceError,
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SearchUiState(),
    )

    public fun onQueryChange(value: String) {
        query.value = value
    }

    public fun onFiltersChange(value: SearchFilters) {
        filters.value = value
    }

    public fun onViewModeChange(mode: CatalogViewMode) =
        form.update { it.copy(viewMode = mode) }

    public fun onOpenFilters() = form.update { it.copy(filtersOpen = true) }

    public fun onCloseFilters() = form.update { it.copy(filtersOpen = false) }

    public fun onSortChange(sort: CatalogSort) {
        filters.update { it.withSort(sort) }
    }

    public fun onToggleManufacturer(id: String) =
        filters.update { it.toggleManufacturer(id) }

    public fun onToggleModel(id: String) = filters.update { it.toggleModel(id) }

    public fun onToggleSeries(id: String) = filters.update { it.toggleSeries(id) }

    public fun onToggleCollection(id: String) =
        filters.update { it.toggleCollection(id) }

    public fun onToggleYear(year: Int) = filters.update { it.toggleYear(year) }

    public fun onToggleColor(id: String) = filters.update { it.toggleColor(id) }

    public fun onToggleScale(scale: String) = filters.update { it.toggleScale(scale) }

    public fun onToggleCondition(condition: Condition) =
        filters.update { it.toggleCondition(condition) }

    public fun onToggleStatus(status: ExemplarStatus) =
        filters.update { it.toggleStatus(status) }

    public fun onToggleLocation(id: String) =
        filters.update { it.toggleLocation(id) }

    public fun onPriceMinChange(value: String) =
        form.update { it.copy(priceMinText = value, priceError = null) }

    public fun onPriceMaxChange(value: String) =
        form.update { it.copy(priceMaxText = value, priceError = null) }

    /** Aplica a faixa digitada; texto vazio significa "sem limite" naquela ponta. */
    public fun onApplyPriceRange() {
        val current = form.value
        val min = parsePriceOrNull(current.priceMinText)
        val max = parsePriceOrNull(current.priceMaxText)
        if (current.priceMinText.isNotBlank() && min == null ||
            current.priceMaxText.isNotBlank() && max == null
        ) {
            form.update {
                it.copy(priceError = UiMessage(R.string.search_invalid_price))
            }
            return
        }
        filters.update { it.copy(priceMinCents = min, priceMaxCents = max) }
        form.update { it.copy(priceError = null) }
    }

    /** Limpa pesquisa, filtros e faixa numa única ação (FR-119). */
    public fun onClearAll() {
        query.value = ""
        filters.value = SearchFilters()
        form.update {
            it.copy(priceMinText = "", priceMaxText = "", priceError = null)
        }
    }

    private fun parsePriceOrNull(raw: String): Long? =
        if (raw.isBlank()) null else MoneyCents.parsePtBrOrNull(raw)?.cents

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val DEBOUNCE_MS = 250L
    }
}
