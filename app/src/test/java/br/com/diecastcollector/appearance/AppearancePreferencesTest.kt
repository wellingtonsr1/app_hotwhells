package br.com.diecastcollector.appearance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.diecastcollector.ui.theme.ThemeMode
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Preferência de aparência sobre um DataStore isolado em diretório temporário
 * (Spec 006, FR-404, FR-412–FR-416, NFR-405).
 */
public class AppearancePreferencesTest {

    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var preferences: AppearancePreferences
    private lateinit var file: java.io.File

    @Before
    public fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        file = Files.createTempDirectory("appearance-test")
            .resolve("appearance.preferences_pb")
            .toFile()
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        preferences = AppearancePreferences(dataStore)
    }

    @After
    public fun tearDown() {
        scope.cancel()
    }

    @Test
    public fun ausenciaDePreferenciaUsaSistema() = runBlocking {
        assertThat(preferences.mode.first()).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    public fun idaEVoltaDosTresValores() = runBlocking {
        ThemeMode.entries.forEach { mode ->
            preferences.setMode(mode)
            assertThat(preferences.mode.first()).isEqualTo(mode)
        }
    }

    @Test
    public fun valorDesconhecidoCaiEmSistema() = runBlocking {
        dataStore.edit { it[stringPreferencesKey(AppearancePreferences.KEY_NAME)] = "purple" }

        assertThat(preferences.mode.first()).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    public fun escritaConcorrenteDeixaUmValorIntegro() = runBlocking {
        val modes = listOf(
            ThemeMode.LIGHT,
            ThemeMode.DARK,
            ThemeMode.SYSTEM,
            ThemeMode.DARK,
            ThemeMode.LIGHT,
        )
        modes.map { mode -> launch { preferences.setMode(mode) } }.joinAll()

        assertThat(preferences.mode.first()).isIn(modes)
    }

    /**
     * A escolha sai da memória e vai para o arquivo — é isso que faz a preferência
     * sobreviver ao processo e ao aparelho (FR-412). Reabrir o arquivo com um segundo
     * `DataStore` vivo no mesmo processo não é suportado pela biblioteca.
     */
    @Test
    public fun escolhaEGravadaNoDisco() = runBlocking {
        preferences.setMode(ThemeMode.DARK)
        assertThat(preferences.mode.first()).isEqualTo(ThemeMode.DARK)

        assertThat(file.exists()).isTrue()
        assertThat(file.length()).isGreaterThan(0L)
    }

    @Test
    public fun decodificacaoAceitaSomenteOsValoresCanonicos() {
        assertThat(AppearancePreferences.decode(null)).isEqualTo(ThemeMode.SYSTEM)
        assertThat(AppearancePreferences.decode("")).isEqualTo(ThemeMode.SYSTEM)
        assertThat(AppearancePreferences.decode("SYSTEM")).isEqualTo(ThemeMode.SYSTEM)
        assertThat(AppearancePreferences.decode("system")).isEqualTo(ThemeMode.SYSTEM)
        assertThat(AppearancePreferences.decode("light")).isEqualTo(ThemeMode.LIGHT)
        assertThat(AppearancePreferences.decode("dark")).isEqualTo(ThemeMode.DARK)
    }

    /**
     * FR-404/NFR-405: falha de leitura não pode chegar à UI. O fluxo cai em
     * [ThemeMode.SYSTEM], que é o padrão seguro — nunca "claro" nem "escuro" por engano.
     */
    @Test
    public fun falhaDeLeituraCaiEmSistemaSemLancar() = runBlocking {
        val ilegivel = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("disco ilegível") }

            override suspend fun updateData(
                transform: suspend (Preferences) -> Preferences,
            ): Preferences = throw IOException("somente leitura")
        }

        val preferencias = AppearancePreferences(ilegivel)

        assertThat(preferencias.mode.first()).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    public fun codificacaoUsaValoresEstaveis() {
        assertThat(AppearancePreferences.encode(ThemeMode.SYSTEM)).isEqualTo("system")
        assertThat(AppearancePreferences.encode(ThemeMode.LIGHT)).isEqualTo("light")
        assertThat(AppearancePreferences.encode(ThemeMode.DARK)).isEqualTo("dark")
    }
}
