package br.com.diecastcollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabela `collection_item` (FR-051–FR-055).
 *
 * A exclusividade (`variant_id` XOR `model_id`) e a unicidade por coleção são
 * regras de domínio: o SQLite só as expressaria com índice parcial, que o `@Index`
 * do Room não representa. A verificação acontece na transação do repositório.
 */
@Entity(
    tableName = "collection_item",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = VariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["variant_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CarModelEntity::class,
            parentColumns = ["id"],
            childColumns = ["model_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["collection_id", "variant_id"]),
        Index(value = ["collection_id", "model_id"]),
        Index(value = ["variant_id"]),
        Index(value = ["model_id"]),
    ],
)
public data class CollectionItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "collection_id")
    val collectionId: String,
    @ColumnInfo(name = "variant_id")
    val variantId: String?,
    @ColumnInfo(name = "model_id")
    val modelId: String?,
    @ColumnInfo(name = "expected", defaultValue = "1")
    val expected: Int = 1,
    @ColumnInfo(name = "non_collectible", defaultValue = "0")
    val nonCollectible: Int = 0,
    @ColumnInfo(name = "notes")
    val notes: String?,
)
