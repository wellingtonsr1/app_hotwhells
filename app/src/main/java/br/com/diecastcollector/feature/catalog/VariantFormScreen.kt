package br.com.diecastcollector.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.PhotoAngle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/**
 * Cadastro completo de variante (T044 - US2, FR-014, FR-020, FR-040).
 *
 * Junto com a variante nasce o primeiro exemplar, que é quem recebe as fotos: no schema
 * `photo` referencia `exemplar`, e uma foto não pode existir solta na variante. O
 * `modelId` vem da tela anterior — a tela não inventa identificador.
 */
@Composable
public fun VariantFormScreen(
    modelId: String,
    onVariantSaved: (variantId: String) -> Unit,
    modifier: Modifier = Modifier,
    initialManufacturerCode: String? = null,
    viewModel: VariantFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Código lido no reconhecimento: entra uma vez, sem sobrescrever digitação (FR-112).
    LaunchedEffect(initialManufacturerCode) {
        viewModel.prefillManufacturerCode(initialManufacturerCode)
    }

    LaunchedEffect(state.savedVariantId) {
        state.savedVariantId?.let(onVariantSaved)
    }

    VariantFormContent(
        state = state,
        onPrimaryColorChange = viewModel::onPrimaryColorChange,
        onSecondaryColorsChange = viewModel::onSecondaryColorsChange,
        onWheelsTypeChange = viewModel::onWheelsTypeChange,
        onBaseTypeChange = viewModel::onBaseTypeChange,
        onDecorationChange = viewModel::onDecorationChange,
        onSeriesChange = viewModel::onSeriesChange,
        onReleaseYearChange = viewModel::onReleaseYearChange,
        onManufacturerCodeChange = viewModel::onManufacturerCodeChange,
        onConditionChange = viewModel::onConditionChange,
        onAddPhoto = viewModel::onAddPhoto,
        onSetPrimary = viewModel::onSetPrimary,
        onRemovePhoto = viewModel::onRemovePhoto,
        onSave = { viewModel.onSave(modelId) },
        onMessageShown = viewModel::onMessageShown,
        showExemplarSection = true,
        modifier = modifier,
    )
}

/**
 * Completa uma variante que ficou sem cor principal ou sem ano (FR-029).
 *
 * É o mesmo formulário, com o bloco do exemplar escondido: a variante já tem seus
 * exemplares, e completar um campo não pode criar uma segunda unidade física. A tela só
 * existe depois que a lista de incompletos entrega o `variantId`.
 */
@Composable
public fun VariantEditScreen(
    variantId: String,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VariantFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(variantId) { viewModel.loadForEdit(variantId) }
    LaunchedEffect(state.savedVariantId) {
        if (state.savedVariantId != null) onSaved()
    }
    LaunchedEffect(state.variantClosed) {
        if (state.variantClosed) onSaved()
    }

    VariantFormContent(
        state = state,
        onPrimaryColorChange = viewModel::onPrimaryColorChange,
        onSecondaryColorsChange = viewModel::onSecondaryColorsChange,
        onWheelsTypeChange = viewModel::onWheelsTypeChange,
        onBaseTypeChange = viewModel::onBaseTypeChange,
        onDecorationChange = viewModel::onDecorationChange,
        onSeriesChange = viewModel::onSeriesChange,
        onReleaseYearChange = viewModel::onReleaseYearChange,
        onManufacturerCodeChange = viewModel::onManufacturerCodeChange,
        onConditionChange = viewModel::onConditionChange,
        onAddPhoto = viewModel::onAddPhoto,
        onSetPrimary = viewModel::onSetPrimary,
        onRemovePhoto = viewModel::onRemovePhoto,
        // Em edição o modelo não é criado nem alterado: o id já está no ViewModel.
        onSave = { viewModel.onSave(modelId = "") },
        onMessageShown = viewModel::onMessageShown,
        showExemplarSection = false,
        showActions = true,
        onArchive = viewModel::onArchive,
        onMerge = viewModel::onMergeInto,
        onSplit = viewModel::onSplitInto,
        siblings = state.siblingVariants,
        exemplars = state.exemplars,
        splitSelection = state.splitSelection,
        onToggleSplitExemplar = viewModel::onToggleSplitExemplar,
        runningAction = state.runningAction,
        createdAt = state.createdAt,
        updatedAt = state.updatedAt,
        modifier = modifier,
    )
}

@Composable
internal fun VariantFormContent(
    state: VariantFormUiState,
    onPrimaryColorChange: (String) -> Unit,
    onSecondaryColorsChange: (String) -> Unit,
    onWheelsTypeChange: (String) -> Unit,
    onBaseTypeChange: (String) -> Unit,
    onDecorationChange: (String) -> Unit,
    onSeriesChange: (String) -> Unit,
    onReleaseYearChange: (String) -> Unit,
    onManufacturerCodeChange: (String) -> Unit,
    onConditionChange: (Condition) -> Unit,
    onAddPhoto: (ByteArray, PhotoAngle) -> Unit,
    onSetPrimary: (String) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onSave: () -> Unit,
    onMessageShown: () -> Unit,
    showExemplarSection: Boolean,
    modifier: Modifier = Modifier,
    showActions: Boolean = false,
    onArchive: () -> Unit = {},
    onMerge: (String) -> Unit = {},
    onSplit: (String) -> Unit = {},
    siblings: List<VariantOption> = emptyList(),
    exemplars: List<VariantExemplarOption> = emptyList(),
    splitSelection: Set<String> = emptySet(),
    onToggleSplitExemplar: (String) -> Unit = {},
    runningAction: Boolean = false,
    createdAt: Instant? = null,
    updatedAt: Instant? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var photoToRemove by remember { mutableStateOf<String?>(null) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var showSplitDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val permissionDeniedMessage = stringResource(R.string.photo_permission_denied)

    val messageText = state.message?.text()

    LaunchedEffect(state.message) {
        messageText?.let { message ->
            snackbarHostState.showSnackbar(message)
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(
                    if (showExemplarSection) {
                        R.string.variant_form_title
                    } else {
                        R.string.variant_edit_title
                    },
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(
                    if (showExemplarSection) {
                        R.string.variant_form_hint
                    } else {
                        R.string.variant_edit_hint
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.primaryColor,
                    onValueChange = onPrimaryColorChange,
                    label = { Text(stringResource(R.string.create_entry_color)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.releaseYearText,
                    onValueChange = onReleaseYearChange,
                    label = { Text(stringResource(R.string.create_entry_year)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = state.secondaryColors,
                onValueChange = onSecondaryColorsChange,
                label = { Text(stringResource(R.string.variant_secondary_colors)) },
                placeholder = { Text(stringResource(R.string.variant_secondary_colors_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.wheelsType,
                onValueChange = onWheelsTypeChange,
                label = { Text(stringResource(R.string.variant_wheels)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.baseType,
                onValueChange = onBaseTypeChange,
                label = { Text(stringResource(R.string.variant_base)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.decoration,
                onValueChange = onDecorationChange,
                label = { Text(stringResource(R.string.variant_decoration)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.seriesName,
                    onValueChange = onSeriesChange,
                    label = { Text(stringResource(R.string.variant_series)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.manufacturerCode,
                    onValueChange = onManufacturerCodeChange,
                    label = { Text(stringResource(R.string.create_entry_code)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            if (showExemplarSection) {
                Text(
                    text = stringResource(R.string.variant_condition),
                    style = MaterialTheme.typography.labelLarge,
                )
                Condition.entries.chunked(3).forEach { rowConditions ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowConditions.forEach { condition ->
                            FilterChip(
                                selected = state.condition == condition,
                                onClick = { onConditionChange(condition) },
                                label = { Text(stringResource(condition.labelRes())) },
                            )
                        }
                    }
                }

                PhotoGrid(
                    photos = state.photos.map { pending ->
                        PhotoTile(
                            id = pending.id,
                            angle = pending.angle,
                            isPrimary = pending.isPrimary,
                            source = pending.bytes,
                            pending = true,
                        )
                    },
                    onAddPhotoBytes = onAddPhoto,
                    onSetPrimary = onSetPrimary,
                    onRemovePhoto = { photoId -> photoToRemove = photoId },
                    onPermissionDenied = {
                        scope.launch { snackbarHostState.showSnackbar(permissionDeniedMessage) }
                    },
                )
            }

            Button(
                onClick = onSave,
                enabled = !state.saving && !state.loading && !runningAction,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (showExemplarSection) {
                            R.string.variant_form_save
                        } else {
                            R.string.variant_edit_save
                        },
                    ),
                )
            }

            if (createdAt != null && updatedAt != null) {
                Text(
                    text = stringResource(
                        R.string.variant_dates,
                        DATE_FORMAT.format(createdAt),
                        DATE_FORMAT.format(updatedAt),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (showActions) {
                Text(
                    text = stringResource(R.string.variant_actions_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedButton(
                    onClick = { showArchiveDialog = true },
                    enabled = !runningAction,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.variant_archive)) }
                OutlinedButton(
                    onClick = { showMergeDialog = true },
                    enabled = !runningAction,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.variant_merge)) }
                OutlinedButton(
                    onClick = { showSplitDialog = true },
                    enabled = !runningAction,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.variant_split)) }
            }
        }
    }

    photoToRemove?.let { photoId ->
        AlertDialog(
            onDismissRequest = { photoToRemove = null },
            title = { Text(stringResource(R.string.photo_remove_title)) },
            text = { Text(stringResource(R.string.photo_remove_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        photoToRemove = null
                        onRemovePhoto(photoId)
                    },
                ) { Text(stringResource(R.string.photo_remove_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { photoToRemove = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text(stringResource(R.string.variant_archive_title)) },
            text = { Text(stringResource(R.string.variant_archive_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showArchiveDialog = false
                        onArchive()
                    },
                ) { Text(stringResource(R.string.variant_archive_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showMergeDialog) {
        VariantTargetDialog(
            title = stringResource(R.string.variant_merge_title),
            message = stringResource(R.string.variant_merge_message),
            options = siblings,
            emptyMessage = stringResource(R.string.variant_merge_empty),
            confirmLabel = stringResource(R.string.variant_merge_confirm),
            onDismiss = { showMergeDialog = false },
            onConfirm = { targetId ->
                showMergeDialog = false
                onMerge(targetId)
            },
        )
    }

    if (showSplitDialog) {
        SplitExemplarsDialog(
            options = siblings,
            exemplars = exemplars,
            selected = splitSelection,
            running = runningAction,
            onToggle = onToggleSplitExemplar,
            onDismiss = { showSplitDialog = false },
            onConfirm = { targetId ->
                showSplitDialog = false
                onSplit(targetId)
            },
        )
    }
}

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())

@Composable
private fun variantOptionText(option: VariantOption): String {
    val parts = listOfNotNull(
        option.colorName,
        option.year?.toString(),
        option.manufacturerCode,
    )
    return parts.joinToString(" · ").ifBlank {
        stringResource(R.string.variant_fallback_label, option.id.take(8))
    }
}

@Composable
private fun VariantTargetDialog(
    title: String,
    message: String,
    options: List<VariantOption>,
    emptyMessage: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var selected by remember(options) { mutableStateOf(options.firstOrNull()?.id) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(message)
                if (options.isEmpty()) {
                    Text(emptyMessage)
                } else {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selected == option.id,
                                onClick = { selected = option.id },
                            )
                            Text(variantOptionText(option))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selected?.let(onConfirm) },
                enabled = selected != null,
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun SplitExemplarsDialog(
    options: List<VariantOption>,
    exemplars: List<VariantExemplarOption>,
    selected: Set<String>,
    running: Boolean,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var target by remember(options) { mutableStateOf(options.firstOrNull()?.id) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.variant_split_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.variant_split_message))
                Text(
                    text = stringResource(R.string.variant_split_select),
                    style = MaterialTheme.typography.labelLarge,
                )
                if (exemplars.isEmpty()) {
                    Text(stringResource(R.string.variant_split_empty))
                } else {
                    exemplars.forEach { exemplar ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = exemplar.id in selected,
                                onCheckedChange = { onToggle(exemplar.id) },
                            )
                            Text(
                                "${exemplar.internalCode} · " +
                                    stringResource(exemplar.condition.labelRes()),
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.variant_merge_target),
                    style = MaterialTheme.typography.labelLarge,
                )
                if (options.isEmpty()) {
                    Text(stringResource(R.string.variant_merge_empty))
                } else {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = target == option.id,
                                onClick = { target = option.id },
                            )
                            Text(variantOptionText(option))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { target?.let(onConfirm) },
                enabled = target != null && selected.isNotEmpty() && !running,
            ) { Text(stringResource(R.string.variant_split_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
