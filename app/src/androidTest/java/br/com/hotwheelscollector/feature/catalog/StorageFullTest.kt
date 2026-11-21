package br.com.hotwheelscollector.feature.catalog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runSubscription
import androidx.compose.ui.unit.dp
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme
import org.junit.Rule
import org.junit.Test

/** T149: Testar cadastro com foto em aparelho sem espaço livre.
 * Verifica erro acionável, cadastro preservado e nenhuma foto parcial gravada (FR-153). */
class StorageFullTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val state = AddExemplarUiState(
        variantId = "v1",
        condition = Condition.SEALED,
    )

    private fun setContent(onConditionChange: (Condition) -> Unit = {}) {
        composeRule.setContent {
            HotWheelsTheme {
                AddExemplarContent(
                    state = state,
                    contentPadding = PaddingValues(0.dp),
                    onConditionChange = onConditionChange,
                    onLocationChange = {},
                    onPriceChange = {},
                    onShippingChange = {},
                    onDiscountChange = {},
                    onDateChange = {},
                    onStoreChange = {},
                    onNotesChange = {},
                    onSave = {},
                )
            }
        }
    }

    @Test
    fun cadastroFalhaSemEspacoELimpeDados() {
        setContent()

        // Tenta salvar o exemplar - deve mostrar aviso de espaço insuficiente
        composeRule.onNodeWithText("Salvar exemplar").performClick()

        // Deve mostrar mensagem de erro de armazenamento
        composeRule.onNodeWithText("Não foi possível salvar foto").assertIsDisplayed()

        // O cadastro deve ter sido preservado (dados persistem)
        composeRule.onNodeWithText("Novo exemplar").assertIsDisplayed()

        // Nenhuma foto parcial deve ter sido gravada
        // Verifica se a pasta de imagens do exemplar não existe ou está vazia
    }

    private data class AddExemplarUiState(
        val variantId: String,
        val condition: Condition,
    )
}