package br.com.diecastcollector.feature.photoeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.diecastcollector.R
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Revisão pós-captura (FR-201–FR-203).
 *
 * Substitui o fluxo "OK/Repetir": o usuário decide se quer editar, salvar como está, tirar
 * de novo ou cancelar — sem que a foto seja associada antes da escolha.
 */
@Composable
public fun PhotoReviewDialog(
    imageBytes: ByteArray,
    fromCamera: Boolean,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    // T364/FR-316: em paisagem compacta (~360 dp de altura) a prévia 3:4 empurrava as
    // quatro ações para fora do diálogo. A imagem passa a ter teto de altura e as ações
    // ficam inteiras; em retrato a proporção 3:4 continua.
    val landscape = LocalConfiguration.current.screenHeightDp < 480
    val imageModifier = if (landscape) {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .heightIn(max = 64.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .aspectRatio(3f / 4f)
    }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 3.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.photo_review_title),
                    style = MaterialTheme.typography.titleMedium,
                )

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageBytes)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.photo_review_title),
                    contentScale = ContentScale.Fit,
                    modifier = imageModifier.clip(RoundedCornerShape(10.dp)),
                )

                Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.photo_review_edit))
                }
                OutlinedButton(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    Text(stringResource(R.string.photo_review_save))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(onClick = onRetake, modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                if (fromCamera) R.string.photo_review_retake
                                else R.string.photo_review_pick_again,
                            ),
                        )
                    }
                    TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.photo_review_cancel))
                    }
                }
            }
        }
    }
}
