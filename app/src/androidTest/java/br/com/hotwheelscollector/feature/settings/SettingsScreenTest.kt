package br.com.hotwheelscollector.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme
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
            HotWheelsTheme {
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
}
