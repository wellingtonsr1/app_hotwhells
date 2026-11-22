package br.com.diecastcollector.feature.catalog

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.ui.UiMessage
import br.com.diecastcollector.ui.theme.DiecastTheme

/**
 * Cadastro rápido (US1): só o que muda de um exemplar para outro.
 *
 * Modelo, variante e cor não aparecem aqui de propósito — já foram escolhidos, e
 * redigitar seria justamente o que a história promete evitar.
 */
@Composable
public fun AddExemplarScreen(
    onSaved: (exemplarId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddExemplarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val messageText = state.message?.text()

    LaunchedEffect(state.message) {
        messageText?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageShown()
        }
    }
    LaunchedEffect(state.savedExemplarId) {
        state.savedExemplarId?.let(onSaved)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        AddExemplarContent(
            state = state,
            contentPadding = padding,
            onConditionChange = viewModel::onConditionChange,
            onLocationChange = viewModel::onLocationChange,
            onPriceChange = viewModel::onPriceChange,
            onShippingChange = viewModel::onShippingChange,
            onDiscountChange = viewModel::onDiscountChange,
            onDateChange = viewModel::onDateChange,
            onStoreChange = viewModel::onStoreChange,
            onNotesChange = viewModel::onNotesChange,
            onSave = viewModel::onSave,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddExemplarContent(
    state: AddExemplarUiState,
    contentPadding: PaddingValues,
    onConditionChange: (Condition) -> Unit,
    onLocationChange: (String?) -> Unit,
    onPriceChange: (String) -> Unit,
    onShippingChange: (String) -> Unit,
    onDiscountChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onStoreChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.add_exemplar_title),
                style = MaterialTheme.typography.headlineSmall,
            )

            // Aviso de repetição: informa, não bloqueia (FR-039).
            state.duplicateNotice?.let { notice ->
                Text(
                    text = notice.text(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                text = stringResource(R.string.add_exemplar_condition),
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

            // Seletor real: só é possível escolher uma localização cadastrada, ou nenhuma.
            // Digitar texto livre aqui criaria localização fantasma e quebraria FR-088.
            var locationMenuOpen by remember { mutableStateOf(false) }
            val selectedLocation = state.locations.firstOrNull { it.id == state.locationId }
            ExposedDropdownMenuBox(
                expanded = locationMenuOpen,
                onExpandedChange = { locationMenuOpen = !locationMenuOpen },
            ) {
                OutlinedTextField(
                    value = selectedLocation?.name ?: stringResource(R.string.add_exemplar_no_location),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.add_exemplar_location)) },
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
                            onLocationChange(null)
                            locationMenuOpen = false
                        },
                    )
                    state.locations.forEach { location ->
                        DropdownMenuItem(
                            text = { Text(location.name) },
                            onClick = {
                                onLocationChange(location.id)
                                locationMenuOpen = false
                            },
                        )
                    }
                }
            }
            if (state.locations.isEmpty()) {
                Text(
                    text = stringResource(R.string.add_exemplar_no_locations_yet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = state.priceText,
                onValueChange = onPriceChange,
                label = { Text(stringResource(R.string.add_exemplar_price)) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.shippingText,
                    onValueChange = onShippingChange,
                    label = { Text(stringResource(R.string.add_exemplar_shipping)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.discountText,
                    onValueChange = onDiscountChange,
                    label = { Text(stringResource(R.string.add_exemplar_discount)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.dateText,
                    onValueChange = onDateChange,
                    label = { Text(stringResource(R.string.add_exemplar_date)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.store,
                    onValueChange = onStoreChange,
                    label = { Text(stringResource(R.string.add_exemplar_store)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(R.string.add_exemplar_notes)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onSave,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.add_exemplar_save))
            }

            if (state.variantId.isBlank()) {
                Text(
                    text = stringResource(R.string.add_exemplar_no_variant),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Rótulo pt-BR de cada condição; o texto mora em `strings.xml` (T131, Art. IV). */
@StringRes
internal fun Condition.labelRes(): Int = when (this) {
    Condition.SEALED -> R.string.condition_sealed
    Condition.LOOSE -> R.string.condition_loose
    Condition.DAMAGED -> R.string.condition_damaged
    Condition.RESTORED -> R.string.condition_restored
    Condition.INCOMPLETE -> R.string.condition_incomplete
}

@Preview(showBackground = true)
@Composable
private fun AddExemplarPreview() {
    DiecastTheme {
        AddExemplarContent(
            state = AddExemplarUiState(
                variantId = "v1",
                duplicateNotice = UiMessage(R.string.add_exemplar_duplicate_variant, 2),
            ),
            contentPadding = PaddingValues(0.dp),
            onConditionChange = {},
            onLocationChange = {},
            onPriceChange = {},
            onShippingChange = {},
            onDiscountChange = {},
            onDateChange = {},
            onStoreChange = {},
            onNotesChange = {},
            onSave = {},
        )
    }
}
