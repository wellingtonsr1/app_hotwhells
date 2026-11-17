package br.com.hotwheelscollector.feature.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.Photo
import br.com.hotwheelscollector.domain.model.PhotoAngle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

/**
 * Grade de fotografias por ângulo com indicação e definição de foto principal (T046 - US2, FR-020, FR-021).
 */
@Composable
public fun PhotoGrid(
    photos: List<Photo>,
    onAddPhotoBytes: (ByteArray, PhotoAngle) -> Unit,
    onSetPrimary: (photoId: String) -> Unit,
    onRemovePhoto: (photoId: String) -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedAngle by remember { mutableStateOf(PhotoAngle.FRONT) }

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
                    label = { Text(angle.toLabel()) },
                )
            }
        }

        // Ações de captura (Câmera ou Photo Picker)
        PhotoCaptureActions(
            selectedAngle = selectedAngle,
            onPhotoBytesReceived = onAddPhotoBytes,
            onPermissionDenied = onPermissionDenied,
        )

        // Grade de fotos capturadas
        if (photos.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(photos, key = { it.id }) { photo ->
                    PhotoItemCard(
                        photo = photo,
                        onSetPrimary = { onSetPrimary(photo.id) },
                        onRemove = { onRemovePhoto(photo.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoItemCard(
    photo: Photo,
    onSetPrimary: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val imageFile = remember(photo.path) { File(context.filesDir, photo.path) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageFile)
                    .crossfade(true)
                    .build(),
                contentDescription = photo.angle.toLabel(),
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
                    text = photo.angle.toLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }

            // Badge e botão de foto principal (FR-021)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(topLeft = 8.dp),
                    )
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onSetPrimary,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (photo.isPrimary) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = stringResource(R.string.photo_set_primary),
                        tint = if (photo.isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.inverseOnSurface,
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

private fun PhotoAngle.toLabel(): String = when (this) {
    PhotoAngle.FRONT -> "Frente"
    PhotoAngle.BACK -> "Traseira"
    PhotoAngle.LEFT -> "Lateral E"
    PhotoAngle.RIGHT -> "Lateral D"
    PhotoAngle.TOP -> "Superior"
    PhotoAngle.BASE -> "Base"
    PhotoAngle.PACKAGING -> "Embalagem"
    PhotoAngle.DETAIL -> "Detalhe"
}
