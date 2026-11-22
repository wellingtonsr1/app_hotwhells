package br.com.diecastcollector.feature.wishlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.model.WishlistItem
import br.com.diecastcollector.domain.model.WishlistStatus
import br.com.diecastcollector.domain.repository.WishlistEntry
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.time.Instant
import org.junit.Assert.assertEquals
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
                WishlistContent(
                    state = WishlistUiState(items = listOf(entry), loading = false),
                    onAdd = {},
                    onEdit = {},
                    onBuy = { bought = it },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Mattel · Twin Mill").assertIsDisplayed()
        composeRule.onNodeWithText("Alta").assertIsDisplayed()
        composeRule.onNodeWithText("Máximo R$ 50,00 · encontrado R$ 45,00").assertIsDisplayed()
        composeRule.onNodeWithText("Loja: Loja do centro").assertIsDisplayed()
        composeRule.onNodeWithText("Abrir link").assertIsDisplayed()

        composeRule.onNodeWithText("Comprei").performClick()
        assertEquals("w1", bought?.item?.id)
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
