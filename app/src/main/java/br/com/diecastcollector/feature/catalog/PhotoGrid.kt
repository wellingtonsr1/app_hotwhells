package br.com.diecastcollector.feature.catalog

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.PhotoAngle
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Uma miniatura da grade.
 *
 * `source` é qualquer coisa que o Coil carregue: `File` para foto já gravada pelo
 * `PhotoRepository` e `ByteArray` para captura que ainda não tem exemplar para receber.
 * `pending` só controla o rótulo — não é estado de negócio.
 */
public data class PhotoTile(
    val id: String,
    val angle: PhotoAngle,
    val isPrimary: Boolean,
    val source: Any,
    val pending: Boolean = false,
)

/**
 * Grade de fotografias por ângulo com indicação e definição de foto principal (T046 - US2,
 * FR-020, FR-021).
 *
 * A grade é construída com `Row`/`Column`, não com `LazyVerticalGrid`: ela é usada dentro
 * de formulários que já rolam, e lista preguiçosa dentro de `verticalScroll` estoura em
 * tempo de execução por receber altura infinita.
 */
@Composable
public fun PhotoGrid(
    photos: List<PhotoTile>,
    onAddPhotoBytes: (ByteArray, PhotoAngle) -> Unit,
    onSetPrimary: (photoId: String) -> Unit,
    onRemovePhoto: (photoId: String) -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedAngle by remember { mutableStateOf(PhotoAngle.FRONT) }
    // Tocar na miniatura abre a foto inteira; o visualizador é local à grade para
    // servir tanto às capturas pendentes quanto às fotos já gravadas.
    var fullScreenIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.photo_grid_title),
            style = MaterialTheme.typography.titleMedium,
        )

        // Seletor de ângulo para a próxima foto (FR-020)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PhotoAngle.entries.forEach { angle ->
                FilterChip(
                    selected = selectedAngle == angle,
                    onClick = { selectedAngle = angle },
                    label = { Text(stringResource(angle.labelRes())) },
                )
            }
        }

        // Ações de captura (câmera ou Photo Picker)
        PhotoCaptureActions(
            selectedAngle = selectedAngle,
            onPhotoBytesReceived = onAddPhotoBytes,
            onPermissionDenied = onPermissionDenied,
        )

        if (photos.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                photos.chunked(2).forEach { rowPhotos ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowPhotos.forEach { tile ->
                            PhotoItemCard(
                                tile = tile,
                                onOpen = {
                                    fullScreenIndex = photos.indexOfFirst { it.id == tile.id }
                                },
                                onSetPrimary = { onSetPrimary(tile.id) },
                                onRemove = { onRemovePhoto(tile.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowPhotos.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    fullScreenIndex?.let { index ->
        PhotoViewerDialog(
            photos = photos,
            initialIndex = index,
            onDismiss = { fullScreenIndex = null },
        )
    }
}

/**
 * Foto em tamanho cheio, sobre fundo escuro, com deslize entre os ângulos do exemplar.
 *
 * `usePlatformDefaultWidth = false` deixa a imagem usar a tela inteira. O gesto horizontal
 * do pager não fecha o visualizador: fechar fica no X e no botão voltar, para um arrasto
 * lateral nunca encerrar a foto por engano.
 */
@Composable
private fun PhotoViewerDialog(
    photos: List<PhotoTile>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)),
        pageCount = { photos.size },
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.95f)),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val tile = photos[page]
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(tile.source)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(tile.angle.labelRes()),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                )
            }

            val current = photos.getOrNull(pagerState.currentPage)
            if (current != null) {
                Surface(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                ) {
                    Text(
                        text = if (photos.size > 1) {
                            stringResource(
                                R.string.photo_viewer_position,
                                stringResource(current.angle.labelRes()),
                                pagerState.currentPage + 1,
                                photos.size,
                            )
                        } else {
                            stringResource(current.angle.labelRes())
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        }
    }
}

@Composable
private fun PhotoItemCard(
    tile: PhotoTile,
    onOpen: () -> Unit,
    onSetPrimary: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            // Proporção retrato: as fotos são tiradas com o celular em pé, e o recorte
            // quadrado cortava as laterais do carrinho. 3:4 ainda cabe em duas colunas
            // numa tela de 360 dp, com os dois botões no canto.
            .aspectRatio(3f / 4f)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(tile.source)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(tile.angle.labelRes()),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Badge do ângulo
            Surface(
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                shape = RoundedCornerShape(bottomEnd = 6.dp),
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Text(
                    text = stringResource(tile.angle.labelRes()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }

            if (tile.pending) {
                Surface(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(topEnd = 6.dp),
                    modifier = Modifier.align(Alignment.BottomStart),
                ) {
                    Text(
                        text = stringResource(R.string.photo_pending),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            // Badge e botão de foto principal (FR-021)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(topStart = 8.dp),
                    )
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onSetPrimary,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (tile.isPrimary) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = stringResource(R.string.photo_set_primary),
                        tint = if (tile.isPrimary) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.inverseOnSurface
                        },
                        modifier = Modifier.size(18.dp),
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.photo_remove),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** Rótulo pt-BR de cada ângulo; o texto mora em `strings.xml`, não no Kotlin (Art. IV). */
@StringRes
internal fun PhotoAngle.labelRes(): Int = when (this) {
    PhotoAngle.FRONT -> R.string.photo_angle_front
    PhotoAngle.BACK -> R.string.photo_angle_back
    PhotoAngle.LEFT -> R.string.photo_angle_left
    PhotoAngle.RIGHT -> R.string.photo_angle_right
    PhotoAngle.TOP -> R.string.photo_angle_top
    PhotoAngle.BASE -> R.string.photo_angle_base
    PhotoAngle.PACKAGING -> R.string.photo_angle_packaging
    PhotoAngle.DETAIL -> R.string.photo_angle_detail
}
