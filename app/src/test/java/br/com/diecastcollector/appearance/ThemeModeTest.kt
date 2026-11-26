package br.com.diecastcollector.appearance

import br.com.diecastcollector.ui.theme.ThemeMode
import br.com.diecastcollector.ui.theme.isDark
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Resolução única de "escuro?" (Spec 006, FR-405, FR-407, BR-403). */
public class ThemeModeTest {

    @Test
    public fun sistemaSegueOTemaDoAndroid() {
        assertThat(ThemeMode.SYSTEM.isDark(systemInDark = true)).isTrue()
        assertThat(ThemeMode.SYSTEM.isDark(systemInDark = false)).isFalse()
    }

    @Test
    public fun modosForcadosIgnoramOTemaDoAndroid() {
        assertThat(ThemeMode.LIGHT.isDark(systemInDark = true)).isFalse()
        assertThat(ThemeMode.LIGHT.isDark(systemInDark = false)).isFalse()
        assertThat(ThemeMode.DARK.isDark(systemInDark = true)).isTrue()
        assertThat(ThemeMode.DARK.isDark(systemInDark = false)).isTrue()
    }

    @Test
    public fun existemExatamenteTresOpcoes() {
        assertThat(ThemeMode.entries).hasSize(3)
    }
}
