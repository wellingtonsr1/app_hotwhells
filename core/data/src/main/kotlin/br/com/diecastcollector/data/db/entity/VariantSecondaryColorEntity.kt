package br.com.diecastcollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Cores secundárias da variante (FR-017). PK composta, sem id próprio. */
@Entity(
    tableName = "variant_secondary_color",
    primaryKeys = ["variant_id", "color_id"],
    foreignKeys = [
        ForeignKey(
            entity = VariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["variant_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ColorEntity::class,
            parentColumns = ["id"],
            childColumns = ["color_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["color_id"])],
)
public data class VariantSecondaryColorEntity(
    @ColumnInfo(name = "variant_id")
    val variantId: String,
    @ColumnInfo(name = "color_id")
    val colorId: String,
)
