package br.com.hotwheelscollector.feature.locations

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.hotwheelscollector.domain.model.LocationArchivePolicy
import br.com.hotwheelscollector.domain.model.LocationType
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme
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
            HotWheelsTheme {
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
            HotWheelsTheme {
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
            HotWheelsTheme {
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
        compose.onNodeWithText("Arquivar os exemplares").assertExists().performClick()
        compose.onAllNodesWithText("Excluir").onLast().performClick()

        assertEquals(LocationArchivePolicy.ArchiveExemplars, policy)
    }

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
