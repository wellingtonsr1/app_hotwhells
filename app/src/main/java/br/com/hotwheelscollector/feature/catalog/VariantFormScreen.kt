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
    viewModel: VariantFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val permissionDeniedMessage = stringResource(R.string.photo_permission_denied)

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageShown()
        }
    }
    LaunchedEffect(state.savedVariantId) {
        state.savedVariantId?.let(onVariantSaved)
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
                text = stringResource(R.string.variant_form_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.variant_form_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.primaryColor,
                    onValueChange = viewModel::onPrimaryColorChange,
                    label = { Text(stringResource(R.string.create_entry_color)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.releaseYearText,
                    onValueChange = viewModel::onReleaseYearChange,
                    label = { Text(stringResource(R.string.create_entry_year)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = state.secondaryColors,
                onValueChange = viewModel::onSecondaryColorsChange,
                label = { Text(stringResource(R.string.variant_secondary_colors)) },
                placeholder = { Text(stringResource(R.string.variant_secondary_colors_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.wheelsType,
                onValueChange = viewModel::onWheelsTypeChange,
                label = { Text(stringResource(R.string.variant_wheels)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.baseType,
                onValueChange = viewModel::onBaseTypeChange,
                label = { Text(stringResource(R.string.variant_base)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.decoration,
                onValueChange = viewModel::onDecorationChange,
                label = { Text(stringResource(R.string.variant_decoration)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.seriesName,
                    onValueChange = viewModel::onSeriesChange,
                    label = { Text(stringResource(R.string.variant_series)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.manufacturerCode,
                    onValueChange = viewModel::onManufacturerCodeChange,
                    label = { Text(stringResource(R.string.create_entry_code)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = stringResource(R.string.variant_condition),
                style = MaterialTheme.typography.labelLarge,
            )
            Condition.entries.chunked(3).forEach { rowConditions ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowConditions.forEach { condition ->
                        FilterChip(
                            selected = state.condition == condition,
                            onClick = { viewModel.onConditionChange(condition) },
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
                onAddPhotoBytes = viewModel::onAddPhoto,
                onSetPrimary = viewModel::onSetPrimary,
                onRemovePhoto = viewModel::onRemovePhoto,
                onPermissionDenied = {
                    scope.launch { snackbarHostState.showSnackbar(permissionDeniedMessage) }
                },
            )

            Button(
                onClick = { viewModel.onSave(modelId) },
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.variant_form_save))
            }
        }
    }
}
