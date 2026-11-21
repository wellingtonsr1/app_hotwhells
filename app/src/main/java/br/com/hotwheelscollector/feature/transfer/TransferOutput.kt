package br.com.hotwheelscollector.feature.transfer

import androidx.work.Data
import br.com.hotwheelscollector.data.importexport.ImportApplyResult
import br.com.hotwheelscollector.domain.backup.BackupException
import br.com.hotwheelscollector.domain.backup.BackupFailure
import br.com.hotwheelscollector.domain.backup.BackupSummary
import java.time.Instant

/**
 * Contrato de dados entre `TransferWorker` e os ViewModels (T134).
 *
 * O resultado atravessa processo e reinício dentro do WorkManager, então só trafegam
 * primitivos. O resumo é remontado do outro lado por [readSummary].
 */
internal object TransferOutput {

    const val ACTION = "action"
    const val ACTION_BACKUP = "backup"
    const val ACTION_INSPECT = "inspect"
    const val ACTION_RESTORE = "restore"
    const val ACTION_IMPORT = "import"

    const val URI = "uri"
    const val FILE = "file"
    const val MODE = "mode"
    const val STAGE = "stage"

    const val KIND = "kind"
    const val KIND_SUMMARY = "summary"
    const val KIND_IMPORT = "import"

    const val FAILURE = "failure"
    const val DETAIL = "detail"

    const val CREATED_AT = "created_at"
    const val SCHEMA_VERSION = "schema_version"
    const val APP_VERSION = "app_version"
    const val DEVICE = "device"
    const val IMAGE_COUNT = "image_count"
    const val TOTAL_BYTES = "total_bytes"
    const val MISSING_IMAGES = "missing_images"
    const val COUNTS = "counts"

    const val IMPORTED = "imported"
    const val SKIPPED = "skipped"
    const val INVALID = "invalid"
    const val CREATED_MANUFACTURERS = "created_manufacturers"
    const val CREATED_MODELS = "created_models"
    const val CREATED_VARIANTS = "created_variants"
    const val CREATED_COLORS = "created_colors"

    fun summary(summary: BackupSummary): Data = Data.Builder()
        .putString(KIND, KIND_SUMMARY)
        .putLong(CREATED_AT, summary.createdAt.toEpochMilli())
        .putInt(SCHEMA_VERSION, summary.schemaVersion)
        .putString(APP_VERSION, summary.appVersion)
        .putString(DEVICE, summary.device)
        .putInt(IMAGE_COUNT, summary.imageCount)
        .putLong(TOTAL_BYTES, summary.totalBytes)
        .putInt(MISSING_IMAGES, summary.missingImages)
        .putString(COUNTS, encodeCounts(summary.counts))
        .build()

    fun readSummary(data: Data): BackupSummary? {
        if (data.getString(KIND) != KIND_SUMMARY) return null
        val createdAt = data.getLong(CREATED_AT, -1)
        val appVersion = data.getString(APP_VERSION) ?: return null
        if (createdAt < 0) return null
        return BackupSummary(
            createdAt = Instant.ofEpochMilli(createdAt),
            schemaVersion = data.getInt(SCHEMA_VERSION, 0),
            appVersion = appVersion,
            device = data.getString(DEVICE),
            counts = decodeCounts(data.getString(COUNTS)),
            imageCount = data.getInt(IMAGE_COUNT, 0),
            totalBytes = data.getLong(TOTAL_BYTES, 0),
            missingImages = data.getInt(MISSING_IMAGES, 0),
        )
    }

    fun imported(result: ImportApplyResult): Data = Data.Builder()
        .putString(KIND, KIND_IMPORT)
        .putInt(IMPORTED, result.importedExemplars)
        .putInt(SKIPPED, result.skippedDuplicates)
        .putInt(INVALID, result.invalidRows)
        .putInt(CREATED_MANUFACTURERS, result.createdManufacturers)
        .putInt(CREATED_MODELS, result.createdModels)
        .putInt(CREATED_VARIANTS, result.createdVariants)
        .putInt(CREATED_COLORS, result.createdColors)
        .build()

    fun readImported(data: Data): ImportApplyResult? {
        if (data.getString(KIND) != KIND_IMPORT) return null
        return ImportApplyResult(
            importedExemplars = data.getInt(IMPORTED, 0),
            skippedDuplicates = data.getInt(SKIPPED, 0),
            invalidRows = data.getInt(INVALID, 0),
            createdManufacturers = data.getInt(CREATED_MANUFACTURERS, 0),
            createdModels = data.getInt(CREATED_MODELS, 0),
            createdVariants = data.getInt(CREATED_VARIANTS, 0),
            createdColors = data.getInt(CREATED_COLORS, 0),
        )
    }

    fun failure(error: Throwable): Data = Data.Builder()
        .putString(FAILURE, (error as? BackupException)?.failure?.name)
        .putString(DETAIL, error.message)
        .build()

    fun readFailure(data: Data): BackupFailure? =
        data.getString(FAILURE)?.let { name -> BackupFailure.entries.firstOrNull { it.name == name } }

    fun readDetail(data: Data): String? = data.getString(DETAIL)

    private fun encodeCounts(counts: Map<String, Int>): String =
        counts.entries.joinToString(";") { (key, value) -> "$key=$value" }

    private fun decodeCounts(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(";").mapNotNull { entry ->
            val separator = entry.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val value = entry.substring(separator + 1).toIntOrNull() ?: return@mapNotNull null
            entry.substring(0, separator) to value
        }.toMap()
    }
}
