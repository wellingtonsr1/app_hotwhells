package br.com.diecastcollector.feature.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.domain.model.FilterOption
import br.com.diecastcollector.domain.model.IsoDate
import br.com.diecastcollector.domain.model.SearchFilterOptions
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.SearchFilters
import br.com.diecastcollector.ui.UiMessage
import br.com.diecastcollector.domain.model.SearchResultItem
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.time.LocalDate
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

    private fun state(
        results: List<SearchResultItem> = emptyList(),
        filters: SearchFilters = SearchFilters(),
        viewMode: CatalogViewMode = CatalogViewMode.GRID,
        filtersOpen: Boolean = false,
        priceMinText: String = "",
        priceError: UiMessage? = null,
    ) = SearchUiState(
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
