package br.com.diecastcollector

import android.content.pm.ActivityInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.navigation.AppShellTags
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
 * Shell por classe de janela (T322, FR-302/FR-303).
 *
 * A activity é a de produção e a janela é redimensionada de verdade com `wm size`/`wm
 * density`: o app recebe mudança de configuração e recalcula a classe como em um tablet ou
 * em janela livre. Nada de classe injetada — o que o teste vê é o que o usuário veria.
 */
@RunWith(AndroidJUnit4::class)
class AppShellResponsiveTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @After
    fun restaurarJanela() {
        DeviceWindow.reset()
    }

    @Test
    fun compactaUsaBarraInferior() {
        DeviceWindow.use(widthDp = 360)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AppShellTags.NAVIGATION_BAR).assertIsDisplayed()
        composeRule.onNodeWithTag(AppShellTags.NAVIGATION_RAIL).assertDoesNotExist()
    }

    @Test
    fun mediaUsaTrilho() {
        DeviceWindow.use(widthDp = 700)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AppShellTags.NAVIGATION_RAIL).assertIsDisplayed()
        composeRule.onNodeWithTag(AppShellTags.NAVIGATION_BAR).assertDoesNotExist()
    }

    @Test
    fun expandidaUsaTrilho() {
        DeviceWindow.use(widthDp = 1000)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AppShellTags.NAVIGATION_RAIL).assertIsDisplayed()
        composeRule.onNodeWithTag(AppShellTags.NAVIGATION_BAR).assertDoesNotExist()
    }

    /**
     * O FAB do acervo encosta na barra inferior com a folga de 16 dp do `Scaffold`.
     *
     * Guarda contra o padding do shell aplicado em dobro: quando a área do conteúdo
     * perdia uma altura de barra, o `Scaffold` interno posicionava o FAB no meio da lista
     * e deixava um vão vazio acima da barra.
     */
    @Test
    fun fabDoCatalogoFica16DpAcimaDaBarraInferior() {
        DeviceWindow.use(widthDp = 360)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Catálogo").performClick()
        val fab = hasText("+ Mini") and hasClickAction()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodes(fab).fetchSemanticsNodes().isNotEmpty()
        }

        val fabBottom = composeRule.onNode(fab).fetchSemanticsNode().boundsInRoot.bottom
        val barTop = composeRule.onNodeWithTag(AppShellTags.NAVIGATION_BAR)
            .fetchSemanticsNode().boundsInRoot.top
        val gapDp = (barTop - fabBottom) / composeRule.density.density

        assertEquals(
            "FAB a $gapDp dp da barra: o padding do shell deve entrar uma vez só",
            16f,
            gapDp,
            0.5f,
        )
    }

    @Test
    fun trocaDeAbaFuncionaNoTrilho() {
        DeviceWindow.use(widthDp = 700)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Catálogo").performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText("+ Mini").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(AppShellTags.NAVIGATION_RAIL).assertIsDisplayed()
    }

    /**
     * T305 (FR-301/FR-304): girar a activity recria a configuração e troca barra por trilho
     * sem perder a aba selecionada. A orientação é pedida na activity de produção; o teste
     * espera a recriação e mede o shell real, não uma classe injetada.
     */
    @Test
    fun girarParaPaisagemTrocaBarraPorTrilhoEPreservaAAba() {
        composeRule.onNodeWithText("Financeiro").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AppShellTags.NAVIGATION_BAR).assertIsDisplayed()

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithTag(AppShellTags.NAVIGATION_RAIL)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(AppShellTags.NAVIGATION_RAIL).assertIsDisplayed()
        composeRule.onNodeWithTag(AppShellTags.NAVIGATION_BAR).assertDoesNotExist()
        // O título da tela também se chama "Financeiro": o que precisa estar selecionado é
        // o item do trilho, não o texto do conteúdo.
        composeRule.onNode(hasText("Financeiro") and isSelected()).assertIsDisplayed()
    }

    @After
    fun restaurarOrientacao() {
        runCatching {
            composeRule.activityRule.scenario.onActivity { activity ->
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    @Test
    fun painelCompactoNaoRolaNaHorizontal() {
        DeviceWindow.use(widthDp = 360)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Coleção vazia").fetchSemanticsNodes().isNotEmpty()
        }

        val horizontalScrollables = composeRule.onAllNodes(
            SemanticsMatcher("rolagem horizontal") { node ->
                node.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange) != null
            },
        ).fetchSemanticsNodes()

        assertEquals(0, horizontalScrollables.size)
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
    }
}
