package br.com.diecastcollector

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.domain.model.Manufacturer
import br.com.diecastcollector.domain.model.Series
import br.com.diecastcollector.feature.organization.SeriesContent
import br.com.diecastcollector.feature.organization.SeriesTags
import br.com.diecastcollector.feature.organization.SeriesUiState
import br.com.diecastcollector.navigation.AppShellTags
import br.com.diecastcollector.testing.DeviceWindow
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.testing.freshInstallRule
import br.com.diecastcollector.ui.theme.DiecastTheme
import br.com.diecastcollector.ui.theme.ThemeMode
import java.time.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Acessibilidade do shell na janela real (T371, FR-326/FR-327, Art. VII.10).
 *
 * Sem captura: o que se mede é a árvore de semântica que o leitor de tela recebe — nenhum
 * controle tocável sem rótulo e a ordem das cinco abas igual à ordem de leitura nas duas
 * formas do shell (esquerda→direita na barra, cima→baixo no trilho).
 */
@RunWith(AndroidJUnit4::class)
class ResponsiveAccessibilityTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @After
    fun restaurarJanela() {
        DeviceWindow.reset()
    }

    /** FR-326: nenhum ícone tocável fica mudo para o leitor de tela, em nenhuma classe. */
    @Test
    fun todoControleTocavelTemRotulo() {
        listOf(360, 700, 1000).forEach { width ->
            DeviceWindow.use(widthDp = width)
            composeRule.waitForIdle()

            val mudos = composeRule.onAllNodes(hasClickAction())
                .fetchSemanticsNodes()
                .filter { node ->
                    val config = node.config
                    val text = config.getOrNull(SemanticsProperties.Text)
                        ?.any { it.text.isNotBlank() } == true
                    val editable = config.getOrNull(SemanticsProperties.EditableText)
                        ?.text?.isNotBlank() == true
                    val description = config.getOrNull(SemanticsProperties.ContentDescription)
                        ?.any { it.isNotBlank() } == true
                    !text && !editable && !description
                }

            assertTrue(
                "Em $width dp há ${mudos.size} controle(s) tocável(is) sem rótulo: " +
                    mudos.map { it.boundsInRoot.toString() },
                mudos.isEmpty(),
            )
        }
    }

    /** FR-327: na barra inferior o foco segue a leitura da esquerda para a direita. */
    @Test
    fun ordemDasAbasSegueALeituraNaBarraInferior() {
        DeviceWindow.use(widthDp = 360)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AppShellTags.NAVIGATION_BAR).assertIsDisplayed()

        var anterior = -1f
        abasDoShell(vertical = false).forEach { (rotulo, intervalo) ->
            assertTrue(
                "Aba \"$rotulo\" começou em ${intervalo.first}, depois de $anterior",
                intervalo.first >= anterior - 1f,
            )
            anterior = intervalo.second
        }
    }

    /** FR-327: no trilho o foco segue a leitura de cima para baixo. */
    @Test
    fun ordemDasAbasSegueALeituraNoTrilho() {
        DeviceWindow.use(widthDp = 700)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AppShellTags.NAVIGATION_RAIL).assertIsDisplayed()

        var anterior = -1f
        abasDoShell(vertical = true).forEach { (rotulo, intervalo) ->
            assertTrue(
                "Aba \"$rotulo\" começou em ${intervalo.first}, depois de $anterior",
                intervalo.first > anterior,
            )
            anterior = intervalo.second
        }
    }

    /**
     * Abas na ordem declarada (a mesma da árvore de semântica), com o intervalo em dp no
     * eixo que a forma do shell usa para empilhar.
     */
    private fun abasDoShell(vertical: Boolean): List<Pair<String, Pair<Float, Float>>> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val rotulos = listOf(
            R.string.dashboard_tab,
            R.string.catalog_tab,
            R.string.wishlist_tab,
            R.string.finance_tab,
            R.string.settings_tab,
        ).map { context.getString(it) }
        val density = context.resources.displayMetrics.density

        val itens = rotulos.map { rotulo ->
            val bounds = composeRule.onNode(hasText(rotulo) and hasClickAction())
                .fetchSemanticsNode()
                .boundsInRoot
            val inicio = if (vertical) bounds.top / density else bounds.left / density
            val fim = if (vertical) bounds.bottom / density else bounds.right / density
            rotulo to (inicio to fim)
        }
        assertEquals("Esperava as cinco abas", 5, itens.size)
        return itens
    }
}

/**
 * Acessibilidade do conteúdo: contraste do tema e rótulo truncado (T371, FR-326/FR-327).
 *
 * Roda sem a activity porque depende só do tema e de uma tela montada em moldura — é o
 * mesmo tema que o app usa, e a moldura é a mesma dos outros testes por classe.
 */
@RunWith(AndroidJUnit4::class)
class ResponsiveAccessibilityContentTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun temaClaroMantemContrasteDeTexto() {
        assertThemeContrast(dark = false)
    }

    @Test
    fun temaEscuroMantemContrasteDeTexto() {
        assertThemeContrast(dark = true)
    }

    /** A escolha do limiar pela WCAG: 18 sp (ou 14 sp em negrito) já é texto grande. */
    @Test
    fun textoGrandeUsaTresParaUmENormalUsaQuatroEMeio() {
        assertEquals(ContrastAssert.NORMAL_TEXT, ContrastAssert.requiredRatio(12f), 0.001)
        assertEquals(ContrastAssert.NORMAL_TEXT, ContrastAssert.requiredRatio(17.9f), 0.001)
        assertEquals(ContrastAssert.LARGE_TEXT, ContrastAssert.requiredRatio(18f), 0.001)
        assertEquals(ContrastAssert.LARGE_TEXT, ContrastAssert.requiredRatio(24f), 0.001)
        assertEquals(
            ContrastAssert.NORMAL_TEXT,
            ContrastAssert.requiredRatio(14f, bold = false),
            0.001,
        )
        assertEquals(ContrastAssert.LARGE_TEXT, ContrastAssert.requiredRatio(14f, bold = true), 0.001)
    }

    /**
     * FR-327: nome longo de série fica truncado na tela, mas a semântica carrega o texto
     * inteiro — é isso que o leitor de tela anuncia.
     */
    @Test
    fun rotuloTruncadoMantemTextoCompletoNaSemantica() {
        val nomeLongo = "Série Especial de Colecionador com Nome Muito Comprido e Sufixo 2026"
        val now = Instant.parse("2026-01-01T00:00:00Z")

        compose.setContent {
            DiecastTheme {
                WindowSizeFrame(width = 360.dp, fontScale = 2f) {
                    SeriesContent(
                        state = SeriesUiState(
                            manufacturers = listOf(
                                Manufacturer(
                                    id = "m1",
                                    name = "Hot Wheels",
                                    createdAt = now,
                                    updatedAt = now,
                                ),
                            ),
                            selectedManufacturerId = "m1",
                            items = listOf(
                                Series(
                                    id = "s1",
                                    manufacturerId = "m1",
                                    name = nomeLongo,
                                    createdAt = now,
                                    updatedAt = now,
                                ),
                            ),
                            loading = false,
                        ),
                        onBack = {},
                        onSelectManufacturer = {},
                        onAdd = { _, _ -> },
                        onRename = { _, _ -> },
                        onArchive = {},
                        onMessageShown = {},
                    )
                }
            }
        }

        compose.onNodeWithTag(SeriesTags.ROW).assertIsDisplayed()
        val node = compose.onAllNodesWithText(nomeLongo, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .single()

        val textos = node.config.getOrNull(SemanticsProperties.Text)
            ?.joinToString("") { it.text }
            .orEmpty()
        assertEquals("A semântica perdeu parte do nome", nomeLongo, textos)

        val layout = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layout)
        assertTrue("O texto deveria estar truncado", layout.first().hasVisualOverflow)
    }

    private fun assertThemeContrast(dark: Boolean) {
        var scheme: ColorScheme? = null
        compose.setContent {
            DiecastTheme(mode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                scheme = MaterialTheme.colorScheme
            }
        }
        compose.waitForIdle()
        val colors = requireNotNull(scheme) { "Tema não expôs o ColorScheme" }

        // Pares usados para texto. `secondary` fica de fora porque nenhuma tela escreve
        // sobre ele: o cartão do painel usa `secondaryContainer`, que é verificado aqui.
        ContrastAssert.assertContrast(
            "onBackground/background",
            colors.onBackground,
            colors.background,
        )
        ContrastAssert.assertContrast("onSurface/surface", colors.onSurface, colors.surface)
        ContrastAssert.assertContrast(
            "onSurfaceVariant/surface",
            colors.onSurfaceVariant,
            colors.surface,
        )
        ContrastAssert.assertContrast(
            "onSurfaceVariant/background",
            colors.onSurfaceVariant,
            colors.background,
        )
        ContrastAssert.assertContrast(
            "onSurfaceVariant/surfaceVariant",
            colors.onSurfaceVariant,
            colors.surfaceVariant,
        )
        ContrastAssert.assertContrast("onPrimary/primary", colors.onPrimary, colors.primary)
        ContrastAssert.assertContrast("onError/error", colors.onError, colors.error)
        ContrastAssert.assertContrast(
            "onSecondaryContainer/secondaryContainer",
            colors.onSecondaryContainer,
            colors.secondaryContainer,
        )
        ContrastAssert.assertContrast(
            "inverseOnSurface/inverseSurface",
            colors.inverseOnSurface,
            colors.inverseSurface,
        )
    }
}
