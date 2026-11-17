package br.com.hotwheelscollector.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.CarModel

/**
 * Tela de cadastro detalhado de modelo e veículo real (T043 - US2, FR-013, FR-014, FR-025).
 */
@Composable
public fun ModelFormScreen(
    onModelSaved: (modelId: String) -> Unit,
    availableModels: List<CarModel> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var manufacturerName by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var scale by remember { mutableStateOf("1:64") }
    var realVehicleMake by remember { mutableStateOf("") }
    var realVehicleModel by remember { mutableStateOf("") }
    var realVehicleYear by remember { mutableStateOf("") }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var existingDuplicateModel by remember { mutableStateOf<CarModel?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                text = stringResource(R.string.model_form_title),
                style = MaterialTheme.typography.headlineSmall,
            )

            OutlinedTextField(
                value = manufacturerName,
                onValueChange = { manufacturerName = it },
                label = { Text(stringResource(R.string.create_entry_manufacturer)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text(stringResource(R.string.create_entry_model)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Autocomplete assistido (T049 - FR-024)
            ModelAutocomplete(
                query = modelName,
                availableModels = availableModels,
                onSelectModel = { selected ->
                    modelName = selected.name
                    scale = selected.scale
                },
            )

            OutlinedTextField(
                value = scale,
                onValueChange = { scale = it },
                label = { Text(stringResource(R.string.create_entry_scale)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Veículo do mundo real (opcional)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = realVehicleMake,
                    onValueChange = { realVehicleMake = it },
                    label = { Text("Marca real (ex: Nissan)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = realVehicleYear,
                    onValueChange = { realVehicleYear = it },
                    label = { Text("Ano real") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = realVehicleModel,
                onValueChange = { realVehicleModel = it },
                label = { Text("Modelo real (ex: Skyline GT-R R34)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    val duplicate = availableModels.find {
                        it.name.equals(modelName.trim(), ignoreCase = true) && it.scale == scale.trim()
                    }
                    if (duplicate != null) {
                        existingDuplicateModel = duplicate
                        showDuplicateDialog = true
                    } else {
                        // Salvar novo modelo
                        onModelSaved("new-model-id")
                    }
                },
                enabled = manufacturerName.isNotBlank() && modelName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Salvar modelo")
            }
        }
    }

    // Diálogo de aviso de modelo existente (FR-025)
    if (showDuplicateDialog && existingDuplicateModel != null) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            title = { Text("Modelo já existente") },
            text = {
                Text("Já existe o modelo '${existingDuplicateModel!!.name}' cadastrado. Deseja usar o existente ou criar outro mesmo assim?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDuplicateDialog = false
                        onModelSaved(existingDuplicateModel!!.id)
                    },
                ) {
                    Text("Usar existente")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDuplicateDialog = false
                        onModelSaved("forced-new-model-id")
                    },
                ) {
                    Text("Criar outro")
                }
            },
        )
    }
}
