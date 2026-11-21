package br.com.hotwheelscollector.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.PhotoAngle
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
) {
    val snackbarHostState = remember { SnackbarHostState() }
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
                    onRemovePhoto = onRemovePhoto,
                    onPermissionDenied = {
                        scope.launch { snackbarHostState.showSnackbar(permissionDeniedMessage) }
                    },
                )
            }

            Button(
                onClick = onSave,
                enabled = !state.saving && !state.loading,
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
        }
    }
}
