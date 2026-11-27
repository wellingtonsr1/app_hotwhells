package br.com.diecastcollector.domain.repository

import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.DuplicateAvailability
import br.com.diecastcollector.domain.model.DuplicateGroup
import br.com.diecastcollector.domain.model.DuplicateOffer
import br.com.diecastcollector.domain.model.IsoDate
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.model.Negotiation
import br.com.diecastcollector.domain.model.NegotiationEvent
import br.com.diecastcollector.domain.model.NegotiationType
import kotlinx.coroutines.flow.Flow

/**
 * Duplicados e disponibilidade (FR-065–FR-067).
 *
 * A consulta agrupa por variante e já traz a oferta de cada exemplar; marcar a
 * disponibilidade é um upsert por exemplar, nunca uma segunda linha.
 */
public interface DuplicateRepository {
    /** Só variantes com mais de um exemplar possuído (FR-065). */
    public fun observeGroups(): Flow<List<DuplicateGroup>>

    public suspend fun offerFor(exemplarId: String): DuplicateOffer?

    /** Oferta atual do exemplar, reativa; `null` quando nunca foi marcada (FR-505). */
    public fun observeOffer(exemplarId: String): Flow<DuplicateOffer?>

    /**
     * Ofertas dos exemplares de uma variante, indexadas por `exemplar_id`.
     *
     * Uma consulta para a lista inteira: é o que impede a tela de exemplares de disparar
     * uma consulta por linha (NFR-503).
     */
    public fun observeOffersByVariant(variantId: String): Flow<Map<String, DuplicateOffer>>

    public suspend fun setOffer(
        exemplarId: String,
        availability: DuplicateAvailability,
        condition: Condition?,
        askingPrice: MoneyCents?,
        notes: String?,
    ): DomainResult<DuplicateOffer>
}

/** Rascunho de negociação aberta pela tela de reserva (FR-068–FR-069). */
public data class NewNegotiation(
    val exemplarId: String,
    val type: NegotiationType,
    val counterparty: String? = null,
    val channel: String? = null,
    val amount: MoneyCents? = null,
    val notes: String? = null,
    /** `false` deixa a negociação em `OPEN` em vez de reservar o exemplar. */
    val reserve: Boolean = true,
)

/**
 * Negociação de venda ou troca (FR-068–FR-073).
 *
 * `open` recusa quando já existe negociação ativa para o exemplar; concluir e cancelar
 * gravam evento na mesma transação, e o histórico nunca é apagado.
 */
public interface NegotiationRepository {
    public fun observeByExemplar(exemplarId: String): Flow<List<Negotiation>>

    /**
     * Negociação ativa de cada exemplar da variante, indexada por `exemplar_id`.
     *
     * Serve ao indicador **Em negociação** da lista de exemplares em uma consulta só
     * (FR-505, NFR-503).
     */
    public fun observeActiveByVariant(variantId: String): Flow<Map<String, Negotiation>>

    /** Eventos de todas as negociações do exemplar, em ordem cronológica (FR-073). */
    public fun observeEventsForExemplar(exemplarId: String): Flow<List<NegotiationEvent>>

    public suspend fun open(draft: NewNegotiation): DomainResult<Negotiation>

    /** Conclui venda: exemplar passa a `SOLD` e o valor/data entram no histórico (FR-070). */
    public suspend fun closeSale(
        negotiationId: String,
        amount: MoneyCents,
        date: IsoDate?,
        counterparty: String? = null,
    ): DomainResult<Unit>

    /** Conclui troca: exemplar sai como `TRADED` e o recebido é vinculado se houver (FR-071). */
    public suspend fun closeTrade(
        negotiationId: String,
        receivedExemplarId: String? = null,
        note: String? = null,
    ): DomainResult<Unit>

    /** Cancela mantendo o registro e devolvendo o exemplar ao estado anterior (FR-072). */
    public suspend fun cancel(negotiationId: String): DomainResult<Unit>
}
