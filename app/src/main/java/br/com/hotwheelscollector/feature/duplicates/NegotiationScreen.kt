package br.com.hotwheelscollector.feature.duplicates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.Exemplar
import br.com.hotwheelscollector.domain.model.Negotiation
import br.com.hotwheelscollector.domain.model.NegotiationStatus
import br.com.hotwheelscollector.domain.model.NegotiationType
import br.com.hotwheelscollector.ui.format.formatMoneyPtBr

/**
 * Negociação do exemplar (T085–T087, FR-068–FR-071).
 *
 * Sem negociação ativa, a tela abre a reserva. Com uma ativa, mostra o que foi combinado
 * e oferece concluir venda, concluir troca ou cancelar; o histórico fica logo abaixo e
 * não some ao concluir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun NegotiationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NegotiationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.negotiation_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                TextButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.duplicates_back))
                }
            }

            Text(
                text = listOfNotNull(
                    state.internalCode ?: stringResource(R.string.negotiation_exemplar_unknown),
                    state.title.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when {
                state.loading -> Text(
                    text = stringResource(R.string.negotiation_loading),
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> {
                    val active = state.active
                    if (active == null) {
                        OpenNegotiationForm(
                            state = state,
                            onTypeChange = viewModel::onTypeChange,
                            onCounterpartyChange = viewModel::onCounterpartyChange,
                            onChannelChange = viewModel::onChannelChange,
                            onAmountChange = viewModel::onAmountChange,
                            onNotesChange = viewModel::onNotesChange,
                            onReserveChange = viewModel::onReserveChange,
                            onOpen = viewModel::onOpen,
                        )
                    } else {
                        ActiveNegotiationCard(
                            negotiation = active,
                            state = state,
                            onCloseSale = viewModel::onCloseSale,
                            onCloseTrade = viewModel::onCloseTrade,
                            onCancel = viewModel::onCancel,
                        )
                    }
                }
            }

            NegotiationHistoryScreen(
                negotiations = state.negotiations,
                events = state.events,
            )
        }
    }
}

@Composable
private fun OpenNegotiationForm(
    state: NegotiationUiState,
    onTypeChange: (NegotiationType) -> Unit,
    onCounterpartyChange: (String) -> Unit,
    onChannelChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onReserveChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.negotiation_open_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.negotiation_open_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NegotiationType.entries.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { onTypeChange(type) },
                        label = {
                            Text(
                                stringResource(
                                    when (type) {
                                        NegotiationType.SALE -> R.string.negotiation_type_sale
                                        NegotiationType.TRADE -> R.string.negotiation_type_trade
                                    },
                                ),
                            )
                        },
                    )
                }
            }

            OutlinedTextField(
                value = state.counterparty,
                onValueChange = onCounterpartyChange,
                label = { Text(stringResource(R.string.negotiation_counterparty)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.channel,
                onValueChange = onChannelChange,
                label = { Text(stringResource(R.string.negotiation_channel)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.amountText,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.negotiation_amount)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(R.string.negotiation_notes)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.negotiation_reserve),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(checked = state.reserve, onCheckedChange = onReserveChange)
            }

            Button(onClick = onOpen, enabled = !state.working) {
                Text(stringResource(R.string.negotiation_open_action))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveNegotiationCard(
    negotiation: Negotiation,
    state: NegotiationUiState,
    onCloseSale: (String, String) -> Unit,
    onCloseTrade: (String?, String) -> Unit,
    onCancel: (String) -> Unit,
) {
    var showSale by remember { mutableStateOf(false) }
    var showTrade by remember { mutableStateOf(false) }
    var showCancel by remember { mutableStateOf(false) }
    var saleAmount by remember(negotiation.id) {
        mutableStateOf(negotiation.amount?.formatPtBr().orEmpty())
    }
    var saleDate by remember(negotiation.id) { mutableStateOf(state.today) }
    var receivedId by remember(negotiation.id) { mutableStateOf<String?>(null) }
    var tradeNote by remember(negotiation.id) { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.negotiation_active_title,
                    statusLabel(negotiation.status),
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(
                    if (negotiation.type == NegotiationType.SALE) {
                        R.string.negotiation_type_sale
                    } else {
                        R.string.negotiation_type_trade
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            negotiation.counterparty?.let {
                Text(
                    text = stringResource(R.string.negotiation_counterparty_value, it),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            negotiation.channel?.let {
                Text(
                    text = stringResource(R.string.negotiation_channel_value, it),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            negotiation.amount?.let {
                Text(
                    text = stringResource(
                        R.string.negotiation_amount_value,
                        formatMoneyPtBr(it.cents),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            negotiation.notes?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when (negotiation.type) {
                    NegotiationType.SALE -> TextButton(
                        onClick = { showSale = true },
                        enabled = !state.working,
                    ) {
                        Text(stringResource(R.string.negotiation_close_sale))
                    }
                    NegotiationType.TRADE -> TextButton(
                        onClick = { showTrade = true },
                        enabled = !state.working,
                    ) {
                        Text(stringResource(R.string.negotiation_close_trade))
                    }
                }
                TextButton(
                    onClick = { showCancel = true },
                    enabled = !state.working,
                ) {
                    Text(stringResource(R.string.negotiation_cancel))
                }
            }
        }
    }

    if (showSale) {
        AlertDialog(
            onDismissRequest = { showSale = false },
            title = { Text(stringResource(R.string.negotiation_close_sale_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = saleAmount,
                        onValueChange = { saleAmount = it },
                        label = { Text(stringResource(R.string.negotiation_received_value)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = saleDate,
                        onValueChange = { saleDate = it },
                        label = { Text(stringResource(R.string.negotiation_close_date)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSale = false
                        onCloseSale(saleAmount, saleDate)
                    },
                ) {
                    Text(stringResource(R.string.negotiation_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSale = false }) {
                    Text(stringResource(R.string.duplicates_sheet_cancel))
                }
            },
        )
    }

    if (showTrade) {
        val selected = state.ownedExemplars.firstOrNull { it.id == receivedId }
        AlertDialog(
            onDismissRequest = { showTrade = false },
            title = { Text(stringResource(R.string.negotiation_close_trade_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var menuOpen by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = menuOpen,
                        onExpandedChange = { menuOpen = !menuOpen },
                    ) {
                        OutlinedTextField(
                            value = selected?.internalCode
                                ?: stringResource(R.string.negotiation_trade_none),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.negotiation_trade_received)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuOpen)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.negotiation_trade_none)) },
                                onClick = {
                                    receivedId = null
                                    menuOpen = false
                                },
                            )
                            state.ownedExemplars.forEach { exemplar: Exemplar ->
                                DropdownMenuItem(
                                    text = { Text(exemplar.internalCode) },
                                    onClick = {
                                        receivedId = exemplar.id
                                        menuOpen = false
                                    },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = tradeNote,
                        onValueChange = { tradeNote = it },
                        label = { Text(stringResource(R.string.negotiation_trade_note)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTrade = false
                        onCloseTrade(receivedId, tradeNote)
                    },
                ) {
                    Text(stringResource(R.string.negotiation_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTrade = false }) {
                    Text(stringResource(R.string.duplicates_sheet_cancel))
                }
            },
        )
    }

    if (showCancel) {
        AlertDialog(
            onDismissRequest = { showCancel = false },
            title = { Text(stringResource(R.string.negotiation_cancel_title)) },
            text = { Text(stringResource(R.string.negotiation_cancel_hint)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancel = false
                        onCancel(negotiation.id)
                    },
                ) {
                    Text(stringResource(R.string.negotiation_cancel_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancel = false }) {
                    Text(stringResource(R.string.negotiation_keep))
                }
            },
        )
    }
}
