package br.com.diecastcollector.feature.catalog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.ui.UiMessage
import br.com.diecastcollector.ui.theme.DiecastTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private fun renderResponsive(
        width: Dp,
        fontScale: Float = 1f,
        onConditionChange: (Condition) -> Unit = {},
    ) {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width, fontScale = fontScale) {
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
    }

    /**
     * T325/T332 (FR-313, SC-303): em 360 dp com fonte 200% as cinco condições têm de
     * caber dentro da tela — antes a última ficava fora, no `Row` que crescia à direita.
     */
    @Test
    fun condicoesCabemInteirasEm360ComFonteDobrada() {
        renderResponsive(width = 360.dp, fontScale = 2f)

        CONDITIONS.forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
            val bounds = composeRule.onNodeWithText(label).getUnclippedBoundsInRoot()
            assertTrue(
                "$label saiu da tela: ${bounds.left}..${bounds.right}",
                bounds.left.value >= 0f && bounds.right.value <= 360f,
            )
        }
    }

    @Test
    fun trocarCondicaoEmFonteDobradaContinuaFuncionando() {
        var chosen: Condition? = null
        renderResponsive(width = 360.dp, fontScale = 2f) { chosen = it }

        composeRule.onNodeWithText("Incompleto").performClick()

        assertEquals(Condition.INCOMPLETE, chosen)
    }

    private companion object {
        val CONDITIONS = listOf("Lacrado", "Solto", "Danificado", "Restaurado", "Incompleto")
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
