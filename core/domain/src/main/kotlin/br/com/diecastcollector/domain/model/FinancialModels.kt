package br.com.diecastcollector.domain.model

import java.time.Instant

/** Aquisição do exemplar: 1:1, com valores sempre em centavos (FR-074–FR-076). */
public data class Acquisition(
    val id: String,
    val exemplarId: String,
    val date: IsoDate,
    val currency: String = "BRL",
    val price: MoneyCents? = null,
    val shipping: MoneyCents? = null,
    val discount: MoneyCents? = null,
    val store: String? = null,
    val seller: String? = null,
    val method: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    /**
     * Custo real = preço + frete − desconto, calculado e nunca armazenado (Art. I.2).
     * `null` quando nenhum valor foi informado — o painel mostra "sem valor", não zero.
     */
    public fun totalCost(): MoneyCents? {
        val base = listOfNotNull(price, shipping, discount)
        if (base.isEmpty() || price == null) return null
        val gross = price + (shipping ?: MoneyCents.ZERO)
        val netCents = gross.cents - (discount?.cents ?: 0L)
        return if (netCents >= 0L) MoneyCents(netCents) else MoneyCents.ZERO
    }
}

/** Avaliação de valor em uma data; o histórico completo é preservado (FR-082–FR-086). */
public data class Valuation(
    val id: String,
    val exemplarId: String,
    val date: IsoDate,
    val value: MoneyCents,
    val currency: String = "BRL",
    val source: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
)

/** Negociação de venda ou troca (FR-068–FR-073). */
public data class Negotiation(
    val id: String,
    val exemplarId: String,
    val type: NegotiationType,
    val status: NegotiationStatus = NegotiationStatus.OPEN,
    val startedAt: Instant,
    val closedAt: Instant? = null,
    val counterparty: String? = null,
    val channel: String? = null,
    val amount: MoneyCents? = null,
    val currency: String? = null,
    val notes: String? = null,
    /** Exemplar recebido quando a negociação foi uma troca (FR-071). */
    val receivedExemplarId: String? = null,
) {
    init {
        require(!(status == NegotiationStatus.CLOSED && closedAt == null)) {
            "Negociação concluída precisa de data de fechamento."
        }
    }
}

/** Evento de negociação: appende-only, sobrevive ao fechamento (FR-069, FR-073). */
public data class NegotiationEvent(
    val id: String,
    val negotiationId: String,
    val at: Instant,
    val description: String? = null,
    val amount: MoneyCents? = null,
)
