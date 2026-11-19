package br.com.hotwheelscollector.feature.organization

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.hotwheelscollector.R

/**
 * Seletor de item da coleção.
 *
 * Duas abas porque o schema permite item de variante e item de modelo (FR-051). A busca
 * filtra a lista local; a coleção pequena não justifica consulta por caractere.
 */
@Composable
public fun CollectionItemPicker(
    options: List<CollectionOption>,
    onPick: (CollectionOption) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(CollectionOptionKind.VARIANT) }

    val filtered = remember(options, query, kind) {
        options
            .filter { it.kind == kind }
            .filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.collection_item_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.collection_item_picker_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == CollectionOptionKind.VARIANT,
                        onClick = { kind = CollectionOptionKind.VARIANT },
                        label = { Text(stringResource(R.string.collection_item_picker_variant)) },
                    )
                    FilterChip(
                        selected = kind == CollectionOptionKind.MODEL,
                        onClick = { kind = CollectionOptionKind.MODEL },
                        label = { Text(stringResource(R.string.collection_item_picker_model)) },
                    )
                }
                HorizontalDivider()
                if (filtered.isEmpty()) {
                    Text(
                        text = stringResource(R.string.collection_item_picker_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(320.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(filtered, key = { "${it.kind}-${it.id}" }) { option ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(option) }
                                    .padding(vertical = 10.dp),
                            ) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        },
    )
}
