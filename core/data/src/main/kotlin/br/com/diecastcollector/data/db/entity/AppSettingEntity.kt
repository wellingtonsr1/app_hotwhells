package br.com.diecastcollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tabela `app_setting` (FR-145, FR-171): preferência em chave/valor. */
@Entity(tableName = "app_setting")
public data class AppSettingEntity(
    @PrimaryKey
    @ColumnInfo(name = "key")
    val key: String,
    @ColumnInfo(name = "value")
    val value: String,
)
