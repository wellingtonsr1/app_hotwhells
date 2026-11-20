package br.com.hotwheelscollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabela `negotiation` (FR-068–FR-073).
 *
 * "Um exemplar não pode ter duas negociações OPEN/RESERVED" (FR-068) precisa de
 * índice parcial único, que o Room não expressa: a regra é verificada na transação
 * de abertura da negociação.
 */
@Entity(
    tableName = "negotiation",
    foreignKeys = [
        ForeignKey(
            entity = ExemplarEntity::class,
            parentColumns = ["id"],
            childColumns = ["exemplar_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ExemplarEntity::class,
            parentColumns = ["id"],
            childColumns = ["received_exemplar_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["exemplar_id", "status"]),
        Index(value = ["status"]),
        Index(value = ["received_exemplar_id"]),
    ],
)
public data class NegotiationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "exemplar_id")
    val exemplarId: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "counterparty")
    val counterparty: String?,
    @ColumnInfo(name = "channel")
    val channel: String?,
    @ColumnInfo(name = "status", defaultValue = "OPEN")
    val status: String = "OPEN",
    @ColumnInfo(name = "started_at")
    val startedAt: Long,
    @ColumnInfo(name = "closed_at")
    val closedAt: Long?,
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long?,
    @ColumnInfo(name = "currency")
    val currency: String?,
    @ColumnInfo(name = "notes")
    val notes: String?,
    /** Exemplar recebido na troca (FR-071); nulo em venda ou troca sem item cadastrado. */
    @ColumnInfo(name = "received_exemplar_id")
    val receivedExemplarId: String? = null,
)
