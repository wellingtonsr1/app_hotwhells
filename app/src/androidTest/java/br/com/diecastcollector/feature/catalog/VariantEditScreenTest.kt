package br.com.diecastcollector.feature.catalog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
