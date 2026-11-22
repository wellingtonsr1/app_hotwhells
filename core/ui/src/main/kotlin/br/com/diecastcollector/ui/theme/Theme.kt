package br.com.diecastcollector.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = FlameRed,
    onPrimary = PureWhite,
    primaryContainer = HotOrangeLight,
    onPrimaryContainer = InkBlack,
    secondary = HotOrange,
    onSecondary = PureWhite,
    background = Chalk,
    onBackground = InkBlack,
    surface = PureWhite,
    onSurface = InkBlack,
    surfaceVariant = Chalk,
    onSurfaceVariant = MutedGrey,
    error = FlameRed,
    onError = PureWhite,
)

private val DarkColors = darkColorScheme(
    primary = FlameRedDark,
    onPrimary = InkBlack,
    primaryContainer = FlameRed,
    onPrimaryContainer = PureWhite,
    secondary = HotOrangeLight,
    onSecondary = InkBlack,
    background = InkBlack,
    onBackground = Chalk,
    surface = Graphite,
    onSurface = Chalk,
    surfaceVariant = SmokyGrey,
    onSurfaceVariant = Chalk,
    error = FlameRedDark,
    onError = InkBlack,
)

/**
 * Tema do aplicativo. Segue a preferência do sistema por padrão e permite escolha
 * explícita nas configurações (tema claro/escuro/sistema).
 * Cores dinâmicas ficam desligadas de propósito: a identidade visual é parte do produto.
 */
@Composable
fun DiecastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DiecastTypography,
        content = content,
    )
}
