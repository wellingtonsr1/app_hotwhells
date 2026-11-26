package br.com.diecastcollector.appearance

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.domain.model.PhotoAngle
import br.com.diecastcollector.domain.model.VariantSummary
import br.com.diecastcollector.feature.catalog.CatalogContent
import br.com.diecastcollector.feature.catalog.CatalogUiState
import br.com.diecastcollector.feature.catalog.PhotoGrid
import br.com.diecastcollector.feature.catalog.PhotoGridTags
import br.com.diecastcollector.feature.catalog.PhotoTile
import br.com.diecastcollector.feature.settings.SettingsScreen
import br.com.diecastcollector.ui.theme.DiecastTheme
import br.com.diecastcollector.ui.theme.ThemeMode
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Contraste renderizado (Spec 006, T416, FR-422, FR-423, SC-409).
 *
 * O contraste sai dos pixels desenhados, não dos tokens: texto sobre a imagem de teste
 * **branca** e **preta** (etiqueta de ângulo da grade), título do cartão da coleção e
 * título da seção Aparência, nos dois modos. A região amostrada é sempre a de um nó
 * semântico conhecido, com os extremos de luminância do bitmap — o antialiasing só produz
 * valores intermediários.
 *
 * **Tolerância declarada**: o mínimo exigido é 4,5:1 para texto normal; a medida aceita
 * 4,2:1 por causa do arredondamento de borda na captura. Texto grande/UI (3:1) não é o
 * caso das amostras.
 */
@RunWith(AndroidJUnit4::class)
public class RenderedContrastTest {

    @get:Rule
    public val composeRule = createComposeRule()

    private val minimoComTolerancia = 4.2

    @Test
    public fun textoDaEtiquetaDeAnguloLeSobreImagemClaraEEscura() {
        val branca = imagem("branca.png", Color.WHITE)
        val preta = imagem("preta.png", Color.BLACK)
        var fonte by mutableStateOf(branca)
        var mode by mutableStateOf(ThemeMode.LIGHT)

        composeRule.setContent {
            DiecastTheme(mode = mode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        PhotoGrid(
                            photos = listOf(PhotoTile("p0", PhotoAngle.FRONT, true, fonte)),
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

        val casos = listOf(
            "branca/Claro" to Triple(branca, ThemeMode.LIGHT, Color.WHITE),
            "branca/Escuro" to Triple(branca, ThemeMode.DARK, Color.WHITE),
            "preta/Claro" to Triple(preta, ThemeMode.LIGHT, Color.BLACK),
            "preta/Escuro" to Triple(preta, ThemeMode.DARK, Color.BLACK),
        )

        casos.forEach { (nome, caso) ->
            val (imagem, alvo, cor) = caso
            fonte = imagem
            mode = alvo
            composeRule.waitForIdle()

            // Espera a imagem estar de fato desenhada: sem isso a etiqueta seria medida
            // sobre o fundo da grade, não sobre a foto.
            composeRule.waitUntil(6_000) {
                val tile = AppearanceTestSupport.bitmapOf(
                    composeRule.onAllNodesWithTag(PhotoGridTags.TILE)[0],
                )
                val centro = tile.getPixel(tile.width / 2, tile.height / 2)
                tile.recycle()
                centro == cor
            }

            // `useUnmergedTree`: a etiqueta é fundida no cartão na árvore mesclada.
            val etiqueta = composeRule
                .onAllNodesWithTag(PhotoGridTags.ANGLE_BADGE, useUnmergedTree = true)[0]
            val medido = AppearanceTestSupport.renderedContrast(etiqueta)
            assertTrue(
                "Etiqueta sobre imagem $nome: %.2f:1 medido, mínimo %.1f:1"
                    .format(medido, minimoComTolerancia),
                medido >= minimoComTolerancia,
            )
        }
    }

    @Test
    public fun tituloDoCartaoETituloDaSecaoLeemNosDoisModos() {
        val foto = imagem("cartao.png", Color.WHITE)
        var mode by mutableStateOf(ThemeMode.LIGHT)
        var tela by mutableStateOf(0)

        composeRule.setContent {
            DiecastTheme(mode = mode) {
                when (tela) {
                    0 -> CatalogContent(
                        state = CatalogUiState(
                            variants = listOf(
                                VariantSummary(
                                    variantId = "v1",
                                    modelName = "Twin Mill",
                                    manufacturerName = "Hot Wheels",
                                    colorName = "Vermelho",
                                    releaseYear = 2024,
                                    exemplarCount = 2,
                                    primaryPhotoPath = "images/aparencia/${foto.name}",
                                ),
                            ),
                            loading = false,
                        ),
                        onVariantClick = {},
                        onOpenExemplars = {},
                        onCreateEntry = {},
                    )
                    else -> SettingsScreen(onAbout = {}, onPrivacy = {})
                }
            }
        }

        val amostras = listOf(
            "cartão" to 0,
            "seção Aparência" to 1,
        )
        listOf(ThemeMode.LIGHT, ThemeMode.DARK).forEach { alvo ->
            mode = alvo
            amostras.forEach { (nome, indice) ->
                tela = indice
                composeRule.waitForIdle()
                val no = if (indice == 0) {
                    composeRule.onNodeWithText("Twin Mill", useUnmergedTree = true)
                } else {
                    composeRule.onNodeWithText("Aparência", useUnmergedTree = true)
                }
                val medido = AppearanceTestSupport.renderedContrast(no)
                assertTrue(
                    "$nome em ${alvo.name}: %.2f:1 medido, mínimo %.1f:1"
                        .format(medido, minimoComTolerancia),
                    medido >= minimoComTolerancia,
                )
            }
        }
    }

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** Imagem chapada, determinística, no diretório de fotos do app. */
    private fun imagem(nome: String, cor: Int): File {
        val arquivo = File(File(context.filesDir, "images/aparencia"), nome)
        arquivo.parentFile?.mkdirs()
        val bitmap = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(cor)
        FileOutputStream(arquivo).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return arquivo
    }
}
