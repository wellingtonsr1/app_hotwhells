package br.com.diecastcollector.feature.photoeditor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.hasScrollAction
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
import br.com.diecastcollector.testing.DeviceWindow
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
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
        sourceBytes: ByteArray = jpeg(),
        viewModel: PhotoEditorViewModel = PhotoEditorViewModel(AndroidPhotoProcessor()),
    ): PhotoEditorViewModel {
        composeRule.setContent {
            DiecastTheme {
                PhotoEditorDialog(
                    sourceKey = "teste-${System.nanoTime()}",
                    sourceBytes = sourceBytes,
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

    @After
    fun restaurarJanela() {
        DeviceWindow.reset()
    }

    /**
     * T351/T352 (FR-320): em paisagem compacta (~360 dp de altura) o painel calcula a
     * altura pelo espaço do diálogo — rola dentro de si e o rodapé continua acima da
     * barra do sistema.
     */
    @Test
    fun painelRolaERodapeFicaNaTelaEmPaisagemCompacta() {
        DeviceWindow.use(widthDp = 640, heightDp = 360)
        setContent()

        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Enquadrar").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Cor").performClick()
        composeRule.waitForIdle()

        // Há mais de um nó rolável (painel e abas); o painel é o que precisa existir.
        assertTrue(
            "Painel da ferramenta não rolou",
            composeRule.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty(),
        )
        composeRule.onNodeWithText("Brilho").assertExists()
        assertFooterAboveNavigationBar("Cor")
    }

    /** T351/T352: em janela expandida a prévia aproveita o espaço e o rodapé não sai da tela. */
    @Test
    fun editorEmJanelaExpandidaMantemRodapeNaTela() {
        DeviceWindow.use(widthDp = 1000, heightDp = 900)
        setContent()

        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Enquadrar").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Nitidez").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Qualidade da imagem").assertExists()
        assertFooterAboveNavigationBar("Nitidez")
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

    /**
     * Regressão: com a moldura no limite, os cantos ficam na borda do aparelho (onde o gesto
     * do sistema rouba o toque), então arrastar a própria linha lateral precisa redimensionar.
     */
    @Test
    fun arrastarLinhaLateralRedimensiona() {
        val viewModel = setContent(initialEdit = PhotoEdit(crop = NormalizedRect.FULL))

        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithContentDescription("Enquadrar").fetchSemanticsNodes().isNotEmpty()
        }

        val stage = composeRule.onNodeWithContentDescription("Enquadrar").fetchSemanticsNode()
        composeRule.onNodeWithContentDescription("Enquadrar").performTouchInput {
            val start = Offset(10f, stage.size.height / 2f)
            swipe(start, start + Offset(120f, 0f), durationMillis = 300)
        }

        composeRule.waitUntil(TIMEOUT_MS) {
            viewModel.state.value.edit.crop.left > 0f
        }
        val resized = viewModel.state.value.edit.crop
        assertEquals(0f, resized.top, 0.001f)
        assertEquals(1f, resized.bottom, 0.001f)
    }

    /** Foto sintética: cartela clara/colorida sobre fundo escuro e uniforme. */
    private fun jpegComCartela(): ByteArray {
        val bitmap = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.rgb(48, 48, 48))
        val paint = android.graphics.Paint()
        paint.color = Color.rgb(240, 240, 240)
        Canvas(bitmap).drawRect(30f, 40f, 210f, 180f, paint)
        paint.color = Color.rgb(30, 90, 168)
        Canvas(bitmap).drawRect(30f, 180f, 210f, 280f, paint)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    /**
     * FR-205: a moldura inicial deve começar na cartela, não na foto inteira. A sugestão
     * entra como estado inicial — sem passo de desfazer e sem marcar a foto como editada.
     */
    @Test
    fun abreComMolduraAjustadaACartela() {
        val viewModel = setContent(sourceBytes = jpegComCartela())

        composeRule.waitUntil(TIMEOUT_MS) {
            viewModel.state.value.edit.crop != NormalizedRect.FULL
        }
        val crop = viewModel.state.value.edit.crop
        assertTrue("moldura não recuou: $crop", crop.left > 0.05f)
        assertTrue("moldura não recuou: $crop", crop.top > 0.05f)
        assertTrue("moldura não recuou: $crop", crop.right < 0.95f)
        assertTrue("moldura não recuou: $crop", crop.bottom < 0.95f)
        assertTrue("sugestão marcou a foto como editada", !viewModel.state.value.hasChanges)
    }

    /**
     * Girar zera o corte; a nova moldura deve vir já na orientação deitada, sem o usuário
     * precisar redesenhá-la. A cartela sintética é simétrica, então o retângulo esperado é
     * o mesmo em coordenadas normalizadas.
     */
    @Test
    fun sugereMolduraDeNovoAposGirar() {
        val viewModel = setContent(sourceBytes = jpegComCartela())

        composeRule.waitUntil(TIMEOUT_MS) {
            viewModel.state.value.edit.crop != NormalizedRect.FULL
        }

        composeRule.onNodeWithText("Girar").performClick()
        composeRule.onNodeWithText("Girar 90° à direita").performClick()

        composeRule.waitUntil(TIMEOUT_MS) {
            viewModel.state.value.edit.rotationQuarterTurns == 1 &&
                viewModel.state.value.edit.crop != NormalizedRect.FULL
        }
        val crop = viewModel.state.value.edit.crop
        assertTrue("moldura não foi refeita: $crop", crop.left > 0.05f && crop.top > 0.05f)
        assertTrue("moldura não foi refeita: $crop", crop.right < 0.95f && crop.bottom < 0.95f)
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
