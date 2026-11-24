package br.com.diecastcollector.feature.photoeditor

import br.com.diecastcollector.domain.model.PhotoEdit

/** Seções de ferramentas do editor (FR-205–FR-212). */
public enum class PhotoTool {
    CROP,
    ORIENT,
    ADJUST,
    SHARPEN,
}

/**
 * Histórico de edição, independente de Android e de Compose (Art. II.6).
 *
 * Cada alteração empilha o estado anterior e limpa o refazer; desfazer/refazer apenas
 * trocam o topo das pilhas. "Restaurar" volta ao neutro **mantendo** o histórico
 * desfazível — o usuário pode se arrepender da restauração (FR-211).
 */
public data class PhotoEditHistory(
    public val current: PhotoEdit = PhotoEdit.NONE,
    public val undo: List<PhotoEdit> = emptyList(),
    public val redo: List<PhotoEdit> = emptyList(),
) {
    public val canUndo: Boolean get() = undo.isNotEmpty()

    public val canRedo: Boolean get() = redo.isNotEmpty()

    /** Há alteração não neutra feita nesta sessão (para confirmar o descarte). */
    public val hasChanges: Boolean
        get() = undo.isNotEmpty() && !current.isNeutral

    /** Aplica uma nova edição; ignora quando é idêntica à atual (sem lixo no histórico). */
    public fun apply(next: PhotoEdit): PhotoEditHistory {
        val normalized = next.normalized()
        if (normalized == current) return this
        return copy(
            current = normalized,
            undo = undo + current,
            redo = emptyList(),
        )
    }

    public fun undo(): PhotoEditHistory {
        val previous = undo.lastOrNull() ?: return this
        return copy(
            current = previous,
            undo = undo.dropLast(1),
            redo = redo + current,
        )
    }

    public fun redo(): PhotoEditHistory {
        val next = redo.lastOrNull() ?: return this
        return copy(
            current = next,
            undo = undo + current,
            redo = redo.dropLast(1),
        )
    }

    /** Restaura todas as ferramentas sem apagar a possibilidade de desfazer. */
    public fun resetAll(): PhotoEditHistory = apply(PhotoEdit.NONE.copy(quality = current.quality))

    public companion object {
        /** Profundidade máxima do histórico: memória previsível em sessões longas. */
        public const val MAX_DEPTH: Int = 50

        /** Sessão sem nenhuma alteração e sem histórico. */
        public val NONE: PhotoEditHistory = PhotoEditHistory()

        public fun from(edit: PhotoEdit): PhotoEditHistory = PhotoEditHistory(current = edit.normalized())
    }
}

/**
 * Aplica a edição mantendo o histórico limitado em [PhotoEditHistory.MAX_DEPTH].
 * Usado pelo ViewModel para que sliders arrastados não acumulem centenas de estados.
 */
internal fun PhotoEditHistory.applyBounded(next: PhotoEdit): PhotoEditHistory {
    val applied = apply(next)
    if (applied.undo.size <= PhotoEditHistory.MAX_DEPTH) return applied
    return applied.copy(undo = applied.undo.takeLast(PhotoEditHistory.MAX_DEPTH))
}
