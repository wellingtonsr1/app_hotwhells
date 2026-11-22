package br.com.diecastcollector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/**
 * Miniatura quadrada da foto principal de um exemplar.
 *
 * O banco guarda o caminho relativo e o arquivo vive em `filesDir`; a resolução é feita
 * aqui, como no detalhe e na galeria. Sem foto, o cartão continua legível: aparece o
 * marcador neutro em vez de um espaço vazio, porque o exemplar existe mesmo sem imagem.
 */
@Composable
internal fun ItemThumbnail(
    photoPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (photoPath.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Filled.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size / 2),
            )
        } else {
            AsyncImage(
                model = File(context.filesDir, photoPath),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
