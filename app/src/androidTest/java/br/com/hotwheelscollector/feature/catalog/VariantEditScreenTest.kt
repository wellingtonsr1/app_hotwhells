package br.com.hotwheelscollector.feature.catalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme
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
            HotWheelsTheme {
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
}
