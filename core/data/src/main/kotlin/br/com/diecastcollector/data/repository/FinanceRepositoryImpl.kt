package br.com.diecastcollector.data.repository

import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.dao.DisposalRow
import br.com.diecastcollector.data.db.dao.MonthSpendingRow
import br.com.diecastcollector.data.db.dao.SpendingRow
import br.com.diecastcollector.domain.model.FinancePeriod
import br.com.diecastcollector.domain.model.FinanceSummary
import br.com.diecastcollector.domain.model.FinancialDisposal
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.model.NegotiationType
import br.com.diecastcollector.domain.model.PeriodSpending
import br.com.diecastcollector.domain.model.SpendingSlice
import java.time.Instant
import br.com.diecastcollector.domain.repository.FinanceRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Leitura financeira derivada (FR-077–FR-086). O repositório só traduz linhas agregadas;
 * a regra de custo e a média vivem no domínio (`FinanceSummary`, `Acquisition`).
 */
@Singleton
public class FinanceRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
) : FinanceRepository {

    override fun observeSummary(): Flow<FinanceSummary> =
        database.financeDao().observeSummary().map { row ->
            FinanceSummary(
                investedCents = row.investedCents,
                estimatedCents = row.estimatedCents,
                ownedExemplars = row.ownedExemplars,
                exemplarsWithValue = row.exemplarsWithValue,
                exemplarsWithoutValuation = row.exemplarsWithoutValuation,
                latestValuationDate = row.latestValuationDate,
            )
        }

    override fun observeSpendingByManufacturer(): Flow<List<SpendingSlice>> =
        database.financeDao().observeSpendingByManufacturer().map { rows -> rows.toSlices() }

    override fun observeSpendingBySeries(): Flow<List<SpendingSlice>> =
        database.financeDao().observeSpendingBySeries().map { rows -> rows.toSlices() }

    override fun observeSpendingByCollection(): Flow<List<SpendingSlice>> =
        database.financeDao().observeSpendingByCollection().map { rows -> rows.toSlices() }

    override fun observeSpendingByMonth(period: FinancePeriod): Flow<List<PeriodSpending>> =
        database.financeDao()
            .observeSpendingByMonth(
                from = period.from?.toIsoString(),
                to = period.to?.toIsoString(),
            )
            .map { rows -> rows.map { it.toPeriodSpending() } }

    override fun observeDisposals(): Flow<List<FinancialDisposal>> =
        database.financeDao().observeDisposals().map { rows -> rows.map { it.toDisposal() } }

    private fun List<SpendingRow>.toSlices(): List<SpendingSlice> =
        map { SpendingSlice(label = it.label, cents = it.cents) }

    private fun DisposalRow.toDisposal(): FinancialDisposal = FinancialDisposal(
        negotiationId = negotiationId,
        exemplarId = exemplarId,
        internalCode = internalCode,
        manufacturerName = manufacturerName,
        modelName = modelName,
        type = NegotiationType.valueOf(type),
        closedAt = Instant.ofEpochMilli(closedAt),
        amount = amountCents?.let(::MoneyCents),
        counterparty = counterparty,
        receivedExemplarCode = receivedCode,
    )

    private fun MonthSpendingRow.toPeriodSpending(): PeriodSpending =
        PeriodSpending(yearMonth = month, cents = cents)
}
