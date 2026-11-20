package br.com.hotwheelscollector.feature.duplicates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.DuplicateAvailability
import br.com.hotwheelscollector.domain.model.DuplicateExemplar
import br.com.hotwheelscollector.feature.catalog.labelRes

/**
 * Marcação de disponibilidade (T084, FR-066–FR-067).
 *
 * Troca, venda, ambos ou indisponível; a condição e o valor pretendido valem para a
 * proposta, não alteram o exemplar. A folha sempre parte da oferta atual do exemplar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun DuplicateActionsSheet(
    selected: DuplicateExemplar,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (DuplicateAvailability, Condition?, String, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var availability by remember(selected.exemplar.id) {
        mutableStateOf(selected.offer?.availability ?: DuplicateAvailability.SALE)
    }
    var condition by remember(selected.exemplar.id) {
        mutableStateOf(selected.offer?.condition ?: selected.exemplar.condition)
    }
    var priceText by remember(selected.exemplar.id) {
        mutableStateOf(selected.offer?.askingPrice?.formatPtBr().orEmpty())
    }
    var notes by remember(selected.exemplar.id) {
        mutableStateOf(selected.offer?.notes.orEmpty())
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.duplicates_sheet_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "${selected.exemplar.internalCode} · ${selected.modelName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.duplicates_sheet_availability),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DuplicateAvailability.entries.take(2).forEach { option ->
                    FilterChip(
                        selected = availability == option,
                        onClick = { availability = option },
                        label = { Text(stringResource(option.labelRes())) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DuplicateAvailability.entries.drop(2).forEach { option ->
                    FilterChip(
                        selected = availability == option,
                        onClick = { availability = option },
                        label = { Text(stringResource(option.labelRes())) },
                    )
                }
            }

            Text(
                text = stringResource(R.string.duplicates_sheet_condition),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Condition.entries.forEach { option ->
                    FilterChip(
                        selected = condition == option,
                        onClick = { condition = option },
                        label = { Text(stringResource(option.labelRes())) },
                    )
                }
            }

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it },
                label = { Text(stringResource(R.string.duplicates_sheet_price)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.duplicates_sheet_notes)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.duplicates_sheet_cancel))
                }
                Button(
                    onClick = { onSave(availability, condition, priceText, notes) },
                    enabled = !saving,
                ) {
                    Text(stringResource(R.string.duplicates_sheet_save))
                }
            }
        }
    }
}

/** Rótulo pt-BR da disponibilidade (Art. IV: texto só na apresentação). */
@Composable
internal fun DuplicateAvailability.labelRes(): Int = when (this) {
    DuplicateAvailability.TRADE -> R.string.duplicates_availability_trade
    DuplicateAvailability.SALE -> R.string.duplicates_availability_sale
    DuplicateAvailability.BOTH -> R.string.duplicates_availability_both
    DuplicateAvailability.UNAVAILABLE -> R.string.duplicates_availability_unavailable
}

