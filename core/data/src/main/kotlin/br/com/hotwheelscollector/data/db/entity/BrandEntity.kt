package br.com.hotwheelscollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Tabela `brand` (FR-014). Nome único dentro do fabricante. */
@Entity(
    tableName = "brand",
    foreignKeys = [
        ForeignKey(
            entity = ManufacturerEntity::class,
            parentColumns = ["id"],
            childColumns = ["manufacturer_id"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["manufacturer_id", "name"], unique = true),
        Index(value = ["manufacturer_id"]),
    ],
)
public data class BrandEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "manufacturer_id")
    val manufacturerId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "archived_at")
    val archivedAt: Long?,
)
