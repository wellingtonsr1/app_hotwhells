package br.com.diecastcollector.feature.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.Series
import br.com.diecastcollector.ui.components.AdaptiveGrid

/** Marcas de teste das linhas de série (T323). */
public object SeriesTags {
    public const val ROW: String = "series-row"
}

/**
 * Séries do fabricante (T070).
 *
 * Criar, renomear e arquivar. Arquivar não apaga a série nem mexe nas variantes: elas
 * continuam existindo, só perdem o agrupamento.
 */
@Composable
public fun SeriesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SeriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    SeriesContent(
        state = state,
        onBack = onBack,
        onSelectManufacturer = viewModel::onSelectManufacturer,
        onAdd = viewModel::onAdd,
        onRename = viewModel::onRename,
        onArchive = viewModel::onArchive,
        onMessageShown = viewModel::onMessageShown,
        modifier = modifier,
    )
}

@Composable
internal fun SeriesContent(
    state: SeriesUiState,
    onBack: () -> Unit,
    onSelectManufacturer: (String) -> Unit,
    onAdd: (name: String, parentId: String?) -> Unit,
    onRename: (id: String, name: String) -> Unit,
    onArchive: (id: String) -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showAdd by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Series?>(null) }
    var archiveTarget by remember { mutableStateOf<Series?>(null) }

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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.series_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ManufacturerPicker(state, onSelectManufacturer)
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.collections_back))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { showAdd = true },
                    enabled = state.selectedManufacturerId != null,
                ) {
                    Text(stringResource(R.string.series_add))
                }
            }

            when {
                state.loading -> Text(
                    text = stringResource(R.string.collections_loading),
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.items.isEmpty() -> Text(
                    text = stringResource(R.string.series_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> AdaptiveGrid(
                    // Nome da série mais duas ações: 280 dp mantém o cartão legível em
                    // 1 coluna no celular e 2–3 no tablet (T319).
                    minItemSize = 280.dp,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.items, key = { it.id }) { series ->
                        SeriesRow(
                            series = series,
                            onRename = { renameTarget = series },
                            onArchive = { archiveTarget = series },
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        SeriesFormDialog(
            title = stringResource(R.string.series_form_title),
            initialName = "",
            parents = state.items,
            onConfirm = { name, parentId ->
                onAdd(name, parentId)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }

    renameTarget?.let { target ->
        SeriesFormDialog(
            title = stringResource(R.string.series_rename_title),
            initialName = target.name,
            parents = state.items.filter { it.id != target.id },
            onConfirm = { name, _ ->
                onRename(target.id, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    archiveTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { archiveTarget = null },
            title = { Text(stringResource(R.string.series_archive)) },
            text = { Text(stringResource(R.string.series_archive_message)) },
            confirmButton = {
                TextButton(onClick = {
                    archiveTarget = null
                    onArchive(target.id)
                }) {
                    Text(stringResource(R.string.collection_detail_archive))
                }
            },
            dismissButton = {
                TextButton(onClick = { archiveTarget = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun ManufacturerPicker(
    state: SeriesUiState,
    onSelectManufacturer: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = state.manufacturers
        .firstOrNull { it.id == state.selectedManufacturerId }
        ?.name
        ?: stringResource(R.string.series_manufacturer)

    Box {
        TextButton(onClick = { expanded = true }) { Text(selectedName) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.manufacturers.forEach { manufacturer ->
                DropdownMenuItem(
                    text = { Text(manufacturer.name) },
                    onClick = {
                        onSelectManufacturer(manufacturer.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SeriesRow(
    series: Series,
    onRename: () -> Unit,
    onArchive: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().testTag(SeriesTags.ROW)) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = series.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onRename) {
                    Text(stringResource(R.string.collection_detail_edit))
                }
                TextButton(onClick = onArchive) {
                    Text(stringResource(R.string.collection_detail_archive))
                }
            }
        }
    }
}

@Composable
private fun SeriesFormDialog(
    title: String,
    initialName: String,
    parents: List<Series>,
    onConfirm: (name: String, parentId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var parentId by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val parentName = parents.firstOrNull { it.id == parentId }?.name
        ?: stringResource(R.string.series_no_parent)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.series_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(stringResource(R.string.series_parent) + ": " + parentName)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.series_no_parent)) },
                            onClick = {
                                parentId = null
                                expanded = false
                            },
                        )
                        parents.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = {
                                    parentId = option.id
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, parentId) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.series_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
