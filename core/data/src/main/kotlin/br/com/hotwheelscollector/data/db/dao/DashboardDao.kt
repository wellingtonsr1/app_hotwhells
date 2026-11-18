package br.com.hotwheelscollector.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Linha de aquisição ou de cadastro recente exibida no painel. */
public data class DashboardEntryRow(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "manufacturer_name") val manufacturerName: String,
    @ColumnInfo(name = "day") val day: String,
)

/** Progresso de uma coleção ativa. */
public data class CollectionProgressRow(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "owned") val owned: Int,
    @ColumnInfo(name = "known") val known: Int,
)

/**
 * Consultas do painel (FR-001–FR-011).
 *
 * `status = 'OWNED'` em todas: exemplar vendido ou perdido continua no banco (FR-070), mas
 * não faz mais parte da coleção e não pode inflar indicador nem valor.
 */
@Dao
public interface DashboardDao {

    @Query("SELECT COUNT(*) FROM exemplar WHERE status = 'OWNED'")
    public fun observeOwnedExemplars(): Flow<Int>

    @Query(
        """
        SELECT COUNT(DISTINCT v.model_id)
        FROM exemplar e
        JOIN variant v ON v.id = e.variant_id
        WHERE e.status = 'OWNED'
        """,
    )
    public fun observeDistinctModels(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT e.variant_id) FROM exemplar e WHERE e.status = 'OWNED'")
    public fun observeDistinctVariants(): Flow<Int>

    @Query(
        """
        SELECT COUNT(DISTINCT m.manufacturer_id)
        FROM exemplar e
        JOIN variant v ON v.id = e.variant_id
        JOIN car_model m ON m.id = v.model_id
        WHERE e.status = 'OWNED'
        """,
    )
    public fun observeDistinctManufacturers(): Flow<Int>

    @Query(
        """
        SELECT COUNT(DISTINCT v.series_id)
        FROM exemplar e
        JOIN variant v ON v.id = e.variant_id
        WHERE e.status = 'OWNED' AND v.series_id IS NOT NULL
        """,
    )
    public fun observeDistinctSeries(): Flow<Int>

    @Query("SELECT COUNT(*) FROM collection WHERE archived_at IS NULL")
    public fun observeActiveCollections(): Flow<Int>

    /** Variantes com mais de um exemplar possuído — repetição, não erro (FR-039). */
    @Query(
        """
        SELECT COUNT(*) FROM (
            SELECT e.variant_id
            FROM exemplar e
            WHERE e.status = 'OWNED'
            GROUP BY e.variant_id
            HAVING COUNT(*) > 1
        )
        """,
    )
    public fun observeVariantsWithDuplicates(): Flow<Int>

    @Query("SELECT COUNT(*) FROM wishlist_item WHERE status = 'WANTED'")
    public fun observeWantedItems(): Flow<Int>

    /** Exemplares possuídos sem nenhuma avaliação: a base do selo de estimativa (T055). */
    @Query(
        """
        SELECT COUNT(*)
        FROM exemplar e
        WHERE e.status = 'OWNED'
          AND NOT EXISTS (SELECT 1 FROM valuation v WHERE v.exemplar_id = e.id)
        """,
    )
    public fun observeExemplarsWithoutValuation(): Flow<Int>

    /** Data da avaliação mais recente entre os possuídos: o "até quando" do selo (T055). */
    @Query(
        """
        SELECT MAX(v.date)
        FROM valuation v
        JOIN exemplar e ON e.id = v.exemplar_id
        WHERE e.status = 'OWNED'
        """,
    )
    public fun observeLatestValuationDate(): Flow<String?>

    @Query(
        """
        SELECT a.id AS id,
               m.name AS model_name,
               mf.name AS manufacturer_name,
               a.date AS day
        FROM acquisition a
        JOIN exemplar e ON e.id = a.exemplar_id
        JOIN variant v ON v.id = e.variant_id
        JOIN car_model m ON m.id = v.model_id
        JOIN manufacturer mf ON mf.id = m.manufacturer_id
        WHERE e.status = 'OWNED'
        ORDER BY a.date DESC, a.created_at DESC
        LIMIT 5
        """,
    )
    public fun observeLatestAcquisitions(): Flow<List<DashboardEntryRow>>

    @Query(
        """
        SELECT e.id AS id,
               m.name AS model_name,
               mf.name AS manufacturer_name,
               strftime('%Y-%m-%d', e.created_at / 1000, 'unixepoch', 'localtime') AS day
        FROM exemplar e
        JOIN variant v ON v.id = e.variant_id
        JOIN car_model m ON m.id = v.model_id
        JOIN manufacturer mf ON mf.id = m.manufacturer_id
        WHERE e.status = 'OWNED'
        ORDER BY e.created_at DESC
        LIMIT 5
        """,
    )
    public fun observeLatestEntries(): Flow<List<DashboardEntryRow>>

    /**
     * Progresso por coleção ativa (FR-004, FR-056).
     *
     * `known` ignora itens marcados como não colecionáveis; `owned` conta o item quando a
     * variante apontada — ou alguma variante do modelo apontado — tem exemplar possuído.
     */
    @Query(
        """
        SELECT c.id AS id,
               c.name AS name,
               COALESCE(SUM(CASE WHEN i.non_collectible = 0 AND (
                    (i.variant_id IS NOT NULL AND EXISTS (
                        SELECT 1 FROM exemplar e
                        WHERE e.variant_id = i.variant_id AND e.status = 'OWNED'
                    ))
                    OR (i.model_id IS NOT NULL AND EXISTS (
                        SELECT 1 FROM exemplar e
                        JOIN variant v ON v.id = e.variant_id
                        WHERE v.model_id = i.model_id AND e.status = 'OWNED'
                    ))
               ) THEN 1 ELSE 0 END), 0) AS owned,
               COALESCE(SUM(CASE WHEN i.non_collectible = 0 THEN 1 ELSE 0 END), 0) AS known
        FROM collection c
        JOIN collection_item i ON i.collection_id = c.id
        WHERE c.archived_at IS NULL
        GROUP BY c.id, c.name
        """,
    )
    public fun observeCollectionProgress(): Flow<List<CollectionProgressRow>>
}
