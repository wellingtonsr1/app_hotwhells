package br.com.diecastcollector.feature.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.VariantSummary
import br.com.diecastcollector.ui.components.ItemThumbnail
import br.com.diecastcollector.ui.theme.DiecastTheme

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
    onOpenExemplars: (variantId: String) -> Unit,
    onCreateEntry: () -> Unit,
    onOpenIncomplete: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCollections: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenBarcode: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    CatalogContent(
        state = state,
        onVariantClick = onVariantClick,
        onOpenExemplars = onOpenExemplars,
        onCreateEntry = onCreateEntry,
        onOpenIncomplete = onOpenIncomplete,
        onOpenSearch = onOpenSearch,
        onOpenCollections = onOpenCollections,
        onOpenDuplicates = onOpenDuplicates,
        onOpenBarcode = onOpenBarcode,
        modifier = modifier,
    )
}

@Composable
internal fun CatalogContent(
    state: CatalogUiState,
    onVariantClick: (String) -> Unit,
    onOpenExemplars: (String) -> Unit,
    onCreateEntry: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenIncomplete: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenCollections: () -> Unit = {},
    onOpenDuplicates: () -> Unit = {},
    onOpenBarcode: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCreateEntry) {
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.catalog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 12.dp, bottom = 4.dp),
                )
                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = stringResource(R.string.search_open),
                    )
                }
            }
            // Atalhos numa linha própria: no cabeçalho eles não cabiam e «Duplicados»
            // quebrava uma letra por linha em telas estreitas.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.hasIncomplete) {
                    TextButton(onClick = onOpenIncomplete) {
                        Text(
                            text = stringResource(
                                R.string.catalog_incomplete_action,
                                state.incompleteCount,
                            ),
                        )
                    }
                }
                TextButton(onClick = onOpenCollections) {
                    Text(stringResource(R.string.collections_open))
                }
                TextButton(onClick = onOpenDuplicates) {
                    Text(stringResource(R.string.duplicates_open))
                }
                TextButton(onClick = onOpenBarcode) {
                    Text(stringResource(R.string.recognition_barcode_title))
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
                        VariantCard(
                            variant = variant,
                            onClick = { onVariantClick(variant.variantId) },
                            onOpenExemplars = { onOpenExemplars(variant.variantId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VariantCard(
    variant: VariantSummary,
    onClick: () -> Unit,
    onOpenExemplars: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemThumbnail(
                    photoPath = variant.primaryPhotoPath,
                    contentDescription = variantPhotoDescription(variant),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = variant.modelName,
                        style = MaterialTheme.typography.titleMedium,
                        // Duas linhas bastam para identificar o carrinho; sem o limite, o
                        // nome longo esticava o cartão e quebrava o ritmo da lista.
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = variantSubtitle(variant),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // O atalho fica abaixo do texto: com a miniatura ao lado, na mesma linha ele
            // espremia o nome em telas estreitas e em fonte ampliada.
            // O cartão continua abrindo o cadastro rápido; este atalho leva às unidades
            // já existentes, onde ficam edição, movimentação e arquivamento (FR-028).
            TextButton(
                onClick = onOpenExemplars,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.catalog_open_exemplars))
            }
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

/**
 * Descrição da miniatura para o leitor de tela: nome do modelo e, quando houver, cor e
 * ano. Só o nome deixaria duas variantes do mesmo modelo com a mesma descrição.
 */
@Composable
private fun variantPhotoDescription(variant: VariantSummary): String =
    stringResource(
        R.string.catalog_thumbnail_photo,
        listOfNotNull(
            variant.modelName,
            variant.colorName,
            variant.releaseYear?.toString(),
        ).joinToString(" · "),
    )

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
    DiecastTheme {
        CatalogContent(
            state = CatalogUiState(
                variants = listOf(
                    VariantSummary("v1", "Twin Mill", "Hot Wheels", "Vermelho", 2024, 2),
                    VariantSummary("v2", "Bone Shaker", "Hot Wheels", null, null, 0),
                ),
                loading = false,
            ),
            onVariantClick = {},
            onOpenExemplars = {},
            onCreateEntry = {},
            onOpenIncomplete = {},
            onOpenSearch = {},
        )
    }
}
