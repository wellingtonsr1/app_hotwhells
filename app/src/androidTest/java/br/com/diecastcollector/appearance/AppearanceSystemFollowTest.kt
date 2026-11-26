package br.com.diecastcollector.appearance

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.MainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **Sistema** acompanha o Android com o app aberto (Spec 006, T410, FR-407, FR-426).
 *
 * Muda o tema do Android pelo shell, espera a paleta virar e confere que o formulário em
 * preenchimento continua preenchido e na mesma rota — a mudança de aparência não pode
 * custar o trabalho do usuário.
 */
@RunWith(AndroidJUnit4::class)
public class AppearanceSystemFollowTest {

    @get:Rule
    public val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    public fun restaurarTemaDoSistema() {
        AppearanceActivitySupport.setSystemNight(false)
    }

    @Test
    public fun sistemaAcompanhaOTemaDoAndroidSemPerderOFormulario() {
        AppearanceActivitySupport.openSettings(composeRule)
        AppearanceActivitySupport.selectMode(composeRule, "Sistema")
        composeRule.waitForIdle()

        // Formulário em preenchimento: o texto vive no ViewModel da rota.
        composeRule.onNode(hasText("Catálogo") and hasClickAction()).performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasText("+ Mini") and hasClickAction()).performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Twin Mill")
        composeRule.waitForIdle()

        val sistemaEscuro = AppearanceActivitySupport.isSystemInDark()
        try {
            AppearanceActivitySupport.setSystemNight(!sistemaEscuro)
            val medido = AppearanceActivitySupport.waitForPalette(
                composeRule,
                dark = !sistemaEscuro,
                timeoutMs = 2_000,
            )
            assertTrue("A mudança do sistema levou $medido ms", medido <= 2_000)

            assertEquals(
                "O texto digitado se perdeu na troca do tema do sistema",
                1,
                composeRule.onAllNodes(hasSetTextAction() and hasText("Twin Mill"))
                    .fetchSemanticsNodes().size,
            )
            composeRule.onNode(hasText("Nova miniatura")).assertExists()
        } finally {
            AppearanceActivitySupport.setSystemNight(sistemaEscuro)
        }
    }
}
