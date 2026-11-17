package br.com.hotwheelscollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabela `variant` (Art. I.1–I.3, FR-035, FR-039–FR-042).
 *
 * `fingerprint` é o hash estável dos atributos distintivos: é o que permite FR-040
 * (não duplicar variante) sem proibir variantes legítimas que só diferem em rodas ou base.
 *
 * O índice único **parcial** (`WHERE archived_at IS NULL`) não é expressável em
 * `@Index`; ele é criado no callback de abertura do banco
 * (`AppDatabase.CALLBACK`), porque arquivar uma variante precisa liberar o fingerprint.
 */
@Entity(
    tableName = "variant",
    foreignKeys = [
        ForeignKey(
            entity = CarModelEntity::class,
            parentColumns = ["id"],
            childColumns = ["model_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = SeriesEntity::class,
            parentColumns = ["id"],
            childColumns = ["series_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ColorEntity::class,
            parentColumns = ["id"],
            childColumns = ["primary_color_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["fingerprint"]),
        Index(value = ["model_id"]),
        Index(value = ["series_id"]),
        Index(value = ["primary_color_id"]),
        Index(value = ["manufacturer_code"]),
    ],
)
public data class VariantEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "model_id")
    val modelId: String,
    @ColumnInfo(name = "fingerprint")
    val fingerprint: String,
    @ColumnInfo(name = "series_id")
    val seriesId: String?,
    @ColumnInfo(name = "primary_color_id")
    val primaryColorId: String?,
    @ColumnInfo(name = "wheels_type")
    val wheelsType: String?,
    @ColumnInfo(name = "base_type")
    val baseType: String?,
    @ColumnInfo(name = "decoration")
    val decoration: String?,
    @ColumnInfo(name = "release_year")
    val releaseYear: Int?,
    @ColumnInfo(name = "collection_number")
    val collectionNumber: String?,
    @ColumnInfo(name = "series_number")
    val seriesNumber: String?,
    @ColumnInfo(name = "edition")
    val edition: String?,
    @ColumnInfo(name = "manufacturer_code")
    val manufacturerCode: String?,
    @ColumnInfo(name = "packaging_type")
    val packagingType: String?,
    @ColumnInfo(name = "archived_at")
    val archivedAt: Long?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
