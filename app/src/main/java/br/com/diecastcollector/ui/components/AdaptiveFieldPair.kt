package br.com.diecastcollector.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Par de campos que fica lado a lado quando há largura para dois campos legíveis e
 * empilha em coluna quando não há (FR-312).
 *
 * O limite é medido no contêiner: em 360 dp — inclusive com fonte ampliada — os dois campos
 * não cabem em [minFieldWidth] cada e viram linha própria; a partir da janela média eles
 * continuam lado a lado. Cada tela passa o `OutlinedTextField` com o `Modifier` recebido,
 * então o campo ocupa `weight(1f)` na linha e a largura toda na coluna.
 */
@Composable
public fun AdaptiveFieldPair(
    modifier: Modifier = Modifier,
    minFieldWidth: Dp = 180.dp,
    spacing: Dp = 12.dp,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth >= minFieldWidth * 2 + spacing) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        }
    }
}
