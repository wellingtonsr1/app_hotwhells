package br.com.diecastcollector.feature.recognition

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.diecastcollector.domain.model.RecognitionSource
import br.com.diecastcollector.domain.recognition.RecognitionField
import br.com.diecastcollector.domain.recognition.RecognitionProposal
import br.com.diecastcollector.domain.recognition.RecognitionSuggestion
import br.com.diecastcollector.ui.theme.DiecastTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** T127 — aceite e rejeição campo a campo (FR-104). */
class RecognitionReviewSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val proposal = RecognitionProposal.of(
        source = RecognitionSource.OCR,
        provider = "mlkit-text",
        suggestions = listOf(
            RecognitionSuggestion(RecognitionField.MODEL, "Twin Mill", 0.9),
            RecognitionSuggestion(RecognitionField.YEAR, "2024", 0.8),
        ),
    )

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
