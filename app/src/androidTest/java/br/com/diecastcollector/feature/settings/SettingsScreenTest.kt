package br.com.diecastcollector.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

public class SettingsScreenTest {

    @get:Rule
    public val composeRule: androidx.compose.ui.test.junit4.ComposeContentTestRule =
        createComposeRule()

    @Test
    public fun ajustesAbreSobreEPrivacidade() {
        var about = false
        var privacy = false

        composeRule.setContent {
            DiecastTheme {
                SettingsScreen(
                    onAbout = { about = true },
                    onPrivacy = { privacy = true },
                )
            }
        }

        composeRule.onNodeWithText("Sobre o app").assertIsDisplayed()
        composeRule.onNodeWithText("Política de privacidade").assertIsDisplayed()

        composeRule.onNodeWithText("Sobre o app").performClick()
        composeRule.onNodeWithText("Política de privacidade").performClick()

        assertTrue(about)
        assertTrue(privacy)
    }

    @Test
    public fun lembreteAvisaQuandoNuncaHouveBackupEAbreOCriador() {
        var backup = false
        var interval: Int? = null
        val now = Instant.parse("2026-11-19T12:00:00Z")

        composeRule.setContent {
            DiecastTheme {
                SettingsScreen(
                    onAbout = {},
                    onPrivacy = {},
                    onBackup = { backup = true },
                    reminder = BackupReminder(lastBackupAt = null, intervalDays = 30),
                    onIntervalChange = { interval = it },
                    now = now,
                )
            }
        }

        // O cartão tem o mesmo formato das outras opções: título + uma linha.
        composeRule.onNodeWithText("Lembrete de backup").assertIsDisplayed()
        composeRule.onNodeWithText("A cada 30 dias · nenhum backup").assertIsDisplayed()

        composeRule.onNodeWithText("Lembrete de backup").performClick()
        composeRule.onNodeWithText("Nenhum backup feito ainda.").assertIsDisplayed()
        composeRule
            .onNodeWithText("Sem backup, a coleção existe só neste aparelho.")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Fazer backup agora").performClick()
        assertTrue(backup)

        composeRule.onNodeWithText("Lembrete de backup").performClick()
        composeRule.onNodeWithText("A cada 7 dias").performClick()
        assertEquals(7, interval)
    }

    @Test
    public fun lembreteMostraDiasDesdeOUltimoBackup() {
        val now = Instant.parse("2026-11-19T12:00:00Z")

        composeRule.setContent {
            DiecastTheme {
                SettingsScreen(
                    onAbout = {},
                    onPrivacy = {},
                    reminder = BackupReminder(
                        lastBackupAt = now.minus(40, ChronoUnit.DAYS),
                        intervalDays = 30,
                    ),
                    now = now,
                )
            }
        }

        composeRule.onNodeWithText("A cada 30 dias · atrasado").assertIsDisplayed()

        composeRule.onNodeWithText("Lembrete de backup").performClick()
        composeRule.onNodeWithText("Último backup em ", substring = true).assertIsDisplayed()
        composeRule
            .onNodeWithText("Último backup há 40 dias — a coleção está só neste aparelho.")
            .assertIsDisplayed()
    }
}
