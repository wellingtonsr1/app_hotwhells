package br.com.diecastcollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabela `recognition_result` (FR-102–FR-107).
 * Sugestão de código de barras/OCR/foto: só entra no catálogo com aceite explícito.
 */
@Entity(
    tableName = "recognition_result",
    foreignKeys = [
        ForeignKey(
            entity = ExemplarEntity::class,
            parentColumns = ["id"],
            childColumns = ["exemplar_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["exemplar_id"]), Index(value = ["draft_id"])],
)
public data class RecognitionResultEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "exemplar_id")
    val exemplarId: String?,
    @ColumnInfo(name = "draft_id")
    val draftId: String?,
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "provider")
    val provider: String,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "confidence")
    val confidence: Double?,
    @ColumnInfo(name = "applied_fields")
    val appliedFields: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
