package br.com.diecastcollector.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Coluna de leitura dos formulários (FR-311, FR-308).
 *
 * Em janela compacta ocupa a largura toda; a partir da média fica em [formMaxWidth]
 * centralizada, para o campo não medir 1.000 dp em um tablet.
 *
 * A rolagem é opcional porque alguns formulários têm ação fixa no rodapé, fora da coluna.
 * Quando ligada, o padding de [contentPadding] é aplicado dentro da rolagem — o conteúdo
 * começa com respiro e o último campo continua alcançável.
 */
@Composable
public fun FormColumn(
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .width(minOf(maxWidth, formMaxWidth))
                .fillMaxWidth()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(contentPadding),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}
