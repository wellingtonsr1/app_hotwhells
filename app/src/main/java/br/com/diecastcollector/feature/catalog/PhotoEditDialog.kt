package br.com.diecastcollector.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.Photo
import br.com.diecastcollector.domain.model.PhotoEdit
import br.com.diecastcollector.feature.photoeditor.PhotoEditorDialog
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Abre o editor para uma fotografia **já cadastrada** (FR-219).
 *
 * A base é sempre o arquivo original (`original_path`, quando existe). Os parâmetros da
 * última edição são carregados para o usuário continuar de onde parou — o original nunca é
 * reprocessado nem sobrescrito (FR-214, FR-215).
 */
@Composable
internal fun PhotoEditDialog(
    photo: Photo,
    onDismiss: () -> Unit,
    onSave: (ByteArray, PhotoEdit) -> Unit,
    onRestoreOriginal: () -> Unit,
) {
    val context = LocalContext.current
    val basePath = photo.originalPath ?: photo.path
    val sourceKey = "${photo.id}:$basePath:${photo.editParams.orEmpty()}"
    var bytes by remember(sourceKey) { mutableStateOf<ByteArray?>(null) }
    var failed by remember(sourceKey) { mutableStateOf(false) }

    LaunchedEffect(sourceKey) {
        val loaded = withContext(Dispatchers.IO) {
            runCatching { File(context.filesDir, basePath).readBytes() }.getOrNull()
        }
        bytes = loaded
        failed = loaded == null
    }

    if (failed) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.photo_editor_title)) },
            text = { Text(stringResource(R.string.photo_editor_error_invalid)) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.photo_editor_close))
                }
            },
        )
        return
    }

    val loaded = bytes
    if (loaded == null) {
        // Leitura do arquivo em andamento: diálogo próprio para não interferir no layout
        // da tela que abriu o editor.
        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.photo_editor_working),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        return
    }

    PhotoEditorDialog(
        sourceKey = sourceKey,
        sourceBytes = loaded,
        initialEdit = PhotoEdit.fromParameters(photo.editParams),
        alreadyEdited = photo.isEdited,
        onDismiss = onDismiss,
        onSaved = onSave,
        onRestoreOriginal = onRestoreOriginal,
    )
}
