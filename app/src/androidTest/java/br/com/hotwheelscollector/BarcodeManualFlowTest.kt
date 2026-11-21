package br.com.hotwheelscollector

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.hotwheelscollector.testing.freshInstallRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * FR-112 — o código lido em «Cadastrar manualmente» chega ao formulário da variante.
 *
 * Sobe o `MainActivity` real e percorre as duas rotas que carregam o código: catálogo →
 * código de barras → cadastro de miniatura (`?code=`) → variante (`?code=`). O ponto do
 * teste é o transporte; por isso ele para quando o código aparece no campo do fabricante,
 * antes de salvar a variante.
 */
@RunWith(AndroidJUnit4::class)
class BarcodeManualFlowTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @Test
    fun codigoLidoSemCorrespondenciaChegaAoCodigoDoFabricante() {
        composeRule.onNodeWithText("Catálogo").performClick()
        waitForText("Nenhuma miniatura ainda")

        composeRule.onNodeWithText("Código de barras").performClick()
        waitForText("Ler código digitado")

        composeRule.onAllNodes(hasSetTextAction())[0].apply {
            performClick()
            performTextInput("HKC25")
        }
        composeRule.onNodeWithText("Ler código digitado").performClick()
        waitForText("Nenhuma correspondência no catálogo.")

        composeRule.onNodeWithText("Cadastrar manualmente").performClick()
        waitForText("Nova miniatura")

        composeRule.onAllNodes(hasSetTextAction())[0].apply {
            performClick()
            performTextInput("Mattel")
        }
        composeRule.onAllNodes(hasSetTextAction())[1].apply {
            performClick()
            performTextInput("Twin Mill")
        }
        composeRule.onNodeWithText("Salvar miniatura").performScrollTo().performClick()
        waitForText("Cadastrar variante")

        waitForText("HKC25")
        composeRule.onNodeWithText("HKC25").performScrollTo().assertIsDisplayed()
    }

    private fun waitForText(text: String, substring: Boolean = false) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text, substring = substring)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
