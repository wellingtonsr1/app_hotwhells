package br.com.hotwheelscollector.ui

import androidx.annotation.StringRes
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.error.DomainError

/**
 * Tradução de erro de domínio para texto em português.
 *
 * O domínio não conhece Android nem recurso de string: devolve `DomainError`, e a
 * apresentação escolhe a mensagem. Motivos livres (`Validation.reason`, `Blocked.reason`,
 * `Storage.reason`) entram como argumento do formato, sempre em pt-BR.
 */
@StringRes
public fun DomainError.messageRes(): Int = when (this) {
    is DomainError.Validation -> R.string.error_validation
    is DomainError.Blocked -> R.string.error_blocked
    is DomainError.Storage -> R.string.error_storage
    DomainError.NotFound -> R.string.error_not_found
    DomainError.Duplicate -> R.string.error_duplicate
    DomainError.StaleWrite -> R.string.error_stale_write
    DomainError.RecognitionUnavailable -> R.string.error_recognition_unavailable
}

/** Detalhe que acompanha a mensagem, quando o erro carrega um motivo específico. */
public fun DomainError.messageDetail(): String? = when (this) {
    is DomainError.Validation -> reason
    is DomainError.Blocked -> reason
    is DomainError.Storage -> reason
    else -> null
}
