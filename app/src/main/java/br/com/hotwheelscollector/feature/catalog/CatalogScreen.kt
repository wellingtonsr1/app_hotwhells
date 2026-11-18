package br.com.hotwheelscollector.feature.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.VariantSummary
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme

/**
 * Entrada do app: as variantes do acervo.
 *
 * Tocar numa variante abre o cadastro rápido de exemplar — é o caminho que a US1
 * pressupõe ("adicionar exemplar a uma variante já cadastrada"). Quando existe modelo
 * sem variante, o cabeçalho mostra o atalho para completá-lo (FR-029).
 */
@Composable
public fun CatalogScreen(
    onVariantClick: (variantId: String) -> Unit,
    onCreateEntry: () -> Unit,
    onOpenIncomplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    CatalogContent(
        state = state,
        onVariantClick = onVariantClick,
        onCreateEntry = onCreateEntry,
        onOpenIncomplete = onOpenIncomplete,
        modifier = modifier,
    )
}

@Composable
internal fun CatalogContent(
    state: CatalogUiState,
    onVariantClick: (String) -> Unit,
    onCreateEntry: () -> Unit,
    onOpenIncomplete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateEntry) {
                Text(stringResource(R.string.catalog_add))
            }
        },
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
                    text = stringResource(R.string.catalog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                if (state.hasIncomplete) {
                    TextButton(
                        onClick = onOpenIncomplete,
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.catalog_incomplete_action,
                                state.incompleteCount,
                            ),
                        )
                    }
                }
            }

            when {
                state.isEmpty -> EmptyCatalog()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 8.dp,
                        bottom = 96.dp,
                    ),
                ) {
                    items(state.variants, key = { it.variantId }) { variant ->
                        VariantCard(variant = variant, onClick = { onVariantClick(variant.variantId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun VariantCard(variant: VariantSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = variant.modelName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = variantSubtitle(variant),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Segunda linha do cartão: fabricante · ano · contagem de exemplares.
 *
 * O texto é montado aqui, na apresentação, e não no domínio: `VariantSummary` é projeção
 * de dados e não deve carregar idioma (T131, Art. IV).
 */
@Composable
private fun variantSubtitle(variant: VariantSummary): String {
    val exemplarText = if (variant.exemplarCount == 0) {
        stringResource(R.string.catalog_exemplar_none)
    } else {
        pluralStringResource(
            R.plurals.catalog_exemplar_count,
            variant.exemplarCount,
            variant.exemplarCount,
        )
    }
    return listOfNotNull(
        variant.manufacturerName,
        variant.releaseYear?.toString(),
        exemplarText,
    ).joinToString(" · ")
}

@Composable
private fun EmptyCatalog() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.catalog_empty_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.catalog_empty_hint),
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
private fun CatalogPreview() {
    HotWheelsTheme {
        CatalogContent(
            state = CatalogUiState(
                variants = listOf(
                    VariantSummary("v1", "Twin Mill", "Hot Wheels", "Vermelho", 2024, 2),
                    VariantSummary("v2", "Bone Shaker", "Hot Wheels", null, null, 0),
                ),
                loading = false,
            ),
            onVariantClick = {},
            onCreateEntry = {},
            onOpenIncomplete = {},
        )
    }
}
