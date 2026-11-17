package br.com.hotwheelscollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Tabela `color` (FR-017): nome único; `hex` é apoio visual opcional. */
@Entity(
    tableName = "color",
    indices = [Index(value = ["name"], unique = true)],
)
public data class ColorEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "hex")
    val hex: String?,
)
