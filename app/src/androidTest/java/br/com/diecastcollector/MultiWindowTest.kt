package br.com.diecastcollector

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.navigation.AppShellTags
import br.com.diecastcollector.testing.DeviceWindow
import br.com.diecastcollector.testing.freshInstallRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Janela de duas telas (T372, FR-301/FR-304/FR-325).
 *
 * O que este arquivo prova: uma janela com a **largura e a altura de um lado de split**
 * (360 × 420 dp, em vez dos 360 × 914 dp de um telefone inteiro) recebe a classe pela largura
 * do próprio contêiner — barra inferior em 360, trilho em 700 —, mantém os controles acima
 * da barra de navegação do sistema e não cria rolagem horizontal.
 *
 * O que **não** está aqui, e por quê: o par de janelas real e a dobra física. Este emulador
 * (`hw_test`, API 36) aceita `am start --windowingMode 3`/`4` e coloca cada activity em um
 * rootTask próprio (`mode=3` e `mode=4` no dumpsys), mas **nunca as emparelha**: o display
 * continua `mWindowingMode=fullscreen` e a tarefa do app fica `visible=false`. O caminho
 * alternativo (`--windowingMode 5` em freeform, com `enable_freeform_support=1`) é recusado
 * pelo `am` desta imagem (`IllegalArgumentException: Unknown option: --bounds`), e o
 * emulador só tem `mDisplayId=0`, então não há segunda tela para abrir a outra janela.
 * Enquanto isso, `wm size`/`wm density` é a única forma real de dar outra janela ao app —
 * é o que [DeviceWindow] faz e o que estas verificações usam.
 */
@RunWith(AndroidJUnit4::class)
class MultiWindowTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @After
    fun restaurarJanela() {
        DeviceWindow.reset()
    }

    @Test
    fun ladoCompactoDeSplitUsaBarraInferior() {
        DeviceWindow.use(widthDp = 360, heightDp = 420)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AppShellTags.NAVIGATION_BAR).assertIsDisplayed()
        composeRule.onNodeWithTag(AppShellTags.NAVIGATION_RAIL).assertDoesNotExist()
    }

    @Test
    fun ladoMedioDeSplitUsaTrilho() {
        DeviceWindow.use(widthDp = 700, heightDp = 420)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AppShellTags.NAVIGATION_RAIL).assertIsDisplayed()
        composeRule.onNodeWithTag(AppShellTags.NAVIGATION_BAR).assertDoesNotExist()
    }

    /** FR-325: em janela curta nenhum controle fica sob a barra de navegação do sistema. */
    @Test
    fun abaFicaAcimaDaBarraDoSistemaEmJanelaCurta() {
        DeviceWindow.use(widthDp = 360, heightDp = 420)
        composeRule.waitForIdle()

        val activity = composeRule.activity
        val decor = activity.window.decorView
        val density = activity.resources.displayMetrics.density
        val navBarBottom = WindowInsetsCompat
            .toWindowInsetsCompat(decor.rootWindowInsets, decor)
            .getInsets(WindowInsetsCompat.Type.navigationBars())
            .bottom
        val limiteDp = (decor.height - navBarBottom) / density

        // O rótulo também aparece no título do painel: a aba é o nó tocável.
        val rotulo = activity.getString(R.string.dashboard_tab)
        val aba = composeRule.onNode(hasText(rotulo) and hasClickAction())
            .getUnclippedBoundsInRoot()
        assertTrue(
            "Aba terminou em ${aba.bottom} dp; a barra do sistema começa em $limiteDp dp",
            aba.bottom.value <= limiteDp + 1f,
        )
    }

    /** FR-303/FR-312: janela curta não ganha rolagem horizontal em lugar nenhum. */
    @Test
    fun janelaCurtaNaoTemRolagemHorizontal() {
        DeviceWindow.use(widthDp = 360, heightDp = 420)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodes(
                SemanticsMatcher("rolagem horizontal") { node ->
                    node.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange) != null
                },
            ).fetchSemanticsNodes().isEmpty()
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
