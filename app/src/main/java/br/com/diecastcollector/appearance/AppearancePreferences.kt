package br.com.diecastcollector.appearance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.diecastcollector.ui.theme.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Preferência local de aparência (Spec 006, FR-404, FR-412–FR-416).
 *
 * Um único valor por instalação (BR-401), fora do banco e fora do pacote de backup da
 * coleção: restaurar um pacote não muda a aparência (FR-415). A leitura é assíncrona e
 * não bloqueia a interface (FR-413); a escrita é transacional (NFR-405).
 *
 * Ausência, valor desconhecido ou falha de leitura equivalem a [ThemeMode.SYSTEM]
 * (FR-404, BR-402) — nunca "claro" nem "escuro" por conveniência.
 */
@Singleton
public class AppearancePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    /**
     * Modo efetivamente gravado. O primeiro valor é [ThemeMode.SYSTEM]: quem observa não
     * precisa bloquear a abertura esperando o disco (NFR-402).
     */
    public val mode: Flow<ThemeMode> = dataStore.data
        .catch { cause ->
            // FR-414: falha de leitura não derruba a UI nem apaga nada — cai em SYSTEM.
            if (cause is CancellationException) throw cause
            emit(emptyPreferences())
        }
        .map { preferences -> decode(preferences[KEY]) }

    /**
     * Grava a escolha. A exceção de escrita sobe para o chamador, que avisa sem bloquear
     * e mantém a aparência anterior (FR-414).
     */
    public suspend fun setMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[KEY] = encode(mode) }
    }

    public companion object {
        /** Chave versionada do valor persistido (data-model.md §1). */
        public const val KEY_NAME: String = "appearance_mode"

        private val KEY: Preferences.Key<String> = stringPreferencesKey(KEY_NAME)

        /** Forma persistida: valores estáveis em minúsculas, independentes do enum. */
        public fun encode(mode: ThemeMode): String = when (mode) {
            ThemeMode.SYSTEM -> "system"
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK -> "dark"
        }

        /** Decodificação tolerante: qualquer valor fora dos canônicos é [ThemeMode.SYSTEM]. */
        public fun decode(value: String?): ThemeMode = when (value) {
            "system" -> ThemeMode.SYSTEM
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }
}
