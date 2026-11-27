package br.com.diecastcollector.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.diecastcollector.data.db.entity.CollectionEntity
import br.com.diecastcollector.data.db.entity.CollectionItemEntity
import br.com.diecastcollector.data.db.entity.NegotiationEntity
import br.com.diecastcollector.data.db.entity.NegotiationEventEntity
import br.com.diecastcollector.data.db.entity.WishlistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
public interface CollectionDao {

    @Query("SELECT * FROM collection WHERE archived_at IS NULL ORDER BY name COLLATE NOCASE")
    public fun observeActive(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collection WHERE id = :id")
    public suspend fun findById(id: String): CollectionEntity?

    @Query("SELECT COUNT(*) FROM collection WHERE parent_id = :parentId AND archived_at IS NULL")
    public suspend fun countActiveChildren(parentId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: CollectionEntity)

    @Update
    public suspend fun update(entity: CollectionEntity)
}

/**
 * Item de coleção com o rótulo resolvido e a contagem de exemplares possuídos.
 *
 * `owned` soma os exemplares da variante apontada e, quando o item é de modelo, os
 * exemplares de todas as variantes daquele modelo — sempre `status = 'OWNED'`, porque
 * vendido/arquivado continua no banco mas não conta como possuído (FR-044).
 */
public data class CollectionItemRow(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "collection_id") val collectionId: String,
    @ColumnInfo(name = "variant_id") val variantId: String?,
    @ColumnInfo(name = "model_id") val modelId: String?,
    @ColumnInfo(name = "expected") val expected: Int,
    @ColumnInfo(name = "non_collectible") val nonCollectible: Int,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "owned") val owned: Int,
    @ColumnInfo(name = "photo_path") val photoPath: String?,
)

@Dao
public interface CollectionItemDao {

    @Query("SELECT * FROM collection_item WHERE collection_id = :collectionId")
    public fun observeByCollection(collectionId: String): Flow<List<CollectionItemEntity>>

    @Query("SELECT * FROM collection_item WHERE collection_id = :collectionId AND variant_id = :variantId LIMIT 1")
    public suspend fun findByVariant(collectionId: String, variantId: String): CollectionItemEntity?

    @Query("SELECT * FROM collection_item WHERE collection_id = :collectionId AND model_id = :modelId LIMIT 1")
    public suspend fun findByModel(collectionId: String, modelId: String): CollectionItemEntity?

    @Query("SELECT * FROM collection_item WHERE id = :id")
    public suspend fun findById(id: String): CollectionItemEntity?

    /**
     * Todos os itens ativos, com rótulo e possuídos. Uma consulta só: a lista de coleções
     * agrupa em memória, em vez de disparar uma consulta por coleção.
     */
    @Query(
        """
        SELECT i.id AS id,
               i.collection_id AS collection_id,
               i.variant_id AS variant_id,
               i.model_id AS model_id,
               i.expected AS expected,
               i.non_collectible AS non_collectible,
               COALESCE(
                   mf.name || ' · ' || cm.name || COALESCE(' · ' || c.name, ''),
                   cm.name,
                   ''
               ) AS label,
               (SELECT COUNT(*) FROM exemplar e
                WHERE e.status = 'OWNED' AND e.variant_id = i.variant_id)
             + (SELECT COUNT(*) FROM exemplar e
                JOIN variant v ON v.id = e.variant_id
                WHERE e.status = 'OWNED' AND v.model_id = i.model_id) AS owned,
               (SELECT p.path
                FROM photo p
                JOIN exemplar e ON e.id = p.exemplar_id
                WHERE e.status = 'OWNED'
                  AND (e.variant_id = i.variant_id
                       OR e.variant_id IN (SELECT vx.id FROM variant vx
                                           WHERE vx.model_id = i.model_id))
                ORDER BY (1 - p.is_primary), p.sort_order, p.captured_at
                LIMIT 1) AS photo_path
        FROM collection_item i
        JOIN collection col ON col.id = i.collection_id AND col.archived_at IS NULL
        LEFT JOIN variant v ON v.id = i.variant_id
        LEFT JOIN car_model cm ON cm.id = COALESCE(i.model_id, v.model_id)
        LEFT JOIN manufacturer mf ON mf.id = cm.manufacturer_id
        LEFT JOIN color c ON c.id = v.primary_color_id
        ORDER BY label COLLATE NOCASE
        """,
    )
    public fun observeAllWithOwned(): Flow<List<CollectionItemRow>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: CollectionItemEntity)

    @Update
    public suspend fun update(entity: CollectionItemEntity)

    @Query("UPDATE collection_item SET non_collectible = :flag WHERE id = :id")
    public suspend fun setNonCollectible(id: String, flag: Int)

    @Query("DELETE FROM collection_item WHERE id = :id")
    public suspend fun deleteById(id: String)
}

/** Linha da lista de desejos com o título resolvido pelos vínculos (ou texto livre). */
public data class WishlistRow(
    @Embedded public val item: WishlistItemEntity,
    @ColumnInfo(name = "title") public val title: String?,
    /** Foto a exibir: a do desejo ou, na falta dela, a de um exemplar do mesmo alvo. */
    @ColumnInfo(name = "display_photo_path") public val photoPath: String?,
)

@Dao
public interface WishlistDao {

    @Query("SELECT * FROM wishlist_item WHERE status = 'WANTED' ORDER BY priority, created_at")
    public fun observeWanted(): Flow<List<WishlistItemEntity>>

    /**
     * Lista completa: o que está em aberto primeiro, depois o histórico, sempre por
     * prioridade e data de criação. O título prefere o texto livre; sem ele, monta
     * «Fabricante · Modelo» a partir da variante ou do modelo do desejo.
     *
     * A foto é a que a pessoa anexou ao desejo; só quando não existe nenhuma ela cai na
     * foto de um exemplar do mesmo alvo, que é o que o acervo já tem para mostrar.
     */
    @Query(
        """
        SELECT w.*,
            CASE
                WHEN TRIM(COALESCE(w.free_text, '')) <> '' THEN TRIM(w.free_text)
                ELSE COALESCE(mf.name || ' · ' || cm.name, cm.name, '')
            END AS title,
            COALESCE(NULLIF(TRIM(COALESCE(w.photo_path, '')), ''),
                (SELECT p.path
                 FROM photo p
                 JOIN exemplar e ON e.id = p.exemplar_id
                 WHERE e.variant_id = w.variant_id
                    OR e.variant_id IN (SELECT vx.id FROM variant vx
                                        WHERE vx.model_id = w.model_id)
                 ORDER BY (1 - p.is_primary), p.sort_order, p.captured_at
                 LIMIT 1)) AS display_photo_path
        FROM wishlist_item w
        LEFT JOIN variant v ON v.id = w.variant_id
        LEFT JOIN car_model cm ON cm.id = COALESCE(w.model_id, v.model_id)
        LEFT JOIN manufacturer mf ON mf.id = cm.manufacturer_id
        ORDER BY CASE w.status WHEN 'WANTED' THEN 0 ELSE 1 END, w.priority, w.created_at
        """,
    )
    public fun observeAllWithTitle(): Flow<List<WishlistRow>>

    @Query("SELECT * FROM wishlist_item WHERE id = :id")
    public suspend fun findById(id: String): WishlistItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: WishlistItemEntity)

    @Update
    public suspend fun update(entity: WishlistItemEntity)
}

@Dao
public interface NegotiationDao {

    @Query("SELECT * FROM negotiation WHERE exemplar_id = :exemplarId ORDER BY started_at DESC")
    public fun observeByExemplar(exemplarId: String): Flow<List<NegotiationEntity>>

    /** Usada na transação que recusa uma segunda negociação ativa (FR-068). */
    @Query("SELECT * FROM negotiation WHERE exemplar_id = :exemplarId AND status IN ('OPEN', 'RESERVED')")
    public suspend fun findActiveForExemplar(exemplarId: String): List<NegotiationEntity>

    /**
     * Negociações ativas de todos os exemplares de uma variante, em uma consulta só
     * (NFR-503): a lista de exemplares mostra **Em negociação** sem consulta por linha.
     */
    @Query(
        """
        SELECT n.* FROM negotiation n
        JOIN exemplar e ON e.id = n.exemplar_id
        WHERE e.variant_id = :variantId AND n.status IN ('OPEN', 'RESERVED')
        """,
    )
    public fun observeActiveByVariant(variantId: String): Flow<List<NegotiationEntity>>

    @Query("SELECT * FROM negotiation WHERE id = :id")
    public suspend fun findById(id: String): NegotiationEntity?

    /** Vínculos que bloqueiam a exclusão do exemplar (FR-028). */
    @Query("SELECT COUNT(*) FROM negotiation WHERE exemplar_id = :exemplarId")
    public suspend fun countByExemplar(exemplarId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: NegotiationEntity)

    @Update
    public suspend fun update(entity: NegotiationEntity)
}

@Dao
public interface NegotiationEventDao {

    @Query("SELECT * FROM negotiation_event WHERE negotiation_id = :negotiationId ORDER BY at")
    public fun observeByNegotiation(negotiationId: String): Flow<List<NegotiationEventEntity>>

    /** Histórico completo do exemplar, de todas as negociações (FR-073). */
    @Query(
        """
        SELECT ne.* FROM negotiation_event ne
        JOIN negotiation n ON n.id = ne.negotiation_id
        WHERE n.exemplar_id = :exemplarId
        ORDER BY ne.at
        """,
    )
    public fun observeByExemplar(exemplarId: String): Flow<List<NegotiationEventEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: NegotiationEventEntity)
}
