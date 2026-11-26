package br.com.diecastcollector.appearance

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.data.importexport.ReportFormat
import br.com.diecastcollector.domain.model.VariantSummary
import br.com.diecastcollector.feature.catalog.CatalogContent
import br.com.diecastcollector.feature.catalog.CatalogUiState
import br.com.diecastcollector.feature.dashboard.DashboardContent
import br.com.diecastcollector.feature.dashboard.DashboardUiState
import br.com.diecastcollector.feature.reports.ReportsContent
import br.com.diecastcollector.feature.reports.ReportsViewModel
import br.com.diecastcollector.feature.settings.DataControlContent
import br.com.diecastcollector.feature.settings.DataControlViewModel
import br.com.diecastcollector.feature.settings.SettingsScreen
import br.com.diecastcollector.feature.transfer.BackupContent
import br.com.diecastcollector.feature.transfer.BackupViewModel
import br.com.diecastcollector.feature.transfer.ImportContent
import br.com.diecastcollector.feature.transfer.ImportViewModel
import br.com.diecastcollector.ui.theme.DiecastTheme
import br.com.diecastcollector.ui.theme.ThemeMode
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Aplicação global por tela representativa (Spec 006, T415, FR-408, FR-411, SC-407).
 *
 * Cada superfície é renderizada nas duas paletas e conferida no bitmap: a cor dominante
 * tem que pertencer à paleta do modo escolhido e nunca à oposta. A estrutura não pode
 * mudar entre os modos — mesma árvore semântica, mesmas ações de clique.
 */
@RunWith(AndroidJUnit4::class)
public class AppearanceGlobalTest {

    @get:Rule
    public val composeRule = createComposeRule()

    private var screen by mutableStateOf(0)
    private var mode by mutableStateOf(ThemeMode.LIGHT)

    @Test
    public fun telasRepresentativasUsamAPaletaDoModoEscolhido() {
        composeRule.setContent {
            DiecastTheme(mode = mode) {
                // Superfície da shell: telas de Ajustes não têm `Scaffold` próprio e, no
                // app, herdam o fundo do `AppNavHost`. Aqui o fundo do modo faz esse papel;
                // se a tela pintar uma cor fixa ou da paleta oposta, ela domina a captura.
                androidx.compose.material3.Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                ) {
                when (screen) {
                    0 -> DashboardContent(state = DashboardUiState.Ready(dashboard()))
                    1 -> CatalogContent(
                        state = CatalogUiState(variants = catalog(), loading = false),
                        onVariantClick = {},
                        onOpenExemplars = {},
                        onCreateEntry = {},
                    )
                    2 -> SettingsScreen(onAbout = {}, onPrivacy = {})
                    3 -> ReportsContent(
                        state = ReportsViewModel.State(),
                        selectedFormat = ReportFormat.CSV,
                        onSelectFormat = {},
                        onBack = {},
                        onGenerate = {},
                        onDismissMessage = {},
                    )
                    4 -> DataControlContent(
                        state = DataControlViewModel.State(),
                        onBack = {},
                        onExport = {},
                        onDelete = {},
                        onDismissMessage = {},
                    )
                    5 -> BackupContent(
                        state = BackupViewModel.State(),
                        onBack = {},
                        onCreate = {},
                        onPick = {},
                        onConfirmRestore = {},
                        onConfirmMerge = {},
                        onDismissPending = {},
                        onDismissMessage = {},
                        onRestart = {},
                    )
                                            6 -> ImportContent(
                        state = ImportViewModel.State(),
                        onBack = {},
                        onPick = {},
                        onSaveErrors = {},
                        onImport = {},
                        onDismissMessage = {},
                    )
                }
                }
            }
        }

        val nomes = listOf(
            "Painel",
            "Catálogo com e sem foto",
            "Ajustes",
            "Relatórios",
            "Dados da coleção",
            "Backup",
            "Importar",
        )
        val estrutura = HashMap<String, Pair<Int, Int>>()

        listOf(ThemeMode.LIGHT, ThemeMode.DARK).forEach { alvo ->
            mode = alvo
            nomes.indices.forEach { indice ->
                screen = indice
                composeRule.waitForIdle()

                val raiz = composeRule.onRoot()
                val bitmap = AppearanceTestSupport.bitmapOf(raiz)
                try {
                    AppearanceTestSupport.assertPalette(
                        "${nomes[indice]} em ${alvo.name}",
                        bitmap,
                        dark = alvo == ThemeMode.DARK,
                    )
                } finally {
                    bitmap.recycle()
                }

                val clicaveis = composeRule.onAllNodes(hasClickAction())
                    .fetchSemanticsNodes().size
                val total = composeRule.onAllNodes(
                    SemanticsMatcher("qualquer nó") { true },
                    useUnmergedTree = true,
                ).fetchSemanticsNodes().size
                estrutura["${nomes[indice]}/${alvo.name}"] = clicaveis to total
            }
        }

        nomes.forEach { nome ->
            assertEquals(
                "$nome mudou de estrutura entre Claro e Escuro",
                estrutura["$nome/LIGHT"],
                estrutura["$nome/DARK"],
            )
        }
    }

    private fun dashboard(): br.com.diecastcollector.domain.model.DashboardSnapshot =
        br.com.diecastcollector.domain.model.DashboardSnapshot(
            ownedExemplars = 7,
            distinctModels = 5,
            distinctVariants = 6,
            distinctManufacturers = 3,
            distinctSeries = 2,
            activeCollections = 2,
            variantsWithDuplicates = 1,
            wantedItems = 3,
            investedCents = 123_456L,
            estimatedCents = 90_000L,
            exemplarsWithoutValuation = 2,
            latestValuationDate = "2026-06-01",
        )

    private fun catalog(): List<VariantSummary> {
        val comFoto = "images/aparencia/front.png"
        File(context.filesDir, comFoto).apply {
            parentFile?.mkdirs()
            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        return listOf(
            VariantSummary(
                variantId = "v1",
                modelName = "Twin Mill",
                manufacturerName = "Hot Wheels",
                colorName = "Vermelho",
                releaseYear = 2024,
                exemplarCount = 2,
                primaryPhotoPath = comFoto,
            ),
            VariantSummary(
                variantId = "v2",
                modelName = "Bone Shaker",
                manufacturerName = "Hot Wheels",
                colorName = null,
                releaseYear = null,
                exemplarCount = 0,
                primaryPhotoPath = null,
            ),
        )
    }

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
}
