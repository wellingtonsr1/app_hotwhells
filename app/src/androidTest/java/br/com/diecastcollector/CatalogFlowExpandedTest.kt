package br.com.diecastcollector

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.testing.DeviceWindow
import br.com.diecastcollector.testing.freshInstallRule
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Cadastro completo em janela expandida (T324, SC-309).
 *
 * O mesmo roteiro de [CatalogFlowTest], agora em 1.000 dp com o trilho lateral: catálogo,
 * miniatura, variante com o primeiro exemplar e repetição da variante. Seis toques, o mesmo
 * número do fluxo compacto — a janela maior não pode exigir passos extras nem esconder ação.
 *
 * Sobe a activity real (Hilt + Room em arquivo) e redimensiona a janela com `wm size` para
 * 1.000 dp: a navegação e a classe de janela exercitadas são as de produção.
 */
@RunWith(AndroidJUnit4::class)
class CatalogFlowExpandedTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @After
    fun restaurarJanela() {
        DeviceWindow.reset()
    }

    @Test
    fun cadastroCompletoEmJanelaExpandida() {
        DeviceWindow.use(widthDp = 1000)
        composeRule.waitForIdle()

        // 1. O acervo abre pelo trilho.
        composeRule.onNodeWithText("Catálogo").performClick()
        waitForText(EMPTY_CATALOG)

        // 2. O FAB abre o cadastro de miniatura.
        composeRule.onNodeWithText("+ Mini").performClick()
        waitForText("Nova miniatura")

        // 3. Fabricante e modelo nos campos reais.
        composeRule.onAllNodes(hasSetTextAction())[0].apply {
            performClick()
            performTextInput("Mattel")
        }
        composeRule.onAllNodes(hasSetTextAction())[1].apply {
            performClick()
            performTextInput("Twin Mill")
        }

        // 4. Salvar o modelo leva ao cadastro completo da variante.
        composeRule.onNodeWithText("Salvar miniatura").performScrollTo().performClick()
        waitForText("Cadastrar variante")

        // 5. A variante cria o primeiro exemplar.
        composeRule.onNodeWithText("Salvar variante e exemplar").performScrollTo().performClick()
        waitForText("1 exemplar", substring = true)

        // 6. O cartão da variante abre o exemplar e a repetição é permitida com aviso.
        composeRule.onNodeWithText("Twin Mill").performScrollTo().performClick()
        waitForText("Esta variante já tem 1 exemplar(es).")
        composeRule.onNodeWithText("Salvar exemplar").performScrollTo().performClick()
        waitForText("2 exemplares", substring = true)
    }

    private fun waitForText(text: String, substring: Boolean = false) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text, substring = substring)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private companion object {
        const val EMPTY_CATALOG = "Nenhuma miniatura ainda"
        const val TIMEOUT_MS = 10_000L
    }
}
