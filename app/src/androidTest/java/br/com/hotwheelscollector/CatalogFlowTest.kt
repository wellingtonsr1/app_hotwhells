package br.com.hotwheelscollector

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
 * Fluxo P1 inteiro no aplicativo de verdade: catálogo vazio, modelo, variante, exemplar,
 * repetição.
 *
 * Sobe o [MainActivity] real — grafo Hilt, `Room` em arquivo, navegação e ViewModels de
 * produção — e conversa com as telas pela semântica do Compose, que é o mesmo caminho do
 * toque. Um teste de conteúdo não pegaria o que este pega: cadastro que não navega, contagem
 * que não atualiza, aviso de variante repetida que some no caminho.
 *
 * O banco é apagado antes da activity subir, então o fluxo sempre começa como numa
 * instalação nova (é o estado que a US1 pressupõe). Precisa de emulador ou aparelho.
 */
@RunWith(AndroidJUnit4::class)
class CatalogFlowTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    /**
     * Instalação limpa: roda antes da activity. Limpa as tabelas em vez de apagar o arquivo
     * do banco, que já está aberto pelo Hilt desde o primeiro teste do processo.
     */
    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @Test
    fun catalogoAteDoisExemplaresDaMesmaVariante() {
        // 1. Instalação nova: o catálogo abre vazio.
        waitForText(EMPTY_CATALOG)

        // 2. O FAB abre o cadastro de miniatura.
        composeRule.onNodeWithText("+ Mini").performClick()
        waitForText("Nova miniatura")

        // 3. Fabricante e modelo nos campos reais, na ordem da tela.
        composeRule.onAllNodes(hasSetTextAction())[0].apply {
            performClick()
            performTextInput("Mattel")
        }
        composeRule.onAllNodes(hasSetTextAction())[1].apply {
            performClick()
            performTextInput("Twin Mill")
        }

        // 4. Salvar o modelo leva ao cadastro completo da variante.
        //    A variante não nasce mais no primeiro passo: cor, rodas, base e fotos são dela.
        composeRule.onNodeWithText("Salvar miniatura").performScrollTo().performClick()
        waitForText("Cadastrar variante")

        // 5. A tela de variante cria também o primeiro exemplar; as fotos ficam para depois.
        composeRule.onNodeWithText("Salvar variante e exemplar").performScrollTo().performClick()
        waitForText("1 exemplar", substring = true)

        // 6. O cartão da variante é o outro caminho de entrada do exemplar.
        composeRule.onNodeWithText("Twin Mill").performScrollTo().performClick()
        waitForText("Esta variante já tem 1 exemplar(es).")

        // 7. Repetir a variante avisa, mas não bloqueia (FR-039).
        composeRule.onNodeWithText("Salvar exemplar").performScrollTo().performClick()
        waitForText("2 exemplares", substring = true)
    }

    /**
     * Espera o texto aparecer: gravar no banco é assíncrono, então a tela muda depois do
     * clique e uma asserção imediata daria falso negativo.
     */
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
