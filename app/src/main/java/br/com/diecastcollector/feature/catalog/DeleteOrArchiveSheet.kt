package br.com.diecastcollector.feature.catalog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.repository.ExemplarDependents

/**
 * Confirmação de arquivamento (FR-028): descreve o efeito antes de agir.
 * Arquivar preserva fotos, aquisição, avaliações e histórico (Art. I.7).
 */
@Composable
public fun ArchiveExemplarDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exemplar_archive_title)) },
        text = { Text(stringResource(R.string.exemplar_archive_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.exemplar_archive_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

/**
 * Confirmação de exclusão (FR-028, Art. I.6).
 *
 * Com qualquer vínculo não existe botão de excluir: a tela informa o que está no caminho e
 * oferece arquivar. Sem vínculos, a exclusão física ainda exige confirmação explícita.
 */
@Composable
public fun DeleteExemplarDialog(
    dependents: ExemplarDependents,
    onConfirm: () -> Unit,
    onArchive: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (dependents.hasAny) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.exemplar_dependents_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.exemplar_dependents_message,
                        dependentsDescription(dependents),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onArchive) {
                    Text(stringResource(R.string.exemplar_archive_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_close))
                }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.exemplar_delete_title)) },
            text = { Text(stringResource(R.string.exemplar_delete_message)) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.exemplar_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

/** Lista legível dos vínculos, montada na apresentação para não carregar idioma no domínio. */
@Composable
private fun dependentsDescription(dependents: ExemplarDependents): String = listOfNotNull(
    dependents.photos.takeIf { it > 0 }
        ?.let { pluralStringResource(R.plurals.exemplar_dep_photos, it, it) },
    dependents.acquisitions.takeIf { it > 0 }
        ?.let { pluralStringResource(R.plurals.exemplar_dep_acquisitions, it, it) },
    dependents.valuations.takeIf { it > 0 }
        ?.let { pluralStringResource(R.plurals.exemplar_dep_valuations, it, it) },
    dependents.movements.takeIf { it > 0 }
        ?.let { pluralStringResource(R.plurals.exemplar_dep_movements, it, it) },
    dependents.negotiations.takeIf { it > 0 }
        ?.let { pluralStringResource(R.plurals.exemplar_dep_negotiations, it, it) },
).joinToString(", ")
