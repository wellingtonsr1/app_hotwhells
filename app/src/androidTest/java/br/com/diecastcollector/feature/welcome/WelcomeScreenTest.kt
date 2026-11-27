package br.com.diecastcollector.feature.welcome

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.ui.theme.DiecastTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Apresentação: a arte da marca aparece com o nome do produto, a assinatura e uma única
 * ação que abre o app. O toque é o que grava a preferência — por isso o teste confere só a
 * chamada de [WelcomeScreen] aqui; a persistência fica em `WelcomePreferencesTest`.
 */
@RunWith(AndroidJUnit4::class)
public class WelcomeScreenTest {

    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun mostraAMarcaEASsinaTUraComAcaoDeEntrada() {
        var started = false
        composeRule.setContent {
            DiecastTheme {
                WelcomeScreen(onStart = { started = true })
            }
        }

        composeRule.onNodeWithText("DIECAST COLLECTOR").assertIsDisplayed()
        composeRule.onNodeWithText("Sua coleção. Sua garagem.").assertIsDisplayed()

        composeRule.onNodeWithText("Começar").assertIsDisplayed().performClick()

        assertTrue("O botão de entrada deveria avisar a tela", started)
    }
}
