package br.com.hotwheelscollector.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.hotwheelscollector.data.db.entity.AcquisitionEntity
import br.com.hotwheelscollector.data.db.entity.LocationEntity
import br.com.hotwheelscollector.data.db.entity.MovementEntity
import br.com.hotwheelscollector.data.db.entity.ValuationEntity
import kotlinx.coroutines.flow.Flow

/** Quantos exemplares possuídos estão em cada localização (para a árvore). */
public data class LocationCountRow(
    @ColumnInfo(name = "location_id") val locationId: String,
    @ColumnInfo(name = "total") val total: Int,
)

@Dao
public interface LocationDao {

    @Query("SELECT * FROM location WHERE archived_at IS NULL ORDER BY type, name COLLATE NOCASE")
    public fun observeActive(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM location WHERE parent_id = :parentId AND archived_at IS NULL")
    public suspend fun findChildren(parentId: String): List<LocationEntity>

    @Query("SELECT * FROM location WHERE id = :id")
    public suspend fun findById(id: String): LocationEntity?

    @Query("SELECT COUNT(*) FROM exemplar WHERE location_id = :locationId")
    public suspend fun countExemplars(locationId: String): Int

    /** Contagem por localização em uma consulta; a árvore monta a soma em memória. */
    @Query(
        """
        SELECT location_id AS location_id, COUNT(*) AS total
        FROM exemplar
        WHERE status = 'OWNED' AND location_id IS NOT NULL
        GROUP BY location_id
        """,
    )
    public fun observeOwnedCounts(): Flow<List<LocationCountRow>>

    /** Todos os exemplares da localização, de qualquer status: a FK não distingue. */
    @Query("SELECT id FROM exemplar WHERE location_id = :locationId")
    public suspend fun findExemplarIdsAt(locationId: String): List<String>

    /**
     * Inventário da localização incluindo as sublocalizações (FR-094–FR-095).
     * A CTE recursiva desce a hierarquia no próprio SQLite, sem uma consulta por nível.
     */
    @Query(
        """
        WITH RECURSIVE subtree(id) AS (
            SELECT id FROM location WHERE id = :locationId
            UNION ALL
            SELECT l.id FROM location l JOIN subtree s ON l.parent_id = s.id
        )
        SELECT e.id AS exemplar_id,
               e.variant_id AS variant_id,
               m.name AS model_name,
               mf.name AS manufacturer_name,
               e.internal_code AS internal_code,
               e.condition AS condition,
               loc.name AS location_name
        FROM exemplar e
        JOIN variant v ON v.id = e.variant_id
        JOIN car_model m ON m.id = v.model_id
        JOIN manufacturer mf ON mf.id = m.manufacturer_id
        LEFT JOIN location loc ON loc.id = e.location_id
        WHERE e.status = 'OWNED' AND e.location_id IN (SELECT id FROM subtree)
        ORDER BY mf.name COLLATE NOCASE, m.name COLLATE NOCASE, e.internal_code
        """,
    )
    public fun observeContents(locationId: String): Flow<List<InventoryRow>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: LocationEntity)

    @Update
    public suspend fun update(entity: LocationEntity)

    @Query("UPDATE location SET archived_at = :at, updated_at = :at WHERE id = :id")
    public suspend fun archive(id: String, at: Long)
}

/** Linha do inventário por localização, com o título resolvido na consulta. */
public data class InventoryRow(
    @ColumnInfo(name = "exemplar_id") val exemplarId: String,
    @ColumnInfo(name = "variant_id") val variantId: String,
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "manufacturer_name") val manufacturerName: String,
    @ColumnInfo(name = "internal_code") val internalCode: String,
    @ColumnInfo(name = "condition") val condition: String,
    @ColumnInfo(name = "location_name") val locationName: String?,
)

@Dao
public interface MovementDao {

    @Query("SELECT * FROM movement WHERE exemplar_id = :exemplarId ORDER BY at DESC")
    public fun observeByExemplar(exemplarId: String): Flow<List<MovementEntity>>

    /** Vínculos que bloqueiam a exclusão do exemplar (FR-028). */
    @Query("SELECT COUNT(*) FROM movement WHERE exemplar_id = :exemplarId")
    public suspend fun countByExemplar(exemplarId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: MovementEntity)
}

@Dao
public interface AcquisitionDao {

    @Query("SELECT * FROM acquisition WHERE exemplar_id = :exemplarId LIMIT 1")
    public suspend fun findByExemplar(exemplarId: String): AcquisitionEntity?

    /** Vínculos que bloqueiam a exclusão do exemplar (FR-028). */
    @Query("SELECT COUNT(*) FROM acquisition WHERE exemplar_id = :exemplarId")
    public suspend fun countByExemplar(exemplarId: String): Int

    /** Soma investida dos exemplares possuídos (FR-007, FR-077). Nada disso é coluna. */
    @Query(
        """
        SELECT COALESCE(SUM(
            COALESCE(a.price_cents, 0) + COALESCE(a.shipping_cents, 0) - COALESCE(a.discount_cents, 0)
        ), 0)
        FROM acquisition a
        JOIN exemplar e ON e.id = a.exemplar_id
        WHERE e.status = 'OWNED'
        """,
    )
    public fun observeTotalInvestedCents(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: AcquisitionEntity)

    @Update
    public suspend fun update(entity: AcquisitionEntity)
}

@Dao
public interface ValuationDao {

    /** Histórico completo, mais recente primeiro (FR-083). */
    @Query("SELECT * FROM valuation WHERE exemplar_id = :exemplarId ORDER BY date DESC, created_at DESC")
    public fun observeByExemplar(exemplarId: String): Flow<List<ValuationEntity>>

    @Query("SELECT * FROM valuation WHERE exemplar_id = :exemplarId ORDER BY date DESC, created_at DESC LIMIT 1")
    public suspend fun findLatest(exemplarId: String): ValuationEntity?

    /** Vínculos que bloqueiam a exclusão do exemplar (FR-028). */
    @Query("SELECT COUNT(*) FROM valuation WHERE exemplar_id = :exemplarId")
    public suspend fun countByExemplar(exemplarId: String): Int

    @Query(
        """
        SELECT COALESCE(SUM(v.value_cents), 0)
        FROM valuation v
        JOIN exemplar e ON e.id = v.exemplar_id
        WHERE e.status = 'OWNED'
          AND v.id = (
            SELECT v2.id FROM valuation v2
            WHERE v2.exemplar_id = v.exemplar_id
            ORDER BY v2.date DESC, v2.created_at DESC
            LIMIT 1
          )
        """,
    )
    public fun observeCurrentEstimatedCents(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: ValuationEntity)
}
