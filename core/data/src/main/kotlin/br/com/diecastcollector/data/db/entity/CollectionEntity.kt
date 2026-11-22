package br.com.diecastcollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabela `collection` (FR-049–FR-056).
 * `manufacturer_id` nulo = coleção multi-fabricante.
 */
@Entity(
    tableName = "collection",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ManufacturerEntity::class,
            parentColumns = ["id"],
            childColumns = ["manufacturer_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["parent_id"]),
        Index(value = ["manufacturer_id"]),
    ],
)
public data class CollectionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "parent_id")
    val parentId: String?,
    @ColumnInfo(name = "manufacturer_id")
    val manufacturerId: String?,
    @ColumnInfo(name = "archived_at")
    val archivedAt: Long?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
