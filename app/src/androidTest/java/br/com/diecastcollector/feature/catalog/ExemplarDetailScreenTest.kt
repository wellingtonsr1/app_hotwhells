package br.com.diecastcollector.feature.catalog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.domain.model.Acquisition
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.domain.model.IsoDate
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.model.Valuation
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Aquisição e avaliação no detalhe do exemplar (T091/T092, FR-074–FR-086).
 *
 * O teste cobra o que o painel depende: custo total calculado a partir de preço/frete e o
 * valor estimado sempre rotulado como estimativa do colecionador.
 */
class ExemplarDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mostraCustoDaAquisicaoEValorEstimadoComRotulo() {
        val now = Instant.parse("2026-11-18T12:00:00Z")
        composeRule.setContent {
            DiecastTheme {
                ExemplarDetailContent(
                    state = ExemplarDetailUiState(
                        exemplarId = "e1",
                        internalCode = "HW-000001",
                        status = ExemplarStatus.OWNED,
                        condition = Condition.SEALED,
                        acquisition = Acquisition(
                            id = "a1",
                            exemplarId = "e1",
                            date = IsoDate(LocalDate.of(2026, 11, 2)),
                            price = MoneyCents(5900),
                            shipping = MoneyCents(1200),
                            store = "Loja do shopping",
                            createdAt = now,
                            updatedAt = now,
                        ),
                        acquisitionPrice = "59,00",
                        acquisitionShipping = "12,00",
                        acquisitionDate = "02/11/2026",
                        valuations = listOf(
                            Valuation(
                                id = "val1",
                                exemplarId = "e1",
                                date = IsoDate(LocalDate.of(2026, 11, 10)),
                                value = MoneyCents(8500),
                                source = "Colecionador",
                                createdAt = now,
                            ),
                        ),
                        loading = false,
                    ),
                    contentPadding = PaddingValues(0.dp),
                    onConditionChange = {},
                    onPackagingChange = {},
                    onNotesChange = {},
                    onTargetLocationChange = {},
                    onMoveNoteChange = {},
                    onSaveDetails = {},
                    onMove = {},
                    onArchive = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText("Custo total: R$ 71,00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Estimativa do colecionador — não é preço de mercado.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Valor estimado atual: R$ 85,00")
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * T326/T332 (FR-313, SC-303): em 360 dp com fonte 200% as cinco condições e as cinco
     * opções de embalagem — incluindo «não informada» — continuam inteiras e alcançáveis.
     */
    @Test
    fun condicaoEEmbalagemCabemInteirasEm360ComFonteDobrada() {
        renderResponsive(width = 360.dp, fontScale = 2f)

        CHIPS.forEach { label ->
            composeRule.onNodeWithText(label).performScrollTo().assertIsDisplayed()
            val bounds = composeRule.onNodeWithText(label).getUnclippedBoundsInRoot()
            assertTrue(
                "$label saiu da tela: ${bounds.left}..${bounds.right}",
                bounds.left.value >= 0f && bounds.right.value <= 360f,
            )
        }
    }

    private fun renderResponsive(width: Dp, fontScale: Float = 1f) {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width, fontScale = fontScale) {
                    ExemplarDetailContent(
                        state = ExemplarDetailUiState(
                            exemplarId = "e1",
                            internalCode = "HW-000001",
                            status = ExemplarStatus.OWNED,
                            condition = Condition.SEALED,
                            loading = false,
                        ),
                        contentPadding = PaddingValues(0.dp),
                        onConditionChange = {},
                        onPackagingChange = {},
                        onNotesChange = {},
                        onTargetLocationChange = {},
                        onMoveNoteChange = {},
                        onSaveDetails = {},
                        onMove = {},
                        onArchive = {},
                        onDelete = {},
                    )
                }
            }
        }
    }

    private companion object {
        val CHIPS = listOf(
            "Lacrado",
            "Solto",
            "Danificado",
            "Restaurado",
            "Incompleto",
            "Lacrada",
            "Aberta",
            "Danificada",
            "Sem embalagem",
            "Não informada",
        )
    }
}
