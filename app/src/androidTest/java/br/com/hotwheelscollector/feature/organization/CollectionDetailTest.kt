package br.com.hotwheelscollector.feature.organization

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.hotwheelscollector.domain.model.CollectionItemStatus
import br.com.hotwheelscollector.domain.model.CollectionProgress
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme
import org.junit.Assert.assertEquals
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
            HotWheelsTheme {
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
            HotWheelsTheme {
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
            HotWheelsTheme {
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
