package br.com.hotwheelscollector.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.FinancePeriod
import br.com.hotwheelscollector.domain.model.FinanceSummary
import br.com.hotwheelscollector.domain.model.FinancialHistory
import br.com.hotwheelscollector.domain.model.IsoDate
import br.com.hotwheelscollector.domain.model.PeriodSpending
import br.com.hotwheelscollector.domain.model.SpendingSlice
import br.com.hotwheelscollector.domain.repository.FinanceRepository
import br.com.hotwheelscollector.domain.usecase.FinancialHistoryUseCase
import br.com.hotwheelscollector.ui.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

public data class FinanceUiState(
    val summary: FinanceSummary = FinanceSummary(),
    val byManufacturer: List<SpendingSlice> = emptyList(),
    val bySeries: List<SpendingSlice> = emptyList(),
    val byCollection: List<SpendingSlice> = emptyList(),
    val byMonth: List<PeriodSpending> = emptyList(),
    val history: FinancialHistory = FinancialHistory(),
    val fromText: String = "",
    val toText: String = "",
    val periodMessage: UiMessage? = null,
    val loading: Boolean = true,
)

@HiltViewModel
public class FinanceViewModel @Inject constructor(
    financeRepository: FinanceRepository,
    financialHistory: FinancialHistoryUseCase,
) : ViewModel() {

    private data class FormState(
        val fromText: String = "",
        val toText: String = "",
        val periodMessage: UiMessage? = null,
    )

    private data class DataState(
        val summary: FinanceSummary,
        val byManufacturer: List<SpendingSlice>,
        val bySeries: List<SpendingSlice>,
        val byCollection: List<SpendingSlice>,
        val byMonth: List<PeriodSpending>,
    )

    private val form = MutableStateFlow(FormState())
    private val period = MutableStateFlow(FinancePeriod())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val data = combine(
        financeRepository.observeSummary(),
        financeRepository.observeSpendingByManufacturer(),
        financeRepository.observeSpendingBySeries(),
        financeRepository.observeSpendingByCollection(),
        period.flatMapLatest { financeRepository.observeSpendingByMonth(it) },
    ) { summary, manufacturers, series, collections, months ->
        DataState(
            summary = summary,
            byManufacturer = manufacturers,
            bySeries = series,
            byCollection = collections,
            byMonth = months,
        )
    }

    public val state: StateFlow<FinanceUiState> = combine(
        data,
        financialHistory.observe(),
        form,
    ) { values, history, formState ->
        FinanceUiState(
            summary = values.summary,
            byManufacturer = values.byManufacturer,
            bySeries = values.bySeries,
            byCollection = values.byCollection,
            byMonth = values.byMonth,
            history = history,
            fromText = formState.fromText,
            toText = formState.toText,
            periodMessage = formState.periodMessage,
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = FinanceUiState(),
    )

    public fun onFromChange(value: String) = form.update { it.copy(fromText = value) }

    public fun onToChange(value: String) = form.update { it.copy(toText = value) }

    public fun onMessageShown() = form.update { it.copy(periodMessage = null) }

    /** Intervalo livre (FR-078): pontas em branco significam "sem limite". */
    public fun onApplyPeriod() {
        val current = form.value
        val from = if (current.fromText.isBlank()) {
            null
        } else {
            IsoDate.parsePtBrOrNull(current.fromText)
                ?: return form.update {
                    it.copy(periodMessage = UiMessage(R.string.error_invalid_date))
                }
        }
        val to = if (current.toText.isBlank()) {
            null
        } else {
            IsoDate.parsePtBrOrNull(current.toText)
                ?: return form.update {
                    it.copy(periodMessage = UiMessage(R.string.error_invalid_date))
                }
        }
        if (from != null && to != null && from > to) {
            return form.update {
                it.copy(periodMessage = UiMessage(R.string.finance_inverted_period))
            }
        }
        period.value = FinancePeriod(from = from, to = to)
        form.update { it.copy(periodMessage = null) }
    }

    public fun onClearPeriod() {
        period.value = FinancePeriod()
        form.update { it.copy(fromText = "", toText = "", periodMessage = null) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
