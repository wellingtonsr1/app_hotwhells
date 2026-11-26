package br.com.diecastcollector.feature.photoeditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.NormalizedRect
import br.com.diecastcollector.domain.model.PhotoAspect
import br.com.diecastcollector.domain.model.PhotoEdit
import br.com.diecastcollector.domain.model.PhotoQuality
import br.com.diecastcollector.ui.UiMessage
import kotlin.math.abs
import kotlinx.coroutines.delay

/**
 * Editor de fotografia em diálogo de tela cheia (ADR-004).
 *
 * Diálogo, e não rota: a grade de fotos aparece dentro de formulários com rascunho não
 * salvo (variante/exemplar novos). Transportar esse rascunho para outra rota perderia
 * campos digitados (Art. IV.7).
 */
@Composable
public fun PhotoEditorDialog(
    sourceKey: String,
    sourceBytes: ByteArray,
    initialEdit: PhotoEdit = PhotoEdit.NONE,
    alreadyEdited: Boolean = false,
    onDismiss: () -> Unit,
    onSaved: (ByteArray, PhotoEdit) -> Unit,
    onRestoreOriginal: (() -> Unit)? = null,
    viewModel: PhotoEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }

    LaunchedEffect(sourceKey) {
        viewModel.start(
            sourceKey = sourceKey,
            sourceBytes = sourceBytes,
            initialEdit = initialEdit,
            alreadyEdited = alreadyEdited,
        )
    }

    LaunchedEffect(state.saveResult) {
        val result = state.saveResult ?: return@LaunchedEffect
        viewModel.consumeSaveResult()
        onSaved(result.bytes, result.edit)
        viewModel.reset()
    }

    fun dismiss() {
        viewModel.reset()
        onDismiss()
    }

    Dialog(
        onDismissRequest = {
            if (state.hasChanges) confirmDiscard = true else dismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // Sem isto a janela do diálogo não reporta insets: com edge-to-edge
            // obrigatório (targetSdk 35+) o rodapé ficava sob a barra de navegação e os
            // botões "Restaurar ajustes" não recebiam toque.
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PhotoEditorContent(
                    state = state,
                    onClose = { if (state.hasChanges) confirmDiscard = true else dismiss() },
                    onTool = viewModel::onTool,
                    onAspect = viewModel::onAspect,
                    onCrop = viewModel::onCrop,
                    onRotateLeft = viewModel::onRotateLeft,
                    onRotateRight = viewModel::onRotateRight,
                    onFlip = viewModel::onFlipHorizontal,
                    onStraighten = viewModel::onStraighten,
                    onBrightness = viewModel::onBrightness,
                    onContrast = viewModel::onContrast,
                    onSaturation = viewModel::onSaturation,
                    onSharpness = viewModel::onSharpness,
                    onQuality = viewModel::onQuality,
                    onUndo = viewModel::onUndo,
                    onRedo = viewModel::onRedo,
                    onResetAll = viewModel::onResetAll,
                    onShowOriginal = viewModel::onShowOriginal,
                    onSave = viewModel::save,
                    onRestoreOriginal = if (alreadyEdited && onRestoreOriginal != null) {
                        { confirmRestore = true }
                    } else {
                        null
                    },
                )

                state.message?.let { message ->
                    EditorMessage(
                        message = message,
                        onShown = viewModel::onMessageShown,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }

        if (confirmDiscard) {
            AlertDialog(
                onDismissRequest = { confirmDiscard = false },
                title = { Text(stringResource(R.string.photo_editor_discard_title)) },
                text = { Text(stringResource(R.string.photo_editor_discard_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmDiscard = false
                            dismiss()
                        },
                    ) {
                        Text(stringResource(R.string.photo_editor_discard_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDiscard = false }) {
                        Text(stringResource(R.string.photo_editor_discard_keep))
                    }
                },
            )
        }

        if (confirmRestore) {
            AlertDialog(
                onDismissRequest = { confirmRestore = false },
                title = { Text(stringResource(R.string.photo_editor_restore_title)) },
                text = { Text(stringResource(R.string.photo_editor_restore_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmRestore = false
                            onRestoreOriginal?.invoke()
                            dismiss()
                        },
                    ) {
                        Text(stringResource(R.string.photo_editor_restore_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmRestore = false }) {
                        Text(stringResource(R.string.photo_editor_discard_keep))
                    }
                },
            )
        }
    }
}

@Composable
private fun PhotoEditorContent(
    state: PhotoEditorUiState,
    onClose: () -> Unit,
    onTool: (PhotoTool) -> Unit,
    onAspect: (PhotoAspect) -> Unit,
    onCrop: (NormalizedRect) -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onFlip: () -> Unit,
    onStraighten: (Float) -> Unit,
    onBrightness: (Float) -> Unit,
    onContrast: (Float) -> Unit,
    onSaturation: (Float) -> Unit,
    onSharpness: (Float) -> Unit,
    onQuality: (PhotoQuality) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onResetAll: () -> Unit,
    onShowOriginal: (Boolean) -> Unit,
    onSave: () -> Unit,
    onRestoreOriginal: (() -> Unit)?,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .then(dialogInsetsPadding()),
    ) {
        // Altura do painel a partir do espaço real do diálogo, já sem os insets (T351):
        // em paisagem a janela tem ~360 dp de altura e 40% da *tela* podia passar do
        // próprio diálogo, empurrando o rodapé para fora.
        val panelMaxHeight = maxHeight * 0.40f
        Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.photo_editor_close),
                )
            }
            Text(
                text = stringResource(R.string.photo_editor_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onUndo, enabled = state.canUndo) {
                Icon(
                    imageVector = Icons.Filled.Undo,
                    contentDescription = stringResource(R.string.photo_editor_undo),
                )
            }
            IconButton(onClick = onRedo, enabled = state.canRedo) {
                Icon(
                    imageVector = Icons.Filled.Redo,
                    contentDescription = stringResource(R.string.photo_editor_redo),
                )
            }
            TextButton(onClick = onSave, enabled = !state.saving && state.started) {
                if (state.saving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.photo_editor_save))
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // Preto fixo, não token de tema: é a área neutra de edição, onde a cor
                // percebida da foto não pode depender da aparência escolhida (FR-419).
                .background(Color.Black),
        ) {
            val preview = state.displayedPreview
            when {
                preview == null || !state.started -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                state.tool == PhotoTool.CROP && !state.showOriginal -> CropStage(
                    bitmap = preview,
                    crop = state.edit.crop,
                    onCropChange = onCrop,
                    modifier = Modifier.fillMaxSize(),
                )

                else -> ZoomablePreview(
                    bitmap = preview,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (state.processing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                )
            }

            if (state.showOriginal) {
                Surface(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.photo_editor_original),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            } else if (state.history.undo.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.photo_editor_edited),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            if (preview != null && !state.showOriginal) {
                CompareChip(
                    onShowOriginal = onShowOriginal,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp),
                )
            }
        }

        ToolTabs(selected = state.tool, onTool = onTool)

        // O rodapé é ancorado embaixo: apertar o espaçamento interno ganha altura para a
        // prévia, e a folga extra no fim é o que afasta "Restaurar ajustes" da barra de
        // navegação do sistema (encostada ali, o toque caía nos botões do aparelho).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = PANEL_BOTTOM_MARGIN),
        ) {
            // O painel cresce conforme a ferramenta ("Cor" tem três controles): limitado e
            // rolável, ele nunca empurra o rodapé para debaixo da barra do sistema.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = panelMaxHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                when (state.tool) {
                    PhotoTool.CROP -> CropPanel(
                        aspect = state.edit.aspect,
                        onAspect = onAspect,
                    )

                    PhotoTool.ORIENT -> OrientPanel(
                        edit = state.edit,
                        onRotateLeft = onRotateLeft,
                        onRotateRight = onRotateRight,
                        onFlip = onFlip,
                        onStraighten = onStraighten,
                    )

                    PhotoTool.ADJUST -> AdjustPanel(
                        edit = state.edit,
                        onBrightness = onBrightness,
                        onContrast = onContrast,
                        onSaturation = onSaturation,
                    )

                    PhotoTool.SHARPEN -> SharpenPanel(
                        edit = state.edit,
                        onSharpness = onSharpness,
                        onQuality = onQuality,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onResetAll, enabled = !state.edit.isNeutral) {
                    Text(stringResource(R.string.photo_editor_reset))
                }
                Spacer(modifier = Modifier.weight(1f))
                if (onRestoreOriginal != null) {
                    TextButton(onClick = onRestoreOriginal) {
                        Text(stringResource(R.string.photo_editor_restore_original))
                    }
                }
            }
        }
        }
    }
}

// ---- Ferramentas --------------------------------------------------------------------

@Composable
private fun ToolTabs(selected: PhotoTool, onTool: (PhotoTool) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PhotoTool.entries.forEach { tool ->
            FilterChip(
                selected = selected == tool,
                onClick = { onTool(tool) },
                label = { Text(stringResource(tool.labelRes())) },
            )
        }
    }
}

@Composable
private fun CropPanel(
    aspect: PhotoAspect,
    onAspect: (PhotoAspect) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PhotoAspect.entries.forEach { item ->
            FilterChip(
                selected = aspect == item,
                onClick = { onAspect(item) },
                label = { Text(stringResource(item.labelRes())) },
            )
        }
    }
}

@Composable
private fun OrientPanel(
    edit: PhotoEdit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onFlip: () -> Unit,
    onStraighten: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onRotateLeft, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.Filled.RotateLeft,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(stringResource(R.string.photo_editor_rotate_left), maxLines = 1)
        }
        OutlinedButton(onClick = onRotateRight, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.Filled.RotateRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(stringResource(R.string.photo_editor_rotate_right), maxLines = 1)
        }
        OutlinedButton(onClick = onFlip, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.Filled.Flip,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(stringResource(R.string.photo_editor_flip), maxLines = 1)
        }
    }
    AdjustmentSlider(
        label = stringResource(R.string.photo_editor_straighten),
        value = edit.straightenDegrees,
        range = -PhotoEdit.MAX_STRAIGHTEN..PhotoEdit.MAX_STRAIGHTEN,
        onChange = onStraighten,
        onReset = { onStraighten(0f) },
    )
}

@Composable
private fun AdjustPanel(
    edit: PhotoEdit,
    onBrightness: (Float) -> Unit,
    onContrast: (Float) -> Unit,
    onSaturation: (Float) -> Unit,
) {
    AdjustmentSlider(
        label = stringResource(R.string.photo_editor_brightness),
        value = edit.brightness,
        range = -1f..1f,
        onChange = onBrightness,
        onReset = { onBrightness(0f) },
    )
    AdjustmentSlider(
        label = stringResource(R.string.photo_editor_contrast),
        value = edit.contrast,
        range = -1f..1f,
        onChange = onContrast,
        onReset = { onContrast(0f) },
    )
    AdjustmentSlider(
        label = stringResource(R.string.photo_editor_saturation),
        value = edit.saturation,
        range = -1f..1f,
        onChange = onSaturation,
        onReset = { onSaturation(0f) },
    )
}

@Composable
private fun SharpenPanel(
    edit: PhotoEdit,
    onSharpness: (Float) -> Unit,
    onQuality: (PhotoQuality) -> Unit,
) {
    AdjustmentSlider(
        label = stringResource(R.string.photo_editor_sharpness),
        value = edit.sharpness,
        range = 0f..1f,
        onChange = onSharpness,
        onReset = { onSharpness(0f) },
    )
    Text(
        text = stringResource(R.string.photo_editor_quality_title),
        style = MaterialTheme.typography.labelLarge,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PhotoQuality.entries.forEach { quality ->
            FilterChip(
                selected = edit.quality == quality,
                onClick = { onQuality(quality) },
                label = { Text(stringResource(quality.labelRes())) },
            )
        }
    }
    Text(
        text = stringResource(R.string.photo_editor_quality_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatValue(value),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = onReset,
                enabled = abs(value) > 0.001f,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.RestartAlt,
                    contentDescription = stringResource(R.string.photo_editor_reset_control),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---- Área de prévia -----------------------------------------------------------------

@Composable
private fun ZoomablePreview(bitmap: ImageBitmap, modifier: Modifier = Modifier) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val next = (scale * zoomChange).coerceIn(1f, 5f)
        scale = next
        offset = if (next <= 1.001f) Offset.Zero else offset + panChange
    }

    Box(modifier = modifier) {
        Image(
            bitmap = bitmap,
            contentDescription = stringResource(R.string.photo_editor_edited),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .transformable(transformState),
        )
        if (scale > 1.01f) {
            IconButton(
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Fullscreen,
                    contentDescription = stringResource(R.string.photo_editor_adapt_view),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun CompareChip(onShowOriginal: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    onShowOriginal(true)
                    tryAwaitRelease()
                    onShowOriginal(false)
                },
            )
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = null,
                // Sobre a película preta do comparador: branco fixo (BR-406).
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = stringResource(R.string.photo_editor_compare),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }
    }
}

private enum class CropDrag { MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/**
 * Imagem inteira com a moldura de corte por cima (FR-205).
 *
 * Arrastar um canto redimensiona; arrastar dentro move. A moldura é normalizada no
 * domínio; aqui só se converte pixel ↔ 0..1. Nada de processar bitmap enquanto o dedo se
 * move — o retângulo muda e a exportação acontece ao salvar.
 */
@Composable
private fun CropStage(
    bitmap: ImageBitmap,
    crop: NormalizedRect,
    onCropChange: (NormalizedRect) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraintsCompat(modifier = modifier) { containerWidth, containerHeight ->
        val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val containerAspect = if (containerHeight <= 0f) 1f else containerWidth / containerHeight
        val imageWidth = if (imageAspect > containerAspect) containerWidth else containerHeight * imageAspect
        val imageHeight = if (imageWidth <= 0f) 0f else imageWidth / imageAspect
        val imageLeft = (containerWidth - imageWidth) / 2f
        val imageTop = (containerHeight - imageHeight) / 2f

        Image(
            bitmap = bitmap,
            contentDescription = stringResource(R.string.photo_editor_tool_crop),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        val density = LocalDensity.current
        val handleRadius = with(density) { 9.dp.toPx() }
        val hitRadius = with(density) { 32.dp.toPx() }

        // O gesto não pode depender de `crop` como chave do `pointerInput`: cada frame do
        // arrasto altera a moldura, reiniciaria o detector e o toque morreria no primeiro
        // pixel. A leitura do valor atual fica por conta de `rememberUpdatedState`.
        val currentCrop by rememberUpdatedState(crop)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bitmap, imageLeft, imageTop, imageWidth, imageHeight) {
                    var mode: CropDrag? = null
                    var startCrop = currentCrop
                    var accumulated = Offset.Zero

                    fun point(x: Float, y: Float) = Offset(
                        imageLeft + x * imageWidth,
                        imageTop + y * imageHeight,
                    )

                    detectDragGestures(
                        onDragStart = { position ->
                            val activeCrop = currentCrop
                            startCrop = activeCrop
                            accumulated = Offset.Zero
                            val corners = listOf(
                                CropDrag.TOP_LEFT to point(activeCrop.left, activeCrop.top),
                                CropDrag.TOP_RIGHT to point(activeCrop.right, activeCrop.top),
                                CropDrag.BOTTOM_LEFT to point(activeCrop.left, activeCrop.bottom),
                                CropDrag.BOTTOM_RIGHT to point(activeCrop.right, activeCrop.bottom),
                            )
                            mode = corners
                                .firstOrNull { (_, corner) -> (position - corner).getDistance() <= hitRadius }
                                ?.first
                                ?: if (
                                    position.x >= imageLeft + activeCrop.left * imageWidth &&
                                    position.x <= imageLeft + activeCrop.right * imageWidth &&
                                    position.y >= imageTop + activeCrop.top * imageHeight &&
                                    position.y <= imageTop + activeCrop.bottom * imageHeight
                                ) {
                                    CropDrag.MOVE
                                } else {
                                    null
                                }
                        },
                        onDragEnd = { mode = null },
                        onDragCancel = { mode = null },
                        onDrag = { change, dragAmount ->
                            val active = mode ?: return@detectDragGestures
                            change.consume()
                            accumulated += dragAmount
                            if (imageWidth <= 0f || imageHeight <= 0f) return@detectDragGestures
                            val dx = accumulated.x / imageWidth
                            val dy = accumulated.y / imageHeight

                            val next = when (active) {
                                CropDrag.MOVE -> {
                                    val width = startCrop.width
                                    val height = startCrop.height
                                    val newLeft = (startCrop.left + dx).coerceIn(0f, 1f - width)
                                    val newTop = (startCrop.top + dy).coerceIn(0f, 1f - height)
                                    NormalizedRect.safe(
                                        newLeft,
                                        newTop,
                                        newLeft + width,
                                        newTop + height,
                                    )
                                }

                                CropDrag.TOP_LEFT -> NormalizedRect.safe(
                                    startCrop.left + dx,
                                    startCrop.top + dy,
                                    startCrop.right,
                                    startCrop.bottom,
                                )

                                CropDrag.TOP_RIGHT -> NormalizedRect.safe(
                                    startCrop.left,
                                    startCrop.top + dy,
                                    startCrop.right + dx,
                                    startCrop.bottom,
                                )

                                CropDrag.BOTTOM_LEFT -> NormalizedRect.safe(
                                    startCrop.left + dx,
                                    startCrop.top,
                                    startCrop.right,
                                    startCrop.bottom + dy,
                                )

                                CropDrag.BOTTOM_RIGHT -> NormalizedRect.safe(
                                    startCrop.left,
                                    startCrop.top,
                                    startCrop.right + dx,
                                    startCrop.bottom + dy,
                                )
                            }
                            onCropChange(next)
                        },
                    )
                },
        ) {
            if (imageWidth <= 0f || imageHeight <= 0f) return@Canvas
            val left = imageLeft + crop.left * imageWidth
            val top = imageTop + crop.top * imageHeight
            val right = imageLeft + crop.right * imageWidth
            val bottom = imageTop + crop.bottom * imageHeight
            // Preto fixo + branco fixo: moldura de recorte é cor de legibilidade sobre a
            // imagem, não superfície de tema — vale igual nos dois modos (BR-406).
            val scrim = Color.Black.copy(alpha = 0.55f)

            // Escurece só o que fica fora da moldura, dentro da imagem.
            drawRect(scrim, Offset(imageLeft, imageTop), Size(imageWidth, (top - imageTop).coerceAtLeast(0f)))
            drawRect(scrim, Offset(imageLeft, bottom), Size(imageWidth, (imageTop + imageHeight - bottom).coerceAtLeast(0f)))
            drawRect(scrim, Offset(imageLeft, top), Size((left - imageLeft).coerceAtLeast(0f), bottom - top))
            drawRect(scrim, Offset(right, top), Size((imageLeft + imageWidth - right).coerceAtLeast(0f), bottom - top))

            drawRect(
                color = Color.White,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
            )

            // Linhas de terços para ajudar o enquadramento.
            val third = Color.White.copy(alpha = 0.35f)
            for (index in 1..2) {
                val x = left + (right - left) * index / 3f
                val y = top + (bottom - top) * index / 3f
                drawLine(third, Offset(x, top), Offset(x, bottom), strokeWidth = 1.dp.toPx())
                drawLine(third, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
            }

            listOf(
                Offset(left, top),
                Offset(right, top),
                Offset(left, bottom),
                Offset(right, bottom),
            ).forEach { corner ->
                drawCircle(Color.White, radius = handleRadius, center = corner)
                drawCircle(Color.Black.copy(alpha = 0.4f), radius = handleRadius / 2f, center = corner)
            }
        }
    }
}

/**
 * Insets do sistema para o diálogo.
 *
 * O `Dialog` do Compose cria uma janela própria, e nem sempre essa janela reporta os
 * insets: em alguns aparelhos o inset inferior chega como 0 (o rodapé ficava sob a barra de
 * navegação, sem receber toque) enquanto o topo vem certo. Por isso são três leituras
 * independentes, e vale a maior por lado:
 *
 * 1. `WindowInsets.safeDrawing` da janela do diálogo (`decorFitsSystemWindows = false`);
 * 2. o `decorView` da Activity que abriu o editor;
 * 3. `WindowManager.currentWindowMetrics`, que não depende de dispatch de insets.
 *
 * Se as três reportarem zero na base, usa-se a altura canônica de `navigation_bar_height`,
 * para que o rodapé continue alcançável.
 */
@Composable
private fun dialogInsetsPadding(): Modifier {
    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val windowInsets = WindowInsets.safeDrawing.asPaddingValues()
    var activityInsets by remember { mutableStateOf(IntArray(4)) }
    val metricsInsets = remember(view) { systemBarInsetsFromWindowMetrics(view.context) }
    val navigationBarFallback = remember(view) { navigationBarHeightPx(view.context) }

    DisposableEffect(view) {
        // Reserva: a Activity que abriu o editor. É dela que vem o inset quando a janela
        // do diálogo ainda não recebeu os seus.
        val activityDecor = view.context.findActivity()?.window?.decorView
        val target = activityDecor ?: view
        fun read(systemInsets: WindowInsetsCompat?) {
            val bars = systemInsets?.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            ) ?: return
            val next = intArrayOf(bars.left, bars.top, bars.right, bars.bottom)
            if (!next.contentEquals(activityInsets)) activityInsets = next
        }
        read(ViewCompat.getRootWindowInsets(target))
        ViewCompat.setOnApplyWindowInsetsListener(target) { _, systemInsets ->
            read(systemInsets)
            systemInsets
        }
        ViewCompat.requestApplyInsets(target)
        onDispose { ViewCompat.setOnApplyWindowInsetsListener(target, null) }
    }

    fun sidePx(index: Int): Int {
        val fromWindow = when (index) {
            0 -> with(density) { windowInsets.calculateLeftPadding(layoutDirection).roundToPx() }
            1 -> with(density) { windowInsets.calculateTopPadding().roundToPx() }
            2 -> with(density) { windowInsets.calculateRightPadding(layoutDirection).roundToPx() }
            else -> with(density) { windowInsets.calculateBottomPadding().roundToPx() }
        }
        return maxOf(fromWindow, activityInsets[index], metricsInsets[index])
    }

    val bottomPx = sidePx(3).let { if (it > 0) it else navigationBarFallback }
    return Modifier.padding(
        start = with(density) { sidePx(0).toDp() },
        top = with(density) { sidePx(1).toDp() },
        end = with(density) { sidePx(2).toDp() },
        bottom = with(density) { bottomPx.toDp() },
    )
}

/**
 * Insets dos system bars pela janela, e não pelo dispatch: `getInsetsIgnoringVisibility`
 * devolve a área reservada mesmo que a barra esteja oculta naquele instante.
 */
private fun systemBarInsetsFromWindowMetrics(context: Context): IntArray {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return IntArray(4)
    val manager = context.getSystemService(WindowManager::class.java) ?: return IntArray(4)
    val insets = manager.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(
        android.view.WindowInsets.Type.systemBars() or
            android.view.WindowInsets.Type.displayCutout(),
    )
    return intArrayOf(insets.left, insets.top, insets.right, insets.bottom)
}

/** Altura canônica da barra de navegação, usada só quando nenhuma fonte reporta inset. */
private fun navigationBarHeightPx(context: Context): Int {
    val resources = context.resources
    val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (id > 0) resources.getDimensionPixelSize(id) else 0
}

/** Sobe a cadeia de contextos até a Activity que hospeda o diálogo. */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * `BoxWithConstraints` em composable separado para manter a matemática de pixel em um
 * único lugar. Devolve largura/altura em pixels.
 */
@Composable
private fun BoxWithConstraintsCompat(
    modifier: Modifier,
    content: @Composable (containerWidth: Float, containerHeight: Float) -> Unit,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        content(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
    }
}

@Composable
private fun EditorMessage(
    message: UiMessage,
    onShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(message) {
        delay(3_000)
        onShown()
    }
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
    ) {
        Text(
            text = message.text(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
        )
    }
}

// ---- Auxiliares ---------------------------------------------------------------------

private val PANEL_BOTTOM_MARGIN = 32.dp

private fun formatValue(value: Float): String {
    val percent = (value * 100).toInt()
    return if (percent > 0) "+$percent" else percent.toString()
}

private fun PhotoTool.labelRes(): Int = when (this) {
    PhotoTool.CROP -> R.string.photo_editor_tool_crop
    PhotoTool.ORIENT -> R.string.photo_editor_tool_orient
    PhotoTool.ADJUST -> R.string.photo_editor_tool_adjust
    PhotoTool.SHARPEN -> R.string.photo_editor_tool_sharpen
}

private fun PhotoAspect.labelRes(): Int = when (this) {
    PhotoAspect.FREE -> R.string.photo_editor_aspect_free
    PhotoAspect.ORIGINAL -> R.string.photo_editor_aspect_original
    PhotoAspect.SQUARE -> R.string.photo_editor_aspect_square
    PhotoAspect.PORTRAIT_4_5 -> R.string.photo_editor_aspect_portrait
    PhotoAspect.LANDSCAPE_16_9 -> R.string.photo_editor_aspect_landscape
}

private fun PhotoQuality.labelRes(): Int = when (this) {
    PhotoQuality.HIGH -> R.string.photo_editor_quality_high
    PhotoQuality.BALANCED -> R.string.photo_editor_quality_balanced
    PhotoQuality.COMPACT -> R.string.photo_editor_quality_compact
}

