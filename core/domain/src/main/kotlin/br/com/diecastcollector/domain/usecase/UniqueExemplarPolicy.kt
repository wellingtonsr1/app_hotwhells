package br.com.diecastcollector.domain.usecase

import br.com.diecastcollector.domain.model.DuplicateAvailability
import br.com.diecastcollector.domain.model.ExemplarStatus

/**
 * Quando avisar que a operação pode tirar do acervo a única unidade de uma variante
 * (RN-003, FR-515).
 *
 * A regra é pura de propósito: as duas telas que oferecem as ações (ficha e lista) e os
 * testes de domínio precisam da mesma resposta, sem consultar banco nem depender de
 * Android (Art. II.2, II.6).
 *
 * O aviso acontece em **dois** momentos — ao confirmar uma disponibilidade que torna o
 * exemplar ofertável e ao abrir a negociação — e nunca quando a variante tem duas ou mais
 * unidades `OWNED`, quando o exemplar não está no acervo, quando a oferta é
 * `UNAVAILABLE` ou quando já existe negociação ativa (o usuário já viu o aviso na
 * abertura).
 */
public object UniqueExemplarPolicy {

    /** Verdadeiro só quando a variante tem exatamente uma unidade no acervo. */
    public fun isUnique(ownedInVariant: Int): Boolean = ownedInVariant == 1

    /**
     * Aviso na confirmação da folha de disponibilidade (FR-515a).
     *
     * `UNAVAILABLE` não dispara: retirar a oferta reduz o risco de saída, não amplia.
     */
    public fun warnsOnAvailability(
        ownedInVariant: Int,
        status: ExemplarStatus,
        availability: DuplicateAvailability,
        hasActiveNegotiation: Boolean,
    ): Boolean =
        isUnique(ownedInVariant) &&
            status == ExemplarStatus.OWNED &&
            !hasActiveNegotiation &&
            availability != DuplicateAvailability.UNAVAILABLE

    /** Aviso antes de abrir a negociação (FR-515b). */
    public fun warnsOnNegotiation(
        ownedInVariant: Int,
        status: ExemplarStatus,
        hasActiveNegotiation: Boolean,
    ): Boolean =
        isUnique(ownedInVariant) &&
            status == ExemplarStatus.OWNED &&
            !hasActiveNegotiation
}
