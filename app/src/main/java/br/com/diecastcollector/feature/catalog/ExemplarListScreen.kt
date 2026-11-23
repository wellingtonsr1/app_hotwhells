package br.com.diecastcollector.feature.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.Exemplar
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.ui.components.ItemThumbnail
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.time.Instant

/**
 * Exemplares de uma variante: ponto de entrada da edição e da movimentação (FR-091).
 * Tocar num cartão abre o detalhe; o FAB mantém o cadastro rápido da US1 no mesmo lugar.
 */
@Composable
public fun ExemplarListScreen(
    onAddExemplar: (variantId: String) -> Unit,
    onOpenExemplar: (exemplarId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExemplarListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    ExemplarListContent(
        state = state,
        onAddExemplar = { onAddExemplar(state.variantId) },
        onOpenExemplar = onOpenExemplar,
        modifier = modifier,
    )
}

@Composable
internal fun ExemplarListContent(
    state: ExemplarListUiState,
    onAddExemplar: () -> Unit,
    onOpenExemplar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                            onClick = { onOpenExemplar(exemplar.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExemplarCard(
    exemplar: Exemplar,
    locationName: String?,
    photoPath: String?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
