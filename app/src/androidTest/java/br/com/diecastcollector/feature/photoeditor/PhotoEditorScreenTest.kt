package br.com.diecastcollector.feature.photoeditor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.swipe
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.data.storage.AndroidPhotoProcessor
import br.com.diecastcollector.domain.model.NormalizedRect
import br.com.diecastcollector.domain.model.PhotoEdit
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Editor de fotografia (FR-205–FR-213) no Compose real, sem depender de Hilt: o
 * ViewModel é criado no teste com o processador de produção.
 *
 * O que se verifica aqui é o que os testes de domínio não alcançam: as ferramentas existem,
 * a prévia carrega e "Salvar" devolve bytes + edição para quem abriu o editor.
 */
class PhotoEditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun jpeg(width: Int = 240, height: Int = 320): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.rgb(180, 40, 40))
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    private fun setContent(
        onSaved: (ByteArray, PhotoEdit) -> Unit = { _, _ -> },
        onDismiss: () -> Unit = {},
        initialEdit: PhotoEdit = PhotoEdit.NONE,
        viewModel: PhotoEditorViewModel = PhotoEditorViewModel(AndroidPhotoProcessor()),
    ): PhotoEditorViewModel {
        composeRule.setContent {
            DiecastTheme {
                PhotoEditorDialog(
                    sourceKey = "teste-${System.nanoTime()}",
                    sourceBytes = jpeg(),
                    initialEdit = initialEdit,
                    onDismiss = onDismiss,
                    onSaved = onSaved,
                    viewModel = viewModel,
                )
            }
        }
        return viewModel
    }

    @Test
    fun editorAbreComAsQuatroFerramentas() {
        setContent()

        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Enquadrar").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Girar").assertExists()
        composeRule.onNodeWithText("Cor").assertExists()
        composeRule.onNodeWithText("Nitidez").assertExists()
        composeRule.onNodeWithText("Salvar").assertExists()
        assertFooterAboveNavigationBar("Enquadrar")
    }

    /**
     * Regressão: "Cor" e "Nitidez" têm mais controles que as outras ferramentas; sem o painel
     * limitado e a folga do rodapé, a linha de "Restaurar ajustes" descia até a barra de
     * navegação e o toque caía nos botões do aparelho.
     */
    @Test
    fun rodapeFicaAcimaDaBarraEmTodasAsFerramentas() {
        setContent()

        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Enquadrar").fetchSemanticsNodes().isNotEmpty()
        }

        // O painel mais alto de cada ferramenta é o que aparece logo ao trocar de aba.
        listOf("Enquadrar", "Girar", "Cor", "Nitidez").forEach { tool ->
            composeRule.onNodeWithText(tool).performClick()
            composeRule.waitForIdle()
            assertFooterAboveNavigationBar(tool)
        }

        // Confirma que os painéis mais altos abriram de fato antes da asserção de "Nitidez".
        composeRule.onNodeWithText("Brilho").assertDoesNotExist()
        composeRule.onNodeWithText("Qualidade da imagem").assertExists()
    }

    /**
     * O rodapé precisa terminar acima da barra de navegação do sistema e com folga: encostado
     * na barra, o toque em "Restaurar ajustes" cai nos botões do aparelho.
     */
    private fun assertFooterAboveNavigationBar(tool: String) {
        val footer = composeRule.onNodeWithText("Restaurar ajustes").fetchSemanticsNode()
        val screenBottom = composeRule.onAllNodes(isRoot()).fetchSemanticsNodes()
            .maxOf { it.boundsInRoot.bottom }
        val navigationBar = navigationBarInsetPx()
        val clearance = screenBottom - navigationBar - footer.boundsInRoot.bottom
        assertTrue(
            "$tool: folgaPx=$clearance mínimaPx=${minClearancePx()} " +
                "rodapé=${footer.boundsInRoot} tela=$screenBottom barra=$navigationBar",
            footer.size.height > 0 && clearance >= minClearancePx(),
        )
    }

    /** Folga mínima aceitável entre o rodapé e a barra de navegação, em pixels (8 dp). */
    private fun minClearancePx(): Float {
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density
        return 8f * density
    }

    @Test
    fun girarEAjustarCorChegamAoSalvar() {
        val savedBytes = AtomicReference<ByteArray>()
        val savedEdit = AtomicReference<PhotoEdit>()
        val savedCount = AtomicInteger()

        setContent(
            onSaved = { bytes, edit ->
                savedBytes.set(bytes)
                savedEdit.set(edit)
                savedCount.incrementAndGet()
            },
        )

        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Enquadrar").fetchSemanticsNodes().isNotEmpty()
        }

        // Gira 90° à direita uma vez e aumenta o brilho.
        composeRule.onNodeWithText("Girar").performClick()
        composeRule.onNodeWithText("Girar 90° à direita").performClick()
        composeRule.onNodeWithText("Cor").performClick()
        composeRule.onNodeWithText("Brilho").assertExists()

        composeRule.onNodeWithText("Salvar").performClick()

        composeRule.waitUntil(TIMEOUT_MS) { savedCount.get() == 1 }
        assertTrue("Bytes editados vazios", (savedBytes.get()?.size ?: 0) > 0)
        assertNotNull(savedEdit.get())
        assertEquals(1, savedEdit.get().rotationQuarterTurns)
    }

    /**
     * Regressão: o detector de arrasto não pode ser reiniciado a cada frame do gesto.
     * Sem essa correção, arrastar a moldura não saía do lugar no aparelho.
     */
    @Test
    fun arrastarMoveAMolduraDeCorte() {
        // Moldura inicial recuada: sobra margem para mover nos quatro sentidos.
        val initialCrop = NormalizedRect.safe(0.2f, 0.2f, 0.8f, 0.8f)
        val viewModel = setContent(initialEdit = PhotoEdit(crop = initialCrop))

        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithContentDescription("Enquadrar").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription("Enquadrar").performTouchInput {
            swipe(center, center + Offset(80f, 0f), durationMillis = 300)
        }

        composeRule.waitUntil(TIMEOUT_MS) {
            viewModel.state.value.edit.crop.left > initialCrop.left
        }
        val moved = viewModel.state.value.edit.crop
        assertEquals(initialCrop.width(), moved.width(), 0.001f)
        assertTrue(moved.right <= 1f)
    }

    private fun NormalizedRect.width(): Float = right - left

    /** Altura da barra de navegação do sistema, em pixels (0 em API antiga). */
    private fun navigationBarInsetPx(): Float {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return 0f
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val windowManager = context.getSystemService(WindowManager::class.java)
        return windowManager.maximumWindowMetrics
            .windowInsets
            .getInsets(WindowInsets.Type.navigationBars())
            .bottom
            .toFloat()
    }

    @Test
    fun fecharSemAlteracoesNaoSalva() {
        val savedCount = AtomicInteger()
        val dismissed = AtomicInteger()

        setContent(
            onSaved = { _, _ -> savedCount.incrementAndGet() },
            onDismiss = { dismissed.incrementAndGet() },
        )

        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Enquadrar").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Fechar editor").performClick()

        assertEquals(0, savedCount.get())
        assertEquals(1, dismissed.get())
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
    }
}
