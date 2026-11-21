package br.com.hotwheelscollector.feature.recognition

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.usecase.CatalogMatch
import java.io.File
import kotlin.math.roundToInt

/**
 * Fluxo de código de barras (T128, FR-109–FR-113).
 *
 * Ler o código apresenta até cinco correspondências do catálogo local com seus dados e a
 * origem; aproveitar os dados exige a conferência e o toque do usuário. Quando o código não
 * existe, a tela informa a ausência e oferece o cadastro manual, guardando a leitura em
 * `recognition_result`. Nenhum exemplar nasce só por ler um código (FR-113).
 *
 * O campo de código digitado não depende da câmera: negar a permissão deixa o fluxo inteiro
 * utilizável, só sem a captura pela câmera (FR-108).
 */
@Composable
public fun BarcodeScanScreen(
    onBack: () -> Unit,
    onUseVariant: (variantId: String) -> Unit,
    onCreateManual: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecognitionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var cameraDenied by remember { mutableStateOf(false) }
    var reviewing by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = tempCameraUri
        if (success && uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) viewModel.onImageCaptured(bytes)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            cameraDenied = false
            val uri = createTempImageUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraDenied = true
        }
    }

    BarcodeScanContent(
        state = state,
        cameraDenied = cameraDenied,
        onBack = onBack,
        onTypedCodeChange = viewModel::onTypedCodeChange,
        onReadTypedCode = viewModel::readTypedCode,
        onUseCamera = {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                val uri = createTempImageUri(context)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        onReview = { reviewing = true },
        onSaveReading = { viewModel.apply(emptySet()) },
        onSelectMatch = viewModel::selectMatch,
        onUseVariant = onUseVariant,
        onCreateManual = onCreateManual,
        onDismissFailure = viewModel::clearFailure,
        modifier = modifier,
    )

    val proposal = state.proposal
    if (reviewing && proposal != null) {
        RecognitionReviewSheet(
            proposal = proposal,
            onApply = { accepted ->
                reviewing = false
                viewModel.apply(accepted.keys)
            },
            onDismissRequest = { reviewing = false },
        )
    }
}

@Composable
internal fun BarcodeScanContent(
    state: RecognitionViewModel.State,
    cameraDenied: Boolean,
    onBack: () -> Unit,
    onTypedCodeChange: (String) -> Unit,
    onReadTypedCode: () -> Unit,
    onUseCamera: () -> Unit,
    onReview: () -> Unit,
    onSaveReading: () -> Unit,
    onSelectMatch: (String) -> Unit,
    onUseVariant: (String) -> Unit,
    onCreateManual: () -> Unit,
    onDismissFailure: () -> Unit,
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
                text = stringResource(R.string.recognition_barcode_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.recognition_back))
            }
        }

        Text(
            text = stringResource(R.string.recognition_barcode_intro),
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = state.typedCode,
            onValueChange = onTypedCodeChange,
            label = { Text(stringResource(R.string.recognition_barcode_code_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onReadTypedCode,
                enabled = !state.busy && state.typedCode.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.recognition_barcode_read))
            }
            OutlinedButton(
                onClick = onUseCamera,
                enabled = !state.busy,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.recognition_barcode_camera))
            }
        }

        if (cameraDenied) {
            Text(
                text = stringResource(R.string.recognition_camera_denied),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (state.busy) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                Text(stringResource(R.string.recognition_barcode_busy))
            }
        }

        state.failure?.let { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.recognition_barcode_failure),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(text = message, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onDismissFailure) {
                        Text(stringResource(R.string.recognition_review_dismiss))
                    }
                }
            }
        }

        val proposal = state.proposal
        if (proposal != null) {
            if (!proposal.identified) {
                Text(
                    text = stringResource(R.string.recognition_review_unidentified_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.recognition_review_unidentified_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = stringResource(R.string.recognition_barcode_suggestions),
                    style = MaterialTheme.typography.titleMedium,
                )
                proposal.suggestions.forEach { suggestion ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = fieldLabel(suggestion.field),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = suggestion.value,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = onReview, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.recognition_barcode_review))
                    }
                    OutlinedButton(onClick = onSaveReading, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.recognition_barcode_save))
                    }
                }
            }

            if (state.hasMatches) {
                Text(
                    text = stringResource(R.string.recognition_barcode_matches),
                    style = MaterialTheme.typography.titleMedium,
                )
                state.matches.forEach { match ->
                    val selected = match.exemplarId == state.selectedExemplarId
                    MatchCard(
                        match = match,
                        selected = selected,
                        onClick = { onSelectMatch(match.exemplarId) },
                    )
                }
                val selectedMatch = state.matches
                    .firstOrNull { it.exemplarId == state.selectedExemplarId }
                Button(
                    onClick = { selectedMatch?.let { onUseVariant(it.variantId) } },
                    enabled = selectedMatch != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.recognition_barcode_use_variant))
                }
            } else {
                Text(
                    text = stringResource(R.string.recognition_barcode_no_match),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.recognition_barcode_manual_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onCreateManual,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.recognition_barcode_create_manual))
                }
            }
        }

        if (state.recordedId != null) {
            Text(
                text = if (state.recordedFields.isEmpty()) {
                    stringResource(R.string.recognition_barcode_recorded_code)
                } else {
                    stringResource(R.string.recognition_barcode_recorded_fields)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.recognition_barcode_no_exemplar),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MatchCard(
    match: CatalogMatch,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "${match.manufacturerName} · ${match.modelName}",
                    style = MaterialTheme.typography.titleSmall,
                )
                val details = listOfNotNull(
                    match.seriesName,
                    match.releaseYear?.toString(),
                    match.manufacturerCode,
                ).joinToString(" · ")
                if (details.isNotEmpty()) {
                    Text(text = details, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = stringResource(
                        R.string.recognition_review_confidence,
                        (match.confidence * 100).roundToInt(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun createTempImageUri(context: Context): Uri {
    val tempFile = File.createTempFile("barcode_capture_", ".jpg", context.cacheDir).apply {
        deleteOnExit()
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile,
    )
}
