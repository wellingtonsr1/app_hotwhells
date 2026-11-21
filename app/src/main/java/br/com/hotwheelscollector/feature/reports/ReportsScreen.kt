package br.com.hotwheelscollector.feature.reports

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
import br.com.hotwheelscollector.data.importexport.PdfReportResult
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Relatórios (T118, FR-126–FR-128).
 *
 * Gera o inventário em PDF no próprio aparelho, sem rede: o botão abre o seletor de
 * arquivos (SAF) e o PDF é escrito no destino escolhido. Progresso e resultado aparecem
 * na própria tela.
 */
@Composable
public fun ReportsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(PDF_MIME),
    ) { uri -> uri?.let(viewModel::generate) }

    ReportsContent(
        state = state,
        onBack = onBack,
        onGenerate = { createLauncher.launch(defaultPdfName()) },
        onDismissMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
internal fun ReportsContent(
    state: ReportsViewModel.State,
    onBack: () -> Unit,
    onGenerate: () -> Unit,
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

        Button(
            onClick = onGenerate,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.reports_generate_pdf))
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
private fun CreatedCard(result: PdfReportResult, onDismiss: () -> Unit) {
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
                text = stringResource(
                    R.string.reports_created_summary,
                    result.totalExemplares,
                    result.pageCount,
                ),
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.reports_dismiss))
            }
        }
    }
}

private fun defaultPdfName(): String {
    val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
    return "colecao-inventario-$stamp.pdf"
}

private const val PDF_MIME = "application/pdf"
