@file:OptIn(ExperimentalLayoutApi::class)

package br.com.diecastcollector.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.R
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.testing.assertWindowWidthClass
import br.com.diecastcollector.testing.windowSizeClassOf
import br.com.diecastcollector.ui.theme.DiecastTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Componentes adaptativos por classe de janela (T313).
 *
 * Cada teste mede o comportamento no contêiner de 360/700/1.000 dp — a mesma derivação que
 * a produção usa. Contar colunas pelos limites dos nós evita depender de captura de tela:
 * uma coluna a mais aparece como um `left` distinto.
 */
@RunWith(AndroidJUnit4::class)
class ResponsiveComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun classesDerivamDaLarguraDoContorno() {
        assertWindowWidthClass(360.dp, WindowWidthSizeClass.Compact)
        assertWindowWidthClass(700.dp, WindowWidthSizeClass.Medium)
        assertWindowWidthClass(1000.dp, WindowWidthSizeClass.Expanded)
        assertEquals(WindowWidthSizeClass.Compact, windowSizeClassOf(411.dp).widthSizeClass)
    }

    @Test
    fun gradeEm360UsaUmaColuna() = assertGridColumns(width = 360.dp, expected = 1)

    @Test
    fun gradeEm700UsaTresColunas() = assertGridColumns(width = 700.dp, expected = 3)

    @Test
    fun gradeEm1000UsaCincoColunas() = assertGridColumns(width = 1000.dp, expected = 5)

    @Test
    fun formularioOcupaLarguraTodaEm360() {
        assertEquals(360f, formChildWidth(360.dp), 1f)
    }

    @Test
    fun formularioLimitaLarguraDeLeituraEm1000() {
        assertEquals(720f, formChildWidth(1000.dp), 1f)
    }

    @Test
    fun chipsQuebramLinhaComFonte200() {
        val labels = listOf("Seminovo", "Lacrado", "Usado", "Custom", "Danificado")
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = density.density, fontScale = 2f),
            ) {
                DiecastTheme {
                    WindowSizeFrame(width = 360.dp) {
                        FlowChipGroup {
                            labels.forEach { label ->
                                FilterChip(
                                    selected = false,
                                    onClick = {},
                                    label = { Text(label) },
                                )
                            }
                        }
                    }
                }
            }
        }

        labels.forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
            val bounds = composeRule.onNodeWithText(label).getUnclippedBoundsInRoot()
            assertTrue("$label saiu da tela: ${bounds.right}", bounds.right.value <= 360f)
        }
    }

    @Test
    fun dialogodeMidiaMostraConteudoEFechar() {
        val closeLabel = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(R.string.common_close)

        composeRule.setContent {
            DiecastTheme {
                MediaDialogScaffold(onDismiss = {}) {
                    Box(Modifier.fillMaxSize().testTag("media-content"))
                }
            }
        }

        composeRule.onNodeWithTag("media-content").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(closeLabel).assertIsDisplayed()
    }

    @Test
    fun acoesViramMenuQuandoNaoCabem() {
        val moreActions = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(R.string.common_more_actions)

        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = 360.dp) {
                    AdaptableCardActions(
                        modifier = Modifier.fillMaxWidth(),
                        actions = listOf(
                            CardAction("Comprar") {},
                            CardAction("Abrir link") {},
                            CardAction("Desistir") {},
                            CardAction("Duplicar") {},
                        ),
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription(moreActions).assertIsDisplayed()
        composeRule.onAllNodesWithText("Comprar").fetchSemanticsNodes().let {
            assertTrue("Rótulo deveria estar no menu, não na linha", it.isEmpty())
        }
    }

    @Test
    fun acoesFicamEmLinhaQuandoCabem() {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = 700.dp) {
                    AdaptableCardActions(
                        modifier = Modifier.fillMaxWidth(),
                        actions = listOf(
                            CardAction("Comprar") {},
                            CardAction("Desistir") {},
                        ),
                    )
                }
            }
        }

        composeRule.onAllNodesWithText("Comprar").fetchSemanticsNodes().let {
            assertTrue("A ação deveria estar visível na linha", it.isNotEmpty())
        }
    }

    private fun assertGridColumns(width: Dp, expected: Int) {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width) {
                    AdaptiveGrid {
                        items(12) {
                            Box(Modifier.size(40.dp).testTag("grid-item"))
                        }
                    }
                }
            }
        }

        val lefts = composeRule.onAllNodesWithTag("grid-item")
            .fetchSemanticsNodes()
            .map { it.boundsInRoot.left }
            .distinct()

        assertEquals("Colunas em $width", expected, lefts.size)
    }

    private fun formChildWidth(width: Dp): Float {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width) {
                    FormColumn {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .testTag("form-child"),
                        )
                    }
                }
            }
        }
        val bounds = composeRule.onNodeWithTag("form-child").getUnclippedBoundsInRoot()
        return (bounds.right - bounds.left).value
    }
}
