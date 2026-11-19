package br.com.hotwheelscollector.feature.locations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.InventoryItem
import br.com.hotwheelscollector.feature.catalog.labelRes

/** Inventário da localização e das sublocalizações (T105/T106). */
@Composable
public fun LocationContentsScreen(
    onBack: () -> Unit,
    onOpenExemplar: (exemplarId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationContentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LocationContentsContent(
        state = state,
        onBack = onBack,
        onOpenExemplar = onOpenExemplar,
        modifier = modifier,
    )
}

@Composable
internal fun LocationContentsContent(
    state: LocationContentsUiState,
    onBack: () -> Unit,
    onOpenExemplar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.name.ifBlank { stringResource(R.string.inventory_title) },
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(top = 12.dp, bottom = 4.dp),
                )
                TextButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.locations_back))
                }
            }

            when {
                state.loading -> Text(
                    text = stringResource(R.string.inventory_loading),
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.items.isEmpty() -> Text(
                    text = stringResource(R.string.inventory_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    Text(
                        text = pluralStringResource(
                            R.plurals.locations_exemplar_count,
                            state.items.size,
                            state.items.size,
                        ) + " · " + stringResource(R.string.inventory_includes_children),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                    ) {
                        items(state.items, key = { it.exemplarId }) { item ->
                            InventoryRow(item = item, onClick = { onOpenExemplar(item.exemplarId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryRow(item: InventoryItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        item.internalCode,
                        item.locationName,
                        stringResource(item.condition.labelRes()),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
