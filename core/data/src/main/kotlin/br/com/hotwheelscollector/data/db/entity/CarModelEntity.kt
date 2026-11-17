package br.com.hotwheelscollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabela `car_model` (Art. I.1–I.2).
 * Único por (`manufacturer_id`, `name`, `scale`); `scale` nasce `'1:64'` (FR-016).
 */
@Entity(
    tableName = "car_model",
    foreignKeys = [
        ForeignKey(
            entity = ManufacturerEntity::class,
            parentColumns = ["id"],
            childColumns = ["manufacturer_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = BrandEntity::class,
            parentColumns = ["id"],
            childColumns = ["brand_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = RealVehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["real_vehicle_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["manufacturer_id", "name", "scale"], unique = true),
        Index(value = ["brand_id"]),
        Index(value = ["real_vehicle_id"]),
    ],
)
public data class CarModelEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "manufacturer_id")
    val manufacturerId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "scale", defaultValue = "1:64")
    val scale: String = "1:64",
    @ColumnInfo(name = "brand_id")
    val brandId: String?,
    @ColumnInfo(name = "real_vehicle_id")
    val realVehicleId: String?,
    @ColumnInfo(name = "body_type")
    val bodyType: String?,
    @ColumnInfo(name = "material")
    val material: String?,
    @ColumnInfo(name = "country_of_manufacture")
    val countryOfManufacture: String?,
    @ColumnInfo(name = "notes")
    val notes: String?,
    @ColumnInfo(name = "archived_at")
    val archivedAt: Long?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
