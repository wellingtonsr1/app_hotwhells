package br.com.diecastcollector.appearance

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.annotation.MainThread
import br.com.diecastcollector.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modo noturno **por aplicativo** do sistema (Spec 006, FR-410, NFR-403).
 *
 * O fundo da janela/splash é criado pelo sistema **antes** de o processo existir, a partir
 * de `values`/`values-night`. O DataStore é assíncrono, então a paleta do app podia chegar
 * depois e o splash ficava com a paleta oposta em modo forçado. `setApplicationNightMode`
 * resolve isso: o sistema persiste a escolha por pacote e a aplica ao criar o processo,
 * inclusive na janela inicial.
 *
 * A preferência continua sendo **uma só** (o DataStore é a fonte da verdade); isto apenas
 * espelha o modo escolhido no mecanismo do sistema. Em **Sistema** o valor é
 * `MODE_NIGHT_AUTO`, que o AOSP traduz para `UI_MODE_NIGHT_UNDEFINED` — isto é, sem
 * sobreposição: o app volta a seguir o modo do Android (FR-407).
 *
 * Abaixo do API 31 o mecanismo não existe: o app continua correto, mas o splash segue o
 * Android até o primeiro quadro (limitação registrada em `docs/validacao.md`).
 */
@Singleton
public class ApplicationNightMode @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Último modo aplicado neste processo, para não repetir a chamada ao sistema. */
    private var applied: ThemeMode? = null

    @MainThread
    public fun apply(mode: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (applied == mode) return
        val manager = context.getSystemService(UiModeManager::class.java) ?: return
        manager.setApplicationNightMode(
            when (mode) {
                // Sem sobreposição por app: segue o modo do Android (FR-407).
                ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            },
        )
        applied = mode
    }
}
