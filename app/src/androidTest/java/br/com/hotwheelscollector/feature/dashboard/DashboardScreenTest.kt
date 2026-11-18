package br.com.hotwheelscollector.feature.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.hotwheelscollector.domain.model.CollectionProgress
import br.com.hotwheelscollector.domain.model.DashboardEntry
import br.com.hotwheelscollector.domain.model.DashboardSnapshot
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Conteúdo do painel (US3, T050–T055) com um snapshot fixo.
 *
 * Teste de conteúdo porque o que se verifica aqui é a decisão de apresentação: os onze
 * indicadores saem com o valor certo, o selo de estimativa avisa de onde vem o número, e
 * falha de leitura aparece como falha — não como coleção zerada.
 */
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mostraOsIndicadoresEOSeloDeEstimativa() {
        show(DashboardUiState.Ready(snapshot()))

        composeRule.onNodeWithText("Exemplares").assertIsDisplayed()
        composeRule.onNodeWithText("7").assertIsDisplayed()
        composeRule.onNodeWithText("Variantes repetidas").assertIsDisplayed()
        composeRule.onNodeWithText("Na lista de desejos").assertIsDisplayed()
        composeRule.onNodeWithText("9").assertIsDisplayed()
        composeRule.onNodeWithText("R$ 1.234,56").assertIsDisplayed()
        composeRule.onNodeWithText("R$ 900,00").assertIsDisplayed()

        // Estimado menor que investido continua legível: o sinal fica no texto.
        composeRule.onNodeWithText("-R$ 334,56").assertIsDisplayed()
        composeRule.onNodeWithText("Estimativa").assertIsDisplayed()
        composeRule.onNodeWithText("Avaliações até 2026-06-01.").assertIsDisplayed()
        composeRule.onNodeWithText(
            "2 exemplar(es) sem avaliação ficam de fora da estimativa.",
        ).assertIsDisplayed()
    }

    @Test
    fun listaAquisicoesRecentesEProgressoDasColecoes() {
        show(DashboardUiState.Ready(snapshot()))

        composeRule.onNodeWithText("Twin Mill").assertExists()
        composeRule.onNodeWithText("Últimas aquisições").assertExists()

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Progresso das coleções"))
        composeRule.onNodeWithText("Hot Wheels 2024").assertExists()
        composeRule.onNodeWithText("3 de 10 itens").assertExists()
    }

    @Test
    fun colecaoVaziaDizOQueFazer() {
        show(DashboardUiState.Ready(DashboardSnapshot()))

        composeRule.onNodeWithText("Coleção vazia").assertIsDisplayed()
        composeRule.onNodeWithText("Cadastre a primeira miniatura para ver o resumo do acervo aqui.")
            .assertIsDisplayed()
    }

    @Test
    fun falhaDeLeituraNaoViraZero() {
        show(DashboardUiState.Error("disco cheio"))

        composeRule.onNodeWithText("Não foi possível ler a coleção").assertIsDisplayed()
        composeRule.onNodeWithText("disco cheio").assertIsDisplayed()
    }

    private fun show(state: DashboardUiState) {
        composeRule.setContent {
            HotWheelsTheme {
                DashboardContent(state = state)
            }
        }
    }

    private fun snapshot(): DashboardSnapshot = DashboardSnapshot(
        ownedExemplars = 7,
        distinctModels = 5,
        distinctVariants = 6,
        distinctManufacturers = 3,
        distinctSeries = 2,
        activeCollections = 4,
        variantsWithDuplicates = 1,
        wantedItems = 9,
        investedCents = 123_456L,
        estimatedCents = 90_000L,
        exemplarsWithoutValuation = 2,
        latestValuationDate = "2026-06-01",
        latestAcquisitions = listOf(
            DashboardEntry("a1", "Twin Mill", "Mattel", "2026-11-02"),
        ),
        latestEntries = listOf(
            DashboardEntry("e1", "Bone Shaker", "Mattel", "2026-11-01"),
        ),
        collectionProgress = listOf(
            CollectionProgress("c1", "Hot Wheels 2024", owned = 3, known = 10),
        ),
    )
}
