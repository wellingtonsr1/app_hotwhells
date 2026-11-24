package br.com.diecastcollector.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.R

/** Uma ação de cartão ou de cabeçalho, com rótulo curto. */
public data class CardAction(
    public val label: String,
    public val onClick: () -> Unit,
)

/**
 * Ações lado a lado que viram menu quando não couberem (FR-309, FR-311).
 *
 * Cada ação reserva [minActionWidth] para o rótulo caber inteiro. Se a soma não couber na
 * largura do contêiner, as ações passam para um menu de três pontos em vez de serem
 * cortadas na borda ou amassarem o título.
 */
@Composable
public fun AdaptableCardActions(
    actions: List<CardAction>,
    modifier: Modifier = Modifier,
    minActionWidth: Dp = 96.dp,
) {
    if (actions.isEmpty()) return

    BoxWithConstraints(modifier = modifier) {
        val fitsInRow = maxWidth >= minActionWidth * actions.size
        if (fitsInRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions.forEach { action ->
                    TextButton(onClick = action.onClick) {
                        Text(
                            text = action.label,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        } else {
            var expanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.common_more_actions),
                        modifier = Modifier.size(24.dp),
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    actions.forEach { action ->
                        DropdownMenuItem(
                            text = { Text(action.label) },
                            onClick = {
                                expanded = false
                                action.onClick()
                            },
                        )
                    }
                }
            }
        }
    }
}
