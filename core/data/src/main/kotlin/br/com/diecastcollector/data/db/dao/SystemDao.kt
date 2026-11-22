package br.com.diecastcollector.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.diecastcollector.data.db.entity.AppSettingEntity
import br.com.diecastcollector.data.db.entity.HistoryEntryEntity
import br.com.diecastcollector.data.db.entity.RecognitionResultEntity
import br.com.diecastcollector.data.db.entity.SearchIndexEntity
import kotlinx.coroutines.flow.Flow

/** Trilha de auditoria: só escrita e leitura — nunca `UPDATE` nem `DELETE` (Art. I.8). */
@Dao
public interface HistoryDao {

    @Query("SELECT * FROM history_entry WHERE entity_type = :entityType AND entity_id = :entityId ORDER BY at DESC")
    public fun observeFor(entityType: String, entityId: String): Flow<List<HistoryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: HistoryEntryEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insertAll(entities: List<HistoryEntryEntity>)
}

@Dao
public interface RecognitionResultDao {

    @Query("SELECT * FROM recognition_result WHERE exemplar_id = :exemplarId ORDER BY created_at DESC")
    public fun observeByExemplar(exemplarId: String): Flow<List<RecognitionResultEntity>>

    @Query("SELECT * FROM recognition_result WHERE draft_id = :draftId ORDER BY created_at DESC")
    public suspend fun findByDraft(draftId: String): List<RecognitionResultEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: RecognitionResultEntity)
}

@Dao
public interface AppSettingDao {

    @Query("SELECT value FROM app_setting WHERE key = :key LIMIT 1")
    public suspend fun findValue(key: String): String?

    @Query("SELECT value FROM app_setting WHERE key = :key LIMIT 1")
    public fun observeValue(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun put(entity: AppSettingEntity)

    @Query("DELETE FROM app_setting WHERE key = :key")
    public suspend fun remove(key: String)
}

@Dao
public interface SearchIndexDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insert(entity: SearchIndexEntity)

    @Query("DELETE FROM search_index WHERE variant_id = :variantId")
    public suspend fun deleteByVariant(variantId: String)

    /** `MATCH` sobre o índice com diacríticos removidos (FR-114–FR-122). */
    @Query("SELECT variant_id FROM search_index WHERE search_index MATCH :query LIMIT :limit")
    public suspend fun searchVariantIds(query: String, limit: Int = 50): List<String>

    @Query("INSERT INTO search_index(search_index) VALUES('rebuild')")
    public suspend fun rebuild()
}

