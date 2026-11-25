package br.com.diecastcollector.feature.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.domain.model.FilterOption
import br.com.diecastcollector.domain.model.IsoDate
import br.com.diecastcollector.domain.model.SearchFilterOptions
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.SearchFilters
import br.com.diecastcollector.ui.UiMessage
import br.com.diecastcollector.domain.model.SearchResultItem
import br.com.diecastcollector.testing.DeviceWindow
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.ui.components.ProvideWindowSizeClass
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Busca e filtros do acervo (T063/T064, FR-114–FR-122).
 *
 * A tela tem que responder o que o colecionador pergunta: quantos exemplares entraram no
 * recorte, quais filtros estão ligados e como desligá-los. A grade, a lista e os
 * agrupamentos compartilham o mesmo cartão.
 */
class SearchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mostraContagemResultadosEChipsAtivos() {
        var cleared = false
        composeRule.setContent {
            DiecastTheme {
                SearchContent(
                    state = state(
                        results = listOf(item("e1", "Twin Mill"), item("e2", "Skyline R34")),
                        filters = SearchFilters(manufacturerIds = setOf("m1")),
                    ),
                    actions = SearchActions(onClearAll = { cleared = true }),
                    onBack = {},
                    onOpenExemplar = {},
                )
            }
        }

        composeRule.onNodeWithText("2 exemplares").assertIsDisplayed()
        composeRule.onNodeWithText("Twin Mill", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Skyline R34", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Mattel").assertIsDisplayed()

        composeRule.onNodeWithText("Limpar tudo").performClick()
        assertTrue(cleared)
    }

    @Test
    fun galeriaMostraFotoPrincipalEMarcadorParaQuemNaoTemFoto() {
        composeRule.setContent {
            DiecastTheme {
                SearchContent(
                    state = state(
                        results = listOf(
                            item("e1", "Twin Mill", photo = "images/e1/front_1.jpg"),
                            item("e2", "Skyline R34"),
                        ),
                        viewMode = CatalogViewMode.GALLERY,
                    ),
                    actions = SearchActions(),
                    onBack = {},
                    onOpenExemplar = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Foto de Twin Mill").assertExists()
        composeRule.onNodeWithText("Sem foto").assertIsDisplayed()
    }

    @Test
    fun agrupamentoMostraCabecalhoComContagem() {
        composeRule.setContent {
            DiecastTheme {
                SearchContent(
                    state = state(
                        results = listOf(item("e1", "Twin Mill"), item("e2", "Twin Mill")),
                        viewMode = CatalogViewMode.BY_MANUFACTURER,
                    ),
                    actions = SearchActions(),
                    onBack = {},
                    onOpenExemplar = {},
                )
            }
        }

        composeRule.onNodeWithText("Mattel · 2").assertIsDisplayed()
    }

    @Test
    fun filtrosAbertosMostramFaixaDePrecoEErroDeValidacao() {
        var applied = false
        composeRule.setContent {
            DiecastTheme {
                SearchContent(
                    state = state(
                        filtersOpen = true,
                        priceMinText = "abc",
                        priceError = UiMessage(R.string.search_invalid_price),
                    ),
                    actions = SearchActions(onApplyPriceRange = { applied = true }),
                    onBack = {},
                    onOpenExemplar = {},
                )
            }
        }

        composeRule.onNodeWithText("Faixa de preço").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Preço inválido. Use o formato 1.234,56.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Aplicar faixa").performScrollTo().performClick()
        assertTrue(applied)
    }

    @Test
    fun estadoVazioExplicaComoVoltarAoTerResultado() {
        composeRule.setContent {
            DiecastTheme {
                SearchContent(
                    state = state(results = emptyList()),
                    actions = SearchActions(),
                    onBack = {},
                    onOpenExemplar = {},
                )
            }
        }

        composeRule.onNodeWithText("Nenhum exemplar encontrado.").assertIsDisplayed()
        composeRule.onNodeWithText("Ajuste a busca ou limpe os filtros.").assertIsDisplayed()
    }

    @Test
    fun abrirResultadoEntregaOExemplarEscolhido() {
        var opened: String? = null
        composeRule.setContent {
            DiecastTheme {
                SearchContent(
                    state = state(results = listOf(item("e7", "Bone Shaker"))),
                    actions = SearchActions(),
                    onBack = {},
                    onOpenExemplar = { opened = it },
                )
            }
        }

        composeRule.onNodeWithText("Bone Shaker", substring = true).performClick()
        assertEquals("e7", opened)
    }

    /**
     * T345/T348 (FR-306, SC-302): a grade mostra 2 colunas em 360 dp, 4 em 700 e 5 em
     * 1.000 dp — antes eram sempre duas.
     */
    @Test
    fun gradeEm360TemDuasColunas() = assertGridColumns(360.dp, 2)

    @Test
    fun gradeEm700TemQuatroColunas() = assertGridColumns(700.dp, 4)

    @Test
    fun gradeEm1000TemCincoColunas() = assertGridColumns(1000.dp, 5)

    /**
     * T346/T348 (FR-318): em janela expandida os filtros ficam em painel lateral com os
     * resultados visíveis ao lado, em vez de uma folha que cobre tudo.
     */
    @Test
    fun filtrosEmJanelaExpandidaAparecemEmPainelLateral() {
        // Janela real de 1.000 dp: o painel lateral precisa caber na tela, não apenas na
        // moldura de teste (T346/T348).
        DeviceWindow.use(widthDp = 1000)
        composeRule.setContent {
            DiecastTheme {
                ProvideWindowSizeClass {
                    SearchContent(
                        state = state(
                            results = (1..4).map { item("e$it", "Modelo $it") },
                            filtersOpen = true,
                        ),
                        actions = SearchActions(),
                        onBack = {},
                        onOpenExemplar = {},
                    )
                }
            }
        }

        val panel = composeRule.onNodeWithTag(SearchTags.FILTER_PANEL)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        composeRule.onNodeWithText("Faixa de preço").performScrollTo().assertIsDisplayed()

        val card = composeRule.onNodeWithText("Mattel · Modelo 1")
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        assertTrue(
            "Resultado começou em ${card.left}, painel termina em ${panel.right}",
            card.left.value >= panel.right.value,
        )
    }

    @After
    fun restaurarJanela() {
        DeviceWindow.reset()
    }

    @Test
    fun filtrosEmJanelaCompactaContinuamEmFolhaModal() {
        renderSearch(width = 360.dp, filtersOpen = true)

        composeRule.onNodeWithTag(SearchTags.FILTER_PANEL).assertDoesNotExist()
        composeRule.onNodeWithText("Faixa de preço").assertExists()
    }

    /**
     * FR-304/T348: mudar a janela de expandida para compacta troca o painel pela folha
     * sem perder a consulta nem os filtros (o estado vem do ViewModel).
     */
    @Test
    fun filtrosEQuerySobrevivemAoRedimensionar() {
        var width by mutableStateOf(1000.dp)
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width) {
                    SearchContent(
                        state = state(
                            results = (1..4).map { item("e$it", "Modelo $it") },
                            query = "Skyline",
                            filtersOpen = true,
                        ),
                        actions = SearchActions(),
                        onBack = {},
                        onOpenExemplar = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag(SearchTags.FILTER_PANEL).assertIsDisplayed()
        composeRule.onNodeWithText("Skyline").assertExists()

        composeRule.runOnIdle { width = 360.dp }

        composeRule.onNodeWithTag(SearchTags.FILTER_PANEL).assertDoesNotExist()
        composeRule.onNodeWithText("Skyline").assertExists()
        composeRule.onNodeWithText("Faixa de preço").assertExists()
    }

    private fun assertGridColumns(width: Dp, expected: Int) {
        renderSearch(width = width)

        val columns = composeRule.onAllNodesWithText("Modelo", substring = true)
            .fetchSemanticsNodes()
            .map { it.boundsInRoot.left }
            .distinct()
            .size

        assertEquals("Colunas em $width", expected, columns)
    }

    private fun renderSearch(
        width: Dp,
        filtersOpen: Boolean = false,
    ) {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width) {
                    SearchContent(
                        state = state(
                            results = (1..12).map { item("e$it", "Modelo $it") },
                            filtersOpen = filtersOpen,
                        ),
                        actions = SearchActions(),
                        onBack = {},
                        onOpenExemplar = {},
                    )
                }
            }
        }
    }

    private fun state(
        results: List<SearchResultItem> = emptyList(),
        query: String = "",
        filters: SearchFilters = SearchFilters(),
        viewMode: CatalogViewMode = CatalogViewMode.GRID,
        filtersOpen: Boolean = false,
        priceMinText: String = "",
        priceError: UiMessage? = null,
    ) = SearchUiState(
        query = query,
        filters = filters,
        options = SearchFilterOptions(
            manufacturers = listOf(FilterOption("m1", "Mattel")),
        ),
        results = results,
        viewMode = viewMode,
        filtersOpen = filtersOpen,
        priceMinText = priceMinText,
        priceError = priceError,
        loading = false,
    )

    private fun item(id: String, model: String, photo: String? = null) = SearchResultItem(
        exemplarId = id,
        variantId = "v-$id",
        internalCode = "HW-$id",
        modelName = model,
        manufacturerName = "Mattel",
        seriesName = "Car Culture",
        collectionName = null,
        primaryColorName = "Vermelho",
        decoration = null,
        realVehicleName = null,
        manufacturerCode = null,
        scale = "1:64",
        releaseYear = 2024,
        condition = Condition.SEALED,
        status = ExemplarStatus.OWNED,
        locationName = null,
        priceCents = 12_000,
        acquisitionDate = IsoDate(LocalDate.of(2026, 1, 10)),
        primaryPhotoPath = photo,
    )
}
