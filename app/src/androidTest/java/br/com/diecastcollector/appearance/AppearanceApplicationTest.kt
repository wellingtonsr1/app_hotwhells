package br.com.diecastcollector.appearance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.feature.settings.SettingsScreen
import br.com.diecastcollector.ui.theme.DiecastTheme
import br.com.diecastcollector.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Aplicação imediata da aparência forçada (Spec 006, T410, FR-406, FR-426).
 *
 * Trocar Claro ↔ Escuro não recria a tela nem volta a lista ao topo: a paleta muda e a
 * posição da rolagem continua onde estava. O acompanhamento do tema do Android em
 * **Sistema** é coberto em [AppearanceSystemFollowTest].
 */
@RunWith(AndroidJUnit4::class)
public class AppearanceApplicationTest {

    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun alternarClaroEEscuroMantemARolagemDaLista() {
        var mode by mutableStateOf(ThemeMode.LIGHT)

        composeRule.setContent {
            DiecastTheme(mode = mode) {
                SettingsScreen(onAbout = {}, onPrivacy = {})
            }
        }

        // Desce até o fim da lista: essa posição é o estado a preservar.
        composeRule.onNodeWithText("Política de privacidade").performScrollTo().assertIsDisplayed()

        composeRule.runOnIdle { mode = ThemeMode.DARK }
        composeRule.waitForIdle()

        // Sem `performScrollTo`: o cartão continua visível na mesma posição.
        composeRule.onNodeWithText("Política de privacidade").assertIsDisplayed()
        AppearanceTestSupport.assertPalette(
            "Ajustes depois de Escuro",
            AppearanceTestSupport.bitmapOf(composeRule.onRoot()),
            dark = true,
        )

        composeRule.runOnIdle { mode = ThemeMode.LIGHT }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Política de privacidade").assertIsDisplayed()
        AppearanceTestSupport.assertPalette(
            "Ajustes depois de Claro",
            AppearanceTestSupport.bitmapOf(composeRule.onRoot()),
            dark = false,
        )
    }
}
