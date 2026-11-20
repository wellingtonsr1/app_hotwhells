package br.com.hotwheelscollector.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.CollectionProgress
import br.com.hotwheelscollector.domain.model.DashboardEntry
import br.com.hotwheelscollector.domain.model.DashboardSnapshot
import br.com.hotwheelscollector.ui.format.formatCentsPtBr
import kotlin.math.abs

/**
 * Painel do estado da coleção (US3, FR-001–FR-011).
 *
 * A tela não formata moeda nem percentual a partir do banco: recebe o snapshot já agregado
 * e só decide como mostrar. Quando a leitura falha, mostra erro — não zero.
 */
@Composable
public fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DashboardContent(state = state, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardContent(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.dashboard_title)) }) },
    ) { padding ->
        val bodyModifier = Modifier
            .padding(padding)
            .fillMaxSize()
        when (state) {
            DashboardUiState.Loading -> Box(bodyModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is DashboardUiState.Error -> ErrorBody(bodyModifier, state)

            is DashboardUiState.Ready -> if (state.snapshot.isEmpty) {
                EmptyBody(bodyModifier)
            } else {
                DashboardBody(bodyModifier, state.snapshot)
            }
        }
    }
}

@Composable
private fun ErrorBody(modifier: Modifier, state: DashboardUiState.Error) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.dashboard_error_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = state.message ?: stringResource(R.string.dashboard_error_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyBody(modifier: Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.dashboard_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.dashboard_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DashboardBody(modifier: Modifier, snapshot: DashboardSnapshot) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { IndicatorGrid(snapshot) }
        item { EstimateCard(snapshot) }
        item { Section(stringResource(R.string.dashboard_latest_acquisitions), snapshot.latestAcquisitions) }
        item { Section(stringResource(R.string.dashboard_latest_entries), snapshot.latestEntries) }
        item { CollectionsSection(snapshot.collectionProgress) }
    }
}

/** Os onze indicadores, três por linha, na ordem em que o colecionador confere o acervo. */
@Composable
private fun IndicatorGrid(snapshot: DashboardSnapshot) {
    val moneyInvested = moneyText(snapshot.investedCents)
    val moneyEstimated = moneyText(snapshot.estimatedCents)
    val moneyDifference = moneyText(snapshot.estimatedDifferenceCents)
    val indicators = listOf(
        stringResource(R.string.dashboard_metric_exemplars) to snapshot.ownedExemplars.toString(),
        stringResource(R.string.dashboard_metric_models) to snapshot.distinctModels.toString(),
        stringResource(R.string.dashboard_metric_variants) to snapshot.distinctVariants.toString(),
        stringResource(R.string.dashboard_metric_manufacturers) to snapshot.distinctManufacturers.toString(),
        stringResource(R.string.dashboard_metric_series) to snapshot.distinctSeries.toString(),
        stringResource(R.string.dashboard_metric_collections) to snapshot.activeCollections.toString(),
        stringResource(R.string.dashboard_metric_duplicates) to snapshot.variantsWithDuplicates.toString(),
        stringResource(R.string.dashboard_metric_wanted) to snapshot.wantedItems.toString(),
        stringResource(R.string.dashboard_metric_invested) to moneyInvested,
        stringResource(R.string.dashboard_metric_estimated) to moneyEstimated,
        stringResource(R.string.dashboard_metric_difference) to moneyDifference,
    )

    val fontScale = LocalConfiguration.current.fontScale
    val columns = if (fontScale >= 1.3f) 2 else COLUMNS

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        indicators.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    MetricCard(label = label, value = value, modifier = Modifier.weight(1f))
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Selo de estimativa (T055): diz de onde vem o número e quantos exemplares ficaram fora.
 * Sem esse aviso, "estimado" pareceria avaliação de mercado.
 */
@Composable
private fun EstimateCard(snapshot: DashboardSnapshot) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.dashboard_estimate_badge),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.dashboard_estimate_note),
                style = MaterialTheme.typography.bodySmall,
            )
            snapshot.latestValuationDate?.let { date ->
                Text(
                    text = stringResource(R.string.dashboard_estimate_until, date),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (snapshot.exemplarsWithoutValuation > 0) {
                Text(
                    text = stringResource(
                        R.string.dashboard_estimate_missing,
                        snapshot.exemplarsWithoutValuation,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, entries: List<DashboardEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (entries.isEmpty()) {
            Text(
                text = stringResource(R.string.dashboard_latest_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Card {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    entries.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = entry.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = entry.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = entry.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionsSection(progress: List<CollectionProgress>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.dashboard_collections),
            style = MaterialTheme.typography.titleMedium,
        )
        if (progress.isEmpty()) {
            Text(
                text = stringResource(R.string.dashboard_collections_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    progress.forEach { CollectionRow(it) }
                }
            }
        }
    }
}

@Composable
private fun CollectionRow(progress: CollectionProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = progress.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(
                    R.string.dashboard_collection_progress,
                    progress.owned,
                    progress.known,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ProgressBar(fraction = progress.percent / 100f)
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/** `R$ 1.234,56`; a formatação de centavos vem de `core/ui` (T098). */
@Composable
private fun moneyText(cents: Long): String {
    val absolute = stringResource(R.string.dashboard_money, formatCentsPtBr(abs(cents)))
    return if (cents < 0L) "-$absolute" else absolute
}

private const val COLUMNS = 3
