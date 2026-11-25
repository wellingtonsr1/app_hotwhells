package br.com.diecastcollector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.R
import coil.compose.AsyncImage
import java.io.File

/**
 * Miniatura quadrada da foto principal de um exemplar.
 *
 * O banco guarda o caminho relativo e o arquivo vive em `filesDir`; a resolução é feita
 * aqui, como no detalhe e na galeria. Sem foto, o cartão continua legível: aparece o
 * marcador neutro em vez de um espaço vazio, porque o exemplar existe mesmo sem imagem.
 *
 * Tocar na foto abre a ampliação. Quando [onOpenGallery] é informado, a ampliação ganha
 * um atalho para a galeria completa — só a lista de exemplares conhece o exemplar por trás
 * da foto, então nas demais listas o atalho não aparece.
 */
@Composable
internal fun ItemThumbnail(
    photoPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    onOpenGallery: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val path = photoPath?.takeIf { it.isNotBlank() }
    // Arquivo apagado ou corrompido não pode virar quadrado vazio: cai no marcador,
    // igual à variante que nunca teve foto.
    var loadFailed by remember(path) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            // Sem foto não há o que ampliar: o cartão segue abrindo o item.
            .then(
                if (path == null || loadFailed) {
                    Modifier
                } else {
                    Modifier.clickable { expanded = true }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            path == null || loadFailed -> Icon(
                imageVector = Icons.Filled.DirectionsCar,
                contentDescription = stringResource(R.string.catalog_thumbnail_no_photo),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size / 2),
            )

            else -> AsyncImage(
                model = File(context.filesDir, path),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = { loadFailed = true },
            )
        }
    }

    if (expanded && path != null && !loadFailed) {
        // Mesmo contêiner do visualizador da grade: `safeDrawing` mantém imagem e
        // fechar fora das barras do sistema (T350, FR-323).
        MediaDialogScaffold(onDismiss = { expanded = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { expanded = false },
            ) {
                AsyncImage(
                    model = File(context.filesDir, path),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                )

                if (onOpenGallery != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                    ) {
                        TextButton(
                            onClick = {
                                expanded = false
                                onOpenGallery()
                            },
                        ) {
                            Text(
                                text = stringResource(R.string.photo_viewer_open_gallery),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
