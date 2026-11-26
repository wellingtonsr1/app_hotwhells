package br.com.diecastcollector.feature.photoeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    // T364/FR-316: a prévia 3:4 empurrava as quatro ações para fora do diálogo quando a
    // janela era baixa (paisagem compacta) ou larga (tablet em paisagem, em que 3:4 sobre a
    // largura do diálogo passa da altura da tela). O teto da prévia vem da altura real do
    // diálogo e o `ContentScale.Fit` mantém a proporção dentro dele.
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 3.dp,
        ) {
            BoxWithConstraints {
                val compacto = maxHeight < 480.dp
                val alturaDaPrevia = if (compacto) 64.dp else maxHeight * 0.45f

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (compacto) 4.dp else 12.dp)
                            .height(alturaDaPrevia)
                            .clip(RoundedCornerShape(10.dp)),
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
}
