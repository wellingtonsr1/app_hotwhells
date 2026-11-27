package br.com.diecastcollector.feature.catalog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.DuplicateAvailability
import br.com.diecastcollector.domain.model.DuplicateOffer
import br.com.diecastcollector.domain.model.Exemplar
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.feature.duplicates.ExemplarOfferSheet
import br.com.diecastcollector.feature.duplicates.OfferSheetSubject
import br.com.diecastcollector.feature.duplicates.UniqueExemplarDialog
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Responsividade e acessibilidade das superfícies alteradas (T524, FR-521, FR-522, SC-513).
 *
 * Renderiza a lista e a ficha em 360/700/1.000 dp, em retrato e paisagem compacta, e com
 * fonte do sistema em 200%: as ações de disponibilidade e negociação precisam continuar
 * inteiras, dentro da largura e com alvo de toque ≥ 48 dp. A folha e o aviso são verificados
 * com fonte dobrada, onde o risco de corte é maior.
 *
 * A semântica de leitor de tela não é exercitada aqui (TalkBack é evidência manual); o que
 * se prova é texto visível e alvo de toque.
 */
@RunWith(AndroidJUnit4::class)
class ExemplarNegotiationResponsiveTest {

    @get:Rule
    val compose = createComposeRule()

    private val now: Instant = Instant.parse("2026-01-01T00:00:00Z")

    private data class Config(val nome: String, val width: Dp, val height: Dp, val fontScale: Float)

    private val configs = listOf(
        Config("compacta 360 dp", 360.dp, 800.dp, 1f),
        Config("média 700 dp", 700.dp, 800.dp, 1f),
        Config("expandida 1.000 dp", 1000.dp, 800.dp, 1f),
        Config("paisagem compacta 640 × 360 dp", 640.dp, 360.dp, 1f),
        Config("compacta 360 dp com fonte 200%", 360.dp, 800.dp, 2f),
    )

    @Test
    fun listaMantemAsDuasAcoesInteirasEmTodasAsJanelas() {
        var config by mutableStateOf(configs.first())
        compose.setContent {
            DiecastTheme {
                WindowSizeFrame(
                    width = config.width,
                    height = config.height,
                    fontScale = config.fontScale,
                ) {
                    ExemplarListContent(
                        state = ExemplarListUiState(
                            variantId = "variant-a",
                            exemplars = listOf(exemplar()),
                            offers = mapOf("e1" to offer(DuplicateAvailability.BOTH, 1_234_500L)),
                            loading = false,
                        ),
                        onAddExemplar = {},
                        onOpenExemplar = {},
                    )
                }
            }
        }

        configs.forEach { alvo ->
            compose.runOnUiThread { config = alvo }
            compose.waitForIdle()

            listOf("Disponibilidade", "Negociar").forEach { acao ->
                // O enquadramento pode ser mais largo que a tela do teste (700/1.000 dp):
                // o que se prova é que o nó existe, tem alvo de 48 dp e cabe na largura.
                compose.onNodeWithText(acao).assertExists()
                alvoDeToque(acao, alvo.nome)
                val bounds = compose.onNodeWithText(acao).getUnclippedBoundsInRoot()
                assertTrue(
                    "«$acao» saiu da largura em ${alvo.nome}: ${bounds.right} > ${alvo.width}",
                    bounds.right.value <= alvo.width.value + 1f,
                )
            }
            compose.onNodeWithText("Troca e venda").assertExists()
        }
    }

    @Test
    fun fichaMantemAsAcoesAlcancaveisEmTodasAsJanelas() {
        var config by mutableStateOf(configs.first())
        compose.setContent {
            DiecastTheme {
                WindowSizeFrame(
                    width = config.width,
                    height = config.height,
                    fontScale = config.fontScale,
                ) {
                    ExemplarDetailContent(
                        state = ExemplarDetailUiState(
                            exemplarId = "e1",
                            variantId = "variant-a",
                            internalCode = "HW-0001",
                            status = ExemplarStatus.OWNED,
                            condition = Condition.LOOSE,
                            offer = offer(DuplicateAvailability.SALE, 4_500L),
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

        configs.forEach { alvo ->
            compose.runOnUiThread { config = alvo }
            compose.waitForIdle()

            compose.onNodeWithText("Negociação").performScrollTo().assertExists()
            listOf("Disponibilidade", "Negociar").forEach { acao ->
                compose.onNodeWithText(acao).performScrollTo().assertExists()
                alvoDeToque(acao, alvo.nome)
                val bounds = compose.onNodeWithText(acao).getUnclippedBoundsInRoot()
                assertTrue(
                    "«$acao» saiu da largura em ${alvo.nome}: ${bounds.right} > ${alvo.width}",
                    bounds.right.value <= alvo.width.value + 1f,
                )
            }
        }
    }

    @Test
    fun folhaEAvisoMantemOsBotoesInteirosComFonteDobrada() {
        var showWarning by mutableStateOf(false)
        compose.setContent {
            DiecastTheme {
                WindowSizeFrame(width = 360.dp, height = 800.dp, fontScale = 2f) {
                    if (showWarning) {
                        UniqueExemplarDialog(onContinue = {}, onCancel = {})
                    } else {
                        ExemplarOfferSheet(
                            subject = OfferSheetSubject(
                                internalCode = "HW-0001",
                                title = "Mattel · Twin Mill · Azul",
                                condition = Condition.LOOSE,
                                offer = null,
                            ),
                            saving = false,
                            onDismiss = {},
                            onSave = { _, _, _, _ -> },
                        )
                    }
                }
            }
        }
        compose.waitForIdle()

        listOf("Salvar", "Cancelar").forEach { botao ->
            compose.onNodeWithText(botao).assertIsDisplayed()
            alvoDeToque(botao, "folha com fonte 200%")
        }
        compose.onNodeWithText("Disponibilidade do exemplar").assertExists()

        compose.runOnUiThread { showWarning = true }
        compose.waitForIdle()

        listOf("Continuar", "Cancelar").forEach { botao ->
            compose.onNodeWithText(botao).assertIsDisplayed()
            alvoDeToque(botao, "aviso com fonte 200%")
        }
        compose.onNodeWithText("Último exemplar da variante").assertExists()
    }

    /**
     * Alvo de toque medido em px sobre a densidade: o botão do Material 3 desenha 40 dp, mas
     * o alvo interativo do componente é expandido para 48 dp (FR-522).
     */
    private fun alvoDeToque(texto: String, cenario: String) {
        val alvo = compose.onNodeWithText(texto).fetchSemanticsNode().touchBoundsInRoot
        val minimo = 48f * compose.density.density
        assertTrue(
            "Alvo de «$texto» em $cenario tem ${alvo.height} px, esperado ao menos $minimo px",
            alvo.height >= minimo - 1f,
        )
    }

    private fun exemplar(): Exemplar = Exemplar(
        id = "e1",
        variantId = "variant-a",
        internalCode = "HW-0001",
        condition = Condition.LOOSE,
        status = ExemplarStatus.OWNED,
        locationId = null,
        notes = null,
        createdAt = now,
        updatedAt = now,
    )

    private fun offer(availability: DuplicateAvailability, priceCents: Long) = DuplicateOffer(
        id = "offer-e1",
        exemplarId = "e1",
        availability = availability,
        condition = Condition.LOOSE,
        askingPrice = MoneyCents(priceCents),
        notes = null,
        createdAt = now,
        updatedAt = now,
    )
}
