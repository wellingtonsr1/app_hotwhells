package br.com.hotwheelscollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabela `series` (FR-048–FR-049).
 * Hierarquia livre por `parent_id`: nenhuma taxonomia do fabricante é presumida (Art. VI.2).
 */
@Entity(
    tableName = "series",
    foreignKeys = [
        ForeignKey(
            entity = ManufacturerEntity::class,
            parentColumns = ["id"],
            childColumns = ["manufacturer_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = SeriesEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["manufacturer_id"]),
        Index(value = ["parent_id"]),
    ],
)
public data class SeriesEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "manufacturer_id")
    val manufacturerId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "parent_id")
    val parentId: String?,
    @ColumnInfo(name = "kind")
    val kind: String?,
    @ColumnInfo(name = "year")
    val year: Int?,
    @ColumnInfo(name = "archived_at")
    val archivedAt: Long?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
