package br.com.diecastcollector.feature.duplicates

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.DuplicateExemplar
import br.com.diecastcollector.domain.model.DuplicateGroup
import br.com.diecastcollector.domain.model.Exemplar
import br.com.diecastcollector.domain.model.Negotiation
import br.com.diecastcollector.domain.model.NegotiationStatus
import br.com.diecastcollector.domain.model.NegotiationType
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Duplicados e histórico por classe de janela (T321, VII.14).
 *
 * A grade de duplicados usa 320 dp de largura mínima: 1 coluna em 360, 2 em 700 e 2 em
 * 1.000 dp — a terceira só caberia acima disso, e a tarefa admite 2–3 em E. O histórico de
 * negociação é somado em duas colunas a partir da janela média, por decisão de desenho
 * (não pela grade).
 */
@RunWith(AndroidJUnit4::class)
class DuplicatesResponsiveTest {

    @get:Rule
    val compose = createComposeRule()

    private val now: Instant = Instant.parse("2026-01-01T00:00:00Z")

    private val titulos = listOf(
        "Mattel · Twin Mill · Azul",
        "Mattel · Bone Shaker · Vermelho",
        "Mattel · Rodger Dodger · Verde",
    )

    @Test
    fun duplicadosUsamColunasPorClasseDeJanela() {
        var width by mutableStateOf(360.dp)
        compose.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width) {
                    DuplicatesContent(
                        state = DuplicatesUiState(groups = grupos(), loading = false),
                        onBack = {},
                        onOpenNegotiation = {},
                        onSelect = {},
                        onDismissSheet = {},
                        onSaveOffer = { _, _, _, _ -> },
                        onMessageShown = {},
                    )
                }
            }
        }

        listOf(360.dp to 1, 700.dp to 2, 1000.dp to 2).forEach { (target, colunas) ->
            compose.runOnUiThread { width = target }
            compose.waitForIdle()
            assertEquals("Colunas de duplicados em $target", colunas, colunasDe(titulos))
        }
    }

    @Test
    fun historicoDeNegociacaoEmpilhaEmCEEUsaDuasColunasEmM() {
        var width by mutableStateOf(360.dp)
        val parceiros = listOf("Interessado A", "Interessado B")
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val linhas = parceiros.map { parceiro ->
            contexto.getString(R.string.negotiation_counterparty_value, parceiro)
        }
        compose.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width) {
                    NegotiationHistoryScreen(
                        negotiations = negociacoes(),
                        events = emptyList(),
                    )
                }
            }
        }

        compose.runOnUiThread { width = 360.dp }
        compose.waitForIdle()
        assertEquals("Histórico em 360 dp", 1, colunasDe(linhas))

        listOf(700.dp, 1000.dp).forEach { target ->
            compose.runOnUiThread { width = target }
            compose.waitForIdle()
            assertEquals("Histórico em $target", 2, colunasDe(linhas))
        }
    }

    /** Conta colunas pelo `left` distinto dos itens: uma coluna a mais aparece como outro x. */
    private fun colunasDe(textos: List<String>): Int = textos
        .map { texto -> compose.onNodeWithText(texto).getUnclippedBoundsInRoot().left.value }
        .distinct()
        .size

    private fun grupos(): List<DuplicateGroup> = titulos.mapIndexed { index, titulo ->
        val partes = titulo.split(" · ")
        DuplicateGroup(
            variantId = "variant-$index",
            manufacturerName = partes[0],
            modelName = partes[1],
            colorName = partes[2],
            exemplars = listOf(
                DuplicateExemplar(
                    exemplar = Exemplar(
                        id = "exemplar-$index",
                        variantId = "variant-$index",
                        internalCode = "HW-000$index",
                        condition = Condition.LOOSE,
                        createdAt = now,
                        updatedAt = now,
                    ),
                    manufacturerName = partes[0],
                    modelName = partes[1],
                    colorName = partes[2],
                ),
            ),
        )
    }

    private fun negociacoes(): List<Negotiation> = listOf(
        Negotiation(
            id = "n1",
            exemplarId = "exemplar-0",
            type = NegotiationType.SALE,
            status = NegotiationStatus.OPEN,
            startedAt = now,
            counterparty = "Interessado A",
        ),
        Negotiation(
            id = "n2",
            exemplarId = "exemplar-1",
            type = NegotiationType.TRADE,
            status = NegotiationStatus.CLOSED,
            startedAt = now,
            closedAt = now,
            counterparty = "Interessado B",
        ),
    )
}
