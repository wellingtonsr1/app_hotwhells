package br.com.diecastcollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabela `exemplar` (Art. I.1, I.12; FR-036–FR-046).
 *
 * `condition` e `status` guardam o nome do enum (SEALED, OWNED, ...), validado no domínio.
 * `row_version` sustenta a detecção de escrita concorrente (Art. I.12).
 */
@Entity(
    tableName = "exemplar",
    foreignKeys = [
        ForeignKey(
            entity = VariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["variant_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["location_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["internal_code"], unique = true),
        Index(value = ["variant_id", "status"]),
        Index(value = ["location_id"]),
    ],
)
public data class ExemplarEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "variant_id")
    val variantId: String,
    @ColumnInfo(name = "internal_code")
    val internalCode: String,
    @ColumnInfo(name = "condition")
    val condition: String,
    @ColumnInfo(name = "packaging_condition")
    val packagingCondition: String?,
    @ColumnInfo(name = "location_id")
    val locationId: String?,
    @ColumnInfo(name = "status", defaultValue = "OWNED")
    val status: String = "OWNED",
    @ColumnInfo(name = "notes")
    val notes: String?,
    @ColumnInfo(name = "row_version", defaultValue = "0")
    val rowVersion: Long = 0L,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
