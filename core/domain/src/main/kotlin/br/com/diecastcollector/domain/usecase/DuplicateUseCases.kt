package br.com.diecastcollector.domain.usecase

import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.DuplicateGroup
import br.com.diecastcollector.domain.model.IsoDate
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.repository.DuplicateRepository
import br.com.diecastcollector.domain.repository.NegotiationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Lista de duplicados agrupada por variante (T083, FR-065).
 *
 * O caso de uso existe para que a tela dependa de uma intenção do domínio — "quais
 * exemplares repetidos eu tenho" — e não da consulta que a implementa hoje. O
 * agrupamento é por `variant_id`, nunca por nome de modelo (SC-005).
 */
public class GetDuplicatesUseCase(
    private val duplicates: DuplicateRepository,
) {
    public fun observeGroups(): Flow<List<DuplicateGroup>> = duplicates.observeGroups()
}

/** Conclusão de venda (T086, FR-070): valor recebido obrigatório, data padrão hoje. */
public class CloseSaleUseCase(
    private val negotiations: NegotiationRepository,
) {
    public suspend fun close(
        negotiationId: String,
        amount: MoneyCents?,
        date: IsoDate?,
    ): DomainResult<Unit> {
        if (amount == null) {
            return Result.failure(
                DomainError.Validation("Informe o valor recebido para concluir a venda."),
            )
        }
        return negotiations.closeSale(negotiationId, amount, date)
    }
}

/**
 * Conclusão de troca (T087, FR-071).
 *
 * O exemplar recebido é opcional — troca por dinheiro ou por item ainda não cadastrado
 * continua registrada como troca, sem inventar um exemplar.
 */
public class CloseTradeUseCase(
    private val negotiations: NegotiationRepository,
) {
    public suspend fun close(
        negotiationId: String,
        receivedExemplarId: String?,
        note: String? = null,
    ): DomainResult<Unit> = negotiations.closeTrade(negotiationId, receivedExemplarId, note)
}
