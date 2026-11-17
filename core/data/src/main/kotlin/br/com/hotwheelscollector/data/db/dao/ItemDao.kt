package br.com.hotwheelscollector.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.hotwheelscollector.data.db.entity.ExemplarEntity
import br.com.hotwheelscollector.data.db.entity.PhotoEntity
import br.com.hotwheelscollector.data.db.entity.VariantEntity
import br.com.hotwheelscollector.data.db.entity.VariantSecondaryColorEntity
import kotlinx.coroutines.flow.Flow

/** Projeção da lista: o que a tela mostra, contado na consulta (Art. I.2). */
public data class VariantSummaryRow(
    @ColumnInfo(name = "variant_id") val variantId: String,
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "manufacturer_name") val manufacturerName: String,
    @ColumnInfo(name = "color_name") val colorName: String?,
    @ColumnInfo(name = "release_year") val releaseYear: Int?,
    @ColumnInfo(name = "exemplar_count") val exemplarCount: Int,
)

@Dao
public interface VariantDao {

    @Query(
        """
        SELECT v.id AS variant_id,
               m.name AS model_name,
               mf.name AS manufacturer_name,
               c.name AS color_name,
               v.release_year AS release_year,
               (SELECT COUNT(*) FROM exemplar e WHERE e.variant_id = v.id) AS exemplar_count
        FROM variant v
        JOIN car_model m ON m.id = v.model_id
        JOIN manufacturer mf ON mf.id = m.manufacturer_id
        LEFT JOIN color c ON c.id = v.primary_color_id
        WHERE v.archived_at IS NULL
        ORDER BY mf.name COLLATE NOCASE, m.name COLLATE NOCASE, v.created_at
        """,
    )
    public fun observeSummaries(): Flow<List<VariantSummaryRow>>

    @Query("SELECT * FROM variant WHERE model_id = :modelId AND archived_at IS NULL ORDER BY created_at")
    public fun observeByModel(modelId: String): Flow<List<VariantEntity>>

    @Query("SELECT * FROM variant WHERE id = :id")
    public suspend fun findById(id: String): VariantEntity?

    /** Variante ativa com o mesmo fingerprint — a checagem que implementa FR-040. */
    @Query("SELECT * FROM variant WHERE fingerprint = :fingerprint AND archived_at IS NULL LIMIT 1")
    public suspend fun findActiveByFingerprint(fingerprint: String): VariantEntity?

    @Query("SELECT * FROM variant WHERE manufacturer_code = :code AND archived_at IS NULL")
    public suspend fun findActiveByManufacturerCode(code: String): List<VariantEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: VariantEntity)

    @Update
    public suspend fun update(entity: VariantEntity)

    @Query("UPDATE variant SET archived_at = :at, updated_at = :at WHERE id = :id")
    public suspend fun archive(id: String, at: Long)
}

@Dao
public interface VariantSecondaryColorDao {

    @Query("SELECT color_id FROM variant_secondary_color WHERE variant_id = :variantId")
    public suspend fun findColorIds(variantId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public suspend fun insertAll(rows: List<VariantSecondaryColorEntity>)

    @Query("DELETE FROM variant_secondary_color WHERE variant_id = :variantId")
    public suspend fun deleteByVariant(variantId: String)
}

@Dao
public interface ExemplarDao {

    @Query("SELECT * FROM exemplar WHERE status = 'OWNED' ORDER BY created_at DESC")
    public fun observeOwned(): Flow<List<ExemplarEntity>>

    @Query("SELECT * FROM exemplar WHERE variant_id = :variantId ORDER BY created_at")
    public fun observeByVariant(variantId: String): Flow<List<ExemplarEntity>>

    @Query("SELECT * FROM exemplar WHERE id = :id")
    public suspend fun findById(id: String): ExemplarEntity?

    @Query("SELECT * FROM exemplar WHERE internal_code = :code LIMIT 1")
    public suspend fun findByInternalCode(code: String): ExemplarEntity?

    @Query("SELECT COUNT(*) FROM exemplar WHERE variant_id = :variantId")
    public suspend fun countByVariant(variantId: String): Int

    @Query("SELECT COUNT(*) FROM exemplar")
    public suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: ExemplarEntity)

    /**
     * Atualização otimista: só grava se a versão lida ainda for a vigente (Art. I.12).
     * Retorna 0 quando outra escrita aconteceu no meio — o chamador recarrega e avisa.
     */
    @Query(
        """
        UPDATE exemplar
        SET condition = :condition,
            packaging_condition = :packagingCondition,
            notes = :notes,
            updated_at = :updatedAt,
            row_version = row_version + 1
        WHERE id = :id AND row_version = :expectedVersion
        """,
    )
    public suspend fun updateWithVersion(
        id: String,
        condition: String,
        packagingCondition: String?,
        notes: String?,
        updatedAt: Long,
        expectedVersion: Long,
    ): Int

    @Query("UPDATE exemplar SET location_id = :locationId, updated_at = :at WHERE id = :id")
    public suspend fun setLocation(id: String, locationId: String?, at: Long)

    @Query("UPDATE exemplar SET status = :status, updated_at = :at WHERE id = :id")
    public suspend fun setStatus(id: String, status: String, at: Long)

    /** União de variantes (FR-041): move os exemplares para a variante que fica. */
    @Query("UPDATE exemplar SET variant_id = :toVariantId, updated_at = :at WHERE variant_id = :fromVariantId")
    public suspend fun reassignVariant(fromVariantId: String, toVariantId: String, at: Long): Int
}

@Dao
public interface PhotoDao {

    @Query("SELECT * FROM photo WHERE exemplar_id = :exemplarId ORDER BY sort_order, captured_at")
    public fun observeByExemplar(exemplarId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photo WHERE exemplar_id = :exemplarId AND is_primary = 1 LIMIT 1")
    public suspend fun findPrimary(exemplarId: String): PhotoEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: PhotoEntity)

    @Query("UPDATE photo SET is_primary = 0 WHERE exemplar_id = :exemplarId")
    public suspend fun clearPrimary(exemplarId: String)

    @Query("UPDATE photo SET is_primary = 1 WHERE id = :photoId")
    public suspend fun markPrimary(photoId: String)

    @Query("DELETE FROM photo WHERE id = :photoId")
    public suspend fun delete(photoId: String)
}
