package br.com.diecastcollector.feature.catalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.diecastcollector.domain.repository.ExemplarDependents
import br.com.diecastcollector.ui.theme.DiecastTheme
import org.junit.Rule
import org.junit.Test

/**
 * Confirmação de exclusão/arquivamento (T142, FR-028, Art. I.6).
 *
 * O que o teste protege: com vínculo, a tela não oferece excluir — oferece arquivar e diz o
 * motivo. Sem vínculo, a exclusão exige um segundo toque explícito.
 */
class DeleteOrArchiveSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun comVinculosExplicaOBloqueioEOfereceArquivar() {
        var archived = false
        composeRule.setContent {
            DiecastTheme {
                DeleteExemplarDialog(
                    dependents = ExemplarDependents(photos = 2, acquisitions = 1),
                    onConfirm = {},
                    onArchive = { archived = true },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Exclusão bloqueada").assertIsDisplayed()
        composeRule.onNodeWithText("Excluir").assertDoesNotExist()
        composeRule.onNodeWithText("Arquivar").performClick()

        assert(archived)
    }

    @Test
    fun semVinculosPedeConfirmacaoExplicitaParaExcluir() {
        var deleted = false
        composeRule.setContent {
            DiecastTheme {
                DeleteExemplarDialog(
                    dependents = ExemplarDependents(),
                    onConfirm = { deleted = true },
                    onArchive = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Excluir definitivamente?").assertIsDisplayed()
        composeRule.onNodeWithText("Excluir").performClick()

        assert(deleted)
    }
}
