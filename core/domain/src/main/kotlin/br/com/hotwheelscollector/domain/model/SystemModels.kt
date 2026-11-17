package br.com.hotwheelscollector.domain.model

import java.time.Instant

/**
 * Trilha de auditoria polimórfica por (`entityType`, `entityId`).
 * Appende-only: nada aqui é editado ou apagado (Art. I.8, FR-027).
 */
public data class HistoryEntry(
    val id: String,
    val entityType: String,
    val entityId: String,
    val field: String,
    val at: Instant,
    val oldValue: String? = null,
    val newValue: String? = null,
    val source: String? = null,
) {
    init {
        require(entityType.isNotBlank()) { "Tipo de entidade é obrigatório na auditoria." }
        require(entityId.isNotBlank()) { "Identificador da entidade é obrigatório na auditoria." }
    }
}

/**
 * Sugestão de reconhecimento (código de barras, OCR ou foto).
 * Nunca alimenta o catálogo sozinha: só depois do aceite explícito (FR-104).
 */
public data class RecognitionResult(
    val id: String,
    val source: RecognitionSource,
    val provider: String,
    val payloadJson: String,
    val createdAt: Instant,
    val exemplarId: String? = null,
    val draftId: String? = null,
    val confidence: Double? = null,
    val appliedFields: String? = null,
) {
    init {
        require(provider.isNotBlank()) { "Provedor de reconhecimento é obrigatório." }
        require(confidence == null || confidence in 0.0..1.0) { "Confiança deve ficar entre 0 e 1." }
    }
}

/** Preferência persistida em chave/valor (FR-145, FR-171). */
public data class AppSetting(
    val key: String,
    val value: String,
) {
    init {
        require(key.isNotBlank()) { "Chave de preferência é obrigatória." }
    }
}

/** Chaves de preferência conhecidas, para não espalhar string solta pelo código. */
public object SettingKeys {
    public const val PHOTO_MAX_BYTES: String = "photo_max_bytes"
    public const val BACKUP_INTERVAL_DAYS: String = "backup_interval_days"
    public const val LAST_BACKUP_AT: String = "last_backup_at"
    public const val THEME: String = "theme"
    public const val MIN_SCALE: String = "min_scale"
}
