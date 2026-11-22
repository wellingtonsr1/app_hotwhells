package br.com.diecastcollector.domain.validation

import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.PackagingCondition

/**
 * Validações de catálogo (US2, FR-013–FR-019, FR-022, Art. I.9).
 *
 * Garante que:
 * 1. Escala seja no formato 1:N com N entre 1 e 999.
 * 2. Condição da miniatura e da embalagem pertençam aos enums permitidos.
 * 3. Prioridade esteja no intervalo [1, 3] (1=alta, 2=média, 3=baixa).
 * 4. Campos vazios, em branco ou com sentinelas textuais sejam sanitizados como NULL (FR-015).
 */
public object CatalogValidation {

    private val SCALE_REGEX = Regex("""^1:[1-9][0-9]{0,2}$""")
    private const val MIN_YEAR = 1886

    /** Valida se a escala está no formato "1:N" (ex: 1:64, 1:43, 1:18). */
    public fun validateScale(scale: String): DomainResult<String> {
        val trimmed = scale.trim()
        return if (SCALE_REGEX.matches(trimmed)) {
            Result.success(trimmed)
        } else {
            Result.failure(DomainError.Validation("Escala deve estar no formato 1:N, como 1:64."))
        }
    }

    /** Valida se a prioridade do item de desejo está entre 1 e 3. */
    public fun validatePriority(priority: Int): DomainResult<Int> {
        return if (priority in 1..3) {
            Result.success(priority)
        } else {
            Result.failure(DomainError.Validation("Prioridade deve estar entre 1 (alta) e 3 (baixa)."))
        }
    }

    /**
     * Sanitiza campos opcionais de texto: converte strings vazias, em branco ou com
     * sentinelas arbitrárias ("N/A", "n/a", "null", "none") em `null` estrito (FR-015, Art. I.9).
     */
    public fun cleanOptionalString(value: String?): String? {
        if (value == null) return null
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        val lower = trimmed.lowercase()
        if (lower == "n/a" || lower == "na" || lower == "null" || lower == "none" || lower == "não informado") {
            return null
        }
        return trimmed
    }

    /** Valida o ano de lançamento se informado; nulo é válido (desconhecido). */
    public fun validateYear(year: Int?, currentYear: Int = 2026): DomainResult<Int?> {
        if (year == null) return Result.success(null)
        val maxYear = currentYear + 2
        return if (year in MIN_YEAR..maxYear) {
            Result.success(year)
        } else {
            Result.failure(DomainError.Validation("Ano deve estar entre $MIN_YEAR e $maxYear."))
        }
    }

    /**
     * Valida que o nome obrigatório (fabricante ou modelo) não seja em branco e
     * respeite o tamanho máximo de 120 caracteres.
     */
    public fun validateRequiredName(name: String, fieldName: String): DomainResult<String> {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> Result.failure(DomainError.Validation("Informe o $fieldName."))
            trimmed.length > 120 -> Result.failure(DomainError.Validation("O $fieldName excede 120 caracteres."))
            else -> Result.success(trimmed)
        }
    }

    /** Valida se a condição do exemplar é válida. */
    public fun validateCondition(condition: Condition): DomainResult<Condition> =
        Result.success(condition)

    /** Valida se a condição da embalagem é válida (ou nula). */
    public fun validatePackagingCondition(condition: PackagingCondition?): DomainResult<PackagingCondition?> =
        Result.success(condition)
}
