package br.com.hotwheelscollector.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.feature.transfer.formatBytes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Controle de dados (T146, FR-142, FR-157, FR-163, Art. III.7).
 *
 * Duas coisas moram aqui, e só elas: **exportar tudo** em formato aberto (o mesmo pacote
 * do backup: banco, fotos, JSON e CSV) e **excluir tudo** do aparelho. A exclusão nunca
 * acontece no primeiro toque: a primeira confirmação explica o que vai embora e oferece
 * exportar antes; a segunda é a definitiva.
 */
@Composable
public fun DataControlRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DataControlViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ZIP_MIME),
    ) { uri -> uri?.let(viewModel::exportTo) }

    DataControlContent(
        state = state,
        onBack = onBack,
        onExport = { createLauncher.launch(defaultExportName()) },
        onDelete = viewModel::deleteAll,
        onDismissMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
public fun DataControlContent(
    state: DataControlViewModel.State,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFirstConfirm by remember { mutableStateOf(false) }
    var showFinalConfirm by remember { mutableStateOf(false) }
    val counts = state.counts

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.data_control_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.data_control_back))
            }
        }

        Text(
            text = stringResource(R.string.data_control_intro),
            style = MaterialTheme.typography.bodyMedium,
        )

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.data_control_counts_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (counts.isEmpty) {
                    Text(stringResource(R.string.data_control_empty))
                } else {
                    Text(
                        text = pluralStringResource(
                            R.plurals.backup_counts_exemplars,
                            counts.exemplars,
                            counts.exemplars,
                        ) + " · " + pluralStringResource(
                            R.plurals.backup_counts_photos,
                            counts.photos,
                            counts.photos,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.data_control_counts_more),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Button(
            onClick = onExport,
            enabled = !state.busy && !counts.isEmpty,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.data_control_export))
        }
        Text(
            text = stringResource(R.string.data_control_export_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(
            onClick = { showFirstConfirm = true },
            enabled = !state.busy && !counts.isEmpty,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(stringResource(R.string.data_control_delete))
        }
        Text(
            text = stringResource(R.string.data_control_delete_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.busy) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                Text(stringResource(R.string.data_control_busy))
            }
        }

        state.exportedBytes?.let { bytes ->
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.data_control_exported_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(stringResource(R.string.data_control_exported_body, formatBytes(bytes)))
                    TextButton(onClick = onDismissMessage) {
                        Text(stringResource(R.string.backup_dismiss))
                    }
                }
            }
        }

        if (state.deleted) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.data_control_deleted_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(stringResource(R.string.data_control_deleted_body))
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.data_control_deleted_back))
                    }
                }
            }
        }

        state.failure?.let { failure ->
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.data_control_error_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = stringResource(
                            when (failure) {
                                DataControlViewModel.Failure.EXPORT -> R.string.data_control_error_export
                                DataControlViewModel.Failure.DELETE -> R.string.data_control_error_delete
                            },
                        ),
                    )
                    state.failureDetail?.takeIf { it.isNotBlank() }?.let { detail ->
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onDismissMessage) {
                        Text(stringResource(R.string.backup_dismiss))
                    }
                }
            }
        }
    }

    if (showFirstConfirm) {
        AlertDialog(
            onDismissRequest = { showFirstConfirm = false },
            title = { Text(stringResource(R.string.data_control_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(
                            R.string.data_control_confirm_body,
                            pluralStringResource(
                                R.plurals.backup_counts_exemplars,
                                counts.exemplars,
                                counts.exemplars,
                            ),
                            pluralStringResource(
                                R.plurals.backup_counts_photos,
                                counts.photos,
                                counts.photos,
                            ),
                        ),
                    )
                    // A oferta de exportar vem antes da exclusão, no próprio aviso (FR-163).
                    TextButton(
                        onClick = {
                            showFirstConfirm = false
                            onExport()
                        },
                    ) {
                        Text(stringResource(R.string.data_control_confirm_export))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFirstConfirm = false
                        showFinalConfirm = true
                    },
                ) {
                    Text(stringResource(R.string.data_control_confirm_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFirstConfirm = false }) {
                    Text(stringResource(R.string.data_control_cancel))
                }
            },
        )
    }

    if (showFinalConfirm) {
        AlertDialog(
            onDismissRequest = { showFinalConfirm = false },
            title = { Text(stringResource(R.string.data_control_final_title)) },
            text = { Text(stringResource(R.string.data_control_final_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFinalConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.data_control_final_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinalConfirm = false }) {
                    Text(stringResource(R.string.data_control_cancel))
                }
            },
        )
    }
}

private fun defaultExportName(): String {
    val stamp = LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
    return "colecao-export-$stamp.zip"
}

private const val ZIP_MIME = "application/zip"
