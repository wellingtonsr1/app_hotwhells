package br.com.diecastcollector.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.diecastcollector.data.db.entity.ExemplarEntity
import br.com.diecastcollector.data.db.entity.PhotoEntity
import br.com.diecastcollector.data.db.entity.VariantEntity
import br.com.diecastcollector.data.db.entity.VariantSecondaryColorEntity
import kotlinx.coroutines.flow.Flow

/** Projeção da lista: o que a tela mostra, contado na consulta (Art. I.2). */
public data class VariantSummaryRow(
    @ColumnInfo(name = "variant_id") val variantId: String,
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "manufacturer_name") val manufacturerName: String,
    @ColumnInfo(name = "color_name") val colorName: String?,
    @ColumnInfo(name = "release_year") val releaseYear: Int?,
    @ColumnInfo(name = "exemplar_count") val exemplarCount: Int,
    @ColumnInfo(name = "primary_photo_path") val primaryPhotoPath: String?,
)

/** Foto principal de um exemplar, para a miniatura das listas. */
public data class ExemplarPhotoRow(
    @ColumnInfo(name = "exemplar_id") val exemplarId: String,
    @ColumnInfo(name = "path") val path: String,
)

/** Projeção do relatório de inventário: exemplar com modelo, variante e contagem de fotos. */
public data class InventoryReportRow(
    @ColumnInfo(name = "internal_code") val internalCode: String,
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "variant_label") val variantLabel: String?,
    @ColumnInfo(name = "exemplar_condition") val condition: String,
    @ColumnInfo(name = "photo_count") val photoCount: Int,
)

/** Projeção de variante com campo faltando: o outro caso de FR-029. */
public data class IncompleteVariantRow(
    @ColumnInfo(name = "variant_id") val variantId: String,
    @ColumnInfo(name = "model_id") val modelId: String,
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "manufacturer_name") val manufacturerName: String,
    @ColumnInfo(name = "scale") val scale: String,
    @ColumnInfo(name = "missing_primary_color") val missingPrimaryColor: Boolean,
    @ColumnInfo(name = "missing_release_year") val missingReleaseYear: Boolean,
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
               (SELECT COUNT(*) FROM exemplar e WHERE e.variant_id = v.id AND e.status = 'OWNED') AS exemplar_count,
               (SELECT p.path
                FROM photo p
                JOIN exemplar e ON e.id = p.exemplar_id
                WHERE e.variant_id = v.id
                ORDER BY (1 - p.is_primary), p.sort_order, p.captured_at
                LIMIT 1) AS primary_photo_path
        FROM variant v
        JOIN car_model m ON m.id = v.model_id
        JOIN manufacturer mf ON mf.id = m.manufacturer_id
        LEFT JOIN color c ON c.id = v.primary_color_id
        WHERE v.archived_at IS NULL
        ORDER BY mf.name COLLATE NOCASE, m.name COLLATE NOCASE, v.created_at
        """,
    )
    public fun observeSummaries(): Flow<List<VariantSummaryRow>>

    /**
     * Variantes ativas sem cor principal ou sem ano — cadastro que não dá para reconhecer
     * e por isso entra na lista de incompletos (FR-029).
     *
     * O `CASE` vira `Boolean` no Room: a consulta devolve 0 ou 1, e o mapeador sabe o que
     * cada coluna significa. Variante arquivada ou de modelo arquivado não entra.
     */
    @Query(
        """
        SELECT v.id AS variant_id,
               m.id AS model_id,
               m.name AS model_name,
               mf.name AS manufacturer_name,
               m.scale AS scale,
               v.primary_color_id IS NULL AS missing_primary_color,
               v.release_year IS NULL AS missing_release_year
        FROM variant v
        JOIN car_model m ON m.id = v.model_id
        JOIN manufacturer mf ON mf.id = m.manufacturer_id
        WHERE v.archived_at IS NULL
          AND m.archived_at IS NULL
          AND (v.primary_color_id IS NULL OR v.release_year IS NULL)
        ORDER BY mf.name COLLATE NOCASE, m.name COLLATE NOCASE, v.created_at
        """,
    )
    public fun observeIncompleteVariants(): Flow<List<IncompleteVariantRow>>

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

    @Query("SELECT * FROM exemplar WHERE variant_id = :variantId AND status = 'OWNED' ORDER BY created_at")
    public fun observeByVariant(variantId: String): Flow<List<ExemplarEntity>>

    @Query("SELECT * FROM exemplar WHERE id = :id")
    public suspend fun findById(id: String): ExemplarEntity?

    @Query("SELECT * FROM exemplar WHERE internal_code = :code LIMIT 1")
    public suspend fun findByInternalCode(code: String): ExemplarEntity?

    /**
     * Linhas do relatório de inventário (T118): exemplares possuídos com o modelo, a cor da
     * variante e a contagem de fotos, já resolvidos na consulta (sem N+1).
     */
    @Query(
        """
        SELECT e.internal_code AS internal_code,
               m.name AS model_name,
               c.name AS variant_label,
               e.condition AS exemplar_condition,
               (SELECT COUNT(*) FROM photo p WHERE p.exemplar_id = e.id) AS photo_count
        FROM exemplar e
        JOIN variant v ON v.id = e.variant_id
        JOIN car_model m ON m.id = v.model_id
        LEFT JOIN color c ON c.id = v.primary_color_id
        WHERE e.status = 'OWNED'
        ORDER BY m.name COLLATE NOCASE, e.internal_code
        """,
    )
    public suspend fun inventoryReportRows(): List<InventoryReportRow>

    @Query("SELECT COUNT(*) FROM exemplar WHERE variant_id = :variantId AND status = 'OWNED'")
    public suspend fun countByVariant(variantId: String): Int

    @Query("SELECT COUNT(*) FROM exemplar")
    public suspend fun countAll(): Int

    /** Códigos internos já existentes, para a importação detectar duplicata (FR-138). */
    @Query("SELECT internal_code FROM exemplar")
    public suspend fun allInternalCodes(): List<String>

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

    /**
     * Separação de variante (FR-042): troca o vínculo de um exemplar com checagem de
     * versão, para não sobrescrever outra edição (Art. I.12).
     */
    @Query(
        """
        UPDATE exemplar
        SET variant_id = :variantId,
            updated_at = :updatedAt,
            row_version = row_version + 1
        WHERE id = :id AND row_version = :expectedVersion
        """,
    )
    public suspend fun setVariant(
        id: String,
        variantId: String,
        updatedAt: Long,
        expectedVersion: Long,
    ): Int

    @Query("UPDATE exemplar SET status = :status, updated_at = :at WHERE id = :id")
    public suspend fun setStatus(id: String, status: String, at: Long)

    /** Só chega aqui o exemplo sem nenhum vínculo: os demais são bloqueados antes (FR-028). */
    @Query("DELETE FROM exemplar WHERE id = :id")
    public suspend fun deleteById(id: String)

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

    /** Foto de capa de cada exemplar da variante: a principal e, nela, a de menor ordem. */
    @Query(
        """
        SELECT p.exemplar_id AS exemplar_id,
               p.path AS path
        FROM photo p
        JOIN exemplar e ON e.id = p.exemplar_id
        WHERE e.variant_id = :variantId
          AND p.id = (SELECT p2.id
                      FROM photo p2
                      WHERE p2.exemplar_id = p.exemplar_id
                      ORDER BY (1 - p2.is_primary), p2.sort_order, p2.captured_at
                      LIMIT 1)
        """,
    )
    public fun observePrimaryPathsByVariant(variantId: String): Flow<List<ExemplarPhotoRow>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: PhotoEntity)

    @Query("UPDATE photo SET is_primary = 0 WHERE exemplar_id = :exemplarId")
    public suspend fun clearPrimary(exemplarId: String)

    @Query("UPDATE photo SET is_primary = 1 WHERE id = :photoId")
    public suspend fun markPrimary(photoId: String)

    @Query("SELECT * FROM photo WHERE id = :id LIMIT 1")
    public suspend fun findById(id: String): PhotoEntity?

    @Query("SELECT COUNT(*) FROM photo WHERE exemplar_id = :exemplarId")
    public suspend fun countByExemplar(exemplarId: String): Int

    /**
     * Torna a derivada o arquivo exibido e preserva o original (FR-214–FR-217).
     * Categoria, ordem, principal e vínculo não entram no UPDATE de propósito (FR-218).
     */
    @Query(
        """
        UPDATE photo SET
            path = :path,
            checksum = :checksum,
            width = :width,
            height = :height,
            bytes = :bytes,
            original_path = :originalPath,
            edit_params = :editParams,
            edited_at = :editedAt
        WHERE id = :photoId
        """,
    )
    public suspend fun updateEdited(
        photoId: String,
        path: String,
        checksum: String,
        width: Int?,
        height: Int?,
        bytes: Long?,
        originalPath: String,
        editParams: String,
        editedAt: Long,
    )

    /** Restauração do original: o arquivo exibido volta a ser o original (FR-216). */
    @Query(
        """
        UPDATE photo SET
            path = :path,
            checksum = :checksum,
            width = :width,
            height = :height,
            bytes = :bytes,
            original_path = NULL,
            edit_params = NULL,
            edited_at = NULL
        WHERE id = :photoId
        """,
    )
    public suspend fun updateRestored(
        photoId: String,
        path: String,
        checksum: String,
        width: Int?,
        height: Int?,
        bytes: Long?,
    )

    @Query("DELETE FROM photo WHERE id = :photoId")
    public suspend fun delete(photoId: String)
}
