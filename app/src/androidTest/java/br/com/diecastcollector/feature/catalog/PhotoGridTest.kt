package br.com.diecastcollector.feature.catalog

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.domain.model.PhotoAngle
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.ui.theme.DiecastTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Grade de fotos por ângulo e definição da principal (T046 - US2, FR-020, FR-021).
 *
 * O que se verifica aqui é o que a transação do `PhotoRepository` não pode provar: existe
 * **um cartão por foto**, e tocar na estrela de um cartão avisa exatamente aquele id — se a
 * grade trocasse a referência de cartão, o usuário trocaria a principal da foto errada.
 *
 * A troca em si (desmarcar a anterior e marcar a nova numa transação) é coberta em
 * `PhotoRepositoryTest.setPrimary altera a foto principal`, que roda no módulo de dados.
 */
class PhotoGridTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val primaryCalls = mutableListOf<String>()
    private val removedCalls = mutableListOf<String>()

    private fun setContent(
        photos: List<PhotoTile>,
        onEditPhoto: ((PhotoTile) -> Unit)? = null,
    ) {
        composeRule.setContent {
            DiecastTheme {
                PhotoGrid(
                    photos = photos,
                    onAddPhotoBytes = { _, _ -> },
                    onSetPrimary = { primaryCalls += it },
                    onRemovePhoto = { removedCalls += it },
                    onPermissionDenied = {},
                    onEditPhoto = onEditPhoto,
                )
            }
        }
    }

    private fun tile(id: String, angle: PhotoAngle, primary: Boolean) = PhotoTile(
        id = id,
        angle = angle,
        isPrimary = primary,
        source = ByteArray(16),
        pending = true,
    )

    private fun twoPhotos() = listOf(
        tile("p1", PhotoAngle.FRONT, primary = true),
        tile("p2", PhotoAngle.BACK, primary = false),
    )

    @Test
    fun gradeMostraUmCartaoPorFotoComOsDoisAngulos() {
        setContent(twoPhotos())

        val stars = composeRule.onAllNodesWithContentDescription("Definir como principal")
            .fetchSemanticsNodes()

        assertEquals(2, stars.size)
    }

    @Test
    fun definirPrincipalNaSegundaFotoAvisaOIdEscolhido() {
        setContent(twoPhotos())

        // [1] é o cartão da traseira, não a principal de hoje.
        composeRule.onAllNodesWithContentDescription("Definir como principal")[1].performClick()

        assertEquals(listOf("p2"), primaryCalls)
    }

    @Test
    fun removerFotoAvisaOIdDoCartaoTocado() {
        setContent(twoPhotos())

        composeRule.onAllNodesWithContentDescription("Remover foto")[0].performClick()

        assertEquals(listOf("p1"), removedCalls)
    }

    /**
     * T349/T352 (FR-322): a grade de fotos acompanha a largura — 2 colunas no celular,
     * 3 na janela média e 4 na expandida.
     */
    @Test
    fun gradeEm360TemDuasColunas() = assertColumns(360.dp, 2)

    @Test
    fun gradeEm700TemTresColunas() = assertColumns(700.dp, 3)

    @Test
    fun gradeEm1000TemQuatroColunas() = assertColumns(1000.dp, 4)

    /** FR-321: a proporção 3:4 das miniaturas não muda com o número de colunas. */
    @Test
    fun miniaturasMantemProporcao3Por4() {
        renderGrid(width = 700.dp)

        composeRule.onAllNodesWithTag(PhotoGridTags.TILE).fetchSemanticsNodes().forEach { tile ->
            val bounds = tile.boundsInRoot
            assertEquals(
                "Proporção ${bounds.width} × ${bounds.height}",
                4f / 3f,
                bounds.height / bounds.width,
                0.02f,
            )
        }
    }

    /** FR-021: a ordem dos ângulos é a ordem dos dados, sem reordenação pela grade. */
    @Test
    fun primeiroCartaoContinuaSendoAFrente() {
        renderGrid(width = 700.dp)

        val primeiro = composeRule.onAllNodesWithTag(PhotoGridTags.TILE)[0]
            .fetchSemanticsNode().boundsInRoot
        val frente = composeRule.onNodeWithContentDescription("Frente")
            .fetchSemanticsNode().boundsInRoot

        assertEquals(primeiro.left, frente.left, 1f)
        assertEquals(primeiro.top, frente.top, 1f)
    }

    private fun assertColumns(width: Dp, expected: Int) {
        renderGrid(width = width)

        // Só a primeira linha conta: a última linha incompleta distribui as sobras e
        // teria posições diferentes das colunas.
        val tiles = composeRule.onAllNodesWithTag(PhotoGridTags.TILE)
            .fetchSemanticsNodes()
            .map { it.boundsInRoot }
        val topo = tiles.minOf { it.top }
        val columns = tiles
            .filter { it.top == topo }
            .map { it.left }
            .distinct()
            .size

        assertEquals("Colunas em $width", expected, columns)
    }

    private fun renderGrid(width: Dp) {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width) {
                    PhotoGrid(
                        photos = listOf(
                            PhotoAngle.FRONT,
                            PhotoAngle.BACK,
                            PhotoAngle.LEFT,
                            PhotoAngle.RIGHT,
                            PhotoAngle.TOP,
                            PhotoAngle.BASE,
                        ).map { angle -> tile("p-${angle.name}", angle, primary = angle == PhotoAngle.FRONT) },
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

    /**
     * T330/T333 (FR-324, SC-304): os três botões sobre a miniatura — editar, definir
     * principal e remover — têm área de toque de pelo menos 48 × 48 dp, mesmo com o
     * ícone pequeno. Antes eram 36/32 dp e o dedo errava o alvo.
     */
    @Test
    fun botoesDaMiniaturaTemAlvoDeAoMenos48Dp() {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = 360.dp) {
                    PhotoGrid(
                        // `pending = false`: o botão de editar só existe para foto
                        // já gravada.
                        photos = twoPhotos().map { it.copy(pending = false) },
                        onAddPhotoBytes = { _, _ -> },
                        onSetPrimary = {},
                        onRemovePhoto = {},
                        onPermissionDenied = {},
                        onEditPhoto = {},
                    )
                }
            }
        }

        listOf("Editar foto", "Definir como principal", "Remover foto").forEach { label ->
            val buttons = composeRule.onAllNodesWithContentDescription(label)
                .fetchSemanticsNodes()
            assertEquals("Botões «$label»", 2, buttons.size)

            buttons.forEachIndexed { index, node ->
                val bounds = node.boundsInRoot
                assertTrue(
                    "$label [$index] com ${bounds.width} × ${bounds.height}",
                    bounds.width >= 48f * composeRule.density.density - 0.5f &&
                        bounds.height >= 48f * composeRule.density.density - 0.5f,
                )
            }
        }
    }
}
