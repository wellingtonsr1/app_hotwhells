package br.com.diecastcollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabela `wishlist_item` (FR-057–FR-064).
 * Desejo pode apontar para modelo, variante ou apenas texto livre — mas nunca para nada.
 */
@Entity(
    tableName = "wishlist_item",
    foreignKeys = [
        ForeignKey(
            entity = CarModelEntity::class,
            parentColumns = ["id"],
            childColumns = ["model_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = VariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["variant_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ExemplarEntity::class,
            parentColumns = ["id"],
            childColumns = ["converted_exemplar_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["model_id"]),
        Index(value = ["variant_id"]),
        Index(value = ["converted_exemplar_id"]),
        Index(value = ["status", "priority"]),
    ],
)
public data class WishlistItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "model_id")
    val modelId: String?,
    @ColumnInfo(name = "variant_id")
    val variantId: String?,
    @ColumnInfo(name = "free_text")
    val freeText: String?,
    @ColumnInfo(name = "priority", defaultValue = "2")
    val priority: Int = 2,
    @ColumnInfo(name = "max_price_cents")
    val maxPriceCents: Long?,
    @ColumnInfo(name = "found_price_cents")
    val foundPriceCents: Long?,
    @ColumnInfo(name = "store")
    val store: String?,
    @ColumnInfo(name = "reference_url")
    val referenceUrl: String?,
    @ColumnInfo(name = "notes")
    val notes: String?,
    /** Foto própria do desejo: o exemplar desejado ainda não existe no acervo. */
    @ColumnInfo(name = "photo_path")
    val photoPath: String? = null,
    @ColumnInfo(name = "status", defaultValue = "WANTED")
    val status: String = "WANTED",
    @ColumnInfo(name = "converted_exemplar_id")
    val convertedExemplarId: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
