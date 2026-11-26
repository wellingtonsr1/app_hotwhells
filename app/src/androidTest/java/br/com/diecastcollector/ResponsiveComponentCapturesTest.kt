package br.com.diecastcollector

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.data.storage.AndroidPhotoProcessor
import br.com.diecastcollector.domain.model.PhotoAngle
import br.com.diecastcollector.domain.model.RecognitionSource
import br.com.diecastcollector.domain.recognition.RecognitionField
import br.com.diecastcollector.domain.recognition.RecognitionProposal
import br.com.diecastcollector.domain.recognition.RecognitionSuggestion
import br.com.diecastcollector.feature.catalog.PhotoGrid
import br.com.diecastcollector.feature.catalog.PhotoGridTags
import br.com.diecastcollector.feature.catalog.PhotoTile
import br.com.diecastcollector.feature.organization.CollectionItemPickerContent
import br.com.diecastcollector.feature.organization.CollectionOption
import br.com.diecastcollector.feature.organization.CollectionOptionKind
import br.com.diecastcollector.feature.photoeditor.PhotoEditorDialog
import br.com.diecastcollector.feature.photoeditor.PhotoEditorViewModel
import br.com.diecastcollector.feature.photoeditor.PhotoReviewDialog
import br.com.diecastcollector.feature.recognition.RecognitionReviewContent
import br.com.diecastcollector.testing.DeviceWindow
import br.com.diecastcollector.ui.components.ItemThumbnail
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Capturas dos componentes e diálogos que dependem de estado montado (T356).
 *
 * A caminhada do `ResponsiveCapturesTest` abre as telas do app; aqui os componentes são
 * montados direto com o estado de que precisam — grade de fotos com três ângulos, editor de
 * fotografia com o processador de produção (sem Hilt, como no `PhotoEditorScreenTest`),
 * seletor de itens da coleção e a revisão pós-captura. Uma classe de janela por teste, com
 * `wm size`/`wm density` de verdade.
 */
@RunWith(AndroidJUnit4::class)
class ResponsiveComponentCapturesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val superficie = mutableStateOf(Superficie.GRADE)

    @After
    fun restaurarJanela() {
        DeviceWindow.reset()
    }

    @Test
    fun componentesCompacta() {
        capturarComponentes(classe = "c", widthDp = 411, heightDp = 914)
    }

    @Test
    fun componentesMedia() {
        capturarComponentes(classe = "m", widthDp = 700, heightDp = 900)
    }

    @Test
    fun componentesExpandida() {
        capturarComponentes(classe = "e", widthDp = 1000, heightDp = 900)
    }

    @Test
    fun componentesPaisagem() {
        capturarComponentes(classe = "p", widthDp = 640, heightDp = 360)
    }

    private enum class Superficie { GRADE, SELETOR, EDITOR, REVISAO, RECONHECIMENTO, MINIATURA }

    private fun capturarComponentes(classe: String, widthDp: Int, heightDp: Int) {
        DeviceWindow.use(widthDp = widthDp, heightDp = heightDp)

        val processador = AndroidPhotoProcessor()
        val foto = jpeg()
        val fotos = listOf(
            PhotoTile("p0", PhotoAngle.FRONT, true, foto),
            PhotoTile("p1", PhotoAngle.BACK, false, foto),
            PhotoTile("p2", PhotoAngle.DETAIL, false, foto),
        )

        composeRule.setContent {
            DiecastTheme {
                // Fundo do tema: sem ele a captura sai transparente (e o PNG preto).
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                ) {
                    when (superficie.value) {
                    // A grade cresce com as linhas: rola dentro da própria coluna.
                    Superficie.GRADE -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    ) {
                        PhotoGrid(
                            photos = fotos,
                            onAddPhotoBytes = { _, _ -> },
                            onSetPrimary = {},
                            onRemovePhoto = {},
                            onPermissionDenied = {},
                            onEditPhoto = {},
                        )
                    }
                    // O seletor já tem a própria `LazyColumn`: exige altura limitada, não rolagem
                    // externa (lista preguiçosa dentro de `verticalScroll` estoura em execução).
                    Superficie.SELETOR -> CollectionItemPickerContent(
                        options = listOf(
                            CollectionOption("variant-a", "Twin Mill · vermelho", CollectionOptionKind.VARIANT),
                            CollectionOption("variant-b", "Twin Mill · azul", CollectionOptionKind.VARIANT),
                            CollectionOption("model2", "Rodger Dodger", CollectionOptionKind.MODEL),
                        ),
                        onPick = {},
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    )
                    // Editor de fotografia (linha 30): mesmo ViewModel de produção do
                    // `PhotoEditorScreenTest`, aberto como o app abre (`Dialog`).
                    Superficie.EDITOR -> PhotoEditorDialog(
                        sourceKey = "captura-$classe",
                        sourceBytes = foto,
                        onDismiss = {},
                        onSaved = { _, _ -> },
                        viewModel = PhotoEditorViewModel(processador),
                    )
                    // Revisão pós-captura (linhas 34 e 35): as quatro ações do diálogo.
                    Superficie.REVISAO -> PhotoReviewDialog(
                        imageBytes = foto,
                        fromCamera = true,
                        onEdit = {},
                        onSave = {},
                        onRetake = {},
                        onCancel = {},
                    )
                    // Folha de reconhecimento (linha 34): seis sugestões, corpo rolável e
                    // ações fixas no rodapé.
                    Superficie.RECONHECIMENTO -> RecognitionReviewContent(
                        proposal = propostaDeReconhecimento,
                        onApply = {},
                        onDismissRequest = {},
                    )
                    // Miniatura de item (linha 32): tocar abre o visualizador com insets. Sem
                    // "Ver galeria completa": a ficha usa a miniatura sem esse atalho, e o
                    // botão sob a barra só apareceria por causa da limitação de insets do
                    // diálogo sob instrumentação.
                    Superficie.MINIATURA -> ItemThumbnail(
                        photoPath = caminhoDaMiniatura,
                        contentDescription = "Twin Mill",
                    )
                    }
                }

            }
        }
        esperar()

        // Grade de fotos (linhas 31 e 32 do checklist).
        capturar("$classe-grade-fotos")

        // Visualizador em tela cheia: o toque vai no centro da miniatura, que é onde o app
        // abre a foto (o cartão em si não tem ação de clique, o Box interno tem).
        if (tentar {
                composeRule.onAllNodesWithTag(PhotoGridTags.TILE)[0]
                    .performTouchInput { click() }
            }
        ) {
            composeRule.waitForIdle()
            esperar(900)
            capturarTela("$classe-visualizador-fotos")
            fecharDialogo()
        }

        // Seletor de itens da coleção (linha 33).
        superficie.value = Superficie.SELETOR
        esperar(600)
        capturar("$classe-seletor-itens")

        // Editor de fotografia (linha 30). Diálogo vive em outra janela: a captura tem de
        // ser a da tela inteira, senão sai a raiz da activity (branca).
        superficie.value = Superficie.EDITOR
        // `takeScreenshot` não passa pelo sincronizador do Compose: sem este `waitForIdle`
        // a captura sai um estado atrás (pegava o seletor, não o editor).
        composeRule.waitForIdle()
        esperar(2_000)
        capturarTela("$classe-editor")
        fecharDialogo()

        // Revisão pós-captura (linha 35).
        superficie.value = Superficie.REVISAO
        composeRule.waitForIdle()
        esperar(1_000)
        capturarTela("$classe-revisao-foto")
        fecharDialogo()

        // Folha de reconhecimento (linha 34).
        superficie.value = Superficie.RECONHECIMENTO
        composeRule.waitForIdle()
        esperar(800)
        capturar("$classe-reconhecimento")

        // Miniatura de item e o visualizador que ela abre (linha 32).
        superficie.value = Superficie.MINIATURA
        composeRule.waitForIdle()
        esperar(800)
        capturar("$classe-miniatura")
        if (tentar { composeRule.onNodeWithContentDescription("Twin Mill").performClick() }) {
            composeRule.waitForIdle()
            esperar(800)
            capturarTela("$classe-miniatura-visualizador")
            fecharDialogo()
        }
    }

    // ------------------------------------------------------------------------ utilidades

    private fun esperar(ms: Long = 700) {
        Thread.sleep(ms)
    }

    private fun raizes(): Int = try {
        composeRule.onAllNodes(isRoot()).fetchSemanticsNodes().size
    } catch (_: Throwable) {
        1
    }

    private fun fecharDialogo() {
        if (raizes() > 1) {
            InstrumentationRegistry.getInstrumentation()
                .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        }
        esperar(600)
    }

    private fun tentar(passo: () -> Unit): Boolean = try {
        passo()
        true
    } catch (_: Throwable) {
        false
    }

    private val propostaDeReconhecimento = RecognitionProposal.of(
        source = RecognitionSource.PHOTO,
        provider = "mlkit-photo",
        suggestions = listOf(
            RecognitionSuggestion(RecognitionField.MANUFACTURER, "Hot Wheels Originals", 0.91),
            RecognitionSuggestion(RecognitionField.MODEL, "Twin Mill III", 0.88),
            RecognitionSuggestion(RecognitionField.SERIES, "Mainline 2024", 0.75),
            RecognitionSuggestion(RecognitionField.YEAR, "2024", 0.93),
            RecognitionSuggestion(RecognitionField.VARIANT, "Azul metálico", 0.62),
            RecognitionSuggestion(RecognitionField.CODE, "HW-2024-0001", 0.66),
        ),
    )

    /** Caminho relativo de uma foto real em `filesDir`, exigido pela miniatura. */
    private val caminhoDaMiniatura: String by lazy {
        val arquivo = File(
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
            "capturas/miniatura.jpg",
        ).apply { parentFile?.mkdirs() }
        arquivo.writeBytes(jpeg())
        "capturas/miniatura.jpg"
    }

    private fun jpeg(width: Int = 240, height: Int = 320): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.rgb(180, 40, 40))
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    /**
     * Captura pela `UiAutomation`: é o único caminho que enxerga a janela do `Dialog`.
     * `captureToImage` devolve a raiz da activity, que fica em branco atrás do diálogo.
     */
    private fun capturarTela(nome: String) {
        val bitmap = try {
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        } catch (erro: Throwable) {
            println("CAPTURA-FALHOU $nome: ${erro.message}")
            return
        }
        if (bitmap == null) {
            println("CAPTURA-FALHOU $nome: tela indisponível")
            return
        }
        gravar(nome, bitmap)
        bitmap.recycle()
    }

    private fun gravar(nome: String, bitmap: Bitmap) {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = contexto.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val chaves = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "$nome.png")
                put(MediaStore.Downloads.MIME_TYPE, "image/png")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/responsive")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, chaves) ?: return
            resolver.openOutputStream(uri)?.use { saida ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, saida)
            }
        } else {
            val diretorio = File(contexto.getExternalFilesDir(null), "capturas").apply { mkdirs() }
            FileOutputStream(File(diretorio, "$nome.png")).use { saida ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, saida)
            }
        }
    }

    private fun capturar(nome: String, dialogo: Boolean = false) {
        val bitmap = try {
            if (dialogo) {
                val raizes = composeRule.onAllNodes(isRoot()).fetchSemanticsNodes()
                composeRule.onAllNodes(isRoot())[raizes.size - 1]
                    .captureToImage()
                    .asAndroidBitmap()
            } else {
                composeRule.onRoot().captureToImage().asAndroidBitmap()
            }
        } catch (erro: Throwable) {
            println("CAPTURA-FALHOU $nome: ${erro.message}")
            return
        }
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = contexto.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val chaves = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "$nome.png")
                put(MediaStore.Downloads.MIME_TYPE, "image/png")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/responsive")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, chaves)
            if (uri == null) {
                println("CAPTURA-FALHOU $nome: MediaStore recusou")
                return
            }
            resolver.openOutputStream(uri)?.use { saida ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, saida)
            }
        } else {
            val diretorio = File(contexto.getExternalFilesDir(null), "capturas").apply { mkdirs() }
            FileOutputStream(File(diretorio, "$nome.png")).use { saida ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, saida)
            }
        }
        bitmap.recycle()
    }
}
