package br.com.diecastcollector.feature.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.diecastcollector.R
import br.com.diecastcollector.data.importexport.ReportFormat
import br.com.diecastcollector.data.importexport.ReportWriters
import br.com.diecastcollector.data.repository.InventoryReport
import br.com.diecastcollector.ui.components.FlowChipGroup
import br.com.diecastcollector.ui.components.FormColumn
import java.time.Instant

/**
 * Relatórios (T118/T116, FR-124–FR-128).
 *
 * Gera o inventário no formato escolhido — PDF, CSV ou XLSX — no próprio aparelho, sem
 * rede: o botão abre o seletor de arquivos (SAF) com o nome datado já sugerido e o arquivo
 * é escrito no destino escolhido. Progresso e resultado aparecem na própria tela.
 */
@Composable
public fun ReportsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var formatIndex by rememberSaveable { mutableIntStateOf(0) }
    val format = ReportFormat.entries[formatIndex]

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ReportFormat.PDF.mimeType),
    ) { uri -> uri?.let { viewModel.generate(it, ReportFormat.PDF) } }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ReportFormat.CSV.mimeType),
    ) { uri -> uri?.let { viewModel.generate(it, ReportFormat.CSV) } }
    val xlsxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ReportFormat.XLSX.mimeType),
    ) { uri -> uri?.let { viewModel.generate(it, ReportFormat.XLSX) } }

    ReportsContent(
        state = state,
        selectedFormat = format,
        onSelectFormat = { selected -> formatIndex = selected.ordinal },
        onBack = onBack,
        onGenerate = {
            val name = ReportWriters.fileName("colecao-inventario", format, Instant.now())
            when (format) {
                ReportFormat.PDF -> pdfLauncher.launch(name)
                ReportFormat.CSV -> csvLauncher.launch(name)
                ReportFormat.XLSX -> xlsxLauncher.launch(name)
            }
        },
        onDismissMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
internal fun ReportsContent(
    state: ReportsViewModel.State,
    selectedFormat: ReportFormat,
    onSelectFormat: (ReportFormat) -> Unit,
    onBack: () -> Unit,
    onGenerate: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // T369: largura de leitura e rolagem próprias em E. Tela empilhada, sem Scaffold:
    // reserva a barra de status e a de navegação aqui.
    FormColumn(
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        scrollable = true,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.reports_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.reports_back))
            }
        }

        Text(
            text = stringResource(R.string.reports_intro),
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(
            text = stringResource(R.string.reports_format_label),
            style = MaterialTheme.typography.titleSmall,
        )
        FlowChipGroup {
            ReportFormat.entries.forEach { format ->
                FilterChip(
                    selected = format == selectedFormat,
                    onClick = { onSelectFormat(format) },
                    label = { Text(formatLabel(format)) },
                    enabled = !state.busy,
                )
            }
        }

        Button(
            onClick = onGenerate,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.reports_generate))
        }

        if (state.busy) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                Text(stringResource(R.string.reports_busy))
            }
        }

        state.created?.let { result -> CreatedCard(result = result, onDismiss = onDismissMessage) }

        state.failure?.let { detail ->
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.reports_error_title),
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
                        Text(stringResource(R.string.reports_dismiss))
                    }
                }
            }
        }
    }
}

@Composable
private fun formatLabel(format: ReportFormat): String = stringResource(
    when (format) {
        ReportFormat.PDF -> R.string.reports_format_pdf
        ReportFormat.CSV -> R.string.reports_format_csv
        ReportFormat.XLSX -> R.string.reports_format_xlsx
    },
)

@Composable
private fun CreatedCard(result: InventoryReport, onDismiss: () -> Unit) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.reports_created_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.reports_created_file, result.fileName),
            )
            Text(
                text = if (result.pageCount > 0) {
                    stringResource(
                        R.string.reports_created_summary,
                        result.totalExemplares,
                        result.pageCount,
                    )
                } else {
                    stringResource(
                        R.string.reports_created_summary_no_pages,
                        result.totalExemplares,
                    )
                },
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.reports_dismiss))
            }
        }
    }
}
