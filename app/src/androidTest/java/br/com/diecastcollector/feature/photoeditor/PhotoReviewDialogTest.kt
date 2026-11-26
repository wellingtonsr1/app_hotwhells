package br.com.diecastcollector.feature.photoeditor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.testing.DeviceWindow
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.ui.theme.DiecastTheme
import org.junit.After
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

    /**
     * T364 (FR-316): em paisagem compacta a prévia tem teto de altura e as quatro ações
     * continuam inteiras e tocáveis.
     */
    @Test
    fun quatroAcoesFicamVisiveisEmPaisagem() {
        DeviceWindow.use(widthDp = 640, heightDp = 360)
        setContent()

        listOf("Editar fotografia", "Salvar sem editar", "Tirar novamente", "Cancelar")
            .forEach { label -> composeRule.onNodeWithText(label).assertIsDisplayed() }
    }

    /**
     * T356/T364: em janela expandida (1.000 × 900 dp) a prévia 3:4 não pode empurrar as
     * ações para fora — o caso apareceu na captura `e-revisao-foto`.
     */
    @Test
    fun quatroAcoesFicamVisiveisEmJanelaExpandida() {
        DeviceWindow.use(widthDp = 1000, heightDp = 900)
        setContent()

        listOf("Editar fotografia", "Salvar sem editar", "Tirar novamente", "Cancelar")
            .forEach { label -> composeRule.onNodeWithText(label).assertIsDisplayed() }
    }

    /** T364: em 360 dp com fonte 200% as ações não se sobrepõem. */
    @Test
    fun quatroAcoesFicamVisiveisComFonteDobrada() {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = 360.dp, fontScale = 2f) {
                    PhotoReviewDialog(
                        imageBytes = ByteArray(32),
                        fromCamera = true,
                        onEdit = {},
                        onSave = {},
                        onRetake = {},
                        onCancel = {},
                    )
                }
            }
        }

        listOf("Editar fotografia", "Salvar sem editar", "Tirar novamente", "Cancelar")
            .forEach { label -> composeRule.onNodeWithText(label).assertIsDisplayed() }
    }

    @After
    fun restaurarJanela() {
        DeviceWindow.reset()
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
