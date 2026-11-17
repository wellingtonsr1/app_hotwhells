package br.com.hotwheelscollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tabela `real_vehicle` (FR-014): o carro do mundo real, reutilizável entre modelos. */
@Entity(tableName = "real_vehicle")
public data class RealVehicleEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "make")
    val make: String,
    @ColumnInfo(name = "model")
    val model: String,
    @ColumnInfo(name = "year")
    val year: Int?,
    @ColumnInfo(name = "country")
    val country: String?,
)
