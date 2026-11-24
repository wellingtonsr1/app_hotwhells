@file:OptIn(ExperimentalMaterial3WindowSizeClassApi::class)

package br.com.diecastcollector.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Largura mínima de um cartão nas grades adaptativas (FR-306).
 *
 * Fica no intervalo de 160–180 dp para que o cartão do acervo nunca fique estreito demais
 * para a legenda de duas linhas. As telas que têm cartões com muito texto podem passar um
 * mínimo maior em [AdaptiveGrid]; o piso da spec continua este.
 */
public val gridMinSize: Dp = 168.dp

/** Largura máxima de leitura dos formulários (FR-311, SC-306). */
public val formMaxWidth: Dp = 720.dp

/**
 * Classe de janela vigente, sempre derivada do contêiner da tela.
 *
 * O padrão é a classe compacta (360 × 640 dp): previews e testes que não instalam o
 * provider continuam renderizando como celular, nunca como tablet por acidente.
 *
 * Toda leitura de classe no app passa por aqui — `LocalConfiguration`, `DisplayMetrics` e
 * `calculateWindowSizeClass(activity)` medem a janela do sistema, que em split-screen e
 * janela livre não é a largura que a tela realmente recebeu (FR-301).
 */
public val LocalWindowSizeClass: ProvidableCompositionLocal<WindowSizeClass> =
    staticCompositionLocalOf {
        WindowSizeClass.calculateFromSize(DpSize(360.dp, 640.dp))
    }

/** Classifica um contêiner de [width] × [height] pelo mesmo critério do provider. */
public fun windowSizeClassFor(width: Dp, height: Dp): WindowSizeClass =
    WindowSizeClass.calculateFromSize(DpSize(width, height))

/**
 * Instala a classificação uma única vez, a partir do espaço que o app recebeu.
 *
 * `BoxWithConstraints` mede o contêiner, e não a tela: em duas janelas cada lado recebe a
 * sua classe. Colocar o provider no topo evita que cada tela meça de novo e divirja.
 */
@Composable
public fun ProvideWindowSizeClass(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sizeClass = remember(maxWidth, maxHeight) {
            WindowSizeClass.calculateFromSize(DpSize(maxWidth, maxHeight))
        }
        CompositionLocalProvider(LocalWindowSizeClass provides sizeClass) {
            content()
        }
    }
}

/** Verdadeiro a partir da classe média (600 dp), quando a navegação vira trilho. */
public val WindowSizeClass.isMediumOrWider: Boolean
    get() = widthSizeClass != WindowWidthSizeClass.Compact

/** Verdadeiro na classe expandida (≥ 840 dp). */
public val WindowSizeClass.isExpanded: Boolean
    get() = widthSizeClass == WindowWidthSizeClass.Expanded
