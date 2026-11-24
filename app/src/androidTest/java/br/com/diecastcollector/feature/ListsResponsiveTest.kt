package br.com.diecastcollector.feature

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.domain.model.CollectionItemStatus
import br.com.diecastcollector.domain.model.CollectionProgress
import br.com.diecastcollector.domain.model.DashboardSnapshot
import br.com.diecastcollector.domain.model.Series
import br.com.diecastcollector.domain.model.VariantSummary
import br.com.diecastcollector.feature.catalog.CatalogContent
import br.com.diecastcollector.feature.catalog.CatalogTags
import br.com.diecastcollector.feature.catalog.CatalogUiState
import br.com.diecastcollector.feature.dashboard.DashboardContent
import br.com.diecastcollector.feature.dashboard.DashboardTags
import br.com.diecastcollector.feature.dashboard.DashboardUiState
import br.com.diecastcollector.feature.organization.CollectionDetailContent
import br.com.diecastcollector.feature.organization.CollectionDetailTags
import br.com.diecastcollector.feature.organization.CollectionDetailUiState
import br.com.diecastcollector.feature.organization.CollectionTags
import br.com.diecastcollector.feature.organization.CollectionsContent
import br.com.diecastcollector.feature.organization.CollectionsUiState
import br.com.diecastcollector.feature.organization.SeriesContent
import br.com.diecastcollector.feature.organization.SeriesTags
import br.com.diecastcollector.feature.organization.SeriesUiState
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Colunas das listas por classe de janela (T323).
 *
 * Cada lista é renderizada no enquadramento de 360/700/1.000 dp com os mesmos composables
 * da produção e um estado fixo. A contagem vem dos limites dos nós: duas colunas aparecem
 * como dois `left` distintos, sem depender de captura de tela.
 *
 * As tags de teste ficam nas telas (`CatalogTags`, `CollectionTags`, `SeriesTags`,
 * `CollectionDetailTags`, `DashboardTags`) para que o teste conte o que o usuário vê.
 */
@RunWith(AndroidJUnit4::class)
class ListsResponsiveTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun catalogoEm360TemUmaColuna() = assertCatalogColumns(360.dp, 1)

    @Test
    fun catalogoEm700TemDuasColunas() = assertCatalogColumns(700.dp, 2)

    @Test
    fun catalogoEm1000TemTresColunas() = assertCatalogColumns(1000.dp, 3)

    @Test
    fun colecoesEm360TemUmaColuna() = assertCollectionColumns(360.dp, 1)

    @Test
    fun colecoesEm700TemDuasColunas() = assertCollectionColumns(700.dp, 2)

    @Test
    fun seriesEm700TemDuasColunas() {
        render(width = 700.dp) {
            SeriesContent(
                state = SeriesUiState(items = seriesItems(), loading = false),
                onBack = {},
                onSelectManufacturer = {},
                onAdd = { _, _ -> },
                onRename = { _, _ -> },
                onArchive = {},
                onMessageShown = {},
            )
        }

        assertEquals(2, columnsOf(SeriesTags.ROW))
    }

    @Test
    fun detalheDaColecaoEm360TemUmaColuna() = assertDetailColumns(360.dp, 1)

    @Test
    fun detalheDaColecaoEm700TemDuasColunas() = assertDetailColumns(700.dp, 2)

    @Test
    fun detalheDaColecaoEm1000ContinuaComDuasColunas() = assertDetailColumns(1000.dp, 2)

    @Test
    fun painelEm360TemTresColunas() = assertDashboardColumns(360.dp, 3)

    @Test
    fun painelEm700TemQuatroColunas() = assertDashboardColumns(700.dp, 4)

    @Test
    fun painelEm1000TemCincoColunas() = assertDashboardColumns(1000.dp, 5)

    @Test
    fun catalogoEm700NaoRolaNaHorizontal() {
        renderCatalog(700.dp)

        val horizontalScrollables = composeRule.onAllNodes(
            SemanticsMatcher("rolagem horizontal") { node ->
                node.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange) != null
            },
        ).fetchSemanticsNodes()

        assertEquals(0, horizontalScrollables.size)
    }

    private fun assertCatalogColumns(width: Dp, expected: Int) {
        renderCatalog(width)
        assertEquals("Colunas do catálogo em $width", expected, columnsOf(CatalogTags.CARD))
    }

    private fun assertCollectionColumns(width: Dp, expected: Int) {
        render(width) {
            CollectionsContent(
                state = CollectionsUiState(items = collectionItems(), loading = false),
                onBack = {},
                onOpenCollection = {},
                onCreate = {},
                onOpenSeries = {},
            )
        }
        assertEquals("Colunas das coleções em $width", expected, columnsOf(CollectionTags.CARD))
    }

    private fun assertDetailColumns(width: Dp, expected: Int) {
        render(width) {
            CollectionDetailContent(
                state = CollectionDetailUiState(
                    collectionId = "c1",
                    loading = false,
                    name = "Série 1",
                    progress = CollectionProgress(
                        id = "c1",
                        name = "Série 1",
                        owned = 1,
                        known = 3,
                        itemCount = 3,
                    ),
                    items = detailItems(),
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
        assertEquals("Colunas dos itens em $width", expected, columnsOf(CollectionDetailTags.ITEM))
    }

    private fun assertDashboardColumns(width: Dp, expected: Int) {
        render(width) {
            DashboardContent(
                state = DashboardUiState.Ready(
                    DashboardSnapshot(ownedExemplars = 12, distinctModels = 3),
                ),
            )
        }
        assertEquals("Colunas do painel em $width", expected, columnsOf(DashboardTags.METRIC))
    }

    private fun renderCatalog(width: Dp) {
        render(width) {
            CatalogContent(
                state = CatalogUiState(variants = variants(), loading = false),
                onVariantClick = {},
                onOpenExemplars = {},
                onCreateEntry = {},
            )
        }
    }

    private fun render(width: Dp, content: @Composable () -> Unit) {
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width) { content() }
            }
        }
    }

    private fun columnsOf(tag: String): Int = composeRule
        .onAllNodesWithTag(tag)
        .fetchSemanticsNodes()
        .map { it.boundsInRoot.left }
        .distinct()
        .size

    private fun variants(): List<VariantSummary> = (1..6).map { index ->
        VariantSummary(
            variantId = "v$index",
            modelName = "Modelo $index",
            manufacturerName = "Hot Wheels",
            colorName = "Vermelho",
            releaseYear = 2024,
            exemplarCount = 1,
        )
    }

    private fun collectionItems(): List<CollectionProgress> = (1..6).map { index ->
        CollectionProgress(
            id = "c$index",
            name = "Coleção $index",
            owned = index,
            known = 6,
            itemCount = 6,
        )
    }

    private fun seriesItems(): List<Series> {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        return (1..6).map { index ->
            Series(
                id = "s$index",
                manufacturerId = "m1",
                name = "Série $index",
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    private fun detailItems(): List<CollectionItemStatus> = (1..3).map { index ->
        CollectionItemStatus(
            itemId = "i$index",
            collectionId = "c1",
            modelId = "m$index",
            label = "Item $index",
            ownedExemplars = 1,
        )
    }
}
