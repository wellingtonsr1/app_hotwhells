package br.com.hotwheelscollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabela `photo` (FR-020–FR-021, R.2–R.3).
 * O arquivo mora em `filesDir/images/<exemplarId>/`; aqui ficam caminho relativo e checksum.
 */
@Entity(
    tableName = "photo",
    foreignKeys = [
        ForeignKey(
            entity = ExemplarEntity::class,
            parentColumns = ["id"],
            childColumns = ["exemplar_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["exemplar_id", "angle", "sort_order"])],
)
public data class PhotoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "exemplar_id")
    val exemplarId: String,
    @ColumnInfo(name = "angle")
    val angle: String,
    @ColumnInfo(name = "path")
    val path: String,
    @ColumnInfo(name = "checksum")
    val checksum: String,
    @ColumnInfo(name = "is_primary", defaultValue = "0")
    val isPrimary: Boolean = false,
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "width")
    val width: Int?,
    @ColumnInfo(name = "height")
    val height: Int?,
    @ColumnInfo(name = "bytes")
    val bytes: Long?,
    @ColumnInfo(name = "captured_at")
    val capturedAt: Long?,
)
