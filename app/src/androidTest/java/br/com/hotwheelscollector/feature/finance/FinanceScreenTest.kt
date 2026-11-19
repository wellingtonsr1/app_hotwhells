package br.com.hotwheelscollector.feature.finance

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import br.com.hotwheelscollector.domain.model.FinanceSummary
import br.com.hotwheelscollector.domain.model.PeriodSpending
import br.com.hotwheelscollector.domain.model.SpendingSlice
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme
import org.junit.Rule
import org.junit.Test

/**
 * Tela de finanças (T095/T096, FR-077–FR-086).
 *
 * O que a tela não pode errar: estimado sempre rotulado como estimativa do colecionador,
 * custo médio dizendo quantos ficaram fora, e a diferença podendo ser negativa.
 */
class FinanceScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mostraResumoAgrupamentosEPeriodo() {
        composeRule.setContent {
            HotWheelsTheme {
                FinanceContent(
                    state = FinanceUiState(
                        summary = FinanceSummary(
                            investedCents = 100_000,
                            estimatedCents = 80_000,
                            ownedExemplars = 6,
                            exemplarsWithValue = 4,
                            exemplarsWithoutValuation = 2,
                            latestValuationDate = "2026-11-15",
                        ),
                        byManufacturer = listOf(SpendingSlice("Mattel", 60_000)),
                        bySeries = listOf(SpendingSlice("Car Culture", 60_000)),
                        byCollection = listOf(SpendingSlice(null, 40_000)),
                        byMonth = listOf(PeriodSpending("2026-11", 70_000)),
                        loading = false,
                    ),
                    onFromChange = {},
                    onToChange = {},
                    onApplyPeriod = {},
                    onClearPeriod = {},
                )
            }
        }

        composeRule.onNodeWithText("R$ 1.000,00").assertIsDisplayed()
        composeRule.onNodeWithText("-R$ 200,00").assertIsDisplayed()
        composeRule.onNodeWithText("R$ 250,00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "Estimativa do colecionador — não é preço de mercado.",
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Avaliações até 15/11/2026.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Gastos por fabricante").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Mattel").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sem coleção").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("11/2026").performScrollTo().assertIsDisplayed()
    }
}
