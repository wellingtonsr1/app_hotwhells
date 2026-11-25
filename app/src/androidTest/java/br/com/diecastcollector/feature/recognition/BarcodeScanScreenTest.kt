package br.com.diecastcollector.feature.recognition

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.diecastcollector.domain.model.RecognitionSource
import br.com.diecastcollector.domain.recognition.RecognitionField
import br.com.diecastcollector.domain.recognition.RecognitionProposal
import br.com.diecastcollector.domain.recognition.RecognitionSuggestion
import br.com.diecastcollector.domain.usecase.CatalogMatch
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.testing.WindowSizeFrameTags
import br.com.diecastcollector.ui.theme.DiecastTheme
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** T128 — correspondência com origem, ausência com cadastro manual e câmera negada (FR-108–113). */
class BarcodeScanScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val proposal = RecognitionProposal.of(
        source = RecognitionSource.BARCODE,
        provider = "mlkit-barcode",
        rawValue = "HKC25",
        suggestions = listOf(
            RecognitionSuggestion(RecognitionField.CODE, "HKC25", 1.0),
        ),
    )

    private val matches = listOf(
        CatalogMatch(
            variantId = "var-1",
            exemplarId = "ex-1",
            modelName = "Twin Mill",
            manufacturerName = "Hot Wheels",
            seriesName = "Car Culture",
            releaseYear = 2024,
            manufacturerCode = "HKC25",
            confidence = 1.0,
            matchedFields = listOf(RecognitionField.CODE),
        ),
        CatalogMatch(
            variantId = "var-2",
            exemplarId = "ex-2",
            modelName = "Bone Shaker",
            manufacturerName = "Hot Wheels",
            seriesName = null,
            releaseYear = 2020,
            manufacturerCode = "HKC25",
            confidence = 0.8,
            matchedFields = listOf(RecognitionField.CODE),
        ),
    )

    @Test
    fun correspondenciaMostraDadosEUsaAVarianteEscolhida() {
        var usedVariant: String? = null
        var selectedExemplarId by mutableStateOf("ex-1")
        composeRule.setContent {
            DiecastTheme {
                BarcodeScanContent(
                    state = RecognitionViewModel.State(
                        proposal = proposal,
                        matches = matches,
                        selectedExemplarId = selectedExemplarId,
                    ),
                    cameraDenied = false,
                    onBack = {},
                    onTypedCodeChange = {},
                    onReadTypedCode = {},
                    onUseCamera = {},
                    onReview = {},
                    onSaveReading = {},
                    onSelectMatch = { selectedExemplarId = it },
                    onUseVariant = { usedVariant = it },
                    onCreateManual = {},
                    onDismissFailure = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Correspondência mostra os dados e a origem do catálogo local (FR-110).
        composeRule.onNodeWithText("Hot Wheels · Twin Mill").assertIsDisplayed()
        composeRule.onNodeWithText("Car Culture · 2024 · HKC25").assertIsDisplayed()

        composeRule.onNodeWithText("Hot Wheels · Bone Shaker").performClick()
        composeRule.onNodeWithText("Adicionar exemplar desta variante").performClick()

        assertEquals("var-2", usedVariant)
    }

    /**
     * T363 (VII.14): em 360 dp com fonte 200% o valor longo não espreme o rótulo nem sai da
     * moldura; as ações de revisão continuam inteiras.
     */
    @Test
    fun correspondenciaLongaCabeEm360ComFonteDobrada() {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = 360.dp, fontScale = 2f) {
                    BarcodeScanContent(
                        state = RecognitionViewModel.State(
                            typedCode = "7891234567890123456789",
                            proposal = proposal,
                            matches = matches,
                            selectedExemplarId = "ex-1",
                        ),
                        cameraDenied = false,
                        onBack = {},
                        onTypedCodeChange = {},
                        onReadTypedCode = {},
                        onUseCamera = {},
                        onReview = {},
                        onSaveReading = {},
                        onSelectMatch = {},
                        onUseVariant = {},
                        onCreateManual = {},
                        onDismissFailure = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Adicionar exemplar desta variante").assertExists()
        val frame = composeRule.onNodeWithTag(WindowSizeFrameTags.FRAME)
            .getUnclippedBoundsInRoot()
        val valor = composeRule.onNodeWithText("7891234567890123456789", substring = true)
            .getUnclippedBoundsInRoot()
        assertTrue(
            "Valor saiu da moldura: ${valor.left}..${valor.right}",
            valor.left.value >= frame.left.value - 1f &&
                valor.right.value <= frame.right.value + 1f,
        )
        val rotulo = composeRule.onNodeWithText("Hot Wheels · Twin Mill").getUnclippedBoundsInRoot()
        assertTrue(
            "Rótulo saiu da moldura: ${rotulo.left}..${rotulo.right}",
            rotulo.left.value >= frame.left.value - 1f &&
                rotulo.right.value <= frame.right.value + 1f,
        )
    }

    @Test
    fun codigoSemCorrespondenciaOfereceCadastroManualComOCodigoLido() {
        var createdWith: String? = null
        composeRule.setContent {
            DiecastTheme {
                BarcodeScanContent(
                    state = RecognitionViewModel.State(proposal = proposal),
                    cameraDenied = false,
                    onBack = {},
                    onTypedCodeChange = {},
                    onReadTypedCode = {},
                    onUseCamera = {},
                    onReview = {},
                    onSaveReading = {},
                    onSelectMatch = {},
                    onUseVariant = {},
                    onCreateManual = { createdWith = it },
                    onDismissFailure = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Nenhuma correspondência no catálogo.").assertIsDisplayed()
        composeRule.onNodeWithText("Cadastrar manualmente").performClick()

        assertEquals("HKC25", createdWith)
    }

    @Test
    fun cameraNegadaMantemOCodigoDigitadoUtilizavel() {
        composeRule.setContent {
            DiecastTheme {
                BarcodeScanContent(
                    state = RecognitionViewModel.State(typedCode = "HKC25"),
                    cameraDenied = true,
                    onBack = {},
                    onTypedCodeChange = {},
                    onReadTypedCode = {},
                    onUseCamera = {},
                    onReview = {},
                    onSaveReading = {},
                    onSelectMatch = {},
                    onUseVariant = {},
                    onCreateManual = {},
                    onDismissFailure = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Sem permissão de câmera", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Ler código digitado").assertIsDisplayed()
    }
}
