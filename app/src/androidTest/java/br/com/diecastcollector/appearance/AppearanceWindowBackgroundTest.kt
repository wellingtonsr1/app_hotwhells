package br.com.diecastcollector.appearance

import android.content.res.Configuration
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.MainActivity
import br.com.diecastcollector.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fundo de janela/splash nos recursos de tema (Spec 006, FR-410, NFR-403).
 *
 * O sistema cria a janela inicial antes do processo, escolhendo `values` ou `values-night`
 * pelo modo noturno do pacote. Este teste prova que a escolha de aparência espelha esse
 * modo: em **Escuro** forçado com Android claro, `window_background` passa a resolver o
 * valor escuro e a activity **não é recriada**; em **Sistema**, volta a seguir o Android.
 *
 * O primeiro quadro de um processo novo (o splash propriamente dito) é medido por
 * `screenrecord` em `docs/validacao.md` §11.5.2, porque só existe fora do processo de teste.
 */
@RunWith(AndroidJUnit4::class)
public class AppearanceWindowBackgroundTest {

    @get:Rule
    public val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    public fun restaurarTemaDoSistema() {
        AppearanceActivitySupport.setSystemNight(false)
    }

    @Test
    public fun aparênciaForcadaDefineORecursoDaJanelaSemRecriarAActivity() {
        AppearanceActivitySupport.setSystemNight(false)
        AppearanceActivitySupport.openSettings(composeRule)

        val activity = composeRule.activity
        assertEquals(
            "O Android deveria estar claro para o cenário forçado",
            Configuration.UI_MODE_NIGHT_NO,
            nightMode(),
        )

        AppearanceActivitySupport.selectMode(composeRule, "Escuro")
        waitForNightMode(Configuration.UI_MODE_NIGHT_YES)

        assertSame(
            "Trocar a aparência não pode recriar a activity",
            activity,
            composeRule.activity,
        )
        assertEquals(
            "O fundo da janela em Escuro deveria usar values-night",
            0xFF121212.toInt(),
            composeRule.activity.getColor(R.color.window_background),
        )
        AppearanceActivitySupport.assertModePalette(composeRule, "Escuro forçado", dark = true)

        AppearanceActivitySupport.selectMode(composeRule, "Sistema")
        waitForNightMode(Configuration.UI_MODE_NIGHT_NO)

        assertSame(
            "Voltar para Sistema não pode recriar a activity",
            activity,
            composeRule.activity,
        )
        assertEquals(
            "Em Sistema o fundo da janela deveria seguir o Android claro",
            0xFFF4F1EE.toInt(),
            composeRule.activity.getColor(R.color.window_background),
        )
        AppearanceActivitySupport.assertModePalette(composeRule, "Sistema claro", dark = false)
    }

    private fun nightMode(): Int =
        composeRule.activity.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK

    private fun waitForNightMode(expected: Int) {
        composeRule.waitUntil(3_000) { nightMode() == expected }
    }
}
