package br.com.diecastcollector.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.domain.model.SearchResultItem
import br.com.diecastcollector.feature.catalog.labelRes
import br.com.diecastcollector.ui.components.ItemThumbnail
import br.com.diecastcollector.ui.format.formatMoneyPtBr
import coil.compose.AsyncImage
import java.io.File

/** Grade e lista detalhada do resultado (T063). */
@Composable
internal fun SearchResultsGrid(
    items: List<SearchResultItem>,
    onOpenExemplar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(items, key = { it.exemplarId }) { item ->
            // A grade já é a visão compacta: sem foto, o texto caberia em duas colunas.
            SearchResultCard(
                item = item,
                onOpen = { onOpenExemplar(item.exemplarId) },
                showThumbnail = false,
            )
        }
    }
}

@Composable
internal fun SearchResultsList(
    items: List<SearchResultItem>,
    onOpenExemplar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(items, key = { it.exemplarId }) { item ->
            SearchResultCard(item = item, onOpen = { onOpenExemplar(item.exemplarId) })
        }
    }
}

/**
 * Galeria fotográfica (FR-116, T064): usa a foto principal de cada exemplar.
 *
 * O arquivo vive em `filesDir` e o banco guarda só o caminho relativo; a resolução
 * acontece aqui, na apresentação, como no restante do app. Exemplar sem foto continua
 * aparecendo com um marcador — sumir da galeria esconderia parte do acervo.
 */
@Composable
internal fun SearchResultsGallery(
    items: List<SearchResultItem>,
    onOpenExemplar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(items, key = { it.exemplarId }) { item ->
            GalleryTile(item = item, onOpen = { onOpenExemplar(item.exemplarId) })
        }
    }
}

@Composable
private fun GalleryTile(
    item: SearchResultItem,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val photoPath = item.primaryPhotoPath
    Card(onClick = onOpen, modifier = modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                if (photoPath != null) {
                    AsyncImage(
                        model = File(context.filesDir, photoPath),
                        contentDescription = stringResource(
                            R.string.search_gallery_photo,
                            item.modelName,
                        ),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Filled.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.search_gallery_no_photo),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "${item.manufacturerName} · ${item.modelName}",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        item.releaseYear?.toString(),
                        item.scale,
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun SearchResultCard(
    item: SearchResultItem,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    showThumbnail: Boolean = true,
) {
    Card(onClick = onOpen, modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showThumbnail) {
                ItemThumbnail(
                    photoPath = item.primaryPhotoPath,
                    contentDescription = stringResource(
                        R.string.catalog_thumbnail_photo,
                        item.modelName,
                    ),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "${item.manufacturerName} · ${item.modelName}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val detail = listOfNotNull(
                    item.seriesName,
                    item.releaseYear?.toString(),
                    item.scale,
                    item.primaryColorName,
                ).joinToString(" · ")
                if (detail.isNotEmpty()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = item.condition.labelText() + " · " + item.status.labelText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.locationName?.let { location ->
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = item.internalCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.priceCents?.let(::formatMoneyPtBr)
                            ?: stringResource(R.string.finance_average_none),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
internal fun Condition.labelText(): String = stringResource(labelRes())

@Composable
internal fun ExemplarStatus.labelText(): String = stringResource(
    when (this) {
        ExemplarStatus.OWNED -> R.string.search_status_owned
        ExemplarStatus.SOLD -> R.string.search_status_sold
        ExemplarStatus.TRADED -> R.string.search_status_traded
        ExemplarStatus.ARCHIVED -> R.string.search_status_archived
    },
)
