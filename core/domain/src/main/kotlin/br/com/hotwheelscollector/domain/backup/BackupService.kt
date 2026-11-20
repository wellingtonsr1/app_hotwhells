package br.com.hotwheelscollector.domain.backup

import java.io.File
import java.io.OutputStream
import java.time.Instant

/** Versão do formato do pacote; a leitura só aceita as conhecidas (FR-143). */
public const val BACKUP_FORMAT_VERSION: Int = 1

/** Um arquivo do pacote, com o tamanho e o hash conferidos na restauração. */
public data class BackupFileEntry(
    val path: String,
    val bytes: Long,
    val sha256: String,
)

/**
 * O que o pacote declara sobre si mesmo. É o que a tela mostra **antes** de o usuário
 * confirmar a substituição dos dados (FR-143).
 */
public data class BackupSummary(
    val createdAt: Instant,
    val schemaVersion: Int,
    val appVersion: String,
    val device: String?,
    val counts: Map<String, Int>,
    val imageCount: Int,
    val totalBytes: Long,
    val missingImages: Int = 0,
)

/** Pacote já validado, pronto para restauração. */
public data class ValidatedBackup(
    val file: File,
    val summary: BackupSummary,
    val files: List<BackupFileEntry>,
)

/** Motivos de recusa; a tela traduz cada um em mensagem acionável. */
public enum class BackupFailure {
    NOT_A_PACKAGE,
    UNSUPPORTED_FORMAT,
    MISSING_FILE,
    SIZE_MISMATCH,
    CHECKSUM_MISMATCH,
    COUNT_MISMATCH,
    INVALID_DATABASE,
    NEWER_SCHEMA,
    FOREIGN_KEY_BROKEN,
    WRITE_FAILED,
}

public class BackupException(
    public val failure: BackupFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Backup manual (FR-140–FR-145, T119): pacote autocontido com o banco, as fotos e um
 * manifesto verificável.
 *
 * Regras que a implementação precisa cumprir:
 * - criar o pacote em streaming, sem montar o zip inteiro em memória;
 * - validar tudo **antes** de tocar nos dados atuais; qualquer falha não grava nada;
 * - a substituição só acontece após confirmação explícita da tela (FR-163).
 *
 * A restauração substitui os dados e fecha o banco injetado: o app precisa ser reaberto
 * depois (o chamador avisa o usuário).
 */
public interface BackupService {

    public suspend fun create(
        output: OutputStream,
        appVersion: String,
        deviceName: String?,
    ): BackupSummary

    public suspend fun inspect(packageFile: File): ValidatedBackup

    public suspend fun restore(packageFile: File): BackupSummary
}
