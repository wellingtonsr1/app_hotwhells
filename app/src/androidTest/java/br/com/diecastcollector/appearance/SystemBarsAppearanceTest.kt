package br.com.diecastcollector.appearance

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.MainActivity
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Barras do sistema com aparência forçada divergente do Android (Spec 006, T417, FR-409).
 *
 * O teste força o Android no modo oposto ao da aparência escolhida e conferir o estilo dos
 * ícones: em **Claro** com Android escuro os ícones continuam escuros (fundo claro), e em
 * **Escuro** com Android claro continuam claros. É a `SideEffect` da `MainActivity` que
 * decide isso a partir do modo resolvido, não da configuração do sistema.
 */
@RunWith(AndroidJUnit4::class)
public class SystemBarsAppearanceTest {

    @get:Rule
    public val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    public fun restaurarTemaDoSistema() {
        AppearanceActivitySupport.setSystemNight(false)
    }

    @Test
    public fun iconesDasBarrasSeguemAAparenciaEscolhida() {
        AppearanceActivitySupport.setSystemNight(true)
        AppearanceActivitySupport.openSettings(composeRule)
        AppearanceActivitySupport.selectMode(composeRule, "Claro")
        AppearanceActivitySupport.assertModePalette(composeRule, "Claro no Android escuro", dark = false)
        assertTrue(
            "Com aparência Claro os ícones das barras precisam ser escuros",
            iconesEscuros(),
        )

        AppearanceActivitySupport.setSystemNight(false)
        AppearanceActivitySupport.openSettings(composeRule)
        AppearanceActivitySupport.selectMode(composeRule, "Escuro")
        AppearanceActivitySupport.assertModePalette(composeRule, "Escuro no Android claro", dark = true)
        assertFalse(
            "Com aparência Escuro os ícones das barras precisam ser claros",
            iconesEscuros(),
        )
    }

    /** Lido na thread principal: a API de insets toca as views da janela. */
    private fun iconesEscuros(): Boolean {
        val escuros = java.util.concurrent.atomic.AtomicBoolean(false)
        composeRule.activityRule.scenario.onActivity { activity ->
            escuros.set(
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    .isAppearanceLightStatusBars,
            )
        }
        return escuros.get()
    }
}
