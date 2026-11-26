package br.com.diecastcollector.appearance

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.MainActivity
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Persistência entre recriações (Spec 006, T411, FR-412, FR-415).
 *
 * Cada modo é escolhido pelo caminho do usuário, a activity é recriada (`recreate`, o mesmo
 * caminho de uma configuração alterada) e o teste confere a paleta aplicada e a marca na
 * seção Aparência. Encerramento real do processo e reinício do aparelho são evidência
 * manual de T413 — `am force-stop` mataria o processo de instrumentação.
 */
@RunWith(AndroidJUnit4::class)
class AppearancePersistenceTest {

    @get:Rule
    public val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    public fun cadaModoPersisteDepoisDeRecriarAActivity() {
        AppearanceActivitySupport.openSettings(composeRule)

        val systemDark = AppearanceActivitySupport.isSystemInDark()
        val modos = listOf(
            "Claro" to false,
            "Escuro" to true,
            "Sistema" to systemDark,
        )

        modos.forEach { (rotulo, escuro) ->
            AppearanceActivitySupport.selectMode(composeRule, rotulo)

            composeRule.activityRule.scenario.recreate()
            composeRule.waitForIdle()
            // A navegação recriada volta à aba inicial: reabre Ajustes antes de conferir.
            AppearanceActivitySupport.openSettings(composeRule)

            composeRule.onNodeWithText(rotulo).assertIsSelected()
            AppearanceActivitySupport.assertModePalette(composeRule, rotulo, escuro)
        }
    }

    /**
     * FR-415: o pacote de backup leva apenas o banco e as fotos, e a preferência mora em
     * `files/datastore/appearance.preferences_pb` — fora das duas. A restauração é simulada
     * substituindo o que o pacote substitui (as fotos) e recriando a activity.
     */
    @Test
    public fun restauracaoDeBackupNaoAlteraAAparencia() {
        AppearanceActivitySupport.openSettings(composeRule)
        AppearanceActivitySupport.selectMode(composeRule, "Escuro")

        val arquivo = AppearanceActivitySupport.preferenceFile()
        assertTrue(
            "A preferência deveria estar em ${arquivo.path}",
            arquivo.isFile,
        )
        assertTrue(
            "A preferência não pode ficar dentro de images/",
            !arquivo.path.contains("${File.separator}images${File.separator}"),
        )
        assertTrue(
            "A preferência não pode ficar dentro do arquivo do banco",
            !arquivo.name.endsWith(".db"),
        )

        File(
            composeRule.activity.filesDir,
            "images",
        ).deleteRecursively()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        AppearanceActivitySupport.openSettings(composeRule)

        composeRule.onNodeWithText("Escuro").assertIsSelected()
        AppearanceActivitySupport.assertModePalette(composeRule, "Escuro", dark = true)
    }
}
