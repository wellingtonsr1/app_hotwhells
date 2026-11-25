package br.com.diecastcollector.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.CatalogSort
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.domain.model.FilterOption
import br.com.diecastcollector.domain.model.SearchFilters
import br.com.diecastcollector.feature.catalog.labelRes
import br.com.diecastcollector.ui.components.FlowChipGroup
import br.com.diecastcollector.ui.components.LocalWindowSizeClass
import br.com.diecastcollector.ui.components.isExpanded
import br.com.diecastcollector.ui.format.formatMoneyPtBr

/** Marcas de teste da busca (T348). */
internal object SearchTags {
    /** Painel lateral de filtros, usado só na janela expandida. */
    const val FILTER_PANEL: String = "search-filter-panel"
}

/** Largura do painel lateral de filtros em janelas expandidas. */
private val FILTER_PANEL_WIDTH = 320.dp

/** Ações da tela agrupadas para os testes de UI não dependerem do Hilt. */
internal class SearchActions(
    val onQueryChange: (String) -> Unit = {},
    val onOpenFilters: () -> Unit = {},
    val onCloseFilters: () -> Unit = {},
    val onSortChange: (CatalogSort) -> Unit = {},
    val onViewModeChange: (CatalogViewMode) -> Unit = {},
    val onClearAll: () -> Unit = {},
    val onToggleManufacturer: (String) -> Unit = {},
    val onToggleModel: (String) -> Unit = {},
    val onToggleSeries: (String) -> Unit = {},
    val onToggleCollection: (String) -> Unit = {},
    val onToggleYear: (Int) -> Unit = {},
    val onToggleColor: (String) -> Unit = {},
    val onToggleScale: (String) -> Unit = {},
    val onToggleCondition: (Condition) -> Unit = {},
    val onToggleStatus: (ExemplarStatus) -> Unit = {},
    val onToggleLocation: (String) -> Unit = {},
    val onPriceMinChange: (String) -> Unit = {},
    val onPriceMaxChange: (String) -> Unit = {},
    val onApplyPriceRange: () -> Unit = {},
)

@Composable
public fun SearchScreen(
    onBack: () -> Unit,
    onOpenExemplar: (exemplarId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    SearchContent(
        state = state,
        actions = SearchActions(
            onQueryChange = viewModel::onQueryChange,
            onOpenFilters = viewModel::onOpenFilters,
            onCloseFilters = viewModel::onCloseFilters,
            onSortChange = viewModel::onSortChange,
            onViewModeChange = viewModel::onViewModeChange,
            onClearAll = viewModel::onClearAll,
            onToggleManufacturer = viewModel::onToggleManufacturer,
            onToggleModel = viewModel::onToggleModel,
            onToggleSeries = viewModel::onToggleSeries,
            onToggleCollection = viewModel::onToggleCollection,
            onToggleYear = viewModel::onToggleYear,
            onToggleColor = viewModel::onToggleColor,
            onToggleScale = viewModel::onToggleScale,
            onToggleCondition = viewModel::onToggleCondition,
            onToggleStatus = viewModel::onToggleStatus,
            onToggleLocation = viewModel::onToggleLocation,
            onPriceMinChange = viewModel::onPriceMinChange,
            onPriceMaxChange = viewModel::onPriceMaxChange,
            onApplyPriceRange = viewModel::onApplyPriceRange,
        ),
        onBack = onBack,
        onOpenExemplar = onOpenExemplar,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchContent(
    state: SearchUiState,
    actions: SearchActions,
    onBack: () -> Unit,
    onOpenExemplar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = LocalWindowSizeClass.current.isExpanded

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.search_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = actions.onOpenFilters) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = stringResource(R.string.search_filters),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Em janela expandida os filtros dividem a tela com os resultados em vez de
            // cobri-los com uma folha modal (T346, FR-318).
            if (expanded && state.filtersOpen) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(FILTER_PANEL_WIDTH)
                        .testTag(SearchTags.FILTER_PANEL),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    FiltersSheet(state = state, actions = actions)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
            ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = actions.onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { actions.onQueryChange("") }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.search_clear_all),
                            )
                        }
                    }
                },
                singleLine = true,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.search_result_count,
                        state.count,
                        state.count,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
                TextButton(onClick = actions.onOpenFilters) {
                    Text(stringResource(R.string.search_filters))
                }
            }

            val activeFilters = activeFilters(state, actions)
            if (activeFilters.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "clear-all") {
                        AssistChip(
                            onClick = actions.onClearAll,
                            label = { Text(stringResource(R.string.search_clear_all)) },
                        )
                    }
                    items(activeFilters) { filter ->
                        AssistChip(
                            onClick = filter.onRemove,
                            label = { Text(filter.label) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.search_clear_all),
                                )
                            },
                        )
                    }
                }
            }

            // Ordenação e visão quebram linha em vez de rolar na horizontal: em 360 dp
            // com fonte ampliada a rolagem escondia opções (T347, FR-313).
            FlowChipGroup {
                Text(
                    text = stringResource(R.string.search_sort),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 14.dp),
                )
                CatalogSort.entries.forEach { sort ->
                    FilterChip(
                        selected = state.filters.sort == sort,
                        onClick = { actions.onSortChange(sort) },
                        label = { Text(sort.label()) },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            FlowChipGroup {
                CatalogViewMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.viewMode == mode,
                        onClick = { actions.onViewModeChange(mode) },
                        label = { Text(mode.label()) },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.loading -> Unit
                    state.isEmpty -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.search_empty),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.search_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    state.viewMode == CatalogViewMode.LIST -> SearchResultsList(
                        items = state.results,
                        onOpenExemplar = onOpenExemplar,
                    )
                    state.viewMode == CatalogViewMode.GRID -> SearchResultsGrid(
                        items = state.results,
                        onOpenExemplar = onOpenExemplar,
                    )
                    state.viewMode == CatalogViewMode.GALLERY -> SearchResultsGallery(
                        items = state.results,
                        onOpenExemplar = onOpenExemplar,
                    )
                    else -> GroupedSearchResults(
                        items = state.results,
                        mode = state.viewMode,
                        onOpenExemplar = onOpenExemplar,
                    )
                }
            }
            }
        }
    }

    if (state.filtersOpen && !expanded) {
        // São muitos filtros: abrir já expandido evita que a folha pareça cortada.
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = actions.onCloseFilters,
            sheetState = sheetState,
        ) {
            FiltersSheet(state = state, actions = actions)
        }
    }
}

@Composable
private fun CatalogSort.label(): String = stringResource(
    when (this) {
        CatalogSort.NAME -> R.string.search_sort_name
        CatalogSort.YEAR -> R.string.search_sort_year
        CatalogSort.ACQUISITION_DATE -> R.string.search_sort_acquisition
        CatalogSort.VALUE -> R.string.search_sort_value
    },
)

@Composable
private fun CatalogViewMode.label(): String = stringResource(
    when (this) {
        CatalogViewMode.GRID -> R.string.search_view_grid
        CatalogViewMode.LIST -> R.string.search_view_list
        CatalogViewMode.GALLERY -> R.string.search_view_gallery
        CatalogViewMode.BY_COLLECTION -> R.string.search_view_collection
        CatalogViewMode.BY_MANUFACTURER -> R.string.search_view_manufacturer
        CatalogViewMode.BY_SERIES -> R.string.search_view_series
    },
)

/**
 * Converte o estado em chips removíveis (FR-119). Cada chip carrega a ação de desligar
 * só aquele critério; "Limpar tudo" remove todos de uma vez.
 */
@Composable
private fun activeFilters(state: SearchUiState, actions: SearchActions): List<ActiveFilter> {
    val filters: SearchFilters = state.filters
    val list = mutableListOf<ActiveFilter>()

    fun addOptions(selected: Set<String>, options: List<FilterOption>, remove: (String) -> Unit) {
        options.filter { it.id in selected }.forEach { option ->
            list += ActiveFilter(option.label) { remove(option.id) }
        }
    }

    addOptions(filters.manufacturerIds, state.options.manufacturers, actions.onToggleManufacturer)
    addOptions(filters.modelIds, state.options.models, actions.onToggleModel)
    addOptions(filters.seriesIds, state.options.series, actions.onToggleSeries)
    addOptions(filters.collectionIds, state.options.collections, actions.onToggleCollection)
    addOptions(filters.colorIds, state.options.colors, actions.onToggleColor)
    addOptions(filters.locationIds, state.options.locations, actions.onToggleLocation)

    filters.years.forEach { year ->
        list += ActiveFilter(year.toString()) { actions.onToggleYear(year) }
    }
    filters.scales.forEach { scale ->
        list += ActiveFilter(scale) { actions.onToggleScale(scale) }
    }
    filters.conditions.forEach { condition ->
        list += ActiveFilter(condition.labelText()) { actions.onToggleCondition(condition) }
    }
    filters.statuses
        .filter { it != ExemplarStatus.OWNED }
        .forEach { status ->
            list += ActiveFilter(status.labelText()) { actions.onToggleStatus(status) }
        }

    if (filters.priceMinCents != null || filters.priceMaxCents != null) {
        val min = filters.priceMinCents?.let(::formatMoneyPtBr) ?: "—"
        val max = filters.priceMaxCents?.let(::formatMoneyPtBr) ?: "—"
        list += ActiveFilter("$min – $max") {
            actions.onPriceMinChange("")
            actions.onPriceMaxChange("")
            actions.onApplyPriceRange()
        }
    }
    return list
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersSheet(state: SearchUiState, actions: SearchActions) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.search_filters),
            style = MaterialTheme.typography.titleLarge,
        )

        FilterSection(stringResource(R.string.create_entry_manufacturer)) {
            OptionChips(
                options = state.options.manufacturers,
                selected = state.filters.manufacturerIds,
                onToggle = actions.onToggleManufacturer,
            )
        }
        FilterSection(stringResource(R.string.search_filter_model)) {
            OptionChips(
                options = state.options.models,
                selected = state.filters.modelIds,
                onToggle = actions.onToggleModel,
            )
        }
        FilterSection(stringResource(R.string.search_filter_series)) {
            OptionChips(
                options = state.options.series,
                selected = state.filters.seriesIds,
                onToggle = actions.onToggleSeries,
            )
        }
        FilterSection(stringResource(R.string.search_filter_collection)) {
            OptionChips(
                options = state.options.collections,
                selected = state.filters.collectionIds,
                onToggle = actions.onToggleCollection,
            )
        }
        FilterSection(stringResource(R.string.create_entry_year)) {
            OptionChips(
                options = state.options.years.map { FilterOption(it.toString(), it.toString()) },
                selected = state.filters.years.map { it.toString() }.toSet(),
                onToggle = { actions.onToggleYear(it.toInt()) },
            )
        }
        FilterSection(stringResource(R.string.create_entry_color)) {
            OptionChips(
                options = state.options.colors,
                selected = state.filters.colorIds,
                onToggle = actions.onToggleColor,
            )
        }
        FilterSection(stringResource(R.string.create_entry_scale)) {
            OptionChips(
                options = state.options.scales.map { FilterOption(it, it) },
                selected = state.filters.scales,
                onToggle = actions.onToggleScale,
            )
        }
        FilterSection(stringResource(R.string.exemplar_condition)) {
            OptionChips(
                options = Condition.entries.map { FilterOption(it.name, it.labelText()) },
                selected = state.filters.conditions.map { it.name }.toSet(),
                onToggle = { name ->
                    Condition.entries.firstOrNull { it.name == name }
                        ?.let(actions.onToggleCondition)
                },
            )
        }
        FilterSection(stringResource(R.string.search_filter_status)) {
            OptionChips(
                options = ExemplarStatus.entries.map { FilterOption(it.name, it.labelText()) },
                selected = state.filters.statuses.map { it.name }.toSet(),
                onToggle = { name ->
                    ExemplarStatus.entries.firstOrNull { it.name == name }
                        ?.let(actions.onToggleStatus)
                },
            )
        }
        FilterSection(stringResource(R.string.add_exemplar_location)) {
            OptionChips(
                options = state.options.locations,
                selected = state.filters.locationIds,
                onToggle = actions.onToggleLocation,
            )
        }
        FilterSection(stringResource(R.string.search_filter_price)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.priceMinText,
                    onValueChange = actions.onPriceMinChange,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.search_price_min)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = state.priceError != null,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.priceMaxText,
                    onValueChange = actions.onPriceMaxChange,
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.search_price_max)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = state.priceError != null,
                    singleLine = true,
                )
            }
            state.priceError?.let { error ->
                Text(
                    text = error.text(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(onClick = actions.onApplyPriceRange) {
                Text(stringResource(R.string.search_price_apply))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = actions.onClearAll, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.search_clear_filters))
            }
            Button(onClick = actions.onCloseFilters, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.search_close))
            }
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 8.dp),
    )
    content()
}

@Composable
private fun OptionChips(
    options: List<FilterOption>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    if (options.isEmpty()) {
        Text(
            text = stringResource(R.string.search_filter_none),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    FlowChipGroup {
        options.forEach { option ->
            FilterChip(
                selected = option.id in selected,
                onClick = { onToggle(option.id) },
                label = { Text(option.label) },
            )
        }
    }
}
