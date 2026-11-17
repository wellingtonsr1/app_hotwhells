package br.com.hotwheelscollector.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repositório de rascunhos de formulário (T047 - US2, FR-023).
 * Persiste rascunhos em preenchimento para recuperação após interrupção.
 */
public interface DraftRepository {
    public suspend fun saveDraft(key: String, payloadJson: String)
    public suspend fun getDraft(key: String): String?
    public fun observeDraft(key: String): Flow<String?>
    public suspend fun clearDraft(key: String)
}
