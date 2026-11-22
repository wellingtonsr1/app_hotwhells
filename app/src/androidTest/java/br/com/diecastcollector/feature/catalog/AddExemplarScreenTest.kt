package br.com.diecastcollector.feature.catalog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.ui.UiMessage
import br.com.diecastcollector.ui.theme.DiecastTheme
import org.junit.Rule
import org.junit.Test

/**
 * Fluxo rápido de exemplar (T031, FR-036).
 *
 * Testa a tela como o colecionador a vê: os campos que existem, o aviso de repetição
 * que **não** bloqueia e a troca de condição. Precisa de emulador ou aparelho.
 */
class AddExemplarScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val state = AddExemplarUiState(
        variantId = "v1",
        condition = Condition.SEALED,
        duplicateNotice = UiMessage(R.string.add_exemplar_duplicate_variant, 2),
    )

    private fun setContent(onConditionChange: (Condition) -> Unit = {}) {
        composeRule.setContent {
            DiecastTheme {
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
    fun mostraApenasCamposDoExemplar() {
        setContent()

        composeRule.onNodeWithText("Novo exemplar").assertIsDisplayed()
        composeRule.onNodeWithText("Preço pago (R$)").assertIsDisplayed()
        composeRule.onNodeWithText("Salvar exemplar").assertIsDisplayed()
    }

    @Test
    fun avisoDeVarianteRepetidaNaoBloqueiaSalvar() {
        setContent()

        composeRule.onNodeWithText("Esta variante já tem 2 exemplar(es).").assertIsDisplayed()
        composeRule.onNodeWithText("Salvar exemplar").performClick()
    }

    @Test
    fun trocarCondicaoNotificaOViewModel() {
        var chosen: Condition? = null
        setContent(onConditionChange = { chosen = it })

        composeRule.onNodeWithText("Solto").performClick()

        composeRule.onNodeWithText("Lacrado").assertIsSelected()
        assert(chosen == Condition.LOOSE)
    }
}
