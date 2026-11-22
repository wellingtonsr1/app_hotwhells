package br.com.diecastcollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabela `duplicate_offer` (FR-066–FR-067).
 *
 * Uma linha por exemplar: a disponibilidade é uma decisão atual, não um histórico.
 * `ON DELETE CASCADE` é intencional — apagar o exemplar (só possível sem vínculos)
 * leva junto a marcação de oferta, que não é trilha de auditoria.
 */
@Entity(
    tableName = "duplicate_offer",
    foreignKeys = [
        ForeignKey(
            entity = ExemplarEntity::class,
            parentColumns = ["id"],
            childColumns = ["exemplar_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["exemplar_id"], unique = true)],
)
public data class DuplicateOfferEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "exemplar_id")
    val exemplarId: String,
    /** `TRADE`, `SALE`, `BOTH` ou `UNAVAILABLE`. */
    @ColumnInfo(name = "availability")
    val availability: String,
    @ColumnInfo(name = "condition")
    val condition: String?,
    @ColumnInfo(name = "asking_price_cents")
    val askingPriceCents: Long?,
    @ColumnInfo(name = "notes")
    val notes: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
