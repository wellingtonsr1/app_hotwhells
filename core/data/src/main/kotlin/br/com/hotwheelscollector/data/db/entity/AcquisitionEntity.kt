package br.com.hotwheelscollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabela `acquisition` (FR-074–FR-076): 1:1 com o exemplar.
 * Todos os valores em centavos; nulos significam "não informado", nunca zero (Art. I.9).
 * O total (`price + shipping − discount`) é sempre calculado, nunca armazenado.
 */
@Entity(
    tableName = "acquisition",
    foreignKeys = [
        ForeignKey(
            entity = ExemplarEntity::class,
            parentColumns = ["id"],
            childColumns = ["exemplar_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["exemplar_id"], unique = true)],
)
public data class AcquisitionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "exemplar_id")
    val exemplarId: String,
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "price_cents")
    val priceCents: Long?,
    @ColumnInfo(name = "shipping_cents")
    val shippingCents: Long?,
    @ColumnInfo(name = "discount_cents")
    val discountCents: Long?,
    @ColumnInfo(name = "currency", defaultValue = "BRL")
    val currency: String = "BRL",
    @ColumnInfo(name = "store")
    val store: String?,
    @ColumnInfo(name = "seller")
    val seller: String?,
    @ColumnInfo(name = "method")
    val method: String?,
    @ColumnInfo(name = "notes")
    val notes: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
