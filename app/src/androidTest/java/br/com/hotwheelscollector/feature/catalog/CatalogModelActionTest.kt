package br.com.hotwheelscollector.feature.catalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.hotwheelscollector.domain.model.VariantSummary
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Passo 2: o cartão do catálogo é a variante, mas leva o modelo dono. «Editar modelo»
 * precisa entregar o `modelId`, não o `variantId` — é o erro que quebraria a tela.
 */
class CatalogModelActionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun botaoEditarModeloEntregaOModeloDaVariante() {
        var editedModelId: String? = null
        composeRule.setContent {
            HotWheelsTheme {
                CatalogContent(
                    state = CatalogUiState(
                        variants = listOf(
                            VariantSummary("v1", "m1", "Twin Mill", "Mattel", "Vermelho", 2024, 1),
                        ),
                        loading = false,
                    ),
                    onVariantClick = {},
                    onOpenExemplars = {},
                    onCreateEntry = {},
                    onEditModel = { editedModelId = it },
                )
            }
        }

        composeRule.onNodeWithText("Twin Mill").assertIsDisplayed()
        composeRule.onNodeWithText("Editar modelo").performClick()

        assertEquals("m1", editedModelId)
    }
}
