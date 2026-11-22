package br.com.diecastcollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Tabela `movement` (FR-091). Registro imutável: nunca é atualizado nem apagado. */
@Entity(
    tableName = "movement",
    foreignKeys = [
        ForeignKey(
            entity = ExemplarEntity::class,
            parentColumns = ["id"],
            childColumns = ["exemplar_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["from_location_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_location_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["exemplar_id", "at"], orders = [Index.Order.ASC, Index.Order.DESC]),
        Index(value = ["from_location_id"]),
        Index(value = ["to_location_id"]),
    ],
)
public data class MovementEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "exemplar_id")
    val exemplarId: String,
    @ColumnInfo(name = "at")
    val at: Long,
    @ColumnInfo(name = "from_location_id")
    val fromLocationId: String?,
    @ColumnInfo(name = "to_location_id")
    val toLocationId: String?,
    @ColumnInfo(name = "note")
    val note: String?,
)
