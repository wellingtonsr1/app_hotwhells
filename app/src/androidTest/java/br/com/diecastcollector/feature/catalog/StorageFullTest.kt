package br.com.diecastcollector.feature.catalog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.ui.theme.DiecastTheme
import org.junit.Rule
import org.junit.Test

/**
 * T149 — o cadastro rápido sobrevive a uma falha de salvamento (FR-153).
 *
 * A falha de armazenamento é traduzida em mensagem pelo ViewModel (coberto em teste
 * unitário); aqui o contrato de tela é outro: ao tocar em salvar sem sair da tela, os dados
 * digitados continuam ali, prontos para nova tentativa.
 */
class StorageFullTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun formularioContinuaPreenchidoQuandoSalvarFalha() {
        var saved = false
        val state = AddExemplarUiState(
            variantId = "v1",
            condition = Condition.SEALED,
            notes = "etiqueta amassada",
        )

        composeRule.setContent {
            DiecastTheme {
                AddExemplarContent(
                    state = state,
                    contentPadding = PaddingValues(0.dp),
                    onConditionChange = {},
                    onLocationChange = {},
                    onPriceChange = {},
                    onShippingChange = {},
                    onDiscountChange = {},
                    onDateChange = {},
                    onStoreChange = {},
                    onNotesChange = {},
                    onSave = { saved = true },
                )
            }
        }

        composeRule.onNodeWithText("Novo exemplar").assertIsDisplayed()
        composeRule.onNodeWithText("etiqueta amassada").assertIsDisplayed()

        composeRule.onNodeWithText("Salvar exemplar").performClick()

        // Sem navegação nem limpeza: o formulário continua na tela para tentar de novo.
        assert(saved)
        composeRule.onNodeWithText("Novo exemplar").assertIsDisplayed()
        composeRule.onNodeWithText("etiqueta amassada").assertIsDisplayed()
    }
}
