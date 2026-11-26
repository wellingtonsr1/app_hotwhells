package br.com.diecastcollector.appearance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fallback da preferência (Spec 006, T412, FR-404, BR-402, NFR-405).
 *
 * Ausente, ilegível e desconhecida têm de cair em **Sistema**, sem crash e sem exceção
 * visível. Aqui os três casos rodam sobre arquivos reais do DataStore no aparelho.
 */
@RunWith(AndroidJUnit4::class)
public class AppearanceFallbackTest {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val arquivos = mutableListOf<File>()

    @After
    public fun limpar() {
        scope.cancel()
        arquivos.forEach { it.parentFile?.deleteRecursively() }
    }

    @Test
    public fun ausenciaDeArquivoUsaSistema() = runBlocking {
        val preferences = AppearancePreferences(store(arquivo("ausente")))

        assertEquals(ThemeMode.SYSTEM, preferences.mode.first())
    }

    @Test
    public fun valorDesconhecidoUsaSistema() = runBlocking {
        val store = store(arquivo("desconhecido"))
        store.edit { it[stringPreferencesKey(AppearancePreferences.KEY_NAME)] = "turquesa" }
        val preferences = AppearancePreferences(store)

        assertEquals(ThemeMode.SYSTEM, preferences.mode.first())
    }

    @Test
    public fun arquivoCorrompidoUsaSistemaSemLancar() = runBlocking {
        val arquivo = arquivo("corrompido")
        arquivo.parentFile?.mkdirs()
        arquivo.writeBytes(byteArrayOf(0x08, 0x7F, 0x00, 0x00, 0x13, 0x37, 0x42))

        val preferences = AppearancePreferences(store(arquivo))

        // Sem handler de corrupção o DataStore lança na leitura; o store precisa engolir e
        // entregar Sistema, senão a abertura do app morre antes do primeiro quadro.
        assertEquals(ThemeMode.SYSTEM, preferences.mode.first())
    }

    private fun arquivo(nome: String): File {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val arquivo = File(
            File(contexto.cacheDir, "appearance-fallback-$nome-${System.nanoTime()}"),
            "appearance.preferences_pb",
        )
        arquivos += arquivo
        return arquivo
    }

    private fun store(arquivo: File): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) { arquivo }
}
