package br.com.diecastcollector.feature.photoeditor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Revisão pós-captura (FR-201–FR-203): as quatro decisões existem e cada uma avisa o seu
 * canal — a foto nunca é associada sem escolha.
 */
class PhotoReviewDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        onEdit: () -> Unit = {},
        onSave: () -> Unit = {},
        onRetake: () -> Unit = {},
        onCancel: () -> Unit = {},
    ) {
        composeRule.setContent {
            DiecastTheme {
                PhotoReviewDialog(
                    imageBytes = ByteArray(32),
                    fromCamera = true,
                    onEdit = onEdit,
                    onSave = onSave,
                    onRetake = onRetake,
                    onCancel = onCancel,
                )
            }
        }
    }

    @Test
    fun mostraAsQuatroOpcoesDepoisDaCaptura() {
        setContent()

        composeRule.onNodeWithText("Usar esta fotografia?").assertExists()
        composeRule.onNodeWithText("Editar fotografia").assertExists()
        composeRule.onNodeWithText("Salvar sem editar").assertExists()
        composeRule.onNodeWithText("Tirar novamente").assertExists()
        composeRule.onNodeWithText("Cancelar").assertExists()
    }

    @Test
    fun editarAbreOCaminhoDoEditor() {
        val edits = AtomicInteger()
        setContent(onEdit = { edits.incrementAndGet() })

        composeRule.onNodeWithText("Editar fotografia").performClick()

        assertEquals(1, edits.get())
    }

    @Test
    fun salvarSemEditarNaoPassaPeloEditor() {
        val saved = AtomicInteger()
        val edits = AtomicInteger()
        setContent(onSave = { saved.incrementAndGet() }, onEdit = { edits.incrementAndGet() })

        composeRule.onNodeWithText("Salvar sem editar").performClick()

        assertEquals(1, saved.get())
        assertEquals(0, edits.get())
    }

    @Test
    fun cancelarDescartaSemSalvar() {
        val saved = AtomicInteger()
        val cancelled = AtomicInteger()
        setContent(onSave = { saved.incrementAndGet() }, onCancel = { cancelled.incrementAndGet() })

        composeRule.onNodeWithText("Cancelar").performClick()

        assertEquals(1, cancelled.get())
        assertEquals(0, saved.get())
    }
}
