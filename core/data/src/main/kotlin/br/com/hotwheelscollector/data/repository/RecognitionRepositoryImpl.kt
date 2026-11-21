package br.com.hotwheelscollector.data.repository

import br.com.hotwheelscollector.data.db.dao.RecognitionResultDao
import br.com.hotwheelscollector.data.db.entity.RecognitionResultEntity
import br.com.hotwheelscollector.domain.recognition.RecognitionApplication
import br.com.hotwheelscollector.domain.recognition.RecognitionProposal
import br.com.hotwheelscollector.domain.repository.RecognitionRepository
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistência da leitura de reconhecimento (T129, FR-102–FR-107).
 *
 * A linha guarda a proposta inteira em `payload_json` (para conferência posterior),
 * `applied_fields` com o que o usuário aceitou e o exemplar apenas quando existe
 * correspondência no catálogo. Nenhuma outra tabela é tocada: reconhecer não cria nem
 * altera variante/exemplar (FR-113).
 */
@Singleton
public class RecognitionRepositoryImpl @Inject constructor(
    private val dao: RecognitionResultDao,
    private val clock: Clock,
) : RecognitionRepository {

    override suspend fun record(
        proposal: RecognitionProposal,
        applied: RecognitionApplication,
        exemplarId: String?,
        draftId: String?,
    ): String {
        val id = UUID.randomUUID().toString()
        dao.insert(
            RecognitionResultEntity(
                id = id,
                exemplarId = exemplarId,
                draftId = draftId,
                source = proposal.source.name,
                provider = proposal.provider,
                payloadJson = payloadOf(proposal, applied),
                confidence = proposal.suggestions.maxOfOrNull { it.confidence },
                appliedFields = applied.fields.keys
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(",") { it.name },
                createdAt = clock.millis(),
            ),
        )
        return id
    }

    private fun payloadOf(
        proposal: RecognitionProposal,
        applied: RecognitionApplication,
    ): String = JSONObject()
        .put("source", proposal.source.name)
        .put("provider", proposal.provider)
        .put("identified", proposal.identified)
        .put("raw_value", proposal.rawValue ?: JSONObject.NULL)
        .put(
            "suggestions",
            JSONArray().apply {
                proposal.suggestions.forEach { suggestion ->
                    put(
                        JSONObject()
                            .put("field", suggestion.field.name)
                            .put("value", suggestion.value)
                            .put("confidence", suggestion.confidence),
                    )
                }
            },
        )
        .put("applied_fields", JSONArray(applied.fields.keys.map { it.name }))
        .toString()
}
