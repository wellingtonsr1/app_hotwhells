package br.com.hotwheelscollector.domain.model

import java.time.Instant

/**
 * Disponibilidade de um exemplar repetido (FR-066).
 *
 * "Ambos" é valor próprio, e não a combinação de dois registros: a mesma unidade física
 * não pode estar em duas listas de oferta diferentes.
 */
public enum class DuplicateAvailability { TRADE, SALE, BOTH, UNAVAILABLE }

/**
 * Marcação de disponibilidade do exemplar (FR-066–FR-067).
 *
 * A condição e o valor pretendido ficam aqui, e não no exemplar: são a proposta da
 * negociação, não o estado físico permanente da peça.
 */
public data class DuplicateOffer(
    val id: String,
    val exemplarId: String,
    val availability: DuplicateAvailability,
    val condition: Condition? = null,
    val askingPrice: MoneyCents? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** Exemplar dentro de um grupo de duplicados, com a oferta e a negociação ativa. */
public data class DuplicateExemplar(
    val exemplar: Exemplar,
    val manufacturerName: String,
    val modelName: String,
    val colorName: String?,
    val offer: DuplicateOffer? = null,
    val activeNegotiation: Negotiation? = null,
)

/**
 * Grupo de duplicados: exemplares possuídos da **mesma variante** (FR-065).
 *
 * O agrupamento é por `variant_id`, nunca por nome de modelo: duas pinturas do mesmo
 * carro são variantes distintas, não duplicatas (SC-005).
 */
public data class DuplicateGroup(
    val variantId: String,
    val manufacturerName: String,
    val modelName: String,
    val colorName: String?,
    val exemplars: List<DuplicateExemplar>,
) {
    val count: Int get() = exemplars.size
}
