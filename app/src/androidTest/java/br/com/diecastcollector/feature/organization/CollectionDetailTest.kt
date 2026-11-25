package br.com.diecastcollector.feature.organization

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.domain.model.CollectionItemStatus
import br.com.diecastcollector.domain.model.CollectionProgress
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.ui.theme.DiecastTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tela de coleção (T066, FR-052–FR-055).
 *
 * O estado vem pronto do ViewModel — o que está em teste aqui é a apresentação: o
 * percentual, a contagem de faltantes e o comando certo saindo do toque certo. O cálculo
 * em si é coberto por `CollectionProgressTest` e `CollectionRepositoryTest`.
 */
@RunWith(AndroidJUnit4::class)
public class CollectionDetailTest {

    @get:Rule
    public val compose: androidx.compose.ui.test.junit4.ComposeContentTestRule =
        createComposeRule()

    @Test
    public fun mostraPercentualContagemEFaltantes() {
        compose.setContent {
            DiecastTheme {
                CollectionDetailContent(
                    state = state(missing = listOf(missingItem("i7"), missingItem("i8"))),
                    onBack = {},
                    onEdit = {},
                    onAdd = {},
                    onToggleNonCollectible = {},
                    onRemoveItem = {},
                    onArchive = {},
                    onMessageShown = {},
                )
            }
        }

        compose.onNodeWithText("60%").assertIsDisplayed()
        compose.onNodeWithText("6 de 10 unidades").assertExists()
        compose.onNodeWithText("Faltando (2)").assertExists()
        // O item faltante aparece duas vezes de propósito: na lista de faltantes e na
        // lista completa, onde pode ser marcado como não colecionável ou removido.
        compose.onAllNodesWithText("Mattel · Modelo i7").assertCountEquals(2)
        compose.onAllNodesWithText("Mattel · Modelo i8").assertCountEquals(2)
    }

    @Test
    public fun colecaoCompletaAvisaQueNaoFaltaNada() {
        compose.setContent {
            DiecastTheme {
                CollectionDetailContent(
                    state = state(
                        owned = 10,
                        missing = emptyList(),
                    ),
                    onBack = {},
                    onEdit = {},
                    onAdd = {},
                    onToggleNonCollectible = {},
                    onRemoveItem = {},
                    onArchive = {},
                    onMessageShown = {},
                )
            }
        }

        compose.onNodeWithText("100%").assertExists()
        compose.onNodeWithText("Nada faltando: todos os itens estão na estante.").assertExists()
    }

    @Test
    public fun marcarNaoColecionavelAvisaOItemTocado() {
        var toggled: CollectionItemStatus? = null
        val owned = CollectionItemStatus(
            itemId = "i1",
            collectionId = "c1",
            variantId = "v1",
            label = "Mattel · Twin Mill · Vermelho",
            ownedExemplars = 1,
        )

        compose.setContent {
            DiecastTheme {
                CollectionDetailContent(
                    state = state(missing = emptyList(), items = listOf(owned)),
                    onBack = {},
                    onEdit = {},
                    onAdd = {},
                    onToggleNonCollectible = { toggled = it },
                    onRemoveItem = {},
                    onArchive = {},
                    onMessageShown = {},
                )
            }
        }

        compose.onAllNodesWithText("Não colecionável").onFirst().performClick()

        assertEquals("i1", toggled?.itemId)
    }

    /**
     * T318: o cabeçalho tem três ações. Em 360 dp elas continuam na linha e inteiras; em
     * 700 dp também. A verificação usa os limites da moldura de 700 dp, porque a janela do
     * emulador é mais estreita que a caixa do teste.
     */
    @Test
    public fun cabecalhoEm360MantemAsTresAcoesInteirasETocaveis() {
        var editados = 0
        renderAt(width = 360.dp, onEdit = { editados++ })

        listOf("Voltar", "Editar", "Arquivar").forEach { label ->
            compose.onNodeWithText(label).assertIsDisplayed()
        }

        compose.onNodeWithText("Editar").performClick()
        assertEquals(1, editados)
    }

    /**
     * Em 700 dp a moldura é maior que a janela do emulador (411 dp), então as ações da
     * direita não são clicáveis a partir da raiz do teste. O que se verifica aqui é que as
     * três continuam na linha e inteiras dentro da largura pedida; o toque é provado em
     * 360 dp, que usa o mesmo `TextButton`.
     */
    @Test
    public fun cabecalhoEm700MantemAsTresAcoesNaLinha() {
        renderAt(width = 700.dp)

        listOf("Voltar", "Editar", "Arquivar").forEach { label ->
            val bounds = compose.onNodeWithText(label).getUnclippedBoundsInRoot()
            assertTrue("$label saiu da moldura: ${bounds.left}..${bounds.right}", bounds.left.value >= 0f)
            assertTrue("$label saiu da moldura: ${bounds.left}..${bounds.right}", bounds.right.value <= 700f)
        }
    }

    /**
     * FR-309/FR-327: com fonte em 200% três rótulos exigem o dobro da largura. Em vez de
     * elipsar «Arquivar» em 360 dp, as ações vão para o menu de três pontos inteiras.
     */
    @Test
    public fun cabecalhoEmFonteDobradaPassaAsAcoesParaOMenu() {
        var editados = 0
        renderAt(width = 360.dp, fontScale = 2f, onEdit = { editados++ })

        compose.onNodeWithContentDescription("Mais ações").assertIsDisplayed()
        assertEquals(
            "Os rótulos deveriam estar no menu, não na linha",
            0,
            compose.onAllNodesWithText("Editar").fetchSemanticsNodes().size,
        )

        compose.onNodeWithContentDescription("Mais ações").performClick()
        compose.onNodeWithText("Editar").assertIsDisplayed()
        compose.onNodeWithText("Editar").performClick()
        assertEquals(1, editados)
    }

    /**
     * FR-304/T323: a posição da grade sobrevive ao redimensionamento (compacta → média) e
     * à recriação da activity (giro). O estado da grade é o único que esta tela guarda em
     * `rememberSaveable`; o nome da coleção e os itens vêm do ViewModel, que o Android
     * mantém na rotação.
     */
    @Test
    public fun rolagemDaColecaoSobreviveAoRedimensionarEAoGirar() {
        var width by mutableStateOf(360.dp)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width) {
                    CollectionDetailContent(
                        state = state(
                            missing = emptyList(),
                            items = (1..30).map { ownedItem(it) },
                        ),
                        onBack = {},
                        onEdit = {},
                        onAdd = {},
                        onToggleNonCollectible = {},
                        onRemoveItem = {},
                        onArchive = {},
                        onMessageShown = {},
                    )
                }
            }
        }

        // Cabeçalho, progresso, título de faltantes, aviso de vazio e título de itens
        // ocupam os índices 0–4; o último item é o 34.
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(34)
        compose.onNodeWithText("Item 30").assertIsDisplayed()

        compose.runOnIdle { width = 700.dp }
        compose.onNodeWithText("Item 30").assertExists()

        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Item 30").assertExists()
    }

    /**
     * T344 (FR-316): em paisagem compacta (~360 dp de altura) o seletor ocupa a altura
     * disponível, a lista rola até o último item e o conteúdo não passa da janela.
     */
    @Test
    public fun seletorDeItemRolaDentroDoEspacoEmPaisagemCompacta() {
        compose.setContent {
            DiecastTheme {
                WindowSizeFrame(width = 640.dp, height = 360.dp) {
                    CollectionItemPickerContent(
                        options = (1..30).map { index ->
                            CollectionOption(
                                id = "v$index",
                                label = "Variante $index",
                                kind = CollectionOptionKind.VARIANT,
                            )
                        },
                        onPick = {},
                    )
                }
            }
        }

        val list = compose.onNode(hasScrollToIndexAction())
        val bounds = list.getUnclippedBoundsInRoot()
        assertTrue("Lista passou da janela: ${bounds.bottom}", bounds.bottom.value <= 360f)

        list.performScrollToIndex(29)
        compose.onNodeWithText("Variante 30").assertIsDisplayed()
    }

    private fun renderAt(
        width: Dp,
        fontScale: Float = 1f,
        onBack: () -> Unit = {},
        onEdit: () -> Unit = {},
        onArchive: () -> Unit = {},
    ) {
        compose.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width, fontScale = fontScale) {
                    CollectionDetailContent(
                        state = state(missing = emptyList(), items = listOf(ownedItem(1))),
                        onBack = onBack,
                        onEdit = onEdit,
                        onAdd = {},
                        onToggleNonCollectible = {},
                        onRemoveItem = {},
                        onArchive = onArchive,
                        onMessageShown = {},
                    )
                }
            }
        }
    }

    private fun ownedItem(index: Int): CollectionItemStatus = CollectionItemStatus(
        itemId = "i$index",
        collectionId = "c1",
        variantId = "v$index",
        label = "Item $index",
        ownedExemplars = 1,
    )

    private fun state(
        owned: Int = 6,
        missing: List<CollectionItemStatus>,
        items: List<CollectionItemStatus> = missing,
    ): CollectionDetailUiState = CollectionDetailUiState(
        collectionId = "c1",
        loading = false,
        name = "Car Culture",
        progress = CollectionProgress(
            id = "c1",
            name = "Car Culture",
            owned = owned,
            known = 10,
            itemCount = 10,
            missing = missing,
        ),
        items = items,
        missing = missing,
    )

    private fun missingItem(id: String): CollectionItemStatus = CollectionItemStatus(
        itemId = id,
        collectionId = "c1",
        variantId = id,
        label = "Mattel · Modelo $id",
        ownedExemplars = 0,
    )
}
