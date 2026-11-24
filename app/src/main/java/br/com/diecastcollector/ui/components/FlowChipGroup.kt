package br.com.diecastcollector.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Grupo de opções que quebra em linhas adicionais (FR-313).
 *
 * Os `Row` de chips do app cresciam para a direita e cortavam a última condição em 360 dp
 * com fonte ampliada. Aqui a ordem e a seleção continuam as mesmas — só a linha passa a
 * quebrar em vez de sair da tela.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun FlowChipGroup(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
    ) {
        // O escopo do FlowRow fica aqui dentro: quem chama coloca chips, e o opt-in da
        // API experimental não vaza para as telas.
        content()
    }
}
