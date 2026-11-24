package br.com.diecastcollector.feature.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.CollectionItemStatus
import br.com.diecastcollector.ui.components.AdaptableCardActions
import br.com.diecastcollector.ui.components.AdaptiveGrid
import br.com.diecastcollector.ui.components.CardAction
import br.com.diecastcollector.ui.components.ItemThumbnail

/** Marcas de teste dos itens da coleção (T323). */
public object CollectionDetailTags {
    public const val ITEM: String = "collection-item"
}

/**
 * Detalhe da coleção (T071, T072).
 *
 * Duas listas com papéis diferentes: "faltando" responde o que comprar; "itens" permite
 * marcar o que não é colecionável ou remover. Marcar como não colecionável não apaga nada —
 * só tira o item do denominador do progresso.
 */
@Composable
public fun CollectionDetailScreen(
    onBack: () -> Unit,
    onEdit: (collectionId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.archived) {
        if (state.archived) onBack()
    }

    CollectionDetailContent(
        state = state,
        onBack = onBack,
        onEdit = { onEdit(state.collectionId) },
        onAdd = viewModel::onAdd,
        onToggleNonCollectible = viewModel::onToggleNonCollectible,
        onRemoveItem = viewModel::onRemoveItem,
        onArchive = viewModel::onArchive,
        onMessageShown = viewModel::onMessageShown,
        modifier = modifier,
    )
}

@Composable
internal fun CollectionDetailContent(
    state: CollectionDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAdd: (CollectionOption) -> Unit,
    onToggleNonCollectible: (CollectionItemStatus) -> Unit,
    onRemoveItem: (CollectionItemStatus) -> Unit,
    onArchive: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showPicker by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }

    val messageText = state.message?.text()

    LaunchedEffect(state.message) {
        messageText?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.progress != null) {
                FloatingActionButton(onClick = { showPicker = true }) {
                    Text(stringResource(R.string.collection_detail_add_item))
                }
            }
        },
    ) { padding ->
        when {
            state.loading -> Text(
                text = stringResource(R.string.collections_loading),
                modifier = Modifier.padding(padding).padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            state.notFound -> Column(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.collection_detail_not_found),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.collections_back))
                }
            }
            else -> AdaptiveGrid(
                modifier = Modifier.padding(padding),
                // O cabeçalho ocupa a linha toda; os itens da coleção ganham a segunda
                // coluna a partir da janela média e param nela mesmo em 1.000 dp, porque
                // o cartão tem foto, nome, contagem e duas ações (T318, FR-307).
                minItemSize = 320.dp,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Header(state, onBack, onEdit, onArchive = { confirmArchive = true })
                }
                item(span = { GridItemSpan(maxLineSpan) }) { ProgressCard(state) }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(
                            R.string.collection_detail_missing_title,
                            state.missing.size,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (state.missing.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.collection_detail_missing_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(state.missing, key = { "missing-${it.itemId}" }) { item ->
                        MissingRow(item)
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.collection_detail_items_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (state.items.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(R.string.collection_detail_items_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(state.items, key = { it.itemId }) { item ->
                        ItemRow(
                            item = item,
                            enabled = !state.busy,
                            onToggleNonCollectible = { onToggleNonCollectible(item) },
                            onRemove = { onRemoveItem(item) },
                        )
                    }
                }
            }
        }
    }

    if (showPicker) {
        CollectionItemPicker(
            options = state.options,
            onPick = { option ->
                onAdd(option)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }

    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text(stringResource(R.string.collection_detail_archive_title)) },
            text = { Text(stringResource(R.string.collection_detail_archive_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmArchive = false
                    onArchive()
                }) {
                    Text(stringResource(R.string.collection_detail_archive))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmArchive = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun Header(
    state: CollectionDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.name,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(
                    R.string.collections_percent,
                    state.progress?.percent ?: 0,
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        // Três ações no cabeçalho: em 360 dp com fonte ampliada elas passam a menu em
        // vez de sair da tela, e o título fica com a largura que sobra (T318, FR-309).
        AdaptableCardActions(
            modifier = Modifier.fillMaxWidth(),
            actions = listOf(
                CardAction(stringResource(R.string.collections_back), onBack),
                CardAction(stringResource(R.string.collection_detail_edit), onEdit),
                CardAction(stringResource(R.string.collection_detail_archive), onArchive),
            ),
        )
    }
}

@Composable
private fun ProgressCard(state: CollectionDetailUiState) {
    val progress = state.progress ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ProgressBar(percent = progress.percent)
            Text(
                text = stringResource(
                    R.string.collections_progress,
                    progress.owned,
                    progress.known,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.collection_detail_units, state.missing.size, progress.itemCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MissingRow(item: CollectionItemStatus) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(
                    R.string.collection_detail_units,
                    item.ownedUnits,
                    item.expected,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ItemRow(
    item: CollectionItemStatus,
    enabled: Boolean,
    onToggleNonCollectible: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().testTag(CollectionDetailTags.ITEM)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A foto vem do exemplar possuído do item (o DAO pega a principal, senão a
            // primeira da variante/modelo). Item sem exemplar possuído mostra o marcador
            // neutro: a linha continua legível e diz "0 de 1".
            ItemThumbnail(
                photoPath = item.photoPath,
                contentDescription = stringResource(R.string.catalog_thumbnail_photo, item.label),
                size = 64.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(
                            R.string.collection_detail_units,
                            item.ownedUnits,
                            item.expected,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onToggleNonCollectible, enabled = enabled) {
                        Text(
                            text = stringResource(
                                if (item.nonCollectible) {
                                    R.string.collection_detail_unmark_non_collectible
                                } else {
                                    R.string.collection_detail_mark_non_collectible
                                },
                            ),
                        )
                    }
                    TextButton(onClick = onRemove, enabled = enabled) {
                        Text(stringResource(R.string.collection_detail_remove))
                    }
                }
            }
        }
    }
}
