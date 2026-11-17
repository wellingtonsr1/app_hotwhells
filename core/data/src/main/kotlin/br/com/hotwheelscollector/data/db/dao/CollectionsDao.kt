package br.com.hotwheelscollector.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.hotwheelscollector.data.db.entity.CollectionEntity
import br.com.hotwheelscollector.data.db.entity.CollectionItemEntity
import br.com.hotwheelscollector.data.db.entity.NegotiationEntity
import br.com.hotwheelscollector.data.db.entity.NegotiationEventEntity
import br.com.hotwheelscollector.data.db.entity.WishlistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
public interface CollectionDao {

    @Query("SELECT * FROM collection WHERE archived_at IS NULL ORDER BY name COLLATE NOCASE")
    public fun observeActive(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collection WHERE id = :id")
    public suspend fun findById(id: String): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: CollectionEntity)

    @Update
    public suspend fun update(entity: CollectionEntity)
}

@Dao
public interface CollectionItemDao {

    @Query("SELECT * FROM collection_item WHERE collection_id = :collectionId")
    public fun observeByCollection(collectionId: String): Flow<List<CollectionItemEntity>>

    @Query("SELECT * FROM collection_item WHERE collection_id = :collectionId AND variant_id = :variantId LIMIT 1")
    public suspend fun findByVariant(collectionId: String, variantId: String): CollectionItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: CollectionItemEntity)

    @Update
    public suspend fun update(entity: CollectionItemEntity)
}

@Dao
public interface WishlistDao {

    @Query("SELECT * FROM wishlist_item WHERE status = 'WANTED' ORDER BY priority, created_at")
    public fun observeWanted(): Flow<List<WishlistItemEntity>>

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

    @Query("SELECT * FROM negotiation WHERE id = :id")
    public suspend fun findById(id: String): NegotiationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: NegotiationEntity)

    @Update
    public suspend fun update(entity: NegotiationEntity)
}

@Dao
public interface NegotiationEventDao {

    @Query("SELECT * FROM negotiation_event WHERE negotiation_id = :negotiationId ORDER BY at")
    public fun observeByNegotiation(negotiationId: String): Flow<List<NegotiationEventEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: NegotiationEventEntity)
}
