package br.com.diecastcollector.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Corpo de diálogo com altura relativa ao espaço disponível (FR-316, T367).
 *
 * As listas de opções (unir, separar, série pai) cresciam sem limite e, em paisagem, empurravam
 * os botões de confirmar/cancelar para fora do diálogo. Aqui o corpo ocupa no máximo
 * [maxHeightFraction] da altura que o diálogo oferece e rola dentro disso; os botões ficam
 * sempre no rodapé, fora da rolagem.
 */
@Composable
public fun DialogList(
    modifier: Modifier = Modifier,
    maxHeightFraction: Float = 0.55f,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(4.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // O slot `text` do AlertDialog pode chegar sem altura máxima; nesse caso o limite
        // vem da tela, senão a rolagem receberia altura infinita.
        val limit = if (maxHeight == Dp.Infinity) {
            (LocalConfiguration.current.screenHeightDp * maxHeightFraction).dp
        } else {
            maxHeight * maxHeightFraction
        }
        Column(
            modifier = Modifier
                .heightIn(max = limit)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}
