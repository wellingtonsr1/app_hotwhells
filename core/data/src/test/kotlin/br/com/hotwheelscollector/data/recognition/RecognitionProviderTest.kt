package br.com.hotwheelscollector.data.recognition

import br.com.hotwheelscollector.domain.model.RecognitionSource
import br.com.hotwheelscollector.domain.recognition.RecognitionField
import br.com.hotwheelscollector.domain.recognition.RecognitionInput
import br.com.hotwheelscollector.domain.recognition.RecognitionProposal
import br.com.hotwheelscollector.domain.recognition.RecognitionSuggestion
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * T122 — leitura inconclusiva vira "não identificado", sem inferência (FR-105).
 *
 * O parser é testado em JVM; a decodificação de imagem pelo ML Kit é nativa e fica para o
 * teste instrumentado. O caminho de código já lido não depende do modelo.
 */
class RecognitionProviderTest {

    @Test
    fun `texto de embalagem vira sugestoes com confianca`() {
        val proposal = proposalFromText(
            """
            HOT WHEELS
            Car Culture
            Nissan Skyline GT-R R34
            2024
            HKC25
            1:64
            """.trimIndent(),
        )

        assertThat(proposal.identified).isTrue()
        val byField = proposal.suggestions.associateBy { it.field }
        assertThat(byField[RecognitionField.MANUFACTURER]?.value).isEqualTo("Hot Wheels")
        assertThat(byField[RecognitionField.MODEL]?.value).isEqualTo("Nissan Skyline GT-R R34")
        assertThat(byField[RecognitionField.YEAR]?.value).isEqualTo("2024")
        assertThat(byField[RecognitionField.CODE]?.value).isEqualTo("HKC25")
        // Escala não é código nem nome.
        assertThat(proposal.suggestions.map { it.value }).doesNotContain("1:64")
        assertThat(proposal.suggestions).isNotEmpty()
        assertThat(proposal.suggestions.all { it.confidence >= RecognitionProposal.CONFIDENCE_THRESHOLD })
            .isTrue()
    }

    @Test
    fun `rotulos explicitos preenchem serie e demais campos`() {
        val proposal = proposalFromText(
            """
            Fabricante: Hot Wheels
            Modelo: Nissan Skyline GT-R R34
            Série: Car Culture
            Ano: 2024
            Código: HKC25
            """.trimIndent(),
        )

        val byField = proposal.suggestions.associateBy { it.field }
        assertThat(byField.keys).containsExactly(
            RecognitionField.MANUFACTURER,
            RecognitionField.MODEL,
            RecognitionField.SERIES,
            RecognitionField.YEAR,
            RecognitionField.CODE,
        )
        assertThat(byField[RecognitionField.SERIES]?.value).isEqualTo("Car Culture")
    }

    @Test
    fun `texto sem informacao suficiente fica nao identificado`() {
        val proposal = proposalFromText("1:64\n-\n")

        assertThat(proposal.identified).isFalse()
        assertThat(proposal.suggestions).isEmpty()
    }

    @Test
    fun `sugestao abaixo do limiar nao entra na proposta`() {
        val proposal = RecognitionProposal.of(
            source = RecognitionSource.OCR,
            provider = "teste",
            suggestions = listOf(
                RecognitionSuggestion(RecognitionField.MODEL, "talvez", 0.4),
            ),
        )

        assertThat(proposal.identified).isFalse()
        assertThat(proposal.suggestions).isEmpty()
    }

    @Test
    fun `codigo ja lido dispensa a camera`() = runBlocking {
        val proposal = MlKitBarcodeProvider().recognize(RecognitionInput.Barcode("HKC25"))

        assertThat(proposal.identified).isTrue()
        assertThat(proposal.source).isEqualTo(RecognitionSource.BARCODE)
        assertThat(proposal.suggestions.single().field).isEqualTo(RecognitionField.CODE)
        assertThat(proposal.suggestions.single().value).isEqualTo("HKC25")
        assertThat(proposal.suggestions.single().confidence).isEqualTo(1.0)
    }

    @Test
    fun `codigo vazio nao vira sugestao`() = runBlocking {
        val proposal = MlKitBarcodeProvider().recognize(RecognitionInput.Barcode("  "))

        assertThat(proposal.identified).isFalse()
    }

    @Test
    fun `OCR nao trata codigo como texto de embalagem`() = runBlocking {
        val proposal = MlKitTextProvider().recognize(RecognitionInput.Barcode("HKC25"))

        assertThat(proposal.identified).isFalse()
        assertThat(proposal.source).isEqualTo(RecognitionSource.OCR)
    }

    private fun proposalFromText(text: String): RecognitionProposal =
        RecognitionProposal.of(
            source = RecognitionSource.OCR,
            provider = "mlkit-text",
            rawValue = text,
            suggestions = PackagingTextParser.parse(text),
        )
}
