package br.com.hotwheelscollector.feature.catalog

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.ExemplarStatus
import br.com.hotwheelscollector.domain.model.Location
import br.com.hotwheelscollector.domain.model.LocationType
import br.com.hotwheelscollector.domain.model.Movement
import br.com.hotwheelscollector.domain.model.PackagingCondition
import br.com.hotwheelscollector.domain.repository.ExemplarDependents
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Edição de um exemplar físico (FR-028, FR-091): condição, embalagem, observações,
 * movimentação com histórico e a escolha entre arquivar e excluir.
 *
 * A exclusão física só é oferecida quando [ExemplarDependents] está vazio; com qualquer
 * vínculo, a tela explica o bloqueio e mantém o arquivamento como caminho (Art. I.6–I.7).
 */
@Composable
public fun ExemplarDetailScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExemplarDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageShown()
        }
    }
    // Arquivou ou excluiu: volta para a lista, que já reflete o novo estado.
    LaunchedEffect(state.finished) {
        if (state.finished) onDone()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        ExemplarDetailContent(
            state = state,
            contentPadding = padding,
            onConditionChange = viewModel::onConditionChange,
            onPackagingChange = viewModel::onPackagingChange,
            onNotesChange = viewModel::onNotesChange,
            onTargetLocationChange = viewModel::onTargetLocationChange,
            onMoveNoteChange = viewModel::onMoveNoteChange,
            onSaveDetails = viewModel::onSaveDetails,
            onMove = viewModel::onMove,
            onArchive = viewModel::onArchive,
            onDelete = viewModel::onDelete,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExemplarDetailContent(
    state: ExemplarDetailUiState,
    contentPadding: PaddingValues,
    onConditionChange: (Condition) -> Unit,
    onPackagingChange: (PackagingCondition?) -> Unit,
    onNotesChange: (String) -> Unit,
    onTargetLocationChange: (String?) -> Unit,
    onMoveNoteChange: (String) -> Unit,
    onSaveDetails: () -> Unit,
    onMove: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var showArchiveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.notFound -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.exemplar_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = state.internalCode,
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (state.isArchived) {
                    Text(
                        text = stringResource(R.string.exemplar_archived_badge),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Text(
                    text = stringResource(R.string.exemplar_condition),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Condition.entries.take(3).forEach { condition ->
                        FilterChip(
                            selected = state.condition == condition,
                            onClick = { onConditionChange(condition) },
                            label = { Text(stringResource(condition.labelRes())) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Condition.entries.drop(3).forEach { condition ->
                        FilterChip(
                            selected = state.condition == condition,
                            onClick = { onConditionChange(condition) },
                            label = { Text(stringResource(condition.labelRes())) },
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.exemplar_packaging),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PackagingCondition.entries.take(2).forEach { packaging ->
                        FilterChip(
                            selected = state.packagingCondition == packaging,
                            onClick = { onPackagingChange(packaging) },
                            label = { Text(stringResource(packaging.labelRes())) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PackagingCondition.entries.drop(2).forEach { packaging ->
                        FilterChip(
                            selected = state.packagingCondition == packaging,
                            onClick = { onPackagingChange(packaging) },
                            label = { Text(stringResource(packaging.labelRes())) },
                        )
                    }
                    FilterChip(
                        selected = state.packagingCondition == null,
                        onClick = { onPackagingChange(null) },
                        label = { Text(stringResource(R.string.packaging_unknown)) },
                    )
                }

                OutlinedTextField(
                    value = state.notes,
                    onValueChange = onNotesChange,
                    label = { Text(stringResource(R.string.exemplar_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = onSaveDetails,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.exemplar_save))
                }

                HorizontalDivider()

                Text(
                    text = stringResource(R.string.exemplar_location),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = locationLabel(state.currentLocationId, state.locations),
                    style = MaterialTheme.typography.bodyMedium,
                )

                var locationMenuOpen by remember { mutableStateOf(false) }
                val targetName = state.targetLocationId?.let { id ->
                    state.locations.firstOrNull { it.id == id }?.name
                } ?: stringResource(R.string.add_exemplar_no_location)
                ExposedDropdownMenuBox(
                    expanded = locationMenuOpen,
                    onExpandedChange = { locationMenuOpen = !locationMenuOpen },
                ) {
                    OutlinedTextField(
                        value = targetName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.exemplar_move_target)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationMenuOpen)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = locationMenuOpen,
                        onDismissRequest = { locationMenuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_exemplar_no_location)) },
                            onClick = {
                                onTargetLocationChange(null)
                                locationMenuOpen = false
                            },
                        )
                        state.locations.forEach { location ->
                            DropdownMenuItem(
                                text = { Text(location.name) },
                                onClick = {
                                    onTargetLocationChange(location.id)
                                    locationMenuOpen = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = state.moveNote,
                    onValueChange = onMoveNoteChange,
                    label = { Text(stringResource(R.string.exemplar_move_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = onMove,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.exemplar_move))
                }

                Text(
                    text = stringResource(R.string.exemplar_history_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                if (state.movements.isEmpty()) {
                    Text(
                        text = stringResource(R.string.exemplar_history_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.movements.forEach { movement ->
                        MovementRow(movement = movement, locations = state.locations)
                    }
                }

                HorizontalDivider()

                OutlinedButton(
                    onClick = { showArchiveDialog = true },
                    enabled = !state.saving && !state.isArchived,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.exemplar_archive))
                }
                TextButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !state.saving && state.canDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.exemplar_delete),
                        color = if (state.canDelete) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                if (!state.canDelete) {
                    Text(
                        text = stringResource(R.string.exemplar_delete_blocked_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showArchiveDialog) {
        ArchiveExemplarDialog(
            onConfirm = {
                showArchiveDialog = false
                onArchive()
            },
            onDismiss = { showArchiveDialog = false },
        )
    }
    if (showDeleteDialog) {
        DeleteExemplarDialog(
            dependents = state.dependents,
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onArchive = {
                showDeleteDialog = false
                onArchive()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun MovementRow(movement: Movement, locations: List<Location>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = MOVEMENT_FORMAT.format(movement.at),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                R.string.exemplar_history_entry,
                locationLabel(movement.fromLocationId, locations),
                locationLabel(movement.toLocationId, locations),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        movement.note?.takeIf { it.isNotBlank() }?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun locationLabel(id: String?, locations: List<Location>): String =
    id?.let { locationId -> locations.firstOrNull { it.id == locationId }?.name }
        ?: stringResource(R.string.add_exemplar_no_location)

/** Rótulo pt-BR da embalagem; o texto mora em `strings.xml` (T131, Art. IV). */
@StringRes
internal fun PackagingCondition.labelRes(): Int = when (this) {
    PackagingCondition.SEALED -> R.string.packaging_sealed
    PackagingCondition.OPENED -> R.string.packaging_opened
    PackagingCondition.DAMAGED -> R.string.packaging_damaged
    PackagingCondition.NONE -> R.string.packaging_none
}

private val MOVEMENT_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())

@Preview(showBackground = true)
@Composable
private fun ExemplarDetailPreview() {
    val now = Instant.parse("2026-11-18T12:00:00Z")
    val location = Location(
        id = "loc1",
        type = LocationType.RACK,
        name = "Estante da sala",
        createdAt = now,
        updatedAt = now,
    )
    HotWheelsTheme {
        ExemplarDetailContent(
            state = ExemplarDetailUiState(
                exemplarId = "e1",
                internalCode = "HW-000001",
                status = ExemplarStatus.OWNED,
                condition = Condition.SEALED,
                packagingCondition = PackagingCondition.SEALED,
                notes = "Caixa com marca de estoque",
                locations = listOf(location),
                currentLocationId = "loc1",
                targetLocationId = "loc1",
                movements = listOf(
                    Movement(
                        id = "mv1",
                        exemplarId = "e1",
                        at = now,
                        fromLocationId = null,
                        toLocationId = "loc1",
                        note = "Cadastro inicial",
                    ),
                ),
                dependents = ExemplarDependents(photos = 2, acquisitions = 1, movements = 1),
                loading = false,
            ),
            contentPadding = PaddingValues(0.dp),
            onConditionChange = {},
            onPackagingChange = {},
            onNotesChange = {},
            onTargetLocationChange = {},
            onMoveNoteChange = {},
            onSaveDetails = {},
            onMove = {},
            onArchive = {},
            onDelete = {},
        )
    }
}
