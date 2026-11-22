package br.com.diecastcollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabela `location` (FR-088–FR-095).
 * `type` guarda o enum em inglês; o português (Ambiente, Estante, Prateleira...) é só apresentação.
 * A profundidade é livre; ciclo é impedido no domínio, não pelo schema.
 */
@Entity(
    tableName = "location",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["parent_id"])],
)
public data class LocationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "parent_id")
    val parentId: String?,
    @ColumnInfo(name = "archived_at")
    val archivedAt: Long?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
