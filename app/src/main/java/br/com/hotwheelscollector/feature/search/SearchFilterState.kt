package br.com.hotwheelscollector.feature.search

import br.com.hotwheelscollector.domain.model.CatalogSort
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.ExemplarStatus
import br.com.hotwheelscollector.domain.model.SearchFilters

/** Modos de visualização do resultado (FR-116–FR-118, T063/T064). */
public enum class CatalogViewMode {
    GRID,
    LIST,
    GALLERY,
    BY_COLLECTION,
    BY_MANUFACTURER,
    BY_SERIES,
}

/** Filtro ativo já com rótulo e ação de remoção (FR-119). */
internal data class ActiveFilter(
    val label: String,
    val onRemove: () -> Unit,
)

internal fun <T> Set<T>.toggle(value: T): Set<T> =
    if (contains(value)) this - value else this + value

internal fun SearchFilters.toggleManufacturer(id: String): SearchFilters =
    copy(manufacturerIds = manufacturerIds.toggle(id))

internal fun SearchFilters.toggleModel(id: String): SearchFilters =
    copy(modelIds = modelIds.toggle(id))

internal fun SearchFilters.toggleSeries(id: String): SearchFilters =
    copy(seriesIds = seriesIds.toggle(id))

internal fun SearchFilters.toggleCollection(id: String): SearchFilters =
    copy(collectionIds = collectionIds.toggle(id))

internal fun SearchFilters.toggleYear(year: Int): SearchFilters =
    copy(years = years.toggle(year))

internal fun SearchFilters.toggleColor(id: String): SearchFilters =
    copy(colorIds = colorIds.toggle(id))

internal fun SearchFilters.toggleScale(scale: String): SearchFilters =
    copy(scales = scales.toggle(scale))

internal fun SearchFilters.toggleCondition(condition: Condition): SearchFilters =
    copy(conditions = conditions.toggle(condition))

internal fun SearchFilters.toggleStatus(status: ExemplarStatus): SearchFilters =
    copy(statuses = statuses.toggle(status))

internal fun SearchFilters.toggleLocation(id: String): SearchFilters =
    copy(locationIds = locationIds.toggle(id))

internal fun SearchFilters.withSort(sort: CatalogSort): SearchFilters = copy(sort = sort)
