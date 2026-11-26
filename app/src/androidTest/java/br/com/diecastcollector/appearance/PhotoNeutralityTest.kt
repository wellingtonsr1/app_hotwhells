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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.domain.model.PhotoAngle
import br.com.diecastcollector.feature.catalog.PhotoGrid
import br.com.diecastcollector.feature.catalog.PhotoGridTags
import br.com.diecastcollector.feature.catalog.PhotoTile
import br.com.diecastcollector.ui.components.ItemThumbnail
import br.com.diecastcollector.ui.theme.DiecastTheme
import br.com.diecastcollector.ui.theme.ThemeMode
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * As fotos não são tingidas pela aparência (Spec 006, T424, FR-419, SC-408).
 *
 * A mesma imagem determinística é desenhada em Claro e em Escuro na grade e na miniatura do
 * cartão. A média dos canais na região central da foto precisa ficar dentro de 2/255 entre
 * os modos — nenhum véu, nenhuma mistura com o fundo do tema.
 */
@RunWith(AndroidJUnit4::class)
public class PhotoNeutralityTest {

    @get:Rule
    public val composeRule = createComposeRule()

    private var mode by mutableStateOf(ThemeMode.LIGHT)
    private var superficie by mutableStateOf(0)

    @Test
    public fun fotoNaoMudaDeCorEntreClaroEEscuro() {
        val arquivo = imagem()
        val descricao = "Foto de Twin Mill"
        val relativo = "images/aparencia/neutra.png"

        composeRule.setContent {
            DiecastTheme(mode = mode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        when (superficie) {
                            0 -> PhotoGrid(
                                photos = listOf(
                                    PhotoTile("p0", PhotoAngle.FRONT, true, arquivo),
                                ),
                                onAddPhotoBytes = { _, _ -> },
                                onSetPrimary = {},
                                onRemovePhoto = {},
                                onPermissionDenied = {},
                                onEditPhoto = {},
                            )
                            else -> ItemThumbnail(
                                photoPath = relativo,
                                contentDescription = descricao,
                                size = 120.dp,
                            )
                        }
                    }
                }
            }
        }

        val esperado = intArrayOf(70, 120, 170)
        listOf(
            "grade" to 0,
            "miniatura" to 1,
        ).forEach { (nome, indice) ->
            superficie = indice
            val medias = mutableListOf<DoubleArray>()
            listOf(ThemeMode.LIGHT, ThemeMode.DARK).forEach { alvo ->
                mode = alvo
                composeRule.waitForIdle()
                val no = if (indice == 0) {
                    composeRule.onAllNodesWithTag(PhotoGridTags.TILE)[0]
                } else {
                    composeRule.onNodeWithContentDescription(descricao)
                }
                // Garante que a imagem carregou antes de medir: o centro tem que ser a
                // cor da imagem de teste, não o fundo da superfície.
                composeRule.waitUntil(6_000) {
                    val bitmap = AppearanceTestSupport.bitmapOf(no)
                    val carregou = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2) ==
                        Color.rgb(70, 120, 170)
                    bitmap.recycle()
                    carregou
                }
                val bitmap = AppearanceTestSupport.bitmapOf(no)
                medias += try {
                    AppearanceTestSupport.meanCenterColor(bitmap)
                } finally {
                    bitmap.recycle()
                }
            }
            val diferenca = (0..2).map { abs(medias[0][it] - medias[1][it]) }
            assertTrue(
                "$nome: diferença de canal $diferenca (esperado ≤ 2/255; " +
                    "claro=${medias[0].toList()}, escuro=${medias[1].toList()}, " +
                    "referência=${esperado.toList()})",
                diferenca.all { it <= 2.0 },
            )
        }
    }

    private fun imagem(): File {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val arquivo = File(contexto.filesDir, "images/aparencia/neutra.png")
        arquivo.parentFile?.mkdirs()
        val bitmap = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.rgb(70, 120, 170))
        FileOutputStream(arquivo).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return arquivo
    }
}
