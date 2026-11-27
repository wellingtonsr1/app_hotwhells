package br.com.diecastcollector.feature.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.DuplicateAvailability
import br.com.diecastcollector.domain.model.Exemplar
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.feature.duplicates.ExemplarOfferSheet
import br.com.diecastcollector.feature.duplicates.OfferSheetSubject
import br.com.diecastcollector.feature.duplicates.UniqueExemplarDialog
import br.com.diecastcollector.feature.duplicates.labelRes
import br.com.diecastcollector.ui.components.ItemThumbnail
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.time.Instant

/**
 * Exemplares de uma variante: ponto de entrada da edição, da disponibilidade e da
 * negociação (FR-091, FR-501, FR-502).
 *
 * Tocar no cartão abre o detalhe; **Disponibilidade** e **Negociar** ficam fora da área do
 * `clickable`, para que a ação não abra a ficha por engano. O rótulo de estado é texto, não
 * cor: **Venda**, **Troca**, **Troca e venda**, **Indisponível** ou **Em negociação**.
 */
@Composable
public fun ExemplarListScreen(
    onAddExemplar: (variantId: String) -> Unit,
    onOpenExemplar: (exemplarId: String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenNegotiation: (exemplarId: String) -> Unit = {},
    viewModel: ExemplarListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = state.message?.text()

    // Recriação da janela: o aviso pendente não volta (SC-505).
    LaunchedEffect(Unit) { viewModel.onWindowRecreated() }

    // A navegação é da tela; o ViewModel só decide se ela acontece antes ou depois do aviso.
    LaunchedEffect(state.openNegotiationId) {
        state.openNegotiationId?.let { exemplarId ->
            onOpenNegotiation(exemplarId)
            viewModel.onNegotiationOpened()
        }
    }

    LaunchedEffect(state.message) {
        messageText?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageShown()
        }
    }

    ExemplarListContent(
        state = state,
        onAddExemplar = { onAddExemplar(state.variantId) },
        onOpenExemplar = onOpenExemplar,
        onOpenOfferSheet = viewModel::onOpenOfferSheet,
        onDismissSheet = viewModel::onDismissSheet,
        onSaveOffer = viewModel::onSaveOffer,
        onContinueUniqueWarning = viewModel::onContinueUniqueWarning,
        onCancelUniqueWarning = viewModel::onCancelUniqueWarning,
        onNegotiate = viewModel::onNegotiate,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
internal fun ExemplarListContent(
    state: ExemplarListUiState,
    onAddExemplar: () -> Unit,
    onOpenExemplar: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenOfferSheet: (String) -> Unit = {},
    onDismissSheet: () -> Unit = {},
    onSaveOffer: (DuplicateAvailability, Condition?, String, String) -> Unit = { _, _, _, _ -> },
    onContinueUniqueWarning: () -> Unit = {},
    onCancelUniqueWarning: () -> Unit = {},
    onNegotiate: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExemplar) {
                Text(stringResource(R.string.exemplars_add))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.exemplars_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )

            when {
                state.isEmpty -> EmptyExemplars()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                ) {
                    items(state.exemplars, key = { it.id }) { exemplar ->
                        ExemplarCard(
                            exemplar = exemplar,
                            locationName = exemplar.locationId?.let { state.locationNames[it] },
                            photoPath = state.photoPaths[exemplar.id],
                            availability = state.offers[exemplar.id]?.availability,
                            negotiating = state.hasActiveNegotiation(exemplar.id),
                            onClick = { onOpenExemplar(exemplar.id) },
                            onOpenOffer = { onOpenOfferSheet(exemplar.id) },
                            onNegotiate = { onNegotiate(exemplar.id) },
                        )
                    }
                }
            }
        }
    }

    if (state.sheetOpen) {
        state.selected?.let { exemplar ->
            ExemplarOfferSheet(
                subject = OfferSheetSubject(
                    internalCode = exemplar.internalCode,
                    title = "",
                    condition = exemplar.condition,
                    offer = state.offers[exemplar.id],
                ),
                saving = state.savingOffer,
                onDismiss = onDismissSheet,
                onSave = onSaveOffer,
            )
        }
    }
    if (state.uniqueWarning) {
        UniqueExemplarDialog(
            onContinue = onContinueUniqueWarning,
            onCancel = onCancelUniqueWarning,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExemplarCard(
    exemplar: Exemplar,
    locationName: String?,
    photoPath: String?,
    availability: DuplicateAvailability?,
    negotiating: Boolean,
    onClick: () -> Unit,
    onOpenOffer: () -> Unit,
    onNegotiate: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ItemThumbnail(
                    photoPath = photoPath,
                    contentDescription = stringResource(
                        R.string.catalog_thumbnail_photo,
                        exemplar.internalCode,
                    ),
                    onOpenGallery = onClick,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = exemplar.internalCode, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = listOfNotNull(
                            stringResource(exemplar.condition.labelRes()),
                            locationName ?: stringResource(R.string.add_exemplar_no_location),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    exemplar.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            // Fora do cartão clicável: agir não abre a ficha (T512, FR-520). Em 360 dp
            // com fonte ampliada as duas ações não cabem ao lado do rótulo, então a linha
            // quebra em vez de sair da tela.
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // Com negociação ativa o rótulo é dominante, mas a disponibilidade
                // registrada continua visível ao lado (FR-511).
                val label = listOfNotNull(
                    availability?.let { stringResource(it.labelRes()) },
                    if (negotiating) stringResource(R.string.duplicates_reserved) else null,
                ).joinToString(" · ").takeIf { it.isNotEmpty() }
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                TextButton(onClick = onOpenOffer) {
                    Text(stringResource(R.string.duplicates_offer_action))
                }
                TextButton(onClick = onNegotiate, enabled = !negotiating) {
                    Text(stringResource(R.string.duplicates_negotiate_action))
                }
            }
        }
    }
}

@Composable
private fun EmptyExemplars() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.exemplars_empty_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.exemplars_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExemplarListPreview() {
    val now = Instant.parse("2026-11-18T12:00:00Z")
    DiecastTheme {
        ExemplarListContent(
            state = ExemplarListUiState(
                variantId = "v1",
                exemplars = listOf(
                    Exemplar(
                        id = "e1",
                        variantId = "v1",
                        internalCode = "HW-000001",
                        condition = Condition.SEALED,
                        status = ExemplarStatus.OWNED,
                        locationId = "loc1",
                        notes = "Caixa com marca de estoque",
                        createdAt = now,
                        updatedAt = now,
                    ),
                ),
                locationNames = mapOf("loc1" to "Estante da sala"),
                loading = false,
            ),
            onAddExemplar = {},
            onOpenExemplar = {},
        )
    }
}
