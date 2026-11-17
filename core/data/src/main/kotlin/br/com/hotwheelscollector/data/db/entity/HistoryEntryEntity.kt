package br.com.hotwheelscollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabela `history_entry` (Art. I.8, FR-027): appende-only.
 * Sem FK física de propósito — a trilha é polimórfica por (`entity_type`, `entity_id`)
 * e precisa sobreviver ao arquivamento da entidade auditada.
 */
@Entity(
    tableName = "history_entry",
    indices = [
        Index(
            value = ["entity_type", "entity_id", "at"],
            orders = [Index.Order.ASC, Index.Order.ASC, Index.Order.DESC],
        ),
    ],
)
public data class HistoryEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    @ColumnInfo(name = "entity_id")
    val entityId: String,
    @ColumnInfo(name = "field")
    val field: String,
    @ColumnInfo(name = "old_value")
    val oldValue: String?,
    @ColumnInfo(name = "new_value")
    val newValue: String?,
    @ColumnInfo(name = "at")
    val at: Long,
    @ColumnInfo(name = "source")
    val source: String?,
)
