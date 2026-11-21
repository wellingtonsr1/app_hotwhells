package br.com.hotwheelscollector.domain.repository

import br.com.hotwheelscollector.domain.recognition.RecognitionApplication
import br.com.hotwheelscollector.domain.recognition.RecognitionProposal

/**
 * Trilha das leituras de reconhecimento (FR-102–FR-107, T129).
 *
 * `recognition_result` guarda o que foi lido e o que o usuário confirmou; não é catálogo e
 * não cria exemplar (FR-113). `applied_fields` é o registro do aceite campo a campo
 * (FR-104, SC-010), e `exemplarId` só é preenchido quando a leitura corresponde a um item
 * que já existe.
 */
public interface RecognitionRepository {

    public suspend fun record(
        proposal: RecognitionProposal,
        applied: RecognitionApplication,
        exemplarId: String? = null,
        draftId: String? = null,
    ): String
}
