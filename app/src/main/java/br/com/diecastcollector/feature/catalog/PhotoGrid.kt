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
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import br.com.diecastcollector.ui.components.LocalWindowSizeClass
import br.com.diecastcollector.ui.components.MediaDialogScaffold
import br.com.diecastcollector.ui.components.isExpanded
import br.com.diecastcollector.ui.components.isMediumOrWider
import coil.compose.AsyncImage
import coil.request.ImageRequest

/** Marcas de teste da grade de fotos (T352). */
public object PhotoGridTags {
    public const val TILE: String = "photo-tile"
}

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
    /** A foto exibida já é uma versão editada; o original continua recuperável (FR-217). */
    val isEdited: Boolean = false,
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
    /** Abre o editor para uma foto já gravada; ausente em formulários com capturas pendentes. */
    onEditPhoto: ((PhotoTile) -> Unit)? = null,
) {
    var selectedAngle by remember { mutableStateOf(PhotoAngle.FRONT) }
    // Tocar na miniatura abre a foto inteira; o visualizador é local à grade para
    // servir tanto às capturas pendentes quanto às fotos já gravadas.
    var fullScreenIndex by remember { mutableStateOf<Int?>(null) }

    // T349: colunas por classe de janela — 2 no celular, 3 na média e 4 na expandida.
    val windowSizeClass = LocalWindowSizeClass.current
    val photoColumns = when {
        windowSizeClass.isExpanded -> 4
        windowSizeClass.isMediumOrWider -> 3
        else -> 2
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.photo_grid_title),
            style = MaterialTheme.typography.titleMedium,
        )

        // Seletor de ângulo para a próxima foto (FR-020). Exceção justificada à quebra de
        // chips: são 9 ângulos e, quebrando em linhas, o seletor ocuparia mais altura que
        // a própria miniatura; a rolagem horizontal mantém um único gesto.
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
                photos.chunked(photoColumns).forEach { rowPhotos ->
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
                                onEdit = onEditPhoto
                                    ?.takeIf { !tile.pending }
                                    ?.let { handler -> { handler(tile) } },
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

    // O visualizador usa o contêiner de mídia com `safeDrawing`: em paisagem com recorte
    // de câmera a imagem e o fechar não ficam sob as barras (T350, FR-323).
    MediaDialogScaffold(onDismiss = onDismiss) {
        Box(modifier = Modifier.fillMaxSize()) {
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

        }
    }
}

@Composable
private fun PhotoItemCard(
    tile: PhotoTile,
    onOpen: () -> Unit,
    onSetPrimary: () -> Unit,
    onRemove: () -> Unit,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(PhotoGridTags.TILE)
            // Proporção retrato: as fotos são tiradas com o celular em pé, e o recorte
            // quadrado cortava as laterais do carrinho. 3:4 se mantém em qualquer
            // número de colunas (FR-321/FR-322).
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

            if (tile.pending || tile.isEdited) {
                Surface(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(topEnd = 6.dp),
                    modifier = Modifier.align(Alignment.BottomStart),
                ) {
                    Text(
                        text = stringResource(
                            if (tile.pending) R.string.photo_pending else R.string.photo_edit_edited_badge,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            // Editar foto já gravada (FR-219); capturas pendentes ainda não têm id salvo.
            if (onEdit != null) {
                Surface(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(bottomStart = 6.dp),
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    // Área de toque de 48 dp com o ícone pequeno (T330, FR-324):
                    // o botão de 36 dp ficava abaixo do mínimo e o dedo errava o alvo
                    // sobre a miniatura.
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.photo_edit_action),
                            tint = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
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
                    modifier = Modifier.size(48.dp),
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
                    modifier = Modifier.size(48.dp),
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
