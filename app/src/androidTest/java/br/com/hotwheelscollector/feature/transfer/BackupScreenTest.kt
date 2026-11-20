package br.com.hotwheelscollector.feature.transfer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.hotwheelscollector.domain.backup.BackupFailure
import br.com.hotwheelscollector.domain.backup.BackupSummary
import br.com.hotwheelscollector.domain.backup.ValidatedBackup
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme
import java.io.File
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tela de backup (T120): o resumo aparece **antes** da substituição, e a restauração só
 * acontece no segundo toque — nunca ao escolher o arquivo.
 */
class BackupScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val summary = BackupSummary(
        createdAt = Instant.parse("2026-11-19T12:00:00Z"),
        schemaVersion = 2,
        appVersion = "0.1.0 (1)",
        device = "samsung SM-A546E",
        counts = mapOf("exemplar" to 3, "photo" to 2),
        imageCount = 2,
        totalBytes = 2_500_000,
    )

    @Test
    fun mostraResumoEExigeConfirmacao() {
        var confirmed = false
        composeRule.setContent {
            HotWheelsTheme {
                BackupContent(
                    state = BackupViewModel.State(
                        pending = ValidatedBackup(File("backup.zip"), summary, emptyList()),
                    ),
                    onBack = {},
                    onCreate = {},
                    onPick = {},
                    onConfirmRestore = { confirmed = true },
                    onDismissPending = {},
                    onDismissMessage = {},
                    onRestart = {},
                )
            }
        }

        composeRule.onNodeWithText("Substituir os dados atuais?").assertIsDisplayed()
        composeRule.onNodeWithText("Criado em", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Aparelho de origem: samsung SM-A546E").assertIsDisplayed()
        composeRule.onNodeWithText("3 exemplares · 2 fotos").assertIsDisplayed()
        composeRule.onNodeWithText("Tamanho: 2,4 MB").assertIsDisplayed()
        assertTrue(!confirmed)

        composeRule.onNodeWithText("Restaurar").performClick()
        assertTrue(confirmed)
    }

    @Test
    fun explicaPacoteCorrompido() {
        composeRule.setContent {
            HotWheelsTheme {
                BackupContent(
                    state = BackupViewModel.State(
                        failure = BackupFailure.CHECKSUM_MISMATCH,
                        failureDetail = "O arquivo images/e1/front.jpg está corrompido.",
                    ),
                    onBack = {},
                    onCreate = {},
                    onPick = {},
                    onConfirmRestore = {},
                    onDismissPending = {},
                    onDismissMessage = {},
                    onRestart = {},
                )
            }
        }

        composeRule.onNodeWithText("Não foi possível concluir").assertIsDisplayed()
        composeRule
            .onNodeWithText("Um arquivo do pacote está corrompido (hash diferente do manifesto).")
            .assertIsDisplayed()
    }
}
