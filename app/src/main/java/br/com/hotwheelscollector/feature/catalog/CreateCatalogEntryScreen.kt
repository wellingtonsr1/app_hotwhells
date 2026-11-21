package br.com.hotwheelscollector.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme

/**
 * Cadastro da primeira miniatura: fabricante, modelo e escala.
 *
 * A variante, o exemplar e as fotos vêm na tela seguinte — aqui só se resolve o que
 * identifica o carro, para não criar uma variante que ninguém consegue completar.
 */
@Composable
public fun CreateCatalogEntryScreen(
    onModelCreated: (modelId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateCatalogEntryViewModel = hiltViewModel(),
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
    LaunchedEffect(state.createdModelId) {
        state.createdModelId?.let(onModelCreated)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.create_entry_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.create_entry_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = state.manufacturer,
                    onValueChange = viewModel::onManufacturerChange,
                    label = { Text(stringResource(R.string.create_entry_manufacturer)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ManufacturerAutocomplete(
                    query = state.manufacturer,
                    availableManufacturers = state.availableManufacturers,
                    onSelectManufacturer = viewModel::onSelectManufacturerSuggestion,
                )
                OutlinedTextField(
                    value = state.model,
                    onValueChange = viewModel::onModelChange,
                    label = { Text(stringResource(R.string.create_entry_model)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ModelAutocomplete(
                    query = state.model,
                    availableModels = state.availableModels,
                    onSelectModel = viewModel::onSelectModelSuggestion,
                )
                OutlinedTextField(
                    value = state.scale,
                    onValueChange = viewModel::onScaleChange,
                    label = { Text(stringResource(R.string.create_entry_scale)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = viewModel::onSave,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.create_entry_save))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateCatalogEntryPreview() {
    HotWheelsTheme {
        Surface {
            Column(modifier = Modifier.padding(PaddingValues(20.dp))) {
                Text(text = "Novo modelo", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
