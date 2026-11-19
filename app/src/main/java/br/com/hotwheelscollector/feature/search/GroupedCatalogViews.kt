package br.com.hotwheelscollector.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.SearchResultItem

/**
 * Resultado agrupado por coleção, fabricante ou série (FR-116).
 *
 * O agrupamento é feito depois da consulta: a ordem de chegada já reflete a ordenação
 * escolhida, e `groupBy` preserva a primeira aparição de cada chave. Os grupos recebem
 * a contagem para o colecionador saber onde há mais exemplares.
 */
@Composable
internal fun GroupedSearchResults(
    items: List<SearchResultItem>,
    mode: CatalogViewMode,
    onOpenExemplar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyOf: (SearchResultItem) -> String? = when (mode) {
        CatalogViewMode.BY_COLLECTION -> { item -> item.collectionName }
        CatalogViewMode.BY_MANUFACTURER -> { item -> item.manufacturerName }
        CatalogViewMode.BY_SERIES -> { item -> item.seriesName }
        CatalogViewMode.GRID, CatalogViewMode.LIST, CatalogViewMode.GALLERY -> { _ -> null }
    }
    val noneLabelRes = when (mode) {
        CatalogViewMode.BY_COLLECTION -> R.string.search_filter_collection
        CatalogViewMode.BY_SERIES -> R.string.search_filter_series
        else -> R.string.create_entry_manufacturer
    }
    val groups = remember(items, mode) {
        items.groupBy(keyOf).toList().sortedWith(
            compareBy(nullsLast()) { (key, _) -> key },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        groups.forEach { (key, groupItems) ->
            item(key = "header-${key ?: "none"}") {
                val label = key ?: stringResource(R.string.search_group_none, stringResource(noneLabelRes))
                Text(
                    text = stringResource(R.string.search_group_header, label, groupItems.size),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(groupItems, key = { it.exemplarId }) { item ->
                SearchResultCard(item = item, onOpen = { onOpenExemplar(item.exemplarId) })
            }
        }
    }
}
