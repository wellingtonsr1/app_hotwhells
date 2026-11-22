package br.com.diecastcollector.feature.duplicates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.Negotiation
import br.com.diecastcollector.domain.model.NegotiationEvent
import br.com.diecastcollector.domain.model.NegotiationStatus
import br.com.diecastcollector.domain.model.NegotiationType
import br.com.diecastcollector.ui.format.formatMoneyPtBr
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Histórico da negociação (T088, FR-073).
 *
 * Conclusão e cancelamento não apagam nada: as negociações aparecem da mais recente para
 * a mais antiga e cada uma carrega os eventos que a compuseram. O usuário vê a sequência
 * — aberta, reservada, concluída/cancelada — sem perder o que já aconteceu.
 */
@Composable
public fun NegotiationHistoryScreen(
    negotiations: List<Negotiation>,
    events: List<NegotiationEvent>,
    modifier: Modifier = Modifier,
) {
    val eventsByNegotiation = events.groupBy { it.negotiationId }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.negotiation_history_title),
            style = MaterialTheme.typography.titleMedium,
        )
        if (negotiations.isEmpty()) {
            Text(
                text = stringResource(R.string.negotiation_history_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        negotiations
            .sortedByDescending { it.closedAt ?: it.startedAt }
            .forEach { negotiation ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "${typeLabel(negotiation.type)} · " +
                                statusLabel(negotiation.status),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(
                                R.string.negotiation_history_started,
                                formatInstant(negotiation.startedAt),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        listOfNotNull(
                            negotiation.counterparty?.let {
                                stringResource(R.string.negotiation_counterparty_value, it)
                            },
                            negotiation.channel?.let {
                                stringResource(R.string.negotiation_channel_value, it)
                            },
                            negotiation.amount?.let {
                                stringResource(
                                    R.string.negotiation_amount_value,
                                    formatMoneyPtBr(it.cents),
                                )
                            },
                        ).forEach { line ->
                            Text(text = line, style = MaterialTheme.typography.bodySmall)
                        }

                        eventsByNegotiation[negotiation.id].orEmpty().forEach { event ->
                            Text(
                                text = buildString {
                                    append(formatInstant(event.at))
                                    event.description?.let { append(" · $it") }
                                    event.amount?.let {
                                        append(" · ${formatMoneyPtBr(it.cents)}")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
    }
}

@Composable
private fun typeLabel(type: NegotiationType): String = stringResource(
    when (type) {
        NegotiationType.SALE -> R.string.negotiation_type_sale
        NegotiationType.TRADE -> R.string.negotiation_type_trade
    },
)

@Composable
internal fun statusLabel(status: NegotiationStatus): String = stringResource(
    when (status) {
        NegotiationStatus.OPEN -> R.string.negotiation_status_open
        NegotiationStatus.RESERVED -> R.string.negotiation_status_reserved
        NegotiationStatus.CLOSED -> R.string.negotiation_status_closed
        NegotiationStatus.CANCELLED -> R.string.negotiation_status_cancelled
    },
)

private val HISTORY_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())

internal fun formatInstant(instant: Instant): String = HISTORY_FORMAT.format(instant)
