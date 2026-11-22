package br.com.diecastcollector.feature.wishlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.WishlistItem
import br.com.diecastcollector.domain.model.WishlistStatus
import br.com.diecastcollector.domain.repository.WishlistEntry
import br.com.diecastcollector.ui.components.ItemThumbnail
import br.com.diecastcollector.ui.format.formatMoneyPtBr

/**
 * Lista de desejos (T077).
 *
 * O que está em aberto vem primeiro, por prioridade. Cada cartão permite converter em
 * aquisição, abrir o link de referência, editar ou desistir; o histórico continua visível
 * com o status em vez de desaparecer.
 */
@Composable
public fun WishlistScreen(
    onAdd: () -> Unit,
    onEdit: (itemId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WishlistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val messageText = state.message?.text()

    LaunchedEffect(state.message) {
        messageText?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        WishlistContent(
            state = state,
            onAdd = onAdd,
            onEdit = onEdit,
            onBuy = viewModel::onBuy,
            onDismiss = viewModel::onDismiss,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
internal fun WishlistContent(
    state: WishlistUiState,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onBuy: (WishlistEntry) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.wishlist_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Button(onClick = onAdd) {
                Text(stringResource(R.string.wishlist_add))
            }
        }

        when {
            state.loading -> Text(
                text = stringResource(R.string.wishlist_loading),
                style = MaterialTheme.typography.bodyMedium,
            )
            state.items.isEmpty() -> Column(
                modifier = Modifier.padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.wishlist_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.wishlist_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.item.id }) { entry ->
                    WishlistCard(
                        entry = entry,
                        working = state.workingId == entry.item.id,
                        onEdit = { onEdit(entry.item.id) },
                        onBuy = { onBuy(entry) },
                        onDismiss = { onDismiss(entry.item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WishlistCard(
    entry: WishlistEntry,
    working: Boolean,
    onEdit: () -> Unit,
    onBuy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val item = entry.item
    val open = item.status == WishlistStatus.WANTED
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = if (open) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val title = entry.title.ifBlank { stringResource(R.string.wishlist_untitled) }
            ItemThumbnail(
                photoPath = entry.photoPath,
                contentDescription = stringResource(R.string.catalog_thumbnail_photo, title),
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = priorityLabel(item.priority),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Text(
                    text = statusLabel(item.status),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                PriceLine(item = item)

                item.store?.let { store ->
                    Text(
                        text = stringResource(R.string.wishlist_store_value, store),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (open) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextButton(onClick = onBuy, enabled = !working) {
                            Text(stringResource(R.string.wishlist_buy))
                        }
                        item.referenceUrl?.let { url ->
                            TextButton(onClick = { openReferenceLink(context, url) }) {
                                Text(stringResource(R.string.wishlist_open_link))
                            }
                        }
                        TextButton(onClick = onDismiss, enabled = !working) {
                            Text(stringResource(R.string.wishlist_dismiss))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceLine(item: WishlistItem) {
    val max = item.maxPrice?.let { formatMoneyPtBr(it.cents) }
    val found = item.foundPrice?.let { formatMoneyPtBr(it.cents) }
    val text = when {
        max != null && found != null ->
            stringResource(R.string.wishlist_prices_both, max, found)
        max != null -> stringResource(R.string.wishlist_price_max, max)
        found != null -> stringResource(R.string.wishlist_price_found, found)
        else -> null
    }
    text?.let {
        Text(text = it, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun priorityLabel(priority: Int): String = when (priority) {
    1 -> stringResource(R.string.wishlist_priority_high)
    3 -> stringResource(R.string.wishlist_priority_low)
    else -> stringResource(R.string.wishlist_priority_medium)
}

@Composable
private fun statusLabel(status: WishlistStatus): String = when (status) {
    WishlistStatus.WANTED -> stringResource(R.string.wishlist_status_wanted)
    WishlistStatus.ACQUIRED -> stringResource(R.string.wishlist_status_acquired)
    WishlistStatus.DISMISSED -> stringResource(R.string.wishlist_status_dismissed)
}
