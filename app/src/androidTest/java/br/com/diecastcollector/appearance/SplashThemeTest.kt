package br.com.diecastcollector.appearance

import android.os.Build
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.MainActivity
import br.com.diecastcollector.R
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Abertura do app (Android 12+).
 *
 * O splash é desenhado pelo sistema a partir do tema da janela. Ele reserva só o fundo da
 * paleta — que já acompanha claro/escuro — e um ícone vazio: a marca aparece uma única vez,
 * na arte que vem em seguida. Se `values-v31` deixar de ser aplicado, o sistema volta ao
 * ícone do app por padrão e o teste quebra.
 */
@RunWith(AndroidJUnit4::class)
public class SplashThemeTest {

    @get:Rule
    public val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    public fun splashMostraSoOFundoDaPaleta() {
        assumeTrue(
            "Splash nativo só existe a partir do API 31",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        )

        val array = composeRule.activity.theme.obtainStyledAttributes(
            intArrayOf(
                android.R.attr.windowSplashScreenBackground,
                android.R.attr.windowSplashScreenAnimatedIcon,
            ),
        )
        try {
            assertEquals(R.color.window_background, array.getResourceId(0, 0))
            assertEquals(R.drawable.ic_splash_blank, array.getResourceId(1, 0))
        } finally {
            array.recycle()
        }
    }
}
