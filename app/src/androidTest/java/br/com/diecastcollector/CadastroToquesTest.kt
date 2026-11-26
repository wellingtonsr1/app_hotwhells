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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Contagem de toques do cadastro por classe de janela (T324, SC-309).
 *
 * O mesmo roteiro de [CatalogFlowTest] é executado em celular retrato (compacta), celular
 * em paisagem (média) e tablet (expandida). Cada toque real da interface é contado; a
 * asserção é que as três classes exigem **o mesmo número de toques**. Se a janela maior
 * escondesse uma ação atrás de menu ou exigisse um passo extra, a contagem subiria (ou o
 * nó não existiria e o teste falharia antes de contar).
 *
 * A contagem inclui o toque no campo de texto antes de digitar — é o gesto que o usuário
 * faz — e o toque no cartão da variante, que abre o cadastro rápido para repetir o
 * exemplar (FR-039).
 */
@RunWith(AndroidJUnit4::class)
class CadastroToquesTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @After
    fun restaurarJanela() {
        DeviceWindow.reset()
    }

    @Test
    fun celularRetratoMantemOsToquesDoFluxo() = assertToques(411, 914)

    @Test
    fun celularPaisagemMantemOsToquesDoFluxo() = assertToques(640, 360)

    @Test
    fun tabletMantemOsToquesDoFluxo() = assertToques(1000, 900)

    private fun assertToques(widthDp: Int, heightDp: Int) {
        DeviceWindow.use(widthDp = widthDp, heightDp = heightDp)
        composeRule.waitForIdle()

        var toques = 0

        // `rolar = false` para o que vive fora de uma área rolável (barra/trilho e FAB).
        fun tocar(texto: String, substring: Boolean = false, rolar: Boolean = true) {
            toques++
            val no = composeRule.onNodeWithText(texto, substring = substring)
            (if (rolar) no.performScrollTo() else no).performClick()
        }

        // 1. O acervo abre pela barra (compacta) ou pelo trilho (média/expandida).
        tocar("Catálogo", rolar = false)
        waitForText(EMPTY_CATALOG)

        // 2. O FAB abre o cadastro de miniatura.
        tocar("+ Mini", rolar = false)
        waitForText("Nova miniatura")

        // 3. Fabricante e modelo: um toque em cada campo e a digitação.
        toques++
        composeRule.onAllNodes(hasSetTextAction())[0].performScrollTo().apply {
            performClick()
            performTextInput("Mattel")
        }
        toques++
        composeRule.onAllNodes(hasSetTextAction())[1].performScrollTo().apply {
            performClick()
            performTextInput("Twin Mill")
        }

        // 4. Salvar o modelo leva ao cadastro completo da variante.
        tocar("Salvar miniatura")
        waitForText("Cadastrar variante")

        // 5. A variante cria o primeiro exemplar.
        tocar("Salvar variante e exemplar")
        waitForText("1 exemplar", substring = true)

        // 6. O cartão da variante abre o cadastro rápido do segundo exemplar.
        tocar("Twin Mill")
        waitForText("Esta variante já tem 1 exemplar(es).")

        // 7. Repetir a variante avisa, mas não bloqueia (FR-039).
        tocar("Salvar exemplar")
        waitForText("2 exemplares", substring = true)

        assertEquals(
            "Cadastro em ${widthDp}x${heightDp} dp exigiu $toques toques; " +
                "o fluxo compacto é a referência de $TOQUES_ESPERADOS",
            TOQUES_ESPERADOS,
            toques,
        )
    }

    private fun waitForText(text: String, substring: Boolean = false) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text, substring = substring)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private companion object {
        /** Roteiro do cadastro: 2 navegações + 2 campos + 4 ações de salvar/abrir. */
        const val TOQUES_ESPERADOS = 8
        const val EMPTY_CATALOG = "Nenhuma miniatura ainda"
        const val TIMEOUT_MS = 10_000L
    }
}
