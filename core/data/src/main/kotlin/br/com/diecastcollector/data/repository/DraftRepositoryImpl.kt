package br.com.diecastcollector.data.repository

import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.entity.AppSettingEntity
import br.com.diecastcollector.domain.repository.DraftRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Persistência de rascunhos de cadastro no banco local via `app_setting` (T047 - US2, FR-023).
 *
 * Garante que formulários em preenchimento possam ser recuperados após interrupção
 * do processo ou rotação da tela, sem depender exclusivamente da memória volátil.
 */
@Singleton
public class DraftRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
) : DraftRepository {

    override suspend fun saveDraft(key: String, payloadJson: String) {
        database.appSettingDao().put(
            AppSettingEntity(
                key = DRAFT_PREFIX + key,
                value = payloadJson,
            ),
        )
    }

    override suspend fun getDraft(key: String): String? {
        return database.appSettingDao().findValue(DRAFT_PREFIX + key)
    }

    override fun observeDraft(key: String): Flow<String?> {
        return database.appSettingDao().observeValue(DRAFT_PREFIX + key)
    }

    override suspend fun clearDraft(key: String) {
        database.appSettingDao().remove(DRAFT_PREFIX + key)
    }

    private companion object {
        const val DRAFT_PREFIX = "draft_"
    }
}
