package br.com.diecastcollector.feature.wishlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.model.WishlistItem
import br.com.diecastcollector.domain.model.WishlistStatus
import br.com.diecastcollector.domain.repository.WishlistEntry
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.testing.WindowSizeFrameTags
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

public class WishlistScreenTest {

    @get:Rule
    public val composeRule: androidx.compose.ui.test.junit4.ComposeContentTestRule =
        createComposeRule()

    @Test
    public fun cartaoMostraPrioridadeValoresEAcaoDeCompra() {
        val entry = WishlistEntry(
            item = WishlistItem(
                id = "w1",
                priority = 1,
                variantId = "variant1",
                maxPrice = MoneyCents(5_000),
                foundPrice = MoneyCents(4_500),
                store = "Loja do centro",
                referenceUrl = "https://exemplo.com/anuncio",
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
            title = "Mattel · Twin Mill",
        )
        var bought: WishlistEntry? = null

        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = 360.dp) {
                    WishlistContent(
                        state = WishlistUiState(items = listOf(entry), loading = false),
                        onAdd = {},
                        onEdit = {},
                        onBuy = { bought = it },
                        onDismiss = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Mattel · Twin Mill").assertIsDisplayed()
        composeRule.onNodeWithText("Alta").assertIsDisplayed()
        composeRule.onNodeWithText("Máximo R$ 50,00 · encontrado R$ 45,00").assertIsDisplayed()
        composeRule.onNodeWithText("Loja: Loja do centro").assertIsDisplayed()

        // Em 360 dp as três ações não cabem inteiras e viram menu (T342, FR-309).
        composeRule.onNodeWithContentDescription("Mais ações").performClick()
        composeRule.onNodeWithText("Abrir link").assertIsDisplayed()
        composeRule.onNodeWithText("Comprei").performClick()
        assertEquals("w1", bought?.item?.id)
    }

    /**
     * T341/T343 (FR-309): em 360 dp com fonte 200% o título não invade a ação
     * "Novo desejo" nem é cortado a uma letra.
     */
    @Test
    public fun cabecalhoMantemTituloEAcaoInteirosEm360ComFonteDobrada() {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = 360.dp, fontScale = 2f) {
                    WishlistContent(
                        state = WishlistUiState(loading = false),
                        onAdd = {},
                        onEdit = {},
                        onBuy = {},
                        onDismiss = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Novo desejo").assertIsDisplayed()
        val titulo = composeRule.onNodeWithText("Lista de desejos").getUnclippedBoundsInRoot()
        val acao = composeRule.onNodeWithText("Novo desejo").getUnclippedBoundsInRoot()
        assertTrue(
            "Título (${titulo.right}) e ação (${acao.left}) se sobrepõem",
            titulo.right.value <= acao.left.value,
        )
        assertTrue("Ação saiu da tela: ${acao.right}", acao.right.value <= 360f)
    }

    /**
     * T336/T343 (FR-311, SC-306): em 1.000 dp o formulário de desejo fica na coluna de
     * 720 dp centrada, não em um campo de 1.000 dp.
     */
    @Test
    public fun formularioRespeitaLarguraDeLeituraEmJanelaExpandida() {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = 1000.dp, height = 900.dp) {
                    WishlistFormContent(
                        state = WishlistFormUiState(),
                        actions = WishlistFormActions(),
                    )
                }
            }
        }

        val moldura = composeRule.onNodeWithTag(WindowSizeFrameTags.FRAME)
            .getUnclippedBoundsInRoot()
        val campo = composeRule.onAllNodes(hasSetTextAction())[0]
            .getUnclippedBoundsInRoot()

        assertTrue("Campo com ${campo.right - campo.left} dp", (campo.right - campo.left).value <= 720f)
        val esquerda = campo.left.value - moldura.left.value
        val direita = moldura.right.value - campo.right.value
        assertTrue("Coluna não centralizada: $esquerda x $direita", kotlin.math.abs(esquerda - direita) <= 1f)
    }

    @Test
    public fun desejoAdquiridoNaoOfereceNovaCompra() {
        val entry = WishlistEntry(
            item = WishlistItem(
                id = "w2",
                freeText = "Camaro preto",
                status = WishlistStatus.ACQUIRED,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
            title = "Camaro preto",
        )

        composeRule.setContent {
            DiecastTheme {
                WishlistContent(
                    state = WishlistUiState(items = listOf(entry), loading = false),
                    onAdd = {},
                    onEdit = {},
                    onBuy = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Adquirido").assertIsDisplayed()
        composeRule.onNodeWithText("Comprei").assertDoesNotExist()
    }

    @Test
    public fun listaVaziaOrientaOCadastro() {
        composeRule.setContent {
            DiecastTheme {
                WishlistContent(
                    state = WishlistUiState(loading = false),
                    onAdd = {},
                    onEdit = {},
                    onBuy = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Nada na lista de desejos").assertIsDisplayed()
        composeRule.onNodeWithText("Novo desejo").assertIsDisplayed()
    }
}
