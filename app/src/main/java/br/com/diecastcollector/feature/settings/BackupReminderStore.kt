package br.com.diecastcollector.feature.settings

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferência local do lembrete de backup.
 *
 * Fica em `SharedPreferences`, fora do banco: restaurar um pacote não pode "enganar" o
 * lembrete com a data de um backup antigo. O relógio é injetável para os testes.
 */
@Singleton
public class BackupReminderStore @Inject constructor(
    @ApplicationContext context: Context,
    private val clock: Clock,
) {

    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _reminder: MutableStateFlow<BackupReminder> = MutableStateFlow(read())
    public val reminder: StateFlow<BackupReminder> = _reminder.asStateFlow()

    /** Chamado só quando o pacote foi gravado com sucesso. */
    public fun markBackup() {
        preferences.edit { putLong(KEY_LAST_BACKUP, clock.millis()) }
        _reminder.value = read()
    }

    public fun setIntervalDays(days: Int) {
        require(days >= 0) { "Intervalo de lembrete não pode ser negativo." }
        preferences.edit { putInt(KEY_INTERVAL_DAYS, days) }
        _reminder.value = read()
    }

    private fun read(): BackupReminder {
        val last = if (preferences.contains(KEY_LAST_BACKUP)) {
            Instant.ofEpochMilli(preferences.getLong(KEY_LAST_BACKUP, 0L))
        } else {
            null
        }
        return BackupReminder(
            lastBackupAt = last,
            intervalDays = preferences.getInt(
                KEY_INTERVAL_DAYS,
                BackupReminder.DEFAULT_INTERVAL_DAYS,
            ),
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "backup_reminder"
        const val KEY_LAST_BACKUP = "last_backup_at"
        const val KEY_INTERVAL_DAYS = "interval_days"
    }
}
