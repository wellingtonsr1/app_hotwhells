package br.com.hotwheelscollector.feature.organization

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.CollectionProgress

/**
 * Coleções (T070).
 *
 * Cada cartão mostra o percentual e quantas unidades já estão na estante. A tela não
 * calcula nada: o progresso vem do `CollectionProgressCalculator`, o mesmo que o painel usa.
 */
@Composable
public fun CollectionsScreen(
    onBack: () -> Unit,
    onOpenCollection: (collectionId: String) -> Unit,
    onCreate: () -> Unit,
    onOpenSeries: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    CollectionsContent(
        state = state,
        onBack = onBack,
        onOpenCollection = onOpenCollection,
        onCreate = onCreate,
        onOpenSeries = onOpenSeries,
        modifier = modifier,
    )
}

@Composable
internal fun CollectionsContent(
    state: CollectionsUiState,
    onBack: () -> Unit,
    onOpenCollection: (String) -> Unit,
    onCreate: () -> Unit,
    onOpenSeries: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Text(stringResource(R.string.collections_add))
            }
        },
    ) { padding ->
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
                    text = stringResource(R.string.collections_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onOpenSeries, modifier = Modifier.padding(top = 12.dp)) {
                        Text(stringResource(R.string.collections_series_action))
                    }
                    TextButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) {
                        Text(stringResource(R.string.collections_back))
                    }
                }
            }

            when {
                state.loading -> Text(
                    text = stringResource(R.string.collections_loading),
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.isEmpty -> EmptyCollections()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 8.dp,
                        bottom = 96.dp,
                    ),
                ) {
                    items(state.items, key = { it.id }) { collection ->
                        CollectionCard(
                            collection = collection,
                            onClick = { onOpenCollection(collection.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionCard(
    collection: CollectionProgress,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.collections_percent, collection.percent),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (collection.hasKnownItems) {
                ProgressBar(percent = collection.percent)
                Text(
                    text = stringResource(
                        R.string.collections_progress,
                        collection.owned,
                        collection.known,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.collections_no_items),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Barra simples: evita depender da assinatura de `LinearProgressIndicator` do Material3. */
@Composable
internal fun ProgressBar(percent: Int, modifier: Modifier = Modifier) {
    val fraction = (percent.coerceIn(0, 100)) / 100f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun EmptyCollections() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.collections_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.collections_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
