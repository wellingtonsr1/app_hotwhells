package br.com.diecastcollector.domain.usecase

import br.com.diecastcollector.domain.model.RecognitionSource
import br.com.diecastcollector.domain.recognition.RecognitionApplication
import br.com.diecastcollector.domain.recognition.RecognitionField
import br.com.diecastcollector.domain.recognition.RecognitionProposal
import br.com.diecastcollector.domain.recognition.RecognitionSuggestion
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * T121 — nenhuma sugestão vira dado aplicado sem aceite explícito (SC-010, FR-104).
 *
 * O que o teste protege: a proposta é só leitura; quem aplica precisa dizer quais campos
 * aceitou, e o que não foi aceito — ou ficou abaixo do limiar de confiança — nunca entra.
 */
class RecognitionProposalTest {

    private val proposal: RecognitionProposal = RecognitionProposal.of(
        source = RecognitionSource.BARCODE,
        provider = "teste",
        rawValue = "HKC25",
        suggestions = listOf(
            RecognitionSuggestion(RecognitionField.MANUFACTURER, "Hot Wheels", 0.9),
            RecognitionSuggestion(RecognitionField.MODEL, "Nissan Skyline GT-R R34", 0.8),
            RecognitionSuggestion(RecognitionField.YEAR, "2024", 0.4),
        ),
    )

    @Test
    fun `proposta sozinha nao aplica nenhum campo`() {
        assertThat(RecognitionApplication.NONE.isEmpty).isTrue()
        assertThat(proposal.identified).isTrue()
    }

    @Test
    fun `apenas os campos aceitos sao aplicados`() {
        val application = RecognitionApplication.accept(
            proposal = proposal,
            acceptedFields = setOf(RecognitionField.MODEL),
        )

        assertThat(application.fields).containsExactly(
            RecognitionField.MODEL,
            "Nissan Skyline GT-R R34",
        )
    }

    @Test
    fun `aceite vazio nao preenche nada`() {
        val application = RecognitionApplication.accept(proposal, emptySet())

        assertThat(application.isEmpty).isTrue()
    }

    @Test
    fun `campo aceito fora da proposta e ignorado`() {
        val application = RecognitionApplication.accept(
            proposal = proposal,
            acceptedFields = setOf(RecognitionField.SERIES),
        )

        assertThat(application.isEmpty).isTrue()
    }

    @Test
    fun `sugestao abaixo do limiar nunca e aplicada`() {
        // O ano (0,4) foi descartado por RecognitionProposal.of; aceitá-lo não inventa dado.
        val application = RecognitionApplication.accept(
            proposal = proposal,
            acceptedFields = setOf(RecognitionField.YEAR),
        )

        assertThat(application.isEmpty).isTrue()
        assertThat(proposal.suggestions.map { it.field }).doesNotContain(RecognitionField.YEAR)
    }

    @Test
    fun `aplicar nao altera a proposta`() {
        RecognitionApplication.accept(
            proposal = proposal,
            acceptedFields = setOf(RecognitionField.MANUFACTURER),
        )

        assertThat(proposal.suggestions).hasSize(2)
        assertThat(proposal.rawValue).isEqualTo("HKC25")
    }
}
