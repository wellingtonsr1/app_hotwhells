package br.com.hotwheelscollector.domain.repository

import br.com.hotwheelscollector.domain.model.FinancePeriod
import br.com.hotwheelscollector.domain.model.FinanceSummary
import br.com.hotwheelscollector.domain.model.FinancialDisposal
import br.com.hotwheelscollector.domain.model.PeriodSpending
import br.com.hotwheelscollector.domain.model.SpendingSlice
import kotlinx.coroutines.flow.Flow

/**
 * Leitura financeira derivada das aquisições e avaliações (FR-077–FR-086).
 *
 * Nada é armazenado: investido, estimado, custo médio e gastos agrupados saem de consulta.
 * Só exemplares com `status = 'OWNED'` entram; vendidos, trocados e arquivados ficam de
 * fora do investido (FR-087).
 */
public interface FinanceRepository {

    public fun observeSummary(): Flow<FinanceSummary>

    public fun observeSpendingByManufacturer(): Flow<List<SpendingSlice>>

    public fun observeSpendingBySeries(): Flow<List<SpendingSlice>>

    public fun observeSpendingByCollection(): Flow<List<SpendingSlice>>

    /** Gastos por mês dentro do intervalo; sem pontas, todo o histórico. */
    public fun observeSpendingByMonth(period: FinancePeriod = FinancePeriod()): Flow<List<PeriodSpending>>

    /**
     * Vendas e trocas concluídas, da mais recente para a mais antiga (FR-087).
     * Canceladas não entram: nada saiu do acervo.
     */
    public fun observeDisposals(): Flow<List<FinancialDisposal>>
}
