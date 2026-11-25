package br.com.diecastcollector.feature.catalog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.PhotoAngle
import br.com.diecastcollector.feature.photoeditor.PhotoEditorDialog
import br.com.diecastcollector.feature.photoeditor.PhotoReviewDialog
import java.io.File

/** Captura em memória aguardando a decisão da revisão (FR-201). */
private data class PendingCapture(
    val bytes: ByteArray,
    val fromCamera: Boolean,
) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * Captura por câmera e seleção via Photo Picker com revisão e editor integrados
 * (T045/T212 - US2, FR-020, FR-108, FR-201–FR-204).
 *
 * A foto só chega a [onPhotoBytesReceived] depois de "Salvar sem editar" ou de uma edição
 * confirmada. "Tirar novamente"/"Cancelar" descartam a captura — inclusive o temporário da
 * câmera, que antes ficava no cache do aplicativo.
 */
@Composable
public fun PhotoCaptureActions(
    selectedAngle: PhotoAngle,
    onPhotoBytesReceived: (ByteArray, PhotoAngle) -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<PendingCapture?>(null) }
    var editing by remember { mutableStateOf<PendingCapture?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    // Launcher do Photo Picker do Android (não exige permissões especiais)
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
            if (bytes != null && bytes.isNotEmpty()) {
                pending = PendingCapture(bytes, fromCamera = false)
            }
        }
    }

    // Launcher da Câmera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success: Boolean ->
        val uri = tempCameraUri
        if (success && uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { stream -> stream.readBytes() }
            if (bytes != null && bytes.isNotEmpty()) {
                pending = PendingCapture(bytes, fromCamera = true)
            }
        }
        // O temporário existe só para a câmera gravar: os bytes já estão em memória.
        tempCameraFile?.delete()
        tempCameraFile = null
        tempCameraUri = null
    }

    // Solicitação de permissão de câmera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            val temp = createTempImageFile(context)
            tempCameraFile = temp
            tempCameraUri = uriFor(context, temp)
            cameraLauncher.launch(uriFor(context, temp))
        } else {
            onPermissionDenied()
        }
    }

    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val temp = createTempImageFile(context)
            tempCameraFile = temp
            tempCameraUri = uriFor(context, temp)
            cameraLauncher.launch(uriFor(context, temp))
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchGallery() {
        pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val galleryLabel = stringResource(R.string.photo_gallery)
    val cameraLabel = stringResource(R.string.photo_camera)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // T370/FR-315: cada botão precisa do rótulo medido na fonte atual mais o padding
        // interno. Se os dois não couberem lado a lado, eles empilham em vez de cortar o
        // texto — em 360 dp com a fonte em 200% os pesos de 1f comeriam o rótulo.
        val measurer = rememberTextMeasurer()
        val textStyle = MaterialTheme.typography.labelLarge
        val density = LocalDensity.current
        val innerPadding = with(density) { PhotoButtonPadding.toPx() }
        val spacing = with(density) { PhotoButtonSpacing.toPx() }
        val required = measurer.measure(galleryLabel, textStyle).size.width +
            measurer.measure(cameraLabel, textStyle).size.width +
            2 * innerPadding + spacing

        if (required > constraints.maxWidth) {
            Column(verticalArrangement = Arrangement.spacedBy(PhotoButtonSpacing)) {
                GalleryButton(modifier = Modifier.fillMaxWidth(), onClick = { launchGallery() })
                CameraButton(modifier = Modifier.fillMaxWidth(), onClick = { launchCamera() })
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PhotoButtonSpacing),
            ) {
                GalleryButton(modifier = Modifier.weight(1f), onClick = { launchGallery() })
                CameraButton(modifier = Modifier.weight(1f), onClick = { launchCamera() })
            }
        }
    }

    pending?.let { capture ->
        PhotoReviewDialog(
            imageBytes = capture.bytes,
            fromCamera = capture.fromCamera,
            onEdit = {
                editing = capture
                pending = null
            },
            onSave = {
                pending = null
                onPhotoBytesReceived(capture.bytes, selectedAngle)
            },
            onRetake = {
                pending = null
                if (capture.fromCamera) launchCamera() else launchGallery()
            },
            onCancel = { pending = null },
        )
    }

    editing?.let { capture ->
        PhotoEditorDialog(
            sourceKey = "capture-${capture.bytes.contentHashCode()}",
            sourceBytes = capture.bytes,
            onDismiss = { editing = null },
            onSaved = { bytes, edit ->
                editing = null
                // Sem alterações, grava os bytes originais: nenhuma recompressão à toa.
                onPhotoBytesReceived(if (edit.isNeutral) capture.bytes else bytes, selectedAngle)
            },
        )
    }
}

private val PhotoButtonSpacing = 8.dp

/**
 * Folga horizontal de cada botão de captura, usada só na medição de [PhotoCaptureActions].
 *
 * O padding real vem do `Button` do Material 3; o valor aqui é a reserva mínima de cada lado
 * para o rótulo não encostar na borda.
 */
private val PhotoButtonPadding = 48.dp

@Composable
private fun GalleryButton(modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier) {
        Text(stringResource(R.string.photo_gallery))
    }
}

@Composable
private fun CameraButton(modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Text(stringResource(R.string.photo_camera))
    }
}

private fun createTempImageFile(context: Context): File =
    File.createTempFile("camera_capture_", ".jpg", context.cacheDir)

private fun uriFor(context: Context, file: File): Uri =
    FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
