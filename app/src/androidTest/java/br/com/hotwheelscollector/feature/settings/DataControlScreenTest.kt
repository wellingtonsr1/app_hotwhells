package br.com.hotwheelscollector.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.hotwheelscollector.domain.repository.UserDataCounts
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Controle de dados (T146, FR-157, FR-163).
 *
 * O que a tela precisa provar: o primeiro aviso não apaga nada, oferece exportar antes, e
 * só a **segunda** confirmação chama a exclusão. Com o acervo vazio, os dois botões ficam
 * desabilitados em vez de prometer uma operação sem efeito.
 */
public class DataControlScreenTest {

    @get:Rule
    public val composeRule: androidx.compose.ui.test.junit4.ComposeContentTestRule =
        createComposeRule()

    private val counts = UserDataCounts(
        exemplars = 3,
        models = 2,
        photos = 4,
        collections = 1,
    )

    @Test
    public fun exclusaoSoAconteceNaSegundaConfirmacao() {
        var deletes = 0
        composeRule.setContent {
            HotWheelsTheme {
                DataControlContent(
                    state = DataControlViewModel.State(counts = counts),
                    onBack = {},
                    onExport = {},
                    onDelete = { deletes++ },
                    onDismissMessage = {},
                )
            }
        }

        composeRule.onNodeWithText("3 exemplares · 4 fotos").assertIsDisplayed()
        composeRule.onNodeWithText("Excluir todos os dados").performClick()
        composeRule.onNodeWithText("Excluir todos os dados?").assertIsDisplayed()

        composeRule.onNodeWithText("Continuar").performClick()
        // Primeira confirmação vencida não apaga: ainda falta a definitiva.
        assertEquals(0, deletes)
        composeRule.onNodeWithText("Última confirmação").assertIsDisplayed()

        composeRule.onNodeWithText("Excluir definitivamente").performClick()
        assertEquals(1, deletes)
    }

    @Test
    public fun primeiroAvisoOfereceExportarAntesDeExcluir() {
        var exports = 0
        composeRule.setContent {
            HotWheelsTheme {
                DataControlContent(
                    state = DataControlViewModel.State(counts = counts),
                    onBack = {},
                    onExport = { exports++ },
                    onDelete = {},
                    onDismissMessage = {},
                )
            }
        }

        composeRule.onNodeWithText("Excluir todos os dados").performClick()
        composeRule.onNodeWithText("Exportar antes de excluir").performClick()

        assertEquals(1, exports)
        // O aviso fecha e a segunda confirmação não aparece sozinha.
        composeRule.onAllNodesWithText("Última confirmação").assertCountEquals(0)
    }

    @Test
    public fun acervoVazioDesabilitaExportarEExcluir() {
        composeRule.setContent {
            HotWheelsTheme {
                DataControlContent(
                    state = DataControlViewModel.State(),
                    onBack = {},
                    onExport = {},
                    onDelete = {},
                    onDismissMessage = {},
                )
            }
        }

        composeRule.onNodeWithText("Não há dados no aparelho.").assertIsDisplayed()
        composeRule.onNodeWithText("Exportar tudo").assertIsNotEnabled()
        composeRule.onNodeWithText("Excluir todos os dados").assertIsNotEnabled()
    }

    @Test
    public fun confirmaQuandoAExclusaoTermina() {
        composeRule.setContent {
            HotWheelsTheme {
                DataControlContent(
                    state = DataControlViewModel.State(deleted = true),
                    onBack = {},
                    onExport = {},
                    onDelete = {},
                    onDismissMessage = {},
                )
            }
        }

        composeRule.onNodeWithText("Dados excluídos").assertIsDisplayed()
        composeRule.onNodeWithText("Voltar aos Ajustes").assertIsDisplayed()
    }
}
