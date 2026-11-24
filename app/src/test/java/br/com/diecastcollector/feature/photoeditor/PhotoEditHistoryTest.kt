package br.com.diecastcollector.feature.photoeditor

import br.com.diecastcollector.domain.model.NormalizedRect
import br.com.diecastcollector.domain.model.PhotoEdit
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Desfazer/refazer e restauração (FR-211, E-01–E-03). */
public class PhotoEditHistoryTest {

    @Test
    public fun `cada alteracao empilha e limpa o refazer`() {
        var history = PhotoEditHistory.NONE
        history = history.apply(PhotoEdit.NONE.copy(brightness = 0.2f))
        history = history.apply(history.current.copy(contrast = 0.4f))

        assertThat(history.undo).hasSize(2)
        assertThat(history.canRedo).isFalse()
        assertThat(history.canUndo).isTrue()
    }

    @Test
    public fun `desfazer e refazer percorrem a ordem correta`() {
        var history = PhotoEditHistory.NONE
        history = history.apply(PhotoEdit.NONE.copy(brightness = 0.2f))
        history = history.apply(history.current.copy(contrast = 0.4f))

        history = history.undo()
        assertThat(history.current.brightness).isEqualTo(0.2f)
        assertThat(history.current.contrast).isEqualTo(0f)

        history = history.undo()
        assertThat(history.current).isEqualTo(PhotoEdit.NONE)
        assertThat(history.canUndo).isFalse()

        history = history.redo()
        assertThat(history.current.brightness).isEqualTo(0.2f)

        history = history.redo()
        assertThat(history.current.contrast).isEqualTo(0.4f)
        assertThat(history.canRedo).isFalse()
    }

    @Test
    public fun `alteracao identica nao entra no historico`() {
        val once = PhotoEditHistory.NONE.apply(PhotoEdit.NONE.copy(saturation = 0.3f))
        val twice = once.apply(once.current)

        assertThat(twice.undo).hasSize(1)
    }

    @Test
    public fun `restaurar volta ao neutro sem apagar o desfazer`() {
        var history = PhotoEditHistory.NONE
        history = history.apply(
            PhotoEdit.NONE.copy(
                crop = NormalizedRect.safe(0.1f, 0.1f, 0.9f, 0.9f),
                sharpness = 0.5f,
            ),
        )
        history = history.resetAll()

        assertThat(history.current).isEqualTo(PhotoEdit.NONE)
        assertThat(history.canUndo).isTrue()
        assertThat(history.hasChanges).isFalse()

        history = history.undo()
        assertThat(history.current.sharpness).isEqualTo(0.5f)
    }

    @Test
    public fun `historico de fotos ja editadas nao conta como alteracao nova`() {
        val loaded = PhotoEdit.fromParameters("v1;brightness=0.4;quality=HIGH")
        val history = PhotoEditHistory.from(loaded)

        assertThat(history.current.brightness).isEqualTo(0.4f)
        assertThat(history.hasChanges).isFalse()
        assertThat(history.canUndo).isFalse()
    }

    @Test
    public fun `profundidade do historico e limitada`() {
        var history = PhotoEditHistory.NONE
        repeat(PhotoEditHistory.MAX_DEPTH + 20) { index ->
            history = history.applyBounded(
                history.current.copy(brightness = (index % 100) / 200f),
            )
        }

        assertThat(history.undo.size).isAtMost(PhotoEditHistory.MAX_DEPTH)
    }
}
