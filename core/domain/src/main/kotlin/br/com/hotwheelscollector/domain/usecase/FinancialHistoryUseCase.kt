package br.com.hotwheelscollector.domain.usecase

import br.com.hotwheelscollector.domain.model.FinancialHistory
import br.com.hotwheelscollector.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Histórico de vendas e trocas concluídas (T097, FR-087).
 *
 * O investimento continua sendo o custo das aquisições dos exemplares **possuídos**.
 * Este caso de uso lê as negociações fechadas e as separa em vendas (com valor recebido)
 * e trocas (contagem), sem alterar nem recalcular o custo histórico de aquisição.
 */
public class FinancialHistoryUseCase(
    private val financeRepository: FinanceRepository,
) {
    public fun observe(): Flow<FinancialHistory> =
        financeRepository.observeDisposals().map { FinancialHistory(entries = it) }
}
