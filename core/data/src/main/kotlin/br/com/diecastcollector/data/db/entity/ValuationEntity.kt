package br.com.diecastcollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabela `valuation` (FR-082–FR-086).
 * "Valor atual" é a primeira linha do índice; o histórico completo nunca é sobrescrito.
 */
@Entity(
    tableName = "valuation",
    foreignKeys = [
        ForeignKey(
            entity = ExemplarEntity::class,
            parentColumns = ["id"],
            childColumns = ["exemplar_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(
            value = ["exemplar_id", "date", "created_at"],
            orders = [Index.Order.ASC, Index.Order.DESC, Index.Order.DESC],
        ),
    ],
)
public data class ValuationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "exemplar_id")
    val exemplarId: String,
    @ColumnInfo(name = "value_cents")
    val valueCents: Long,
    @ColumnInfo(name = "currency", defaultValue = "BRL")
    val currency: String = "BRL",
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "source")
    val source: String?,
    @ColumnInfo(name = "notes")
    val notes: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
