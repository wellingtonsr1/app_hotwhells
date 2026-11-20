package br.com.hotwheelscollector.feature.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Estado de Ajustes: por ora, só o lembrete de backup (T120). */
@HiltViewModel
public class SettingsViewModel @Inject constructor(
    private val reminderStore: BackupReminderStore,
) : ViewModel() {

    public val reminder: StateFlow<BackupReminder> = reminderStore.reminder

    public fun setIntervalDays(days: Int) {
        reminderStore.setIntervalDays(days)
    }
}
