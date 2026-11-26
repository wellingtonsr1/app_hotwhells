package br.com.diecastcollector.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.diecastcollector.R
import br.com.diecastcollector.ui.components.FormColumn
import br.com.diecastcollector.ui.theme.ThemeMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Ajustes (T132, T133, T120).
 *
 * Só entram aqui opções que já existem de verdade. Importação e relatórios dependem do
 * restante da US10 e não aparecem como item desabilitado sem explicação.
 *
 * O lembrete de backup segue o mesmo formato das demais opções — título e uma linha de
 * situação — e abre os detalhes (último backup, intervalo e atalho) ao toque.
 */
@Composable
public fun SettingsRoute(
    onAbout: () -> Unit,
    onPrivacy: () -> Unit,
    onLocations: () -> Unit,
    onBackup: () -> Unit,
    onDataControl: () -> Unit,
    onReports: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val reminder by viewModel.reminder.collectAsState()
    val appearance by viewModel.appearance.collectAsState()
    val appearanceWriteFailed by viewModel.appearanceWriteFailed.collectAsState()
    SettingsScreen(
        onAbout = onAbout,
        onPrivacy = onPrivacy,
        onLocations = onLocations,
        onBackup = onBackup,
        onDataControl = onDataControl,
        onReports = onReports,
        onImport = onImport,
        modifier = modifier,
        reminder = reminder,
        onIntervalChange = viewModel::setIntervalDays,
        appearance = appearance,
        onAppearanceChange = viewModel::setAppearance,
        appearanceWriteFailed = appearanceWriteFailed,
    )
}

@Composable
public fun SettingsScreen(
    onAbout: () -> Unit,
    onPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    onLocations: () -> Unit = {},
    onBackup: () -> Unit = {},
    onDataControl: () -> Unit = {},
    onReports: () -> Unit = {},
    onImport: () -> Unit = {},
    reminder: BackupReminder? = null,
    onIntervalChange: (Int) -> Unit = {},
    appearance: ThemeMode = ThemeMode.SYSTEM,
    onAppearanceChange: (ThemeMode) -> Unit = {},
    appearanceWriteFailed: Boolean = false,
    now: Instant = Instant.now(),
) {
    var showReminder by remember { mutableStateOf(false) }

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        // T369: os 8 cartões param de esticar em E e a coluna rola dentro da altura
        // disponível.
        FormColumn(
            modifier = Modifier.padding(padding),
            scrollable = true,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        // Spec 006, FR-402: seção dentro de Ajustes, sem rota nova. Fica na mesma
        // FormColumn das demais opções, herdando o limite de 720 dp e a rolagem (Spec 005).
        AppearanceSection(
            appearance = appearance,
            onAppearanceChange = onAppearanceChange,
            writeFailed = appearanceWriteFailed,
        )
        if (reminder != null) {
            val overdue = reminder.isOverdue(now)
            SettingsRow(
                title = stringResource(R.string.settings_backup_reminder),
                subtitle = reminderSummary(reminder, overdue),
                subtitleColor = if (overdue) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                onClick = { showReminder = true },
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
            title = stringResource(R.string.reports_open),
            subtitle = stringResource(R.string.reports_open_subtitle),
            onClick = onReports,
        )
        SettingsRow(
            title = stringResource(R.string.import_open),
            subtitle = stringResource(R.string.import_open_subtitle),
            onClick = onImport,
        )
        SettingsRow(
            title = stringResource(R.string.settings_data_control),
            subtitle = stringResource(R.string.settings_data_control_subtitle),
            onClick = onDataControl,
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
    }

    if (showReminder && reminder != null) {
        AlertDialog(
            onDismissRequest = { showReminder = false },
            title = { Text(stringResource(R.string.settings_backup_reminder)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = reminder.lastBackupAt?.let { last ->
                            stringResource(
                                R.string.settings_backup_reminder_last,
                                REMINDER_DATE.format(last),
                            )
                        } ?: stringResource(R.string.settings_backup_reminder_never),
                    )
                    if (reminder.isOverdue(now)) {
                        Text(
                            text = reminder.daysSinceLast(now)?.let { days ->
                                pluralStringResource(
                                    R.plurals.settings_backup_reminder_overdue,
                                    days.toInt(),
                                    days,
                                )
                            } ?: stringResource(R.string.settings_backup_reminder_overdue_never),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings_backup_reminder_choose),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    BackupReminder.INTERVAL_OPTIONS.forEach { days ->
                        TextButton(
                            onClick = {
                                onIntervalChange(days)
                                showReminder = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = if (days == 0) {
                                    stringResource(R.string.settings_backup_reminder_off)
                                } else {
                                    pluralStringResource(
                                        R.plurals.settings_backup_reminder_every,
                                        days,
                                        days,
                                    )
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReminder = false
                        onBackup()
                    },
                ) {
                    Text(stringResource(R.string.settings_backup_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminder = false }) {
                    Text(stringResource(R.string.settings_backup_reminder_close))
                }
            },
        )
    }
}

/**
 * Seção **Aparência** (Spec 006, FR-401–FR-403, FR-424, FR-425).
 *
 * Seleção única com [RadioButton]: a marca não depende só de cor e o grupo é anunciado
 * como seleção única pelo leitor de tela. Cada linha inteira é tocável e tem no mínimo
 * 48 dp de altura, mesmo com fonte ampliada.
 */
@Composable
private fun AppearanceSection(
    appearance: ThemeMode,
    onAppearanceChange: (ThemeMode) -> Unit,
    writeFailed: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_appearance),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.settings_appearance_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    val selected = appearance == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onAppearanceChange(mode) },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(mode.labelRes()),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            if (writeFailed) {
                Text(
                    text = stringResource(R.string.settings_appearance_write_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_appearance_system
    ThemeMode.LIGHT -> R.string.settings_appearance_light
    ThemeMode.DARK -> R.string.settings_appearance_dark
}

/** Uma linha, como as outras opções: intervalo + situação. */
@Composable
private fun reminderSummary(reminder: BackupReminder, overdue: Boolean): String {
    if (!reminder.enabled) return stringResource(R.string.settings_backup_reminder_off)
    val interval = pluralStringResource(
        R.plurals.settings_backup_reminder_every,
        reminder.intervalDays,
        reminder.intervalDays,
    )
    val status = when {
        reminder.lastBackupAt == null -> stringResource(R.string.settings_backup_reminder_status_none)
        overdue -> stringResource(R.string.settings_backup_reminder_status_overdue)
        else -> stringResource(R.string.settings_backup_reminder_status_ok)
    }
    return "$interval · $status"
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = subtitleColor,
            )
        }
    }
}

private val REMINDER_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())
