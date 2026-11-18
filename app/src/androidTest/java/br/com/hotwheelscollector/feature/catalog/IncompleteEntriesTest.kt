package br.com.hotwheelscollector.feature.catalog

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.hotwheelscollector.MainActivity
import br.com.hotwheelscollector.testing.freshInstallRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Lista de cadastros incompletos (T048 - US2, FR-029).
 *
 * Os dois casos de FR-029, pelo app de verdade (Hilt, `Room`, navegação):
 *
 * 1. o cadastro parou no modelo e ele não tem variante — a lista mostra o motivo
 *    «Sem variante cadastrada» e o cartão retoma o cadastro;
 * 2. a variante existe mas ficou sem cor principal e sem ano — a lista mostra
 *    «Falta: cor principal, ano» e o cartão **não** finge ser clicável, porque complementar
 *    variante exige uma tela de edição que ainda não existe.
 *
 * O banco é limpo antes de cada teste; precisa de emulador ou aparelho.
 */
@RunWith(AndroidJUnit4::class)
class IncompleteEntriesTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    /** Instalação limpa: banco sem registros e sem fotos de execuções anteriores. */
    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @Test
    fun modeloSemVarianteEntraNaListaComMotivoERetomaOCadastro() {
        createModel(manufacturer = "Mattel", model = "Twin Mill")

        // Sair da tela de variante é o que deixa o cadastro pela metade.
        pressBack()
        waitForText("Incompletos (1)")

        composeRule.onNodeWithText("Incompletos (1)").performClick()
        waitForText("Cadastros incompletos")

        composeRule.onNodeWithText("Twin Mill").assertHasClickAction()
        composeRule.onNodeWithText("Sem variante cadastrada").assertExists()

        // O cartão retoma o cadastro pelo ponto onde parou.
        composeRule.onNodeWithText("Twin Mill").performClick()
        waitForText("Cadastrar variante")
    }

    @Test
    fun varianteSemCorESemAnoEntraNaListaComOsCamposQueFaltam() {
        createModel(manufacturer = "Mattel", model = "Bone Shaker")

        // Salvar a variante sem cor principal e sem ano: é permitido de propósito, e é o
        // que a lista de incompletos existe para cobrar.
        composeRule.onNodeWithText("Salvar variante e exemplar").performScrollTo().performClick()
        waitForText("1 exemplar", substring = true)
        waitForText("Incompletos (1)")

        composeRule.onNodeWithText("Incompletos (1)").performClick()
        waitForText("Cadastros incompletos")

        composeRule.onNodeWithText("Bone Shaker").assertExists()
        composeRule.onNodeWithText("Falta: cor principal, ano").assertExists()

        // Ainda não há tela de edição de variante: o cartão não pode parecer clicável.
        composeRule.onNodeWithText("Bone Shaker").assertHasNoClickAction()
    }

    /** Modelo criado pelo fluxo real, parando na tela de variante. */
    private fun createModel(manufacturer: String, model: String) {
        // A primeira tela é o painel (US3): o acervo abre na aba «Catálogo».
        composeRule.onNodeWithText("Catálogo").performClick()
        waitForText("Nenhuma miniatura ainda")
        composeRule.onNodeWithText("+ Mini").performClick()
        waitForText("Nova miniatura")

        composeRule.onAllNodes(hasSetTextAction())[0].apply {
            performClick()
            performTextInput(manufacturer)
        }
        composeRule.onAllNodes(hasSetTextAction())[1].apply {
            performClick()
            performTextInput(model)
        }

        composeRule.onNodeWithText("Salvar miniatura").performScrollTo().performClick()
        waitForText("Cadastrar variante")
    }

    private fun pressBack() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
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
        const val TIMEOUT_MS = 10_000L
    }
}
