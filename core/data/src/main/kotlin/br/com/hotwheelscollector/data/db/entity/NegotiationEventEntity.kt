package br.com.hotwheelscollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Tabela `negotiation_event` (FR-069, FR-073): appende-only, nunca apagado. */
@Entity(
    tableName = "negotiation_event",
    foreignKeys = [
        ForeignKey(
            entity = NegotiationEntity::class,
            parentColumns = ["id"],
            childColumns = ["negotiation_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["negotiation_id", "at"], orders = [Index.Order.ASC, Index.Order.ASC])],
)
public data class NegotiationEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "negotiation_id")
    val negotiationId: String,
    @ColumnInfo(name = "at")
    val at: Long,
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long?,
)
