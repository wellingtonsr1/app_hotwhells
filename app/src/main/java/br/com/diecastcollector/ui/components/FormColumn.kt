package br.com.diecastcollector.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Coluna de leitura dos formulários (FR-311, FR-308).
 *
 * Em janela compacta ocupa a largura toda; a partir da média fica em [formMaxWidth]
 * centralizada, para o campo não medir 1.000 dp em um tablet. Quem usa escolhe a rolagem —
 * a coluna em si não rola, porque alguns formulários têm ação fixa no rodapé.
 */
@Composable
public fun FormColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .width(minOf(maxWidth, formMaxWidth))
                .fillMaxWidth(),
            content = content,
        )
    }
}
