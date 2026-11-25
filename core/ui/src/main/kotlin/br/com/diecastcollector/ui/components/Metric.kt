package br.com.diecastcollector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Barra de progresso simples (T354): uma definição só, usada pelo painel, pelas coleções e
 * pelo detalhe da coleção. Não depende da assinatura de `LinearProgressIndicator` do
 * Material3, que já mudou entre versões.
 */
@Composable
public fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    barHeight: Dp = 8.dp,
) {
    val clamped = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(barHeight / 2))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/**
 * Cartão de indicador (T354): o painel mostra o número antes do rótulo; o financeiro mostra
 * o rótulo antes do número. A ordem é a única diferença, então virou parâmetro em vez de
 * duas funções particulares que já tinham começado a divergir.
 */
@Composable
public fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueFirst: Boolean = false,
    tag: String? = null,
) {
    Card(modifier = if (tag == null) modifier else modifier.testTag(tag)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            if (valueFirst) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = value, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
