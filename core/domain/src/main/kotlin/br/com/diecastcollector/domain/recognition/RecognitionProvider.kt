package br.com.diecastcollector.domain.recognition

import br.com.diecastcollector.domain.model.RecognitionSource

/** Campos que o reconhecimento pode sugerir (FR-102/FR-107). */
public enum class RecognitionField {
    MANUFACTURER,
    MODEL,
    SERIES,
    YEAR,
    VARIANT,
    CODE,
}

/**
 * Uma sugestão isolada, com a confiança medida pelo provedor.
 *
 * O valor é sempre texto: quem aplica no formulário decide a conversão (ano, código) e o
 * usuário confirma campo a campo (FR-104).
 */
public data class RecognitionSuggestion(
    val field: RecognitionField,
    val value: String,
    val confidence: Double,
) {
    init {
        require(value.isNotBlank()) { "Sugestão sem valor." }
        require(confidence in 0.0..1.0) { "Confiança deve ficar entre 0 e 1." }
    }
}

/**
 * Resultado de uma leitura, pronto para revisão — nunca aplicado sozinho.
 *
 * `suggestions` só contém o que passou do limiar: quando nada passa, a proposta é
 * "não identificado" e nenhum campo é preenchido por inferência (FR-105). O contrato não
 * tem escrita: aplicar é decisão explícita da tela, campo a campo (FR-104, SC-010).
 */
public data class RecognitionProposal(
    val source: RecognitionSource,
    val suggestions: List<RecognitionSuggestion>,
    val provider: String,
    val rawValue: String? = null,
) {
    public val identified: Boolean get() = suggestions.isNotEmpty()

    public companion object {
        /** Abaixo disto a leitura é inconclusiva e vira "não identificado". */
        public const val CONFIDENCE_THRESHOLD: Double = 0.6

        /** Filtra, ordena e remove repetições do mesmo campo/valor antes de propor. */
        public fun of(
            source: RecognitionSource,
            provider: String,
            suggestions: List<RecognitionSuggestion>,
            rawValue: String? = null,
        ): RecognitionProposal = RecognitionProposal(
            source = source,
            provider = provider,
            rawValue = rawValue,
            suggestions = suggestions
                .filter { it.confidence >= CONFIDENCE_THRESHOLD }
                .sortedByDescending { it.confidence }
                .distinctBy { it.field to it.value },
        )

        public fun unidentified(
            source: RecognitionSource,
            provider: String,
            rawValue: String? = null,
        ): RecognitionProposal = RecognitionProposal(
            source = source,
            provider = provider,
            rawValue = rawValue,
            suggestions = emptyList(),
        )
    }
}

/**
 * O que o usuário aceitou de uma proposta (FR-104, SC-010).
 *
 * Existe como tipo próprio para que "aplicar" seja um ato explícito e verificável: a
 * proposta sozinha não aplica nada, e só os campos marcados chegam aqui. Campos aceitos que
 * não estão na proposta são ignorados — a tela não inventa valor (FR-105).
 */
public data class RecognitionApplication(
    val fields: Map<RecognitionField, String>,
) {
    public val isEmpty: Boolean get() = fields.isEmpty()

    public companion object {

        public val NONE: RecognitionApplication = RecognitionApplication(emptyMap())

        /** Filtra a proposta pelos campos marcados pelo usuário, sem inferir nada. */
        public fun accept(
            proposal: RecognitionProposal,
            acceptedFields: Set<RecognitionField>,
        ): RecognitionApplication = RecognitionApplication(
            proposal.suggestions
                .filter { it.field in acceptedFields }
                .distinctBy { it.field }
                .associate { it.field to it.value },
        )
    }
}

/** Entrada do reconhecimento: uma imagem da câmera ou um código já lido. */
public sealed interface RecognitionInput {

    public class Image(public val bytes: ByteArray) : RecognitionInput

    public class Barcode(public val value: String) : RecognitionInput
}

/**
 * Provedor de reconhecimento assistido (T123).
 *
 * Regras do contrato: roda no aparelho, não grava nada e não altera o catálogo — devolve
 * apenas sugestões com confiança e origem. Um provedor indisponível deve falhar com
 * `DomainError.RecognitionUnavailable`, e a falta de permissão de câmera não pode impedir
 * o resto do app (FR-106/FR-108).
 */
public interface RecognitionProvider {

    public val source: RecognitionSource

    public suspend fun recognize(input: RecognitionInput): RecognitionProposal
}
