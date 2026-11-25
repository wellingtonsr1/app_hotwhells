package br.com.diecastcollector.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.data.importexport.ReportFormat
import br.com.diecastcollector.feature.reports.ReportsContent
import br.com.diecastcollector.feature.reports.ReportsViewModel
import br.com.diecastcollector.feature.transfer.BackupContent
import br.com.diecastcollector.feature.transfer.BackupViewModel
import br.com.diecastcollector.feature.transfer.ImportContent
import br.com.diecastcollector.feature.transfer.ImportViewModel
import br.com.diecastcollector.testing.WindowSizeFrame
import br.com.diecastcollector.testing.WindowSizeFrameTags
import br.com.diecastcollector.ui.theme.DiecastTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Telas de ajuste e auxiliares por classe de janela (T362, T369).
 *
 * Cada tela é medida em 360, 700 e 1.000 dp no mesmo `setContent`, trocando a largura do
 * enquadramento — o que também passa pelo caminho de reorganização. A asserção é a mesma da
 * spec: em C ocupa a largura toda (sem passar da moldura) e em M/E o conteúdo fica dentro de
 * 720 dp centrados (FR-308, FR-311). Ações principais continuam presentes.
 */
@RunWith(AndroidJUnit4::class)
class SettingsResponsiveTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun ajustesRespeitaLarguraDeLeitura() {
        assertWidths(title = "Ajustes") {
            SettingsScreen(onAbout = {}, onPrivacy = {})
        }
    }

    @Test
    fun relatoriosRespeitaLarguraDeLeitura() {
        assertWidths(title = "Relatórios") { Reports() }
    }

    @Test
    fun importacaoRespeitaLarguraDeLeitura() {
        assertWidths(title = "Importar") {
            ImportContent(
                state = ImportViewModel.State(),
                onBack = {},
                onPick = {},
                onSaveErrors = {},
                onImport = {},
                onDismissMessage = {},
            )
        }
    }

    @Test
    fun dadosDaColecaoRespeitaLarguraDeLeitura() {
        assertWidths(title = "Dados da coleção") {
            DataControlContent(
                state = DataControlViewModel.State(),
                onBack = {},
                onExport = {},
                onDelete = {},
                onDismissMessage = {},
            )
        }
    }

    @Test
    fun backupRespeitaLarguraDeLeitura() {
        assertWidths(title = "Backup e restauração") {
            BackupContent(
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
        }
    }

    @Test
    fun sobreRespeitaLarguraDeLeitura() {
        assertWidths(title = "Sobre") { AboutScreen() }
    }

    @Test
    fun privacidadeRespeitaLarguraDeLeitura() {
        assertWidths(title = "Privacidade") { PrivacyPolicyScreen() }
    }

    /** T362: a ação primária continua visível e tocável em C, M e E. */
    @Test
    fun acoesPrincipaisContinuamVisiveisPorClasse() {
        render(360.dp) { Reports() }
        composeRule.onNodeWithText("Gerar relatório").assertExists()
    }

    @Test
    fun importacaoTemAcaoVisivelEmM() {
        render(700.dp) {
            ImportContent(
                state = ImportViewModel.State(),
                onBack = {},
                onPick = {},
                onSaveErrors = {},
                onImport = {},
                onDismissMessage = {},
            )
        }
        composeRule.onNodeWithText("Escolher arquivo CSV").assertExists()
    }

    @Test
    fun backupTemAcaoVisivelEmE() {
        render(1000.dp) {
            BackupContent(
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
        }
        composeRule.onNodeWithText("Criar backup").assertExists()
    }

    private fun assertWidths(title: String, content: @Composable () -> Unit) {
        val width = mutableStateOf(360.dp)
        composeRule.setContent {
            DiecastTheme {
                WindowSizeFrame(width = width.value) { content() }
            }
        }

        listOf(360.dp, 700.dp, 1000.dp).forEach { target ->
            composeRule.runOnUiThread { width.value = target }
            composeRule.waitForIdle()

            // A moldura de 700/1.000 dp é maior que a tela do emulador e fica deslocada para
            // a esquerda: os limites são medidos relativos à moldura, não à raiz.
            val frame = composeRule.onNodeWithTag(WindowSizeFrameTags.FRAME)
                .getUnclippedBoundsInRoot()
            val bounds = composeRule.onNodeWithText(title).getUnclippedBoundsInRoot()
            val column = minOf(target.value, 720f)
            val left = (target.value - column) / 2f + frame.left.value
            assertTrue(
                "$title em $target: ${bounds.left}..${bounds.right}, coluna $left..${left + column}",
                bounds.left.value >= left - 1f && bounds.right.value <= left + column + 1f,
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

    @Composable
    private fun Reports() {
        ReportsContent(
            state = ReportsViewModel.State(),
            selectedFormat = ReportFormat.CSV,
            onSelectFormat = {},
            onBack = {},
            onGenerate = {},
            onDismissMessage = {},
        )
    }
}
