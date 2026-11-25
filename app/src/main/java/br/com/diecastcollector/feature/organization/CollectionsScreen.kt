package br.com.diecastcollector.feature.organization

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.CollectionProgress
import br.com.diecastcollector.ui.components.AdaptiveGrid
import br.com.diecastcollector.ui.components.ItemThumbnail
import br.com.diecastcollector.ui.components.ProgressBar

/** Marcas de teste dos cartões de coleção (T323). */
public object CollectionTags {
    public const val CARD: String = "collection-card"
}

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
                else -> AdaptiveGrid(
                    // Percentual, nome e barra de progresso pedem cartão largo:
                    // 280 dp dá 1 coluna em 360, 2 em 700 e 3 em 1.000 dp (T317).
                    minItemSize = 280.dp,
                    contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
            .testTag(CollectionTags.CARD)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ItemThumbnail(
                    photoPath = collection.photoPath,
                    contentDescription = stringResource(
                        R.string.catalog_thumbnail_photo,
                        collection.name,
                    ),
                )
                Spacer(Modifier.width(12.dp))
                Row(
                    modifier = Modifier.weight(1f),
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
            }
            if (collection.hasKnownItems) {
                ProgressBar(fraction = collection.percent / 100f)
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
