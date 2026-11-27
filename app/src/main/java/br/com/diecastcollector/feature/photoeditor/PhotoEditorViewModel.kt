package br.com.diecastcollector.feature.photoeditor

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.NormalizedRect
import br.com.diecastcollector.domain.model.PhotoAspect
import br.com.diecastcollector.domain.model.PhotoEdit
import br.com.diecastcollector.domain.model.PhotoQuality
import br.com.diecastcollector.data.storage.PhotoProcessor
import br.com.diecastcollector.ui.UiMessage
import br.com.diecastcollector.ui.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Resultado da exportação para quem abriu o editor decidir onde salvar. */
public data class PhotoSaved(
    val bytes: ByteArray,
    val edit: PhotoEdit,
) {
    // ByteArray em data class: igualdade por identidade é suficiente aqui.
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

public data class PhotoEditorUiState(
    val sourceKey: String = "",
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val history: PhotoEditHistory = PhotoEditHistory(),
    val tool: PhotoTool = PhotoTool.CROP,
    /** Prévia com o corte aplicado (ferramentas de cor, giro e nitidez). */
    val preview: ImageBitmap? = null,
    /** Prévia sem corte, para a moldura de enquadramento. */
    val basePreview: ImageBitmap? = null,
    /** Original sem nenhuma edição, para o comparador. */
    val originalPreview: ImageBitmap? = null,
    val showOriginal: Boolean = false,
    val processing: Boolean = false,
    val saving: Boolean = false,
    val alreadyEdited: Boolean = false,
    val started: Boolean = false,
    val message: UiMessage? = null,
    val saveResult: PhotoSaved? = null,
) {
    public val edit: PhotoEdit get() = history.current
    public val canUndo: Boolean get() = history.canUndo
    public val canRedo: Boolean get() = history.canRedo
    public val hasChanges: Boolean get() = history.hasChanges

    /** Imagem que a área de prévia deve mostrar agora. */
    public val displayedPreview: ImageBitmap?
        get() = when {
            showOriginal -> originalPreview
            tool == PhotoTool.CROP -> basePreview
            else -> preview
        }
}

/**
 * Orquestra a prévia e a exportação do editor (FR-205–FR-212).
 *
 * Prévia e exportação usam o mesmo `PhotoProcessor` (ADR-003). A prévia é recalculada
 * com debounce para um slider arrastado não disparar dezenas de processamentos; quando a
 * ferramenta é o corte, arrastar a moldura não processa pixel nenhum — só a sobreposição
 * muda.
 */
@HiltViewModel
public class PhotoEditorViewModel @Inject constructor(
    private val processor: PhotoProcessor,
) : ViewModel() {

    private val _state = MutableStateFlow(PhotoEditorUiState())
    public val state: StateFlow<PhotoEditorUiState> = _state.asStateFlow()

    private var source: ByteArray = ByteArray(0)
    private var previewJob: Job? = null
    private var suggestJob: Job? = null
    /** Cada edição do usuário invalida uma sugestão de moldura ainda em andamento. */
    private var editGeneration: Int = 0
    private var baseKey: String? = null
    private var finalKey: String? = null

    /** Carrega uma imagem no editor. Chamadas repetidas para a mesma origem são ignoradas. */
    public fun start(
        sourceKey: String,
        sourceBytes: ByteArray,
        initialEdit: PhotoEdit = PhotoEdit.NONE,
        alreadyEdited: Boolean = false,
    ) {
        if (sourceBytes.isEmpty()) {
            _state.update { it.copy(message = UiMessage(R.string.photo_editor_error_invalid)) }
            return
        }
        if (_state.value.started && _state.value.sourceKey == sourceKey) return

        previewJob?.cancel()
        suggestJob?.cancel()
        editGeneration = 0
        source = sourceBytes
        baseKey = null
        finalKey = null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            _state.update { it.copy(message = UiMessage(R.string.photo_editor_error_invalid)) }
            return
        }

        _state.value = PhotoEditorUiState(
            sourceKey = sourceKey,
            sourceWidth = bounds.outWidth,
            sourceHeight = bounds.outHeight,
            history = PhotoEditHistory.from(initialEdit),
            tool = PhotoTool.CROP,
            alreadyEdited = alreadyEdited,
            started = true,
        )
        schedulePreview(force = true)

        // A moldura começa na cartela quando a foto ainda não tem edição própria. A
        // sugestão entra como estado inicial (sem passo de desfazer) e só se o usuário
        // não tiver mexido em nada enquanto o detector rodava.
        if (!alreadyEdited && initialEdit.isNeutral) {
            launchCropSuggestion(initialEdit.rotationQuarterTurns, editGeneration)
        }
    }

    public fun onTool(tool: PhotoTool) {
        if (_state.value.tool == tool) return
        _state.update { it.copy(tool = tool) }
        schedulePreview()
    }

    // ---- Enquadramento e orientação ------------------------------------------------

    public fun onAspect(aspect: PhotoAspect) {
        updateEdit { it.withAspect(aspect, orientedWidth(), orientedHeight()) }
    }

    public fun onCrop(rect: NormalizedRect) {
        updateEdit { it.withCrop(rect, orientedWidth(), orientedHeight()) }
    }

    public fun onRotateLeft() = rotateBy(-1)

    public fun onRotateRight() = rotateBy(1)

    private fun rotateBy(delta: Int) {
        updateEdit {
            // Girar troca as dimensões: refazer o corte do zero evita uma moldura sem
            // sentido (ex.: retrato 4:5 numa imagem agora paisagem).
            it.copy(
                rotationQuarterTurns = it.rotationQuarterTurns + delta,
                crop = NormalizedRect.FULL,
                aspect = PhotoAspect.ORIGINAL,
            )
        }
        // A cartela continua na foto, agora deitada: sugere a moldura na nova orientação.
        launchCropSuggestion(_state.value.edit.rotationQuarterTurns, editGeneration)
    }

    public fun onFlipHorizontal() {
        updateEdit { it.copy(flipHorizontal = !it.flipHorizontal) }
    }

    public fun onStraighten(value: Float) {
        updateEdit { it.copy(straightenDegrees = value) }
    }

    // ---- Cor e nitidez -------------------------------------------------------------

    public fun onBrightness(value: Float) {
        updateEdit { it.copy(brightness = value) }
    }

    public fun onContrast(value: Float) {
        updateEdit { it.copy(contrast = value) }
    }

    public fun onSaturation(value: Float) {
        updateEdit { it.copy(saturation = value) }
    }

    public fun onSharpness(value: Float) {
        updateEdit { it.copy(sharpness = value) }
    }

    /** Qualidade não altera pixels no histórico: é decisão de exportação (FR-212). */
    public fun onQuality(quality: PhotoQuality) {
        _state.update { current ->
            current.copy(history = current.history.copy(current = current.edit.copy(quality = quality)))
        }
    }

    // ---- Histórico -----------------------------------------------------------------

    public fun onUndo() {
        val history = _state.value.history
        if (!history.canUndo) return
        _state.update { it.copy(history = history.undo()) }
        schedulePreview()
    }

    public fun onRedo() {
        val history = _state.value.history
        if (!history.canRedo) return
        _state.update { it.copy(history = history.redo()) }
        schedulePreview()
    }

    public fun onResetAll() {
        _state.update { it.copy(history = it.history.resetAll()) }
        schedulePreview()
    }

    // ---- Comparação, salvamento e mensagens ---------------------------------------

    public fun onShowOriginal(show: Boolean) {
        _state.update { it.copy(showOriginal = show) }
    }

    public fun save() {
        val current = _state.value
        if (current.saving || !current.started) return
        _state.update { it.copy(saving = true, message = null) }
        viewModelScope.launch {
            runCatching { processor.export(source, current.edit) }
                .onSuccess { processed ->
                    _state.update {
                        it.copy(
                            saving = false,
                            saveResult = PhotoSaved(processed.bytes, current.edit),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            saving = false,
                            message = error.toUiMessage(R.string.photo_editor_error_save),
                        )
                    }
                }
        }
    }

    public fun consumeSaveResult() = _state.update { it.copy(saveResult = null) }

    public fun onMessageShown() = _state.update { it.copy(message = null) }

    /**
     * Limpa a sessão ao fechar o editor. Sem isso, abrir a mesma foto de novo reusaria
     * ajustes antigos — o diálogo é servido por um ViewModel único da Activity.
     */
    public fun reset() {
        previewJob?.cancel()
        previewJob = null
        suggestJob?.cancel()
        suggestJob = null
        source = ByteArray(0)
        baseKey = null
        finalKey = null
        _state.value = PhotoEditorUiState()
    }

    // ---- Internos ------------------------------------------------------------------

    private fun updateEdit(transform: (PhotoEdit) -> PhotoEdit) {
        editGeneration++
        suggestJob?.cancel()
        val history = _state.value.history
        _state.update { it.copy(history = history.applyBounded(transform(history.current))) }
        schedulePreview()
    }

    /**
     * Propõe a moldura da cartela como estado inicial (sem passo de desfazer). O giro entra
     * no detector para o retângulo já sair nas coordenadas da imagem deitada; se o usuário
     * editar antes de a resposta chegar, a geração muda e a sugestão é descartada.
     */
    private fun launchCropSuggestion(rotationQuarterTurns: Int, generation: Int) {
        suggestJob?.cancel()
        suggestJob = viewModelScope.launch {
            val crop = withContext(Dispatchers.Default) {
                runCatching { processor.suggestCrop(source, rotationQuarterTurns) }.getOrNull()
            } ?: return@launch
            _state.update { current ->
                val edit = current.edit
                val stillExpected = generation == editGeneration &&
                    edit.crop.isFull &&
                    edit.rotationQuarterTurns == rotationQuarterTurns
                if (stillExpected) {
                    current.copy(history = PhotoEditHistory.from(edit.copy(crop = crop)))
                } else {
                    current
                }
            }
        }
    }

    private fun orientedWidth(): Int =
        if (_state.value.edit.rotationQuarterTurns % 2 == 0) {
            _state.value.sourceWidth
        } else {
            _state.value.sourceHeight
        }

    private fun orientedHeight(): Int =
        if (_state.value.edit.rotationQuarterTurns % 2 == 0) {
            _state.value.sourceHeight
        } else {
            _state.value.sourceWidth
        }

    /**
     * Recalcula o que estiver desatualizado. Duas prévias coexistem:
     * - `basePreview` sem corte (moldura de enquadramento);
     * - `preview` com a edição completa (demais ferramentas).
     * O original é processado uma única vez por sessão (comparador).
     */
    private fun schedulePreview(force: Boolean = false) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(PREVIEW_DEBOUNCE_MS)
            val current = _state.value
            if (!current.started) return@launch

            val isCrop = current.tool == PhotoTool.CROP
            val baseEdit = current.edit.copy(crop = NormalizedRect.FULL)
            val needsBase = isCrop && (force || baseKey != baseEdit.toParameters())
            val needsFinal = !isCrop &&
                (force || current.preview == null || finalKey != current.edit.toParameters())

            if (!needsBase && !needsFinal) return@launch

            _state.update { it.copy(processing = true) }
            try {
                if (needsBase) {
                    val base = withContext(Dispatchers.Default) {
                        processor.preview(source, baseEdit)
                    }
                    val bitmap = decode(base.bytes)
                    if (bitmap != null) {
                        baseKey = baseEdit.toParameters()
                        _state.update { it.copy(basePreview = bitmap) }
                    }
                }
                if (needsFinal) {
                    val complete = withContext(Dispatchers.Default) {
                        processor.preview(source, current.edit)
                    }
                    val bitmap = decode(complete.bytes)
                    if (bitmap != null) {
                        finalKey = current.edit.toParameters()
                        _state.update { it.copy(preview = bitmap) }
                    }
                }
                if (_state.value.originalPreview == null) {
                    val original = withContext(Dispatchers.Default) {
                        processor.preview(source, PhotoEdit.NONE)
                    }
                    val bitmap = decode(original.bytes)
                    if (bitmap != null) {
                        _state.update { it.copy(originalPreview = bitmap) }
                    }
                }
            } catch (cancel: CancellationException) {
                // Arrastar um slider cancela a prévia anterior: isso é o fluxo normal de
                // debounce, não uma falha — o aviso de erro só confundiria o usuário.
                throw cancel
            } catch (error: Throwable) {
                _state.update {
                    it.copy(message = error.toUiMessage(R.string.photo_editor_error_process))
                }
            } finally {
                _state.update { it.copy(processing = false) }
            }
        }
    }

    private suspend fun decode(bytes: ByteArray): ImageBitmap? = withContext(Dispatchers.Default) {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }

    private companion object {
        const val PREVIEW_DEBOUNCE_MS = 120L
    }
}
