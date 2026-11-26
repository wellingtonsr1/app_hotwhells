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
    // Papéis que os componentes do Material 3 usam por padrão (cartão, barra de navegação,
    // indicador, contornos). Sem eles, o componente cai na paleta lavanda do Material 3 e
    // sai da identidade do app (FR-408, T414).
    surfaceBright = PureWhite,
    surfaceDim = Chalk,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = PureWhite,
    surfaceContainer = Chalk,
    surfaceContainerHigh = PureWhite,
    surfaceContainerHighest = PureWhite,
    inverseSurface = InkBlack,
    inverseOnSurface = Chalk,
    outline = MutedGrey,
    outlineVariant = MutedGrey.copy(alpha = 0.4f),
    secondaryContainer = HotOrangeLight,
    onSecondaryContainer = InkBlack,
    tertiary = MutedGrey,
    onTertiary = PureWhite,
    errorContainer = Chalk,
    onErrorContainer = FlameRed,
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
    // Mesmos papéis do modo claro, com os tokens escuros: cartão em grafite sobre o fundo
    // tinta, barra em grafite e contorno legível — nenhuma superfície lavanda (T414).
    surfaceBright = SmokyGrey,
    surfaceDim = InkBlack,
    surfaceContainerLowest = InkBlack,
    surfaceContainerLow = Graphite,
    surfaceContainer = Graphite,
    surfaceContainerHigh = SmokyGrey,
    surfaceContainerHighest = Graphite,
    inverseSurface = Chalk,
    inverseOnSurface = InkBlack,
    outline = MutedGrey,
    outlineVariant = SmokyGrey,
    secondaryContainer = SmokyGrey,
    onSecondaryContainer = Chalk,
    tertiary = MutedGrey,
    onTertiary = InkBlack,
    errorContainer = SmokyGrey,
    onErrorContainer = FlameRedDark,
)

/**
 * Tema do aplicativo (Spec 006).
 *
 * A resolução da aparência acontece só aqui: [ThemeMode.SYSTEM] consulta o tema do
 * Android (inclusive mudanças com o app aberto); [ThemeMode.LIGHT] e [ThemeMode.DARK]
 * prevalecem sobre ele. As únicas paletas são `LightColors` e `DarkColors` — não existe
 * segunda tabela de cores nem segunda função de tema (FR-405).
 *
 * Cores dinâmicas ficam desligadas de propósito: a identidade visual é parte do produto.
 * O padrão [ThemeMode.SYSTEM] mantém previews e testes existentes compilando sem mudança
 * (FR-429).
 */
@Composable
fun DiecastTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = mode.isDark(isSystemInDarkTheme())
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DiecastTypography,
        content = content,
    )
}
