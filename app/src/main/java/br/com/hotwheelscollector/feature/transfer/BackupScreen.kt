package br.com.hotwheelscollector.feature.transfer

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.backup.BackupFailure
import br.com.hotwheelscollector.domain.backup.BackupSummary
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Backup manual (T119/T120, FR-140–FR-145).
 *
 * O fluxo é deliberadamente em duas etapas: escolher o arquivo mostra o **resumo
 * validado**, e só o segundo toque substitui os dados (FR-163). O pacote inclui banco,
 * fotos e manifesto; o Auto Backup do Android não cobre as fotos, e a tela avisa isso.
 */
@Composable
public fun BackupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ZIP_MIME),
    ) { uri -> uri?.let(viewModel::create) }
    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::inspect) }

    BackupContent(
        state = state,
        onBack = onBack,
        onCreate = { createLauncher.launch(defaultBackupName()) },
        onPick = { openLauncher.launch(arrayOf(ZIP_MIME, "application/octet-stream")) },
        onConfirmRestore = viewModel::confirmRestore,
        onDismissPending = viewModel::dismissPending,
        onDismissMessage = viewModel::clearMessage,
        onRestart = { restart(context) },
        modifier = modifier,
    )
}

@Composable
internal fun BackupContent(
    state: BackupViewModel.State,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onPick: () -> Unit,
    onConfirmRestore: () -> Unit,
    onDismissPending: () -> Unit,
    onDismissMessage: () -> Unit,
    onRestart: () -> Unit,
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
                text = stringResource(R.string.backup_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.backup_back))
            }
        }

        Text(
            text = stringResource(R.string.backup_intro),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.backup_auto_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = onCreate,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.backup_create))
        }
        OutlinedButton(
            onClick = onPick,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.backup_restore))
        }

        if (state.busy) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                Text(stringResource(R.string.backup_busy))
            }
        }

        state.created?.let { summary ->
            SummaryCard(
                title = stringResource(R.string.backup_created_title),
                summary = summary,
                onDismiss = onDismissMessage,
            )
        }

        if (state.failure != null) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.backup_error_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(text = stringResource(state.failure.messageRes()))
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

        state.restored?.let { summary ->
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.backup_restored_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    CountsText(
                        exemplars = summary.counts["exemplar"] ?: 0,
                        photos = summary.imageCount,
                    )
                    Text(
                        text = stringResource(R.string.backup_restored_body),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = onRestart,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.backup_restart))
                    }
                }
            }
        }
    }

    state.pending?.let { pending ->
        AlertDialog(
            onDismissRequest = onDismissPending,
            title = { Text(stringResource(R.string.backup_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(
                            R.string.backup_confirm_created_at,
                            DATE_FORMAT.format(pending.summary.createdAt),
                        ),
                    )
                    pending.summary.device?.let { device ->
                        Text(stringResource(R.string.backup_confirm_device, device))
                    }
                    CountsText(
                        exemplars = pending.summary.counts["exemplar"] ?: 0,
                        photos = pending.summary.imageCount,
                    )
                    Text(
                        stringResource(
                            R.string.backup_confirm_size,
                            formatBytes(pending.summary.totalBytes),
                        ),
                    )
                    if (pending.summary.missingImages > 0) {
                        Text(
                            pluralStringResource(
                                R.plurals.backup_confirm_missing_images,
                                pending.summary.missingImages,
                                pending.summary.missingImages,
                            ),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(stringResource(R.string.backup_confirm_warning))
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmRestore) {
                    Text(stringResource(R.string.backup_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissPending) {
                    Text(stringResource(R.string.backup_cancel))
                }
            },
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    summary: BackupSummary,
    onDismiss: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.backup_confirm_created_at, DATE_FORMAT.format(summary.createdAt)),
            )
            CountsText(
                exemplars = summary.counts["exemplar"] ?: 0,
                photos = summary.imageCount,
            )
            Text(stringResource(R.string.backup_confirm_size, formatBytes(summary.totalBytes)))
            if (summary.missingImages > 0) {
                Text(
                    text = pluralStringResource(
                        R.plurals.backup_missing_images,
                        summary.missingImages,
                        summary.missingImages,
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = stringResource(R.string.backup_created_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.backup_dismiss))
            }
        }
    }
}

@Composable
private fun CountsText(exemplars: Int, photos: Int) {
    Text(
        text = pluralStringResource(R.plurals.backup_counts_exemplars, exemplars, exemplars) +
            " · " +
            pluralStringResource(R.plurals.backup_counts_photos, photos, photos),
    )
}

private fun BackupFailure.messageRes(): Int = when (this) {
    BackupFailure.NOT_A_PACKAGE -> R.string.backup_error_not_a_package
    BackupFailure.UNSUPPORTED_FORMAT -> R.string.backup_error_unsupported_format
    BackupFailure.MISSING_FILE -> R.string.backup_error_missing_file
    BackupFailure.SIZE_MISMATCH -> R.string.backup_error_size
    BackupFailure.CHECKSUM_MISMATCH -> R.string.backup_error_checksum
    BackupFailure.COUNT_MISMATCH -> R.string.backup_error_count
    BackupFailure.INVALID_DATABASE -> R.string.backup_error_database
    BackupFailure.NEWER_SCHEMA -> R.string.backup_error_newer_schema
    BackupFailure.FOREIGN_KEY_BROKEN -> R.string.backup_error_foreign_key
    BackupFailure.WRITE_FAILED -> R.string.backup_error_write
}

private fun formatBytes(bytes: Long): String {
    val megabytes = bytes / (1024.0 * 1024.0)
    return if (megabytes >= 1.0) {
        String.format(LOCALE_PT_BR, "%.1f MB", megabytes)
    } else {
        String.format(LOCALE_PT_BR, "%.0f KB", bytes / 1024.0)
    }
}

private fun defaultBackupName(): String {
    val stamp = java.time.LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
    return "colecao-backup-$stamp.zip"
}

/** Depois da restauração o banco injetado está fechado: só um processo novo o reabre. */
private fun restart(context: Context) {
    val intent = context.packageManager
        .getLaunchIntentForPackage(context.packageName)
        ?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    if (intent != null) context.startActivity(intent)
    exitProcess(0)
}

private const val ZIP_MIME = "application/zip"

private val LOCALE_PT_BR: Locale = Locale.forLanguageTag("pt-BR")

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())
