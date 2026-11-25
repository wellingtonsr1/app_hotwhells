package br.com.diecastcollector.feature.organization

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.domain.model.Manufacturer
import br.com.diecastcollector.domain.model.Series
import br.com.diecastcollector.feature.organization.CollectionFormContent
import br.com.diecastcollector.feature.organization.CollectionFormUiState
import br.com.diecastcollector.feature.organization.SeriesContent
import br.com.diecastcollector.feature.organization.SeriesUiState
import br.com.diecastcollector.testing.DeviceWindow
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.testing.WindowSizeFrameTags
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.time.Instant
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Formulário de coleção e diálogo de série por classe (T362, T367).
 *
 * A coluna de coleção já usa `FormColumn`; o teste mede o limite de 720 dp em E e a largura
 * total em C. O formulário de série é um `AlertDialog`, que tem janela própria: em paisagem
 * o teste usa a janela real do aparelho (640 × 360 dp) e confere que o corpo rola e os
 * botões continuam visíveis.
 */
@RunWith(AndroidJUnit4::class)
class OrganizationResponsiveTest {

    @get:Rule
    val composeRule = createComposeRule()

    @After
    fun restaurarJanela() {
        DeviceWindow.reset()
    }

    @Test
    fun formularioDeColecaoRespeitaLarguraDeLeitura() {
        var width by mutableStateOf(360.dp)
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width) {
                    CollectionFormContent(
                        state = CollectionFormUiState(
                            name = "Coleção 2026",
                            description = "Hot Wheels mainline",
                            editing = true,
                        ),
                        onNameChange = {},
                        onDescriptionChange = {},
                        onParentChange = {},
                        onSave = {},
                        onMessageShown = {},
                        onCancel = {},
                    )
                }
            }
        }

        listOf(360.dp, 700.dp, 1000.dp).forEach { target ->
            composeRule.runOnUiThread { width = target }
            composeRule.waitForIdle()

            // A moldura > tela fica deslocada; medir relativo à moldura.
            val frame = composeRule.onNodeWithTag(WindowSizeFrameTags.FRAME)
                .getUnclippedBoundsInRoot()
            val title = composeRule.onNodeWithText("Coleção 2026").getUnclippedBoundsInRoot()
            val column = minOf(target.value, 720f)
            val left = (target.value - column) / 2f + frame.left.value
            assertTrue(
                "Título em $target: ${title.left}..${title.right}",
                title.left.value >= left - 1f && title.right.value <= left + column + 1f,
            )
            composeRule.onNodeWithText("Salvar coleção").assertExists()
        }
    }

    /** T367/FR-316: em paisagem o formulário de série rola e "Salvar série" fica visível. */
    @Test
    fun formularioDeSerieRolaEmPaisagem() {
        DeviceWindow.use(widthDp = 640, heightDp = 360)

        renderSeries(parents = 30)

        composeRule.onNodeWithText("+ Série").performClick()
        composeRule.onNodeWithText("Nome da série").assertExists()
        composeRule.onNodeWithText("Salvar série").assertIsDisplayed()

        val scrollables = composeRule.onAllNodes(hasScrollAction()).fetchSemanticsNodes()
        assertTrue("O corpo do diálogo deveria rolar", scrollables.isNotEmpty())
    }

    private fun renderSeries(parents: Int, onAdd: (String, String?) -> Unit = { _, _ -> }) {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = 360.dp) {
                    SeriesContent(
                        state = SeriesUiState(
                            manufacturers = listOf(
                                Manufacturer(
                                    id = "m1",
                                    name = "Hot Wheels",
                                    createdAt = now,
                                    updatedAt = now,
                                ),
                            ),
                            selectedManufacturerId = "m1",
                            items = (1..parents).map { index ->
                                Series(
                                    id = "s$index",
                                    manufacturerId = "m1",
                                    name = "Série $index",
                                    createdAt = now,
                                    updatedAt = now,
                                )
                            },
                            loading = false,
                        ),
                        onBack = {},
                        onSelectManufacturer = {},
                        onAdd = onAdd,
                        onRename = { _, _ -> },
                        onArchive = {},
                        onMessageShown = {},
                    )
                }
            }
        }
    }
}
