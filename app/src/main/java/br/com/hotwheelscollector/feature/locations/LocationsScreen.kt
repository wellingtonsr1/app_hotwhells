package br.com.hotwheelscollector.feature.locations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.LocationArchivePolicy
import br.com.hotwheelscollector.domain.model.LocationType

/**
 * Árvore de localizações (T102) e o diálogo de exclusão com decisão explícita (T104).
 *
 * A exclusão nunca decide pelos exemplares: com exemplares dentro, o colecionador escolhe
 * mover ou arquivar; com sublocalização ativa, a exclusão é recusada e explicada.
 */
@Composable
public fun LocationsScreen(
    onBack: () -> Unit,
    onOpenContents: (locationId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LocationsContent(
        state = state,
        onBack = onBack,
        onOpenContents = onOpenContents,
        onAdd = viewModel::onAdd,
        onRename = viewModel::onRename,
        onArchive = viewModel::onArchive,
        onMessageShown = viewModel::onMessageShown,
        modifier = modifier,
    )
}

@Composable
internal fun LocationsContent(
    state: LocationsUiState,
    onBack: () -> Unit,
    onOpenContents: (String) -> Unit,
    onAdd: (LocationType, String, String?) -> Unit,
    onRename: (String, String) -> Unit,
    onArchive: (String, LocationArchivePolicy) -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var addOpen by remember { mutableStateOf(false) }
    var addParentId by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<LocationRowModel?>(null) }
    var deleteTarget by remember { mutableStateOf<LocationRowModel?>(null) }

    val message = state.message?.asText()
    LaunchedEffect(state.message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                addParentId = null
                addOpen = true
            }) {
                Text(stringResource(R.string.locations_add))
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
                    text = stringResource(R.string.locations_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                TextButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.locations_back))
                }
            }

            when {
                state.loading -> Text(
                    text = stringResource(R.string.locations_loading),
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.isEmpty -> Column(
                    modifier = Modifier.padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.locations_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.locations_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 8.dp,
                        bottom = 96.dp,
                    ),
                ) {
                    items(state.rows, key = { it.id }) { row ->
                        LocationRow(
                            row = row,
                            onOpen = { onOpenContents(row.id) },
                            onAddChild = {
                                addParentId = row.id
                                addOpen = true
                            },
                            onRename = { renameTarget = row },
                            onDelete = { deleteTarget = row },
                        )
                    }
                }
            }
        }
    }

    if (addOpen) {
        AddLocationDialog(
            parentId = addParentId,
            parentName = state.rows.firstOrNull { it.id == addParentId }?.name,
            onConfirm = { type, name ->
                onAdd(type, name, addParentId)
                addOpen = false
            },
            onDismiss = { addOpen = false },
        )
    }

    renameTarget?.let { target ->
        RenameLocationDialog(
            initialName = target.name,
            onConfirm = { name ->
                onRename(target.id, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    deleteTarget?.let { target ->
        DeleteLocationDialog(
            row = target,
            moveTargets = state.moveTargets(target.id),
            onConfirm = { policy ->
                onArchive(target.id, policy)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun LocationRow(
    row: LocationRowModel,
    onOpen: () -> Unit,
    onAddChild: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (row.depth * 16).dp)
            .clickable(onClick = onOpen),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.locations_exemplar_count,
                        row.exemplarCount,
                        row.exemplarCount,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = buildString {
                    append(stringResource(row.type.labelRes()))
                    if (row.hasChildren) {
                        append(" · ")
                        append(stringResource(R.string.locations_subtree_count, row.subtreeCount))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onAddChild) {
                    Text(stringResource(R.string.locations_add_child))
                }
                TextButton(onClick = onRename) {
                    Text(stringResource(R.string.locations_rename))
                }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.locations_delete))
                }
            }
        }
    }
}

@Composable
private fun AddLocationDialog(
    parentId: String?,
    parentName: String?,
    onConfirm: (LocationType, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(LocationType.SHELF) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (parentId == null) {
                        R.string.locations_add_title
                    } else {
                        R.string.locations_add_child_title
                    },
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.locations_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(
                            stringResource(R.string.locations_type) + ": " +
                                stringResource(type.labelRes()),
                        )
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        LocationType.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(stringResource(option.labelRes())) },
                                onClick = {
                                    type = option
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.locations_parent) + ": " +
                        (parentName ?: stringResource(R.string.locations_no_parent)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(type, name) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.locations_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun RenameLocationDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.locations_rename_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.locations_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.locations_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
internal fun DeleteLocationDialog(
    row: LocationRowModel,
    moveTargets: List<LocationRowModel>,
    onConfirm: (LocationArchivePolicy) -> Unit,
    onDismiss: () -> Unit,
) {
    var moveChosen by remember { mutableStateOf(false) }
    var targetId by remember { mutableStateOf<String?>(null) }
    var targetExpanded by remember { mutableStateOf(false) }

    val blocked = row.hasChildren
    // A exclusão só trata os possuídos desta localização; com sublocalização ela nem passa.
    val hasExemplars = row.exemplarCount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.locations_delete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(row.name, style = MaterialTheme.typography.titleMedium)
                when {
                    blocked -> Text(
                        pluralStringResource(
                            R.plurals.locations_delete_blocked,
                            row.childCount,
                            row.childCount,
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                    !hasExemplars -> Text(stringResource(R.string.locations_delete_empty))
                    else -> {
                        Text(
                            pluralStringResource(
                                R.plurals.locations_delete_with_exemplars,
                                row.exemplarCount,
                                row.exemplarCount,
                            ),
                        )
                        // Empilhados: lado a lado, o rótulo de "Mover" ocupa a largura do
                        // diálogo e o de "Arquivar" fica fora da tela.
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = moveChosen,
                                onClick = { moveChosen = true },
                                label = { Text(stringResource(R.string.locations_delete_move)) },
                            )
                            FilterChip(
                                selected = !moveChosen,
                                onClick = { moveChosen = false },
                                label = { Text(stringResource(R.string.locations_delete_archive)) },
                            )
                        }
                        if (moveChosen) {
                            Box {
                                TextButton(onClick = { targetExpanded = true }) {
                                    val label = moveTargets.firstOrNull { it.id == targetId }?.name
                                        ?: stringResource(R.string.locations_move_target)
                                    Text(
                                        stringResource(R.string.locations_move_target) + ": " + label,
                                    )
                                }
                                DropdownMenu(
                                    expanded = targetExpanded,
                                    onDismissRequest = { targetExpanded = false },
                                ) {
                                    moveTargets.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.name) },
                                            onClick = {
                                                targetId = option.id
                                                targetExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!blocked) {
                val enabled = !hasExemplars || (moveChosen && targetId != null) || !moveChosen
                TextButton(
                    onClick = {
                        val policy = when {
                            !hasExemplars -> LocationArchivePolicy.None
                            moveChosen -> LocationArchivePolicy.MoveTo(targetId.orEmpty())
                            else -> LocationArchivePolicy.ArchiveExemplars
                        }
                        onConfirm(policy)
                    },
                    enabled = enabled,
                ) {
                    Text(stringResource(R.string.locations_delete_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        },
    )
}

@Composable
private fun LocationsMessage.asText(): String = when (this) {
    is LocationsMessage.MovedExemplars ->
        pluralStringResource(R.plurals.locations_archived_moved, count, count)
    is LocationsMessage.ArchivedExemplars ->
        pluralStringResource(R.plurals.locations_archived_archived, count, count)
    LocationsMessage.ArchivedLocation -> stringResource(R.string.locations_archived_empty)
    is LocationsMessage.Failure -> message.text()
}

private fun LocationType.labelRes(): Int = when (this) {
    LocationType.ENVIRONMENT -> R.string.location_type_environment
    LocationType.CABINET -> R.string.location_type_cabinet
    LocationType.RACK -> R.string.location_type_rack
    LocationType.SHELF -> R.string.location_type_shelf
    LocationType.CRATE -> R.string.location_type_crate
    LocationType.DRAWER -> R.string.location_type_drawer
    LocationType.DISPLAY -> R.string.location_type_display
    LocationType.COMPARTMENT -> R.string.location_type_compartment
    LocationType.CUSTOM -> R.string.location_type_custom
}
