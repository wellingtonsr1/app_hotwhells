package br.com.diecastcollector.feature.catalog

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.domain.model.VariantSummary
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Cartões da Coleção (spec 003): miniatura, marcador sem foto e atalho para os exemplares.
 *
 * O que precisa ficar provado é o que o usuário enxerga: a foto certa por cartão, um
 * marcador quando não há foto e o mesmo caminho de navegação de antes.
 */
class CatalogScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun carrinhoSemFotoMostraMarcadorAcessivel() {
        show(listOf(variant("v1", "Twin Mill", photo = null)))

        composeRule.onNodeWithContentDescription(NO_PHOTO).assertIsDisplayed()
    }

    @Test
    fun miniaturaDescreveModeloCorEAno() {
        val path = "images/ex-1/front.jpg"
        writeValidPhoto(path)
        show(
            listOf(
                variant(
                    id = "v1",
                    model = "Twin Mill",
                    color = "Vermelho",
                    year = 2024,
                    photo = path,
                ),
            ),
        )

        val description = "Foto de Twin Mill · Vermelho · 2024"
        composeRule.waitUntil(IMAGE_TIMEOUT_MS) {
            composeRule.onAllNodesWithContentDescription(description)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(description).assertIsDisplayed()
    }

    @Test
    fun arquivoInvalidoCaiNoMarcadorSemQuebrarOCartao() {
        val path = "images/ex-quebrado/front.jpg"
        File(context.filesDir, path).apply {
            parentFile?.mkdirs()
            writeText("isto não é uma imagem")
        }
        show(listOf(variant("v1", "Twin Mill", photo = path)))

        composeRule.waitUntil(IMAGE_TIMEOUT_MS) {
            composeRule.onAllNodesWithContentDescription(NO_PHOTO)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(NO_PHOTO).assertIsDisplayed()
        composeRule.onNodeWithText("Twin Mill").assertIsDisplayed()
    }

    @Test
    fun listaLongaNaoCompoeCartoesForaDaTela() {
        show((0 until 60).map { variant("v$it", "Modelo $it", photo = null) })

        composeRule.onNodeWithText("Modelo 0").assertIsDisplayed()
        composeRule.onNodeWithText("Modelo 59").assertDoesNotExist()

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(59)

        composeRule.onNodeWithText("Modelo 59").assertIsDisplayed()
    }

    /**
     * SC-002: amostra fixa de 20 variantes — 5 com foto principal, 5 com várias fotos sem
     * principal, 5 sem foto e 5 com foto de exemplar vendido. Cada cartão tem que exibir
     * a própria imagem, nunca a de outro modelo.
     */
    @Test
    fun amostraDoSc002MantemCadaMiniaturaNoProprioCartao() {
        val variants = mutableListOf<VariantSummary>()
        repeat(5) { i ->
            val path = "images/principal-$i/front.jpg"
            writeValidPhoto(path)
            variants += variant("p$i", "Principal $i", "Azul", 2020 + i, 1, path)
        }
        repeat(5) { i ->
            val path = "images/sem-principal-$i/front.jpg"
            writeValidPhoto(path)
            variants += variant("s$i", "SemPrincipal $i", "Verde", 2010 + i, 2, path)
        }
        repeat(5) { i ->
            variants += variant("n$i", "SemFoto $i", null, null, 1, null)
        }
        repeat(5) { i ->
            val path = "images/vendido-$i/front.jpg"
            writeValidPhoto(path)
            variants += variant("v$i", "Vendido $i", "Preto", 2000 + i, 0, path)
        }
        show(variants)

        variants.forEachIndexed { index, item ->
            composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(index)
            composeRule.onNodeWithText(item.modelName).assertIsDisplayed()

            if (item.primaryPhotoPath == null) {
                assertTrue(
                    "Cartão ${item.modelName} deveria mostrar o marcador",
                    composeRule.onAllNodesWithContentDescription(NO_PHOTO)
                        .fetchSemanticsNodes()
                        .isNotEmpty(),
                )
            } else {
                val description = "Foto de ${item.modelName} · ${item.colorName} · ${item.releaseYear}"
                composeRule.waitUntil(IMAGE_TIMEOUT_MS) {
                    composeRule.onAllNodesWithContentDescription(description)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule.onNodeWithContentDescription(description).assertExists()
            }
        }
    }

    @Test
    fun cartaoSemAnoNaoInventaCampo() {
        show(
            listOf(
                variant(
                    id = "v1",
                    model = "Bone Shaker",
                    color = null,
                    year = null,
                    count = 0,
                    photo = null,
                ),
            ),
        )

        composeRule.onNodeWithText("Hot Wheels · nenhum exemplar").assertIsDisplayed()
    }

    @Test
    fun atalhoVerExemplaresLevaAVarianteDoCartao() {
        var opened: String? = null
        show(
            variants = listOf(
                variant("v1", "Twin Mill", photo = null),
                variant("v2", "Bone Shaker", photo = null),
            ),
            onOpenExemplars = { opened = it },
        )

        composeRule.onAllNodesWithText("Ver exemplares")[1].performClick()

        assertEquals("v2", opened)
    }

    /**
     * FR-014 e o defeito 5 de `docs/validacao.md`: em fonte 200% o subtítulo quebrava
     * «2 exemplare s» no meio da palavra e a miniatura não podia ser comprimida.
     */
    @Test
    fun emFonteDobradaAMiniaturaMantem64DpESubtituloNaoParteAPalavra() {
        val subtitle = "Hot Wheels · 2024 · 2 exemplares"
        show(
            variants = listOf(
                variant("v1", "Twin Mill", color = "Vermelho", year = 2024, count = 2, photo = null),
            ),
            fontScale = 2f,
        )

        // O cartão funde a semântica dos filhos; a árvore não fundida dá o ícone do
        // marcador, que ocupa metade da miniatura — 32 dp de uma caixa de 64 dp.
        val marker = composeRule
            .onNodeWithContentDescription(NO_PHOTO, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals(
            "A miniatura encolheu com a fonte ampliada",
            32f,
            (marker.right - marker.left).value,
            0.5f,
        )

        composeRule.onNodeWithText(subtitle).assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(subtitle)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()

        assertTrue("O subtítulo passou de duas linhas: ${layout.lineCount}", layout.lineCount <= 2)
        val brokenWord = (0 until layout.lineCount - 1).any { line ->
            val end = layout.getLineEnd(line, visibleEnd = true)
            subtitle.getOrNull(end - 1)?.isLetter() == true &&
                subtitle.getOrNull(end)?.isLetter() == true
        }
        assertFalse("O subtítulo partiu uma palavra ao quebrar linha", brokenWord)
    }

    @Test
    fun cartaoComFotoContinuaAbrindoOCadastroRapido() {
        var clicked: String? = null
        show(
            variants = listOf(variant("v1", "Twin Mill", photo = "images/ex-1/front.jpg")),
            onVariantClick = { clicked = it },
        )

        // O toque no texto do cartão é o caminho que a US1 pressupõe; a foto tem a
        // ampliação como alvo próprio (decisão de 2026-11-23).
        composeRule.onNodeWithText("Twin Mill").performClick()

        assertEquals("v1", clicked)
    }

    /**
     * FR-304/T323: o acervo não tem filtro próprio (a busca é outra tela), então o estado a
     * preservar aqui é o ponto da grade. Redimensionar 360 → 700 dp e recriar a activity
     * (giro) mantêm o cartão em que o usuário estava.
     */
    @Test
    fun rolagemDoCatalogoSobreviveAoRedimensionarEAoGirar() {
        var width by mutableStateOf(360.dp)
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width) {
                    CatalogContent(
                        state = CatalogUiState(
                            variants = (0 until 40).map { variant("v$it", "Modelo $it", photo = null) },
                            loading = false,
                        ),
                        onVariantClick = {},
                        onOpenExemplars = {},
                        onCreateEntry = {},
                    )
                }
            }
        }

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(30)
        composeRule.onNodeWithText("Modelo 30").assertIsDisplayed()

        composeRule.runOnIdle { width = 700.dp }
        composeRule.onNodeWithText("Modelo 30").assertExists()

        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithText("Modelo 30").assertExists()
    }

    private fun show(
        variants: List<VariantSummary>,
        onVariantClick: (String) -> Unit = {},
        onOpenExemplars: (String) -> Unit = {},
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            // Fonte do sistema ampliada: o cartão precisa continuar legível (FR-014).
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = density.density, fontScale = fontScale),
            ) {
                DiecastTheme {
                    CatalogContent(
                        state = CatalogUiState(variants = variants, loading = false),
                        onVariantClick = onVariantClick,
                        onOpenExemplars = onOpenExemplars,
                        onCreateEntry = {},
                    )
                }
            }
        }
    }

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun writeValidPhoto(path: String) {
        val file = File(context.filesDir, path)
        file.parentFile?.mkdirs()
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun variant(
        id: String,
        model: String,
        color: String? = null,
        year: Int? = null,
        count: Int = 1,
        photo: String?,
    ): VariantSummary = VariantSummary(
        variantId = id,
        modelName = model,
        manufacturerName = "Hot Wheels",
        colorName = color,
        releaseYear = year,
        exemplarCount = count,
        primaryPhotoPath = photo,
    )

    private companion object {
        const val NO_PHOTO = "Sem foto do carrinho"
        const val IMAGE_TIMEOUT_MS = 5_000L
    }
}
