package br.com.diecastcollector.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.diecastcollector.R
import br.com.diecastcollector.ui.components.FlowChipGroup
import br.com.diecastcollector.ui.components.FormColumn

/**
 * Modelo e veículo real (T043, FR-014).
 *
 * O modelo nasce no fluxo do `+ Mini`; esta tela completa o que ficou de fora e permite
 * apontar o carro do mundo real. Um veículo digitado nos campos de baixo é criado e
 * vinculado no mesmo «Salvar» — sem passo extra nem cadastro órfão.
 */
@Composable
public fun ModelFormScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModelFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val saved = state.saved
    LaunchedEffect(saved) {
        if (saved) onSaved()
    }
    ModelFormContent(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::setName,
        onScaleChange = viewModel::setScale,
        onBodyTypeChange = viewModel::setBodyType,
        onMaterialChange = viewModel::setMaterial,
        onCountryChange = viewModel::setCountryOfManufacture,
        onNotesChange = viewModel::setNotes,
        onSelectVehicle = viewModel::selectRealVehicle,
        onVehicleMakeChange = viewModel::setVehicleMake,
        onVehicleModelChange = viewModel::setVehicleModel,
        onVehicleYearChange = viewModel::setVehicleYear,
        onVehicleCountryChange = viewModel::setVehicleCountry,
        onSave = viewModel::save,
        modifier = modifier,
    )
}

@Composable
internal fun ModelFormContent(
    state: ModelFormUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onScaleChange: (String) -> Unit,
    onBodyTypeChange: (String) -> Unit,
    onMaterialChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSelectVehicle: (String?) -> Unit,
    onVehicleMakeChange: (String) -> Unit,
    onVehicleModelChange: (String) -> Unit,
    onVehicleYearChange: (String) -> Unit,
    onVehicleCountryChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // T334: campos ≤ 720 dp centralizados; autocompletes acompanham a coluna.
    FormColumn(
        modifier = modifier,
        scrollable = true,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.model_form_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.model_form_back))
            }
        }

        if (state.loading) {
            CircularProgressIndicator()
            return@FormColumn
        }

        OutlinedTextField(
            value = state.modelName,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.model_form_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.scale,
            onValueChange = onScaleChange,
            label = { Text(stringResource(R.string.model_form_scale)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.bodyType,
            onValueChange = onBodyTypeChange,
            label = { Text(stringResource(R.string.model_form_body)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.material,
            onValueChange = onMaterialChange,
            label = { Text(stringResource(R.string.model_form_material)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.countryOfManufacture,
            onValueChange = onCountryChange,
            label = { Text(stringResource(R.string.model_form_country)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.notes,
            onValueChange = onNotesChange,
            label = { Text(stringResource(R.string.model_form_notes)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = stringResource(R.string.model_form_real_vehicle),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.model_form_real_vehicle_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Com vários veículos cadastrados a linha crescia para a direita e cortava os
        // últimos chips em 360 dp; o grupo passa a quebrar linha (T328, FR-313).
        FlowChipGroup {
            FilterChip(
                selected = state.realVehicleId == null,
                onClick = { onSelectVehicle(null) },
                label = { Text(stringResource(R.string.model_form_real_vehicle_none)) },
            )
            state.realVehicles.forEach { vehicle ->
                FilterChip(
                    selected = state.realVehicleId == vehicle.id,
                    onClick = { onSelectVehicle(vehicle.id) },
                    label = {
                        Text(
                            listOfNotNull(
                                vehicle.make,
                                vehicle.model,
                                vehicle.year?.toString(),
                            ).joinToString(" "),
                        )
                    },
                )
            }
        }

        Text(
            text = stringResource(R.string.model_form_new_vehicle),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = stringResource(R.string.model_form_new_vehicle_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.vehicleMake,
            onValueChange = onVehicleMakeChange,
            label = { Text(stringResource(R.string.model_form_make)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.vehicleModel,
            onValueChange = onVehicleModelChange,
            label = { Text(stringResource(R.string.model_form_vehicle_model)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.vehicleYear,
                onValueChange = onVehicleYearChange,
                label = { Text(stringResource(R.string.model_form_vehicle_year)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.vehicleCountry,
                onValueChange = onVehicleCountryChange,
                label = { Text(stringResource(R.string.model_form_vehicle_country)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        state.error?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            onClick = onSave,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (state.saving) {
                    stringResource(R.string.model_form_saving)
                } else {
                    stringResource(R.string.model_form_save)
                },
            )
        }
    }
}

private fun ModelFormError.messageRes(): Int = when (this) {
    ModelFormError.REQUIRED_NAME -> R.string.model_form_error_name
    ModelFormError.INVALID_SCALE -> R.string.model_form_error_scale
    ModelFormError.DUPLICATE -> R.string.model_form_error_duplicate
    ModelFormError.INVALID_YEAR -> R.string.model_form_error_year
    ModelFormError.STORAGE -> R.string.model_form_error_storage
}
