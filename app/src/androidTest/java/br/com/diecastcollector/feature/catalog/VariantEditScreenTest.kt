package br.com.diecastcollector.feature.catalog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.ui.theme.DiecastTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Edição de variante incompleta (FR-029): o mesmo formulário, preenchido, sem o bloco de
 * exemplar — completar cor e ano não pode criar uma segunda unidade física.
 */
class VariantEditScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mostraCamposPreenchidosSemBlocoDeExemplarESalva() {
        var saved = false
        composeRule.setContent {
            DiecastTheme {
                VariantFormContent(
                    state = VariantFormUiState(
                        primaryColor = "Vermelho",
                        releaseYearText = "2023",
                        secondaryColors = "Azul",
                        wheelsType = "5SP",
                        editing = true,
                        loading = false,
                    ),
                    onPrimaryColorChange = {},
                    onSecondaryColorsChange = {},
                    onWheelsTypeChange = {},
                    onBaseTypeChange = {},
                    onDecorationChange = {},
                    onSeriesChange = {},
                    onReleaseYearChange = {},
                    onManufacturerCodeChange = {},
                    onConditionChange = {},
                    onAddPhoto = { _, _ -> },
                    onSetPrimary = {},
                    onRemovePhoto = {},
                    onSave = { saved = true },
                    onMessageShown = {},
                    showExemplarSection = false,
                )
            }
        }

        composeRule.onNodeWithText("Editar variante").assertIsDisplayed()
        composeRule.onNodeWithText("Vermelho").assertIsDisplayed()
        composeRule.onNodeWithText("2023").assertIsDisplayed()
        // O bloco do exemplar não aparece na edição.
        composeRule.onNodeWithText("Condição").assertDoesNotExist()

        composeRule.onNodeWithText("Salvar alterações").performScrollTo().performClick()
        assertTrue(saved)
    }

    @Test
    fun arquivarPedeConfirmacaoAntesDeAgir() {
        var archived = false
        actionsContent(onArchive = { archived = true })

        composeRule.onNodeWithText("Arquivar variante").performScrollTo().performClick()
        composeRule.onNodeWithText("Arquivar esta variante?").assertIsDisplayed()
        assertTrue(!archived)

        composeRule.onNodeWithText("Arquivar").performClick()
        assertTrue(archived)
    }

    @Test
    fun unirEscolheOutraVarianteDoModelo() {
        var mergedInto: String? = null
        actionsContent(
            siblings = listOf(VariantOption(id = "v2", colorName = "Azul", year = 2024)),
            onMerge = { mergedInto = it },
        )

        composeRule.onNodeWithText("Unir com outra variante").performScrollTo().performClick()
        composeRule.onNodeWithText("Azul · 2024").assertIsDisplayed()
        composeRule.onNodeWithText("Unir").performClick()

        assertTrue(mergedInto == "v2")
    }

    @Test
    fun separarSoConfirmaComExemplarMarcado() {
        var splitInto: String? = null
        actionsContent(
            siblings = listOf(VariantOption(id = "v2", colorName = "Azul")),
            exemplars = listOf(
                VariantExemplarOption(
                    id = "e1",
                    internalCode = "HW-000001",
                    condition = br.com.diecastcollector.domain.model.Condition.LOOSE,
                ),
            ),
            onSplit = { splitInto = it },
        )

        composeRule.onNodeWithText("Separar exemplares").performScrollTo().performClick()
        composeRule.onNodeWithText("Separar").assertIsNotEnabled()

        composeRule.onAllNodes(isToggleable())[0].performClick()
        composeRule.onNodeWithText("Separar").assertIsEnabled().performClick()

        assertTrue(splitInto == "v2")
    }

    /**
     * T367 (FR-316): a lista de variantes para unir rola dentro do diálogo e o botão "Unir"
     * continua visível, mesmo com muitas opções.
     */
    @Test
    fun dialogoDeUniaoRolaComMuitasOpcoes() {
        actionsContent(
            siblings = (1..20).map { index ->
                VariantOption(id = "v$index", colorName = "Cor $index", year = 2020 + index)
            },
        )

        composeRule.onNodeWithText("Unir com outra variante").performScrollTo().performClick()

        composeRule.onNodeWithText("Unir").assertIsDisplayed()
        assertTrue(
            "A lista deveria rolar dentro do diálogo",
            composeRule.onAllNodes(androidx.compose.ui.test.hasScrollAction())
                .fetchSemanticsNodes().isNotEmpty(),
        )
    }

    /**
     * T338/T343 (FR-312): em 360 dp com fonte 200% o par cor/ano empilha — os dois campos
     * não cabem lado a lado com rótulo legível.
     */
    @Test
    fun parCorEAnoEmpilhaEm360ComFonteDobrada() {
        pairContent(width = 360.dp, fontScale = 2f)

        val cor = composeRule.onNodeWithText("Vermelho").getUnclippedBoundsInRoot()
        val ano = composeRule.onNodeWithText("2023").getUnclippedBoundsInRoot()

        assertTrue(
            "Campos deveriam empilhar: topo $cor e $ano",
            ano.top.value - cor.top.value > 20f,
        )
    }

    /** Na janela média os mesmos campos continuam lado a lado. */
    @Test
    fun parCorEAnoFicaLadoALadoEm900() {
        pairContent(width = 900.dp)

        val cor = composeRule.onNodeWithText("Vermelho").getUnclippedBoundsInRoot()
        val ano = composeRule.onNodeWithText("2023").getUnclippedBoundsInRoot()

        assertTrue(
            "Campos deveriam ficar na mesma linha: topo $cor e $ano",
            kotlin.math.abs(ano.top.value - cor.top.value) < 2f,
        )
        assertTrue("Campos sobrepostos", cor.right.value <= ano.left.value)
    }

    private fun pairContent(width: Dp, fontScale: Float = 1f) {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width, height = 900.dp, fontScale = fontScale) {
                    VariantFormContent(
                        state = VariantFormUiState(
                            primaryColor = "Vermelho",
                            releaseYearText = "2023",
                            editing = true,
                            loading = false,
                        ),
                        onPrimaryColorChange = {},
                        onSecondaryColorsChange = {},
                        onWheelsTypeChange = {},
                        onBaseTypeChange = {},
                        onDecorationChange = {},
                        onSeriesChange = {},
                        onReleaseYearChange = {},
                        onManufacturerCodeChange = {},
                        onConditionChange = {},
                        onAddPhoto = { _, _ -> },
                        onSetPrimary = {},
                        onRemovePhoto = {},
                        onSave = {},
                        onMessageShown = {},
                        showExemplarSection = false,
                    )
                }
            }
        }
    }

    private fun actionsContent(
        siblings: List<VariantOption> = emptyList(),
        exemplars: List<VariantExemplarOption> = emptyList(),
        onArchive: () -> Unit = {},
        onMerge: (String) -> Unit = {},
        onSplit: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            DiecastTheme {
                var selection by remember { mutableStateOf(emptySet<String>()) }
                VariantFormContent(
                    state = VariantFormUiState(
                        primaryColor = "Vermelho",
                        editing = true,
                        loading = false,
                    ),
                    onPrimaryColorChange = {},
                    onSecondaryColorsChange = {},
                    onWheelsTypeChange = {},
                    onBaseTypeChange = {},
                    onDecorationChange = {},
                    onSeriesChange = {},
                    onReleaseYearChange = {},
                    onManufacturerCodeChange = {},
                    onConditionChange = {},
                    onAddPhoto = { _, _ -> },
                    onSetPrimary = {},
                    onRemovePhoto = {},
                    onSave = {},
                    onMessageShown = {},
                    showExemplarSection = false,
                    showActions = true,
                    siblings = siblings,
                    exemplars = exemplars,
                    splitSelection = selection,
                    onToggleSplitExemplar = { id ->
                        selection = if (id in selection) selection - id else selection + id
                    },
                    onArchive = onArchive,
                    onMerge = onMerge,
                    onSplit = onSplit,
                )
            }
        }
    }
}
