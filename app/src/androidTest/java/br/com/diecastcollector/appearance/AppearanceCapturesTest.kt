package br.com.diecastcollector.appearance

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.domain.model.PhotoAngle
import br.com.diecastcollector.domain.model.VariantSummary
import br.com.diecastcollector.feature.catalog.CatalogContent
import br.com.diecastcollector.feature.catalog.CatalogUiState
import br.com.diecastcollector.feature.catalog.PhotoGrid
import br.com.diecastcollector.feature.catalog.PhotoTile
import br.com.diecastcollector.feature.dashboard.DashboardContent
import br.com.diecastcollector.feature.dashboard.DashboardUiState
import br.com.diecastcollector.feature.settings.SettingsScreen
import br.com.diecastcollector.testing.DeviceWindow
import br.com.diecastcollector.ui.theme.DiecastTheme
import br.com.diecastcollector.ui.theme.ThemeMode
import java.io.File
import java.io.FileOutputStream
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Capturas Claro × Escuro nas quatro classes de janela (Spec 006, T418, SC-407, SC-408).
 *
 * Mesmo percurso de tela da Spec 005 (painel, catálogo com e sem foto, Ajustes e grade de
 * fotos), agora em pares por modo. A janela é redimensionada de verdade (`wm size`), como
 * nas capturas responsivas. Os PNGs vão para `Download/responsive/aparencia/` pelo
 * MediaStore; o `adb pull` copia para `build/responsive/aparencia/` e o checklist fica em
 * `docs/validacao.md`.
 */
@RunWith(AndroidJUnit4::class)
public class AppearanceCapturesTest {

    @get:Rule
    public val composeRule = createComposeRule()

    private var mode by mutableStateOf(ThemeMode.LIGHT)
    private var screen by mutableStateOf(0)
    private var photo by mutableStateOf<File?>(null)

    @After
    public fun restaurarJanela() {
        DeviceWindow.reset()
    }

    @Test
    public fun capturasCompacta() {
        capturar("c", widthDp = 411, heightDp = 914)
    }

    @Test
    public fun capturasMedia() {
        capturar("m", widthDp = 700, heightDp = 900)
    }

    @Test
    public fun capturasExpandida() {
        capturar("e", widthDp = 1000, heightDp = 900)
    }

    @Test
    public fun capturasPaisagem() {
        capturar("p", widthDp = 640, heightDp = 360)
    }

    private fun capturar(classe: String, widthDp: Int, heightDp: Int) {
        DeviceWindow.use(widthDp = widthDp, heightDp = heightDp)
        photo = imagem()

        composeRule.setContent {
            DiecastTheme(mode = mode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (screen) {
                        0 -> DashboardContent(state = DashboardUiState.Ready(snapshot()))
                        1 -> CatalogContent(
                            state = CatalogUiState(variants = variantes(), loading = false),
                            onVariantClick = {},
                            onOpenExemplars = {},
                            onCreateEntry = {},
                        )
                        2 -> SettingsScreen(onAbout = {}, onPrivacy = {})
                        else -> Column(Modifier.padding(16.dp)) {
                            PhotoGrid(
                                photos = listOf(
                                    PhotoTile("p0", PhotoAngle.FRONT, true, photo!!),
                                ),
                                onAddPhotoBytes = { _, _ -> },
                                onSetPrimary = {},
                                onRemovePhoto = {},
                                onPermissionDenied = {},
                                onEditPhoto = {},
                            )
                        }
                    }
                }
            }
        }

        val telas = listOf(0 to "painel", 1 to "catalogo", 2 to "ajustes", 3 to "fotos")
        var capturadas = 0
        listOf(ThemeMode.LIGHT to "claro", ThemeMode.DARK to "escuro").forEach { (alvo, rotulo) ->
            mode = alvo
            telas.forEach { (indice, nome) ->
                screen = indice
                composeRule.waitForIdle()
                Thread.sleep(500)
                val nomeArquivo = "$classe-$rotulo-$nome"
                val bitmap = try {
                    composeRule.onRoot().captureToImage().asAndroidBitmap()
                } catch (erro: Throwable) {
                    println("CAPTURA-FALHOU $nomeArquivo: ${erro.message}")
                    return@forEach
                }
                gravar(nomeArquivo, bitmap)
                bitmap.recycle()
                capturadas++
            }
        }

        assertTrue(
            "Só $capturadas de ${telas.size * 2} capturas foram gravadas",
            capturadas == telas.size * 2,
        )
    }

    private fun snapshot(): br.com.diecastcollector.domain.model.DashboardSnapshot =
        br.com.diecastcollector.domain.model.DashboardSnapshot(
            ownedExemplars = 7,
            distinctModels = 5,
            distinctVariants = 6,
            distinctManufacturers = 3,
            distinctSeries = 2,
            activeCollections = 2,
            variantsWithDuplicates = 1,
            wantedItems = 3,
            investedCents = 123_456L,
            estimatedCents = 90_000L,
            exemplarsWithoutValuation = 2,
            latestValuationDate = "2026-06-01",
        )

    private fun variantes(): List<VariantSummary> = listOf(
        VariantSummary(
            variantId = "v1",
            modelName = "Twin Mill",
            manufacturerName = "Hot Wheels",
            colorName = "Vermelho",
            releaseYear = 2024,
            exemplarCount = 2,
            primaryPhotoPath = "images/aparencia/captura.png",
        ),
        VariantSummary(
            variantId = "v2",
            modelName = "Bone Shaker",
            manufacturerName = "Hot Wheels",
            colorName = null,
            releaseYear = null,
            exemplarCount = 0,
            primaryPhotoPath = null,
        ),
    )

    private fun imagem(): File {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val arquivo = File(contexto.filesDir, "images/aparencia/captura.png")
        arquivo.parentFile?.mkdirs()
        val bitmap = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(40, 40, 44))
        canvas.drawRect(0f, 0f, 240f, 100f, android.graphics.Paint().apply {
            color = Color.rgb(210, 40, 40)
        })
        canvas.drawRect(30f, 160f, 210f, 260f, android.graphics.Paint().apply {
            color = Color.rgb(235, 235, 225)
        })
        FileOutputStream(arquivo).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return arquivo
    }

    private fun gravar(nome: String, bitmap: Bitmap) {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = contexto.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val chaves = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "$nome.png")
                put(MediaStore.Downloads.MIME_TYPE, "image/png")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/responsive/aparencia")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, chaves)
                ?: return
            resolver.openOutputStream(uri)?.use { saida ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, saida)
            }
        } else {
            val diretorio = File(contexto.getExternalFilesDir(null), "aparencia")
                .apply { mkdirs() }
            FileOutputStream(File(diretorio, "$nome.png")).use { saida ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, saida)
            }
        }
    }
}
