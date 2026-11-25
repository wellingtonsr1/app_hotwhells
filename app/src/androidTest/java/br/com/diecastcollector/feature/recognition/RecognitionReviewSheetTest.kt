package br.com.diecastcollector.feature.recognition

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.domain.model.RecognitionSource
import br.com.diecastcollector.domain.recognition.RecognitionField
import br.com.diecastcollector.domain.recognition.RecognitionProposal
import br.com.diecastcollector.domain.recognition.RecognitionSuggestion
import br.com.diecastcollector.testing.DeviceWindow
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.testing.WindowSizeFrameTags
import br.com.diecastcollector.ui.theme.DiecastTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** T127 — aceite e rejeição campo a campo (FR-104); T368 — folha por classe (FR-317). */
class RecognitionReviewSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @After
    fun restaurarJanela() {
        DeviceWindow.reset()
    }

    private val proposal = RecognitionProposal.of(
        source = RecognitionSource.OCR,
        provider = "mlkit-text",
        suggestions = listOf(
            RecognitionSuggestion(RecognitionField.MODEL, "Twin Mill", 0.9),
            RecognitionSuggestion(RecognitionField.YEAR, "2024", 0.8),
        ),
    )

    /** Seis sugestões: o corpo passa da altura disponível e precisa rolar. */
    private val sixFields = RecognitionProposal.of(
        source = RecognitionSource.PHOTO,
        provider = "mlkit-photo",
        suggestions = listOf(
            RecognitionSuggestion(RecognitionField.MANUFACTURER, "Hot Wheels Originals", 0.91),
            RecognitionSuggestion(RecognitionField.MODEL, "Twin Mill III", 0.88),
            RecognitionSuggestion(RecognitionField.SERIES, "Mainline 2024", 0.75),
            RecognitionSuggestion(RecognitionField.YEAR, "2024", 0.93),
            RecognitionSuggestion(RecognitionField.VARIANT, "Azul metálico", 0.62),
            RecognitionSuggestion(RecognitionField.CODE, "HW-2024-0001", 0.66),
        ),
    )

    /**
     * T368/FR-317 (C + F): em 360 dp com fonte 200% o corpo rola e as ações ficam no rodapé.
     *
     * O conteúdo é medido em uma moldura de 360 × 640 dp, que é a altura útil de uma janela
     * compacta com o teclado fechado; as ações são procuradas depois de rolar até o último
     * campo, o que só é possível se elas estiverem fora da rolagem.
     */
    @Test
    fun corpoRolaEAcoesFicamFixasEm360ComFonteDobrada() {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = 360.dp, height = 640.dp, fontScale = 2f) {
                    RecognitionReviewContent(
                        proposal = sixFields,
                        onApply = {},
                        onDismissRequest = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Aplicar selecionados").assertIsDisplayed()
        composeRule.onNodeWithTag("recognition-field-CODE").performScrollTo().assertIsDisplayed()

        // A moldura de 640 dp é centrada na tela do teste; medir relativo a ela.
        val frame = composeRule.onNodeWithTag(WindowSizeFrameTags.FRAME)
            .getUnclippedBoundsInRoot()
        val apply = composeRule.onNodeWithText("Aplicar selecionados")
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        assertTrue(
            "Ação fixa em ${apply.top}..${apply.bottom}, moldura em ${frame.top}",
            apply.bottom.value <= frame.top.value + 640f + 1f,
        )
    }

    /**
     * T368/FR-317 (P): na paisagem real (640 × 360 dp) a folha abre com as ações à vista e
     * a última sugestão só depois de rolar — antes o botão saía da janela.
     */
    @Test
    fun acoesFicamVisiveisEmPaisagemCompacta() {
        DeviceWindow.use(widthDp = 640, heightDp = 360)

        composeRule.setContent {
            DiecastTheme {
                RecognitionReviewSheet(
                    proposal = sixFields,
                    onApply = {},
                    onDismissRequest = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Aplicar selecionados").assertIsDisplayed()
        composeRule.onNodeWithText("Descartar").assertIsDisplayed()
        composeRule.onNodeWithTag("recognition-field-CODE").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Aplicar selecionados").assertIsDisplayed()
    }

    /** T368/FR-317 (E): a coluna de leitura para em 720 dp e fica centralizada. */
    @Test
    fun larguraDeLeituraLimitaAColunaEmJanelaExpandida() {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = 1000.dp, height = 900.dp) {
                    RecognitionReviewContent(
                        proposal = sixFields,
                        onApply = {},
                        onDismissRequest = {},
                    )
                }
            }
        }

        // A moldura de 1.000 dp é centrada na tela do teste; medir relativo a ela.
        val frame = composeRule.onNodeWithTag(WindowSizeFrameTags.FRAME)
            .getUnclippedBoundsInRoot()
        val left = frame.left.value + (1000f - 720f) / 2f
        val title = composeRule.onNodeWithText("Revisar sugestões").getUnclippedBoundsInRoot()
        assertTrue("Título em ${title.left}..${title.right}", title.left.value >= left - 1f)
        assertTrue("Título em ${title.left}..${title.right}", title.right.value <= left + 720f + 1f)

        val apply = composeRule.onNodeWithText("Aplicar selecionados").getUnclippedBoundsInRoot()
        assertTrue("Ação em ${apply.left}..${apply.right}", apply.right.value <= left + 720f + 1f)
        assertTrue("Ação em ${apply.left}..${apply.right}", apply.left.value >= left - 1f)
    }

    @Test
    fun aplicaSomenteOsCamposAceitos() {
        var applied: Map<RecognitionField, String>? = null
        composeRule.setContent {
            DiecastTheme {
                RecognitionReviewSheet(
                    proposal = proposal,
                    onApply = { applied = it },
                    onDismissRequest = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Revisar sugestões").assertIsDisplayed()
        composeRule.onNodeWithText("Twin Mill").assertIsDisplayed()
        composeRule.onNodeWithText("2024").assertIsDisplayed()
        composeRule.onNodeWithText("Confiança: 90%").assertIsDisplayed()

        // Nada nasce aceito: sem marcação explícita não há o que aplicar.
        composeRule.onNodeWithText("Aplicar selecionados").assertIsNotEnabled()
        assertNull(applied)

        composeRule.onNodeWithTag("recognition-field-MODEL").performClick()
        composeRule.onNodeWithText("Aplicar selecionados").assertIsEnabled().performClick()

        assertEquals(mapOf(RecognitionField.MODEL to "Twin Mill"), applied)
    }

    @Test
    fun aceitarTodosMarcaTodosOsCampos() {
        var applied: Map<RecognitionField, String>? = null
        composeRule.setContent {
            DiecastTheme {
                RecognitionReviewSheet(
                    proposal = proposal,
                    onApply = { applied = it },
                    onDismissRequest = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Aceitar todos").performClick()
        composeRule.onNodeWithText("Aplicar selecionados").performClick()

        assertEquals(
            mapOf(RecognitionField.MODEL to "Twin Mill", RecognitionField.YEAR to "2024"),
            applied,
        )
    }

    @Test
    fun leituraInconclusivaNaoOfereceAplicacao() {
        var dismissed = false
        composeRule.setContent {
            DiecastTheme {
                RecognitionReviewSheet(
                    proposal = RecognitionProposal.unidentified(
                        RecognitionSource.BARCODE,
                        "mlkit-barcode",
                    ),
                    onApply = {},
                    onDismissRequest = { dismissed = true },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Não identificado").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum campo foi preenchido", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Aplicar selecionados").assertDoesNotExist()
        composeRule.onNodeWithText("Descartar").performClick()

        assertEquals(true, dismissed)
    }
}
