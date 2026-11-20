package br.com.hotwheelscollector.feature.duplicates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.DuplicateAvailability
import br.com.hotwheelscollector.domain.model.DuplicateExemplar
import br.com.hotwheelscollector.domain.model.DuplicateGroup
import br.com.hotwheelscollector.feature.catalog.labelRes
import br.com.hotwheelscollector.ui.format.formatMoneyPtBr

/**
 * Duplicados por variante (T080/T084, FR-065–FR-067).
 *
 * Cada cartão é uma variante com mais de um exemplar possuído — o mesmo modelo em outra
 * pintura não entra. Do exemplar saem as duas ações da história: marcar disponibilidade e
 * negociar.
 */
@Composable
public fun DuplicatesScreen(
    onBack: () -> Unit,
    onOpenNegotiation: (exemplarId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DuplicatesViewModel = hiltViewModel(),
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
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.duplicates_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                TextButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.duplicates_back))
                }
            }

            when {
                state.loading -> Text(
                    text = stringResource(R.string.duplicates_loading),
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.groups.isEmpty() -> Column(
                    modifier = Modifier.padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.duplicates_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.duplicates_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                ) {
                    items(state.groups, key = { it.variantId }) { group ->
                        DuplicateGroupCard(
                            group = group,
                            onSelect = viewModel::onSelect,
                            onNegotiate = onOpenNegotiation,
                        )
                    }
                }
            }
        }
    }

    state.selected?.let { selected ->
        DuplicateActionsSheet(
            selected = selected,
            saving = state.saving,
            onDismiss = viewModel::onDismissSheet,
            onSave = viewModel::onSaveOffer,
        )
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    onSelect: (String) -> Unit,
    onNegotiate: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = listOfNotNull(
                    group.manufacturerName,
                    group.modelName,
                    group.colorName,
                ).joinToString(" · "),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.duplicates_count,
                    group.count,
                    group.count,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            group.exemplars.forEach { duplicate ->
                DuplicateRow(
                    duplicate = duplicate,
                    onSelect = { onSelect(duplicate.exemplar.id) },
                    onNegotiate = { onNegotiate(duplicate.exemplar.id) },
                )
            }
        }
    }
}

@Composable
private fun DuplicateRow(
    duplicate: DuplicateExemplar,
    onSelect: () -> Unit,
    onNegotiate: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = duplicate.exemplar.internalCode,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(duplicate.exemplar.condition.labelRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = offerLine(duplicate),
            style = MaterialTheme.typography.bodySmall,
            color = if (duplicate.activeNegotiation != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onSelect) {
                Text(stringResource(R.string.duplicates_offer_action))
            }
            TextButton(onClick = onNegotiate) {
                Text(stringResource(R.string.duplicates_negotiate_action))
            }
        }
    }
}

@Composable
private fun offerLine(duplicate: DuplicateExemplar): String {
    val availability = duplicate.offer?.availability
    val parts = buildList {
        if (availability == null || availability == DuplicateAvailability.UNAVAILABLE) {
            add(stringResource(R.string.duplicates_availability_unavailable))
        } else {
            add(stringResource(availability.labelRes()))
            duplicate.offer?.askingPrice?.let { add(formatMoneyPtBr(it.cents)) }
        }
        if (duplicate.activeNegotiation != null) {
            add(stringResource(R.string.duplicates_reserved))
        }
    }
    return parts.joinToString(" · ")
}
