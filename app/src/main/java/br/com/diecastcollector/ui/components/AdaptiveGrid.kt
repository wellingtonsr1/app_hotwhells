package br.com.diecastcollector.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Grade que decide o número de colunas pela largura disponível (FR-306, FR-307).
 *
 * Substitui `GridCells.Fixed(2)` e as listas de cartão de largura total. O mínimo padrão é
 * [gridMinSize]; telas com cartões de muito texto podem aumentar o mínimo para ter menos
 * colunas em vez de cartões apertados.
 */
@Composable
public fun AdaptiveGrid(
    modifier: Modifier = Modifier,
    minItemSize: Dp = gridMinSize,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minItemSize),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/**
 * Colunas que [AdaptiveGrid] produz para uma largura, com o mesmo arredondamento.
 *
 * Existe para telas que precisam alinhar algo à grade (um cabeçalho na primeira linha, por
 * exemplo) sem repetir a conta. Não é usada pelo próprio grid, que mede sozinho.
 */
public fun columnsFor(
    width: Dp,
    minItemSize: Dp = gridMinSize,
    horizontalPadding: Dp = 16.dp,
    spacing: Dp = 12.dp,
): Int {
    val usable = width - horizontalPadding * 2
    if (usable <= 0.dp) return 1
    return ((usable + spacing) / (minItemSize + spacing)).toInt().coerceAtLeast(1)
}
