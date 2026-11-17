package br.com.hotwheelscollector.domain.error

/**
 * Erro de domínio, independente de plataforma.
 *
 * É uma `Exception` de propósito: as operações devolvem `Result<T>`, e `Result.failure`
 * exige `Throwable`. Cada caso carrega o que a interface precisa para explicar o problema
 * em português; o `code` é o que vai para a trilha de auditoria e para os testes.
 */
public sealed class DomainError(
    public val code: String,
    message: String,
) : Exception(message) {

    /** Entrada inválida, com o motivo específico (nome vazio, valor negativo...). */
    public data class Validation(public val reason: String) : DomainError("validation", reason)

    public data object NotFound : DomainError("not_found", "Registro não encontrado.")

    /** Registro duplicado (código interno repetido, variante com mesmo fingerprint). */
    public data object Duplicate : DomainError("duplicate", "Já existe um registro igual.")

    /** Escrita concorrente: `row_version` mudou entre leitura e gravação (Art. I.12). */
    public data object StaleWrite : DomainError("stale_write", "Registro alterado em outra tela.")

    /** Operação bloqueada por vínculo existente, como localização com exemplares (FR-093). */
    public data class Blocked(public val reason: String) : DomainError("blocked", reason)

    /** Falha de arquivo: sem espaço, permissão negada, foto corrompida (FR-153). */
    public data class Storage(public val reason: String) : DomainError("storage", reason)

    /** Implementação do provedor de reconhecimento indisponível. */
    public data object RecognitionUnavailable :
        DomainError("recognition_unavailable", "Reconhecimento indisponível.")
}

/** Resultado de uma operação de domínio, já com o erro tipado. */
public typealias DomainResult<T> = Result<T>
