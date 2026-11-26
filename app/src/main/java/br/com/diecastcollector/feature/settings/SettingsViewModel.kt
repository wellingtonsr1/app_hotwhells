package br.com.diecastcollector.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.diecastcollector.appearance.AppearancePreferences
import br.com.diecastcollector.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado de Ajustes: lembrete de backup (T120) e aparência (Spec 006).
 *
 * O tema não é resolvido aqui — o ViewModel só guarda/expõe o modo escolhido; a paleta é
 * decidida em `DiecastTheme` (FR-405).
 */
@HiltViewModel
public class SettingsViewModel @Inject constructor(
    private val reminderStore: BackupReminderStore,
    private val appearancePreferences: AppearancePreferences,
) : ViewModel() {

    public val reminder: StateFlow<BackupReminder> = reminderStore.reminder

    /** Modo efetivamente aplicado; a seção Aparência mostra exatamente este valor. */
    public val appearance: StateFlow<ThemeMode> = appearancePreferences.mode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    private val _appearanceWriteFailed = MutableStateFlow(false)

    /** Aviso não bloqueante de falha de escrita (FR-414); nenhum diálogo modal. */
    public val appearanceWriteFailed: StateFlow<Boolean> = _appearanceWriteFailed.asStateFlow()

    public fun setIntervalDays(days: Int) {
        reminderStore.setIntervalDays(days)
    }

    public fun setAppearance(mode: ThemeMode) {
        viewModelScope.launch {
            val ok = runCatching { appearancePreferences.setMode(mode) }.isSuccess
            // Em falha o fluxo não muda: a aparência anterior continua valendo.
            _appearanceWriteFailed.value = !ok
        }
    }
}
