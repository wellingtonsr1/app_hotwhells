package br.com.hotwheelscollector.feature.transfer

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.data.importexport.ImportApplyResult
import br.com.hotwheelscollector.data.importexport.ImportPreview

/**
 * Importação de CSV (T115, FR-134–FR-139).
 *
 * Fluxo em duas etapas: escolher o arquivo mostra a pré-visualização com as contagens e o
 * motivo de cada erro; só o toque em "Importar válidas" grava. O relatório de erros pode
 * ser salvo em CSV antes de importar.
 */
@Composable
public fun ImportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::load) }

    val errorReportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(CSV_MIME),
    ) { uri -> uri?.let(viewModel::saveErrorReport) }

    ImportContent(
        state = state,
        onBack = onBack,
        onPick = { openLauncher.launch(CSV_MIME_ARRAY) },
        onSaveErrors = { errorReportLauncher.launch("import-erros.csv") },
        onImport = viewModel::import,
        onDismissMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
internal fun ImportContent(
    state: ImportViewModel.State,
    onBack: () -> Unit,
    onPick: () -> Unit,
    onSaveErrors: () -> Unit,
    onImport: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                text = stringResource(R.string.import_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.import_back))
            }
        }

        Text(
            text = stringResource(R.string.import_intro),
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(
            onClick = onPick,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.import_pick))
        }

        if (state.busy) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                Text(stringResource(R.string.import_busy))
            }
        }

        state.preview?.let { preview ->
            PreviewCard(
                preview = preview,
                onSaveErrors = onSaveErrors,
                onImport = onImport,
                canImport = !state.busy && preview.validRows > 0,
            )
        }

        state.imported?.let { result -> AppliedCard(result = result, onDismiss = onDismissMessage) }

        state.failure?.let { detail ->
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.import_error_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    if (detail.isNotBlank()) {
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onDismissMessage) {
                        Text(stringResource(R.string.import_dismiss))
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(
    preview: ImportPreview,
    onSaveErrors: () -> Unit,
    onImport: () -> Unit,
    canImport: Boolean,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.import_preview_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    R.string.import_preview_summary,
                    preview.totalRows,
                    preview.validRows,
                    preview.invalidRows,
                    preview.duplicateRows,
                ),
            )
            if (preview.problems.isNotEmpty()) {
                OutlinedButton(
                    onClick = onSaveErrors,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.import_save_errors))
                }
            }
            Button(
                onClick = onImport,
                enabled = canImport,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.import_apply))
            }
        }
    }
}

@Composable
private fun AppliedCard(result: ImportApplyResult, onDismiss: () -> Unit) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.import_result_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    R.string.import_result_body,
                    result.importedExemplars,
                    result.skippedDuplicates,
                ),
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.import_dismiss))
            }
        }
    }
}

private const val CSV_MIME = "text/csv"

private val CSV_MIME_ARRAY = arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "text/plain",
    "application/csv",
    "application/octet-stream",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/zip",
)
