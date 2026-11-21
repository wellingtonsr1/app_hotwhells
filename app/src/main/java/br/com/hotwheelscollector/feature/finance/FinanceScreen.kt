package br.com.hotwheelscollector.feature.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.FinanceSummary
import br.com.hotwheelscollector.domain.model.FinancialDisposal
import br.com.hotwheelscollector.domain.model.FinancialHistory
import br.com.hotwheelscollector.domain.model.NegotiationType
import br.com.hotwheelscollector.domain.model.PeriodSpending
import br.com.hotwheelscollector.domain.model.SpendingSlice
import br.com.hotwheelscollector.ui.format.formatIsoDatePtBr
import br.com.hotwheelscollector.ui.format.formatMoneyPtBr
import br.com.hotwheelscollector.ui.format.formatYearMonthPtBr
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Finanças da coleção (US8, FR-077–FR-086).
 *
 * Investido, estimado, diferença e custo médio saem do mesmo snapshot; gastos por
 * fabricante, série, coleção e mês vêm de agregações no banco. Todo valor estimado é
 * rotulado como estimativa do colecionador, com a data das avaliações.
 */
@Composable
public fun FinanceScreen(
    modifier: Modifier = Modifier,
    viewModel: FinanceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val periodMessageText = state.periodMessage?.text()

    LaunchedEffect(state.periodMessage) {
        periodMessageText?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        FinanceContent(
            state = state,
            modifier = Modifier.padding(padding),
            onFromChange = viewModel::onFromChange,
            onToChange = viewModel::onToChange,
            onApplyPeriod = viewModel::onApplyPeriod,
            onClearPeriod = viewModel::onClearPeriod,
        )
    }
}

@Composable
internal fun FinanceContent(
    state: FinanceUiState,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onApplyPeriod: () -> Unit,
    onClearPeriod: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.finance_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                label = stringResource(R.string.dashboard_metric_invested),
                value = formatMoneyPtBr(state.summary.investedCents),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = stringResource(R.string.dashboard_metric_estimated),
                value = formatMoneyPtBr(state.summary.estimatedCents),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                label = stringResource(R.string.dashboard_metric_difference),
                value = formatMoneyPtBr(state.summary.differenceCents),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = stringResource(R.string.finance_average_cost),
                value = state.summary.averageCostCents
                    ?.let { formatMoneyPtBr(it) }
                    ?: stringResource(R.string.finance_average_none),
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = stringResource(R.string.valuation_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.summary.latestValuationDate?.let { date ->
            Text(
                text = stringResource(R.string.dashboard_estimate_until, formatIsoDatePtBr(date)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.summary.exemplarsWithoutValuation > 0) {
            Text(
                text = stringResource(
                    R.string.dashboard_estimate_missing,
                    state.summary.exemplarsWithoutValuation,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.summary.exemplarsWithoutValue > 0) {
            Text(
                text = pluralStringResource(
                    R.plurals.finance_average_note,
                    state.summary.exemplarsWithoutValue,
                    state.summary.exemplarsWithoutValue,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FinancialHistorySection(history = state.history)

        SpendingSection(
            title = stringResource(R.string.finance_spending_manufacturer),
            slices = state.byManufacturer,
        )
        SpendingSection(
            title = stringResource(R.string.finance_spending_series),
            slices = state.bySeries,
            emptyLabel = stringResource(R.string.finance_no_series),
        )
        SpendingSection(
            title = stringResource(R.string.finance_spending_collection),
            slices = state.byCollection,
            emptyLabel = stringResource(R.string.finance_no_collection),
        )

        Text(
            text = stringResource(R.string.finance_spending_month),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.fromText,
                onValueChange = onFromChange,
                label = { Text(stringResource(R.string.finance_period_from)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.toText,
                onValueChange = onToChange,
                label = { Text(stringResource(R.string.finance_period_to)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onApplyPeriod, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.finance_period_apply))
            }
            OutlinedButton(onClick = onClearPeriod, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.finance_period_clear))
            }
        }
        if (state.byMonth.isEmpty()) {
            EmptySpending()
        } else {
            state.byMonth.forEach { month ->
                SpendingRow(
                    label = formatYearMonthPtBr(month.yearMonth),
                    cents = month.cents,
                )
            }
        }
    }
}

/**
 * Vendas e trocas concluídas (FR-087).
 *
 * Fica visível na mesma tela do investido, mas em bloco próprio: são saídas do acervo e
 * não entram em nenhuma soma de gasto. O custo histórico de aquisição continua o mesmo.
 */
@Composable
private fun FinancialHistorySection(history: FinancialHistory) {
    Text(
        text = stringResource(R.string.finance_history_title),
        style = MaterialTheme.typography.titleMedium,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard(
            label = stringResource(R.string.finance_history_sales),
            value = formatMoneyPtBr(history.salesCents),
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = stringResource(R.string.finance_history_trades),
            value = stringResource(
                R.string.finance_history_trades_value,
                history.tradeCount,
            ),
            modifier = Modifier.weight(1f),
        )
    }
    Text(
        text = stringResource(R.string.finance_history_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (history.entries.isEmpty()) {
        Text(
            text = stringResource(R.string.finance_history_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        history.entries.forEach { entry -> DisposalRowItem(entry) }
    }
}

@Composable
private fun DisposalRowItem(entry: FinancialDisposal) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${DISPOSAL_DATE.format(entry.closedAt)} · ${entry.internalCode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    if (entry.type == NegotiationType.SALE) {
                        R.string.finance_history_sale
                    } else {
                        R.string.finance_history_trade
                    },
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = listOf(entry.manufacturerName, entry.modelName).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
        )
        val detail = listOfNotNull(
            entry.amount?.let { formatMoneyPtBr(it.cents) },
            entry.counterparty?.let {
                stringResource(R.string.finance_history_counterparty, it)
            },
            entry.receivedExemplarCode?.let {
                stringResource(R.string.finance_history_received, it)
            },
        ).joinToString(" · ")
        if (detail.isNotBlank()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val DISPOSAL_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SpendingSection(
    title: String,
    slices: List<SpendingSlice>,
    emptyLabel: String? = null,
) {
    Text(text = title, style = MaterialTheme.typography.titleMedium)
    if (slices.isEmpty()) {
        EmptySpending()
    } else {
        slices.forEach { slice ->
            SpendingRow(
                label = slice.label ?: emptyLabel.orEmpty(),
                cents = slice.cents,
            )
        }
    }
}

@Composable
private fun SpendingRow(label: String, cents: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = formatMoneyPtBr(cents), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptySpending() {
    Text(
        text = stringResource(R.string.finance_spending_empty),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true)
@Composable
private fun FinancePreview() {
    HotWheelsTheme {
        FinanceContent(
            state = FinanceUiState(
                summary = FinanceSummary(
                    investedCents = 123_456,
                    estimatedCents = 90_000,
                    ownedExemplars = 7,
                    exemplarsWithValue = 5,
                    exemplarsWithoutValuation = 2,
                    latestValuationDate = "2026-11-10",
                ),
                byManufacturer = listOf(SpendingSlice("Mattel", 100_000)),
                bySeries = listOf(SpendingSlice("Car Culture", 80_000)),
                byCollection = listOf(SpendingSlice(null, 50_000)),
                byMonth = listOf(PeriodSpending("2026-11", 60_000)),
                loading = false,
            ),
            onFromChange = {},
            onToChange = {},
            onApplyPeriod = {},
            onClearPeriod = {},
        )
    }
}
