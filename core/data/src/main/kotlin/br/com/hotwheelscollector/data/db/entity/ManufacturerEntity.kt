package br.com.hotwheelscollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Tabela `manufacturer` (FR-047). `country` nulo significa desconhecido (Art. I.9). */
@Entity(
    tableName = "manufacturer",
    indices = [Index(value = ["name"], unique = true)],
)
public data class ManufacturerEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "country")
    val country: String?,
    @ColumnInfo(name = "archived_at")
    val archivedAt: Long?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
