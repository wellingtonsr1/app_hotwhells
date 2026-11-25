package br.com.diecastcollector.feature.locations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.InventoryItem
import br.com.diecastcollector.domain.model.LocationArchivePolicy
import br.com.diecastcollector.domain.model.LocationType
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.testing.WindowSizeFrameTags
import br.com.diecastcollector.ui.theme.DiecastTheme
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Árvore de localizações e diálogo de exclusão (T102, T104).
 *
 * O que a tela precisa garantir: hierarquia legível com contagem, exclusão bloqueada com
 * sublocalização ativa e escolha explícita entre mover e arquivar os exemplares. A
 * transação em si é coberta por `LocationRepositoryTest`.
 */
@RunWith(AndroidJUnit4::class)
public class LocationsTest {

    @get:Rule
    public val compose: androidx.compose.ui.test.junit4.ComposeContentTestRule =
        createComposeRule()

    @Test
    public fun arvoreMostraHierarquiaTipoEContagem() {
        compose.setContent {
            DiecastTheme {
                LocationsContent(
                    state = state(
                        rows = listOf(
                            row("rack", "Estante A", LocationType.RACK, depth = 0, children = true),
                            row("shelf", "Prateleira 3", LocationType.SHELF, depth = 1, own = 2),
                            row("box", "Caixa 12", LocationType.CRATE, depth = 0, own = 1),
                        ),
                    ),
                    onBack = {},
                    onOpenContents = {},
                    onAdd = { _, _, _ -> },
                    onRename = { _, _ -> },
                    onArchive = { _, _ -> },
                    onMessageShown = {},
                )
            }
        }

        compose.onNodeWithText("Estante A").assertIsDisplayed()
        compose.onNodeWithText("Prateleira 3").assertIsDisplayed()
        compose.onNodeWithText("Caixa 12").assertIsDisplayed()
        compose.onNodeWithText("2 exemplares").assertExists()
        compose.onNodeWithText("1 exemplar").assertExists()
    }

    @Test
    public fun excluirComSublocalizacaoAtivaExplicaOBloqueio() {
        compose.setContent {
            DiecastTheme {
                LocationsContent(
                    state = state(
                        rows = listOf(
                            row("rack", "Estante A", LocationType.RACK, depth = 0, children = true),
                            row("shelf", "Prateleira 3", LocationType.SHELF, depth = 1, own = 2),
                        ),
                    ),
                    onBack = {},
                    onOpenContents = {},
                    onAdd = { _, _, _ -> },
                    onRename = { _, _ -> },
                    onArchive = { _, _ -> },
                    onMessageShown = {},
                )
            }
        }

        compose.onAllNodesWithText("Excluir").onFirst().performClick()

        compose.onNodeWithText(
            "Ela tem 1 sublocalização ativa. Organize ou exclua a sublocalização antes.",
        ).assertExists()
        // Bloqueado: o diálogo não oferece confirmar, só fechar.
        compose.onNodeWithText("Fechar").assertExists()
    }

    @Test
    public fun excluirComExemplaresPermiteArquivarOsExemplares() {
        var policy: LocationArchivePolicy? = null

        compose.setContent {
            DiecastTheme {
                LocationsContent(
                    state = state(
                        rows = listOf(row("box", "Caixa 12", LocationType.CRATE, depth = 0, own = 2)),
                    ),
                    onBack = {},
                    onOpenContents = {},
                    onAdd = { _, _, _ -> },
                    onRename = { _, _ -> },
                    onArchive = { _, chosen -> policy = chosen },
                    onMessageShown = {},
                )
            }
        }

        compose.onAllNodesWithText("Excluir").onFirst().performClick()
        // `assertIsDisplayed` e não `assertExists`: com os chips lado a lado o de arquivar
        // existia na árvore de semântica, mas ficava fora dos limites do diálogo.
        compose.onNodeWithText("Arquivar os exemplares").assertIsDisplayed().performClick()
        compose.onAllNodesWithText("Excluir").onLast().performClick()

        assertEquals(LocationArchivePolicy.ArchiveExemplars, policy)
    }

    /**
     * T361 (VII.14): a árvore acompanha a classe sem cortar nomes longos nem criar rolagem
     * horizontal — em 360, 700 e 1.000 dp o nome fica dentro da moldura.
     */
    @Test
    public fun arvoreRespeitaAsClassesDeJanela() {
        var width by mutableStateOf(360.dp)
        compose.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width) {
                    LocationsContent(
                        state = state(
                            rows = listOf(
                                row(
                                    "rack",
                                    "Estante da parede da sala de coleção A",
                                    LocationType.RACK,
                                    depth = 0,
                                    own = 12,
                                ),
                            ),
                        ),
                        onBack = {},
                        onOpenContents = {},
                        onAdd = { _, _, _ -> },
                        onRename = { _, _ -> },
                        onArchive = { _, _ -> },
                        onMessageShown = {},
                    )
                }
            }
        }

        listOf(360.dp, 700.dp, 1000.dp).forEach { target ->
            compose.runOnUiThread { width = target }
            compose.waitForIdle()
            val frame = compose.onNodeWithTag(WindowSizeFrameTags.FRAME)
                .getUnclippedBoundsInRoot()
            val nome = compose.onNodeWithText("Estante da parede da sala de coleção A")
                .getUnclippedBoundsInRoot()
            assertTrue(
                "Nome saiu da moldura em $target: ${nome.left}..${nome.right}",
                nome.left.value >= frame.left.value - 1f &&
                    nome.right.value <= frame.right.value + 1f,
            )
        }
    }

    /**
     * T361/T320: a grade do inventário usa a largura da janela — 1 coluna em 360, 2 em 700
     * e 3 em 1.000 dp, com os nomes dentro da moldura e sem rolagem horizontal.
     */
    @Test
    public fun conteudosDaLocalizacaoRespeitamAsClasses() {
        var width by mutableStateOf(360.dp)
        val titulos = (1..6).map { index -> "Modelo $index" }
        compose.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width) {
                    LocationContentsContent(
                        state = LocationContentsUiState(
                            loading = false,
                            locationId = "rack",
                            name = "Estante A",
                            items = titulos.mapIndexed { index, titulo ->
                                InventoryItem(
                                    exemplarId = "e$index",
                                    variantId = "v$index",
                                    title = titulo,
                                    internalCode = "HW-00000$index",
                                    condition = Condition.LOOSE,
                                    locationName = "Estante A",
                                )
                            },
                        ),
                        onBack = {},
                        onOpenExemplar = {},
                    )
                }
            }
        }

        listOf(360.dp to 1, 700.dp to 2, 1000.dp to 3).forEach { (target, colunas) ->
            compose.runOnUiThread { width = target }
            compose.waitForIdle()
            compose.onNodeWithText("Estante A").assertIsDisplayed()
            assertEquals("Colunas do inventário em $target", colunas, colunasDe(titulos))
        }
    }

    /**
     * T320: a árvore de localizações acompanha a classe — 1 coluna em 360, 2 em 700 e 3 em
     * 1.000 dp — em vez de uma lista única esticada no tablet.
     */
    @Test
    public fun locaisUsamColunasPorClasseDeJanela() {
        var width by mutableStateOf(360.dp)
        val nomes = (1..6).map { index -> "Zona $index" }
        compose.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width) {
                    LocationsContent(
                        state = state(
                            rows = nomes.mapIndexed { index, nome ->
                                row(
                                    id = "loc$index",
                                    name = nome,
                                    type = LocationType.RACK,
                                    depth = 0,
                                    own = index,
                                )
                            },
                        ),
                        onBack = {},
                        onOpenContents = {},
                        onAdd = { _, _, _ -> },
                        onRename = { _, _ -> },
                        onArchive = { _, _ -> },
                        onMessageShown = {},
                    )
                }
            }
        }

        listOf(360.dp to 1, 700.dp to 2, 1000.dp to 3).forEach { (target, colunas) ->
            compose.runOnUiThread { width = target }
            compose.waitForIdle()
            assertEquals("Colunas de localizações em $target", colunas, colunasDe(nomes))
        }
    }

    /** Conta colunas pelo `left` distinto dos itens: uma coluna a mais aparece como outro x. */
    private fun colunasDe(nomes: List<String>): Int = nomes
        .map { nome -> compose.onNodeWithText(nome).getUnclippedBoundsInRoot().left.value }
        .distinct()
        .size

    private fun state(rows: List<LocationRowModel>): LocationsUiState =
        LocationsUiState(rows = rows, loading = false)

    private fun row(
        id: String,
        name: String,
        type: LocationType,
        depth: Int,
        own: Int = 0,
        children: Boolean = false,
    ): LocationRowModel = LocationRowModel(
        id = id,
        name = name,
        type = type,
        depth = depth,
        exemplarCount = own,
        subtreeCount = own,
        hasChildren = children,
        childCount = if (children) 1 else 0,
    )
}
