package br.com.hotwheelscollector.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Ajustes (T132, T133, T120).
 *
 * Só entram aqui opções que já existem de verdade. Importação e relatórios dependem do
 * restante da US10 e não aparecem como item desabilitado sem explicação.
 *
 * O lembrete de backup aparece quando o intervalo escolhido vence; ele não bloqueia nada,
 * mas deixa visível que a coleção está só neste aparelho.
 */
@Composable
public fun SettingsRoute(
    onAbout: () -> Unit,
    onPrivacy: () -> Unit,
    onLocations: () -> Unit,
    onBackup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val reminder by viewModel.reminder.collectAsState()
    SettingsScreen(
        onAbout = onAbout,
        onPrivacy = onPrivacy,
        onLocations = onLocations,
        onBackup = onBackup,
        modifier = modifier,
        reminder = reminder,
        onIntervalChange = viewModel::setIntervalDays,
    )
}

@Composable
public fun SettingsScreen(
    onAbout: () -> Unit,
    onPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    onLocations: () -> Unit = {},
    onBackup: () -> Unit = {},
    reminder: BackupReminder? = null,
    onIntervalChange: (Int) -> Unit = {},
    now: Instant = Instant.now(),
) {
    var showIntervals by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        if (reminder != null) {
            BackupReminderCard(
                reminder = reminder,
                now = now,
                onBackup = onBackup,
                onChangeInterval = { showIntervals = true },
            )
        }
        SettingsRow(
            title = stringResource(R.string.locations_open),
            subtitle = stringResource(R.string.locations_open_subtitle),
            onClick = onLocations,
        )
        SettingsRow(
            title = stringResource(R.string.settings_backup),
            subtitle = stringResource(R.string.settings_backup_subtitle),
            onClick = onBackup,
        )
        SettingsRow(
            title = stringResource(R.string.settings_about),
            subtitle = stringResource(R.string.settings_about_subtitle),
            onClick = onAbout,
        )
        SettingsRow(
            title = stringResource(R.string.settings_privacy),
            subtitle = stringResource(R.string.settings_privacy_subtitle),
            onClick = onPrivacy,
        )
        Text(
            text = stringResource(R.string.settings_offline_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showIntervals) {
        AlertDialog(
            onDismissRequest = { showIntervals = false },
            title = { Text(stringResource(R.string.settings_backup_reminder_choose)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BackupReminder.INTERVAL_OPTIONS.forEach { days ->
                        TextButton(
                            onClick = {
                                onIntervalChange(days)
                                showIntervals = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = if (days == 0) {
                                    stringResource(R.string.settings_backup_reminder_off)
                                } else {
                                    pluralStringResource(R.plurals.settings_backup_reminder_every, days, days)
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIntervals = false }) {
                    Text(stringResource(R.string.settings_backup_reminder_close))
                }
            },
        )
    }
}

@Composable
private fun BackupReminderCard(
    reminder: BackupReminder,
    now: Instant,
    onBackup: () -> Unit,
    onChangeInterval: () -> Unit,
) {
    val overdue = reminder.isOverdue(now)
    Card {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_backup_reminder),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = reminder.lastBackupAt?.let { last ->
                    stringResource(
                        R.string.settings_backup_reminder_last,
                        REMINDER_DATE.format(last),
                    )
                } ?: stringResource(R.string.settings_backup_reminder_never),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (reminder.enabled) {
                    pluralStringResource(R.plurals.settings_backup_reminder_every, reminder.intervalDays, reminder.intervalDays)
                } else {
                    stringResource(R.string.settings_backup_reminder_off)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (overdue) {
                Text(
                    text = reminder.daysSinceLast(now)?.let { days ->
                        pluralStringResource(
                            R.plurals.settings_backup_reminder_overdue,
                            days.toInt(),
                            days,
                        )
                    } ?: stringResource(R.string.settings_backup_reminder_overdue_never),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onBackup) {
                    Text(stringResource(R.string.settings_backup_now))
                }
                TextButton(onClick = onChangeInterval) {
                    Text(stringResource(R.string.settings_backup_reminder_change))
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val REMINDER_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())
