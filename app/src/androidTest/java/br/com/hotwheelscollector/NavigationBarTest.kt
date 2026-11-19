package br.com.hotwheelscollector

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.hotwheelscollector.testing.freshInstallRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Barra de navegação com o app de verdade (T050).
 *
 * As cinco áreas ganharam aba e conteúdo próprio: o teste percorre todas e confere o
 * estado vazio ou o conteúdo inicial de cada uma. Também garante que o app abre no painel
 * e que o acervo continua a um toque de distância.
 */
@RunWith(AndroidJUnit4::class)
class NavigationBarTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    /** Instalação limpa: o painel de um banco sem registros é o estado vazio. */
    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @Test
    fun abreNoPainelEPercorreAsCincoAbas() {
        // 1. A primeira tela é o painel, e sem dados ele explica o que fazer.
        waitForText("Coleção vazia")

        // 2. O acervo é uma aba, não a tela inicial.
        composeRule.onNodeWithText("Catálogo").performClick()
        waitForText("+ Mini")

        // 3. Desejos: sem registros, a lista explica o que cadastrar.
        composeRule.onNodeWithText("Desejos").performClick()
        waitForText("Nada na lista de desejos")

        // 4. Financeiro: resumo e agrupamentos calculados a partir das aquisições.
        composeRule.onNodeWithText("Financeiro").performClick()
        waitForText("Gastos por fabricante")

        // 5. Ajustes: hub com Sobre e privacidade offline.
        composeRule.onNodeWithText("Ajustes").performClick()
        waitForText("Sobre o app")

        // 6. E dá para voltar ao painel pela barra.
        composeRule.onNodeWithText("Painel").performClick()
        waitForText("Coleção vazia")
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
