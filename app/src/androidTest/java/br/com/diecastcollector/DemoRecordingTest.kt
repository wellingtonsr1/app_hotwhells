package br.com.diecastcollector

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.testing.freshInstallRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/** Roteiro de gravação: painel com Investido/Estimado/Diferença preenchidos.

 * Roda o app de verdade (MainActivity, Hilt, Room em arquivo) e drive a UI pelo
 * Compose Semantics — o mesmo caminho que o toque real percorre. Sem stubs de ViewModel:
 * o `CatalogFlowTest` já mostra que esse caminho funciona com o banco real.
 * O roteiro reutiliza exatamente esse fluxo, que é o que o vídeo precisa demonstrar.
 *
 * O vídeo começa com o app aberto no painel (Dashboard), que é a tela inicial do app.
 * Para que Investido/Estimado/Diferença apareçam preenchidos, o roteiro primeiro popula
 * uma miniatura/variante/exemplar com preço e só então volta ao painel — e pausa aí,
 * com os três valores já visíveis.
 */
@RunWith(AndroidJUnit4::class)
class DemoRecordingTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    /** Instalação nova a cada teste: banco vazio e sem fotos dos testes anteriores. */
    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @Test
    fun painelComInvestidoEstimadoEDiferencaPreenchidos() {
        // O app abre no painel (startDestination = Dashboard). Deixamos o painel vazio
        // visível por alguns segundos para o vídeo começar com ele.
        waitForText("Painel")
        composeRule.waitUntil(timeoutMillis = 3_000L) { true }

        // Popula o acervo: Miniatura → Variante → Exemplar com preço.
        preencheCatalogo()

        // Volta ao painel (que é a aba «Painel», mostrar os 11 indicadores).
        composeRule.onNodeWithText("Painel").performClick()
        waitForPainelPreenchido()

        // Mantém o painel preenchido por tempo suficiente para o vídeo gravá-lo.
        composeRule.waitUntil(timeoutMillis = 8_000L) { true }
    }

    /** Catálogo vazio → + Mini → modelo → variante → exemplar com preço → volta ao catálogo. */
    private fun preencheCatalogo() {
        // 1. Abre a aba do catálogo.
        composeRule.onNodeWithText("Catálogo").performClick()
        waitForText("Nenhuma miniatura ainda")

        // 2. O FAB abre o cadastro da miniatura.
        composeRule.onNodeWithText("+ Mini").performClick()
        waitForText("Nova miniatura")

        // 3. Fabricante e modelo nos campos reais, na ordem da tela.
        typeIntoField(0, "Mattel")
        typeIntoField(1, "Twin Mill")

        // 4. Salvar o modelo leva ao cadastro da variante.
        composeRule.onNodeWithText("Salvar miniatura").performScrollTo().performClick()
        waitForText("Cadastrar variante")

        // 5. Cor principal e ano (os dois atributos que importam para o vídeo).
        typeIntoField(0, "Vermelho")
        typeIntoField(1, "2024")

        // 6. Condição do exemplar, que nasce junto com a variante.
        composeRule.onNodeWithText("Solto").performScrollTo().performClick()

        // 7. Salvar variante + exemplar devolve ao catálogo, já com a miniatura.
        composeRule.onNodeWithText("Salvar variante e exemplar").performScrollTo().performClick()
        waitForText("1 exemplar", substring = true)
        composeRule.onNodeWithText("Twin Mill").assertExists()

        // 8. A variante já tem exemplar: tocar nela abre o novo exemplar com preço.
        composeRule.onNodeWithText("Twin Mill").performScrollTo().performClick()
        waitForText("Esta variante já tem 1 exemplar(es).")

        // 9. Preço pago digitado e salvo.
        composeRule.onNodeWithText("Preço pago (R$)").performScrollTo().performClick()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("49,90")
        composeRule.onNodeWithText("Salvar exemplar").performScrollTo().performClick()
        waitForText("2 exemplares", substring = true)

        // 10. Volta ao catálogo para confirmação.
        composeRule.onNodeWithText("Catálogo").performClick()
        waitForText("Twin Mill")
        composeRule.onNodeWithText("2 exemplares", substring = true).assertExists()
    }

    /** Espera o painel estar pronto e mostrar os três valores principais preenchidos. */
    private fun waitForPainelPreenchido() {
        // O painel carrega com Loading; espera ele virar Ready com conteúdo.
        waitForText("R$ 49,90")
        waitForText("Investido")
        waitForText("Estimado")
        waitForText("Diferença")
    }

    private fun typeIntoField(index: Int, text: String) {
        composeRule.onAllNodes(hasSetTextAction())[index].apply {
            performClick()
            performTextInput(text)
        }
    }

    private fun waitForText(text: String, substring: Boolean = false) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text, substring = substring)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
    }
}
