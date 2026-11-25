package br.com.diecastcollector.feature.catalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.domain.model.RealVehicle
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.testing.WindowSizeFrameTags
import br.com.diecastcollector.ui.theme.DiecastTheme
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
            DiecastTheme {
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

    /**
     * T334/T343 (FR-311, SC-306): em 1.000 dp o formulário fica na coluna de 720 dp
     * centralizada — o campo não mede 1.000 dp.
     */
    @Test
    fun camposRespeitamLarguraDeLeituraEmJanelaExpandida() {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = 1000.dp, height = 900.dp) {
                    ModelFormContent(
                        state = ModelFormUiState(
                            loading = false,
                            modelName = "Twin Mill",
                            scale = "1:64",
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
                        onSave = {},
                    )
                }
            }
        }

        val moldura = composeRule.onNodeWithTag(WindowSizeFrameTags.FRAME)
            .getUnclippedBoundsInRoot()
        val campo = composeRule.onAllNodes(hasSetTextAction())[0]
            .getUnclippedBoundsInRoot()

        assertTrue("Campo com ${campo.right - campo.left} dp", (campo.right - campo.left).value <= 720f)
        val esquerda = campo.left.value - moldura.left.value
        val direita = moldura.right.value - campo.right.value
        assertTrue("Coluna não centralizada: $esquerda x $direita", kotlin.math.abs(esquerda - direita) <= 1f)
    }
}
