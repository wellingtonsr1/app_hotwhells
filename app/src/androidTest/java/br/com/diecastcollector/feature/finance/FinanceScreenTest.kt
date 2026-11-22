package br.com.diecastcollector.feature.finance

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import br.com.diecastcollector.domain.model.FinanceSummary
import br.com.diecastcollector.domain.model.FinancialDisposal
import br.com.diecastcollector.domain.model.FinancialHistory
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.model.NegotiationType
import br.com.diecastcollector.domain.model.PeriodSpending
import br.com.diecastcollector.domain.model.SpendingSlice
import java.time.Instant
import br.com.diecastcollector.ui.theme.DiecastTheme
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
            DiecastTheme {
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
                        history = FinancialHistory(
                            entries = listOf(
                                FinancialDisposal(
                                    negotiationId = "n1",
                                    exemplarId = "e1",
                                    internalCode = "HW-0001",
                                    manufacturerName = "Mattel",
                                    modelName = "Twin Mill",
                                    type = NegotiationType.SALE,
                                    closedAt = Instant.parse("2026-11-20T12:00:00Z"),
                                    amount = MoneyCents(15_000),
                                    counterparty = "Ana",
                                ),
                            ),
                        ),
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

        // T097: vendas ficam fora do investido, em bloco próprio e com o valor recebido.
        composeRule.onNodeWithText("Vendas e trocas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Total em vendas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("R$ 150,00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Contraparte: Ana", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Venda").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "Vendas e trocas ficam fora do investido; o custo histórico de aquisição não muda.",
        ).performScrollTo().assertIsDisplayed()
    }
}
