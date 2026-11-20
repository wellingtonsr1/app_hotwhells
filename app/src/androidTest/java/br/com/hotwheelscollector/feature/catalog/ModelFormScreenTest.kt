package br.com.hotwheelscollector.feature.catalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import br.com.hotwheelscollector.domain.model.RealVehicle
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Modelo e veículo real (T043): a tela mostra os campos do modelo, o veículo já cadastrado
 * como opção e o bloco de criação de um novo; salvar só dispara pelo botão.
 */
class ModelFormScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mostraCamposVeiculoExistenteESalva() {
        var saved = false
        composeRule.setContent {
            HotWheelsTheme {
                ModelFormContent(
                    state = ModelFormUiState(
                        loading = false,
                        modelName = "Twin Mill",
                        scale = "1:64",
                        realVehicles = listOf(
                            RealVehicle("rv1", "Plymouth", "Road Runner", 1970, "EUA"),
                        ),
                        realVehicleId = "rv1",
                    ),
                    onBack = {},
                    onNameChange = {},
                    onScaleChange = {},
                    onBodyTypeChange = {},
                    onMaterialChange = {},
                    onCountryChange = {},
                    onNotesChange = {},
                    onSelectVehicle = {},
                    onVehicleMakeChange = {},
                    onVehicleModelChange = {},
                    onVehicleYearChange = {},
                    onVehicleCountryChange = {},
                    onSave = { saved = true },
                )
            }
        }

        composeRule.onNodeWithText("Modelo e veículo real").assertIsDisplayed()
        composeRule.onNodeWithText("Nome do modelo").assertIsDisplayed()
        composeRule.onNodeWithText("Veículo real").assertIsDisplayed()
        composeRule.onNodeWithText("Plymouth Road Runner 1970").assertIsDisplayed()
        composeRule.onNodeWithText("Novo veículo real").assertIsDisplayed()

        composeRule.onNodeWithText("Salvar").performScrollTo().performClick()
        assertTrue(saved)
    }
}
