package br.com.hotwheelscollector.domain.usecase

import br.com.hotwheelscollector.domain.recognition.RecognitionApplication
import br.com.hotwheelscollector.domain.recognition.RecognitionProposal
import br.com.hotwheelscollector.domain.repository.RecognitionRepository

/**
 * Registra uma leitura e o que foi aceito dela (T129, FR-104/FR-113).
 *
 * Não grava nada quando não há leitura nem aceite: abrir a câmera ou digitar um código não
 * pode deixar rastro. O registro não toca no catálogo — nem variante, nem exemplar
 * (FR-113).
 */
public class RecordRecognitionResultUseCase(
    private val recognitionRepository: RecognitionRepository,
) {

    public suspend operator fun invoke(
        proposal: RecognitionProposal,
        applied: RecognitionApplication,
        exemplarId: String? = null,
        draftId: String? = null,
    ): String? {
        if (proposal.rawValue.isNullOrBlank() && applied.isEmpty) return null
        return recognitionRepository.record(
            proposal = proposal,
            applied = applied,
            exemplarId = exemplarId,
            draftId = draftId,
        )
    }
}
