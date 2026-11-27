package br.com.diecastcollector.feature.duplicates

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.com.diecastcollector.R

/**
 * Aviso de preservação da coleção para exemplar único (T509, FR-515, RN-003).
 *
 * Aparece em dois momentos — ao confirmar uma disponibilidade que torna a única unidade
 * ofertável e ao abrir a negociação — e não impede a operação: apenas explica que, concluída
 * a venda ou a troca, o exemplar deixa de integrar a coleção ativa. Não há "não mostrar de
 * novo": a decisão é patrimonial e precisa continuar visível.
 *
 * **Cancelar** é a ação de descarte: quem chama não pode gravar nem navegar nesse caminho.
 */
@Composable
public fun UniqueExemplarDialog(
    onContinue: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.unique_exemplar_warning_title)) },
        text = { Text(stringResource(R.string.unique_exemplar_warning_body)) },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(stringResource(R.string.unique_exemplar_warning_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.unique_exemplar_warning_cancel))
            }
        },
    )
}
