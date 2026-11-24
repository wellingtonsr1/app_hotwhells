package br.com.diecastcollector.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.ui.components.LocalWindowSizeClass
import br.com.diecastcollector.ui.components.windowSizeClassFor

/**
 * Enquadramento de teste por classe de janela (T307, VII.14).
 *
 * A tela do teste é sempre a do emulador; para exercitar 360, 700 e 1.000 dp sem trocar
 * `wm size`, o conteúdo é renderizado dentro de uma caixa com a largura pedida e recebe a
 * classe calculada dessa largura. O provider da produção é o mesmo, então o teste cobre a
 * derivação de verdade — não uma classe injetada à mão.
 */
public fun windowSizeClassOf(width: Dp, height: Dp = 800.dp): WindowSizeClass =
    windowSizeClassFor(width, height)

/**
 * Renderiza [content] em uma caixa de [width] × [height] dp com a classe correspondente.
 *
 * `requiredWidth`/`requiredHeight` em vez de `width`/`height`: a raiz do teste é a tela do
 * emulador, e uma caixa de 1.000 dp dentro de 411 dp seria encolhida — o teste passaria a
 * medir a tela, que é exatamente o erro que a feature corrige. Com a exigência, o conteúdo
 * é medido no tamanho pedido mesmo que ultrapasse a raiz.
 */
@Composable
public fun WindowSizeFrame(
    width: Dp,
    height: Dp = 800.dp,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClassOf(width, height)) {
        Box(modifier = Modifier.requiredWidth(width).requiredHeight(height)) {
            content()
        }
    }
}

/**
 * Confere que [width] cai na classe esperada.
 *
 * Falha explícita em vez de seguir com uma classe silenciosamente diferente: um teste que
 * diz "média" e roda na compacta não prova nada.
 */
public fun assertWindowWidthClass(width: Dp, expected: WindowWidthSizeClass) {
    val actual = windowSizeClassOf(width).widthSizeClass
    check(actual == expected) {
        "Largura de $width caiu em $actual, mas o teste espera $expected"
    }
}
