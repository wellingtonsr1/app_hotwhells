package br.com.hotwheelscollector.feature.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.IncompleteEntry
import br.com.hotwheelscollector.domain.model.MissingField

/**
 * Cadastros incompletos (T048 - US2, FR-029): modelo que ficou sem variante e variante que
 * ficou sem cor principal ou sem ano.
 *
 * A lista é derivada da consulta, não armazenada — no momento em que o campo é preenchido,
 * a linha sai sozinha. Modelo sem variante abre «Editar modelo»; variante sem cor ou sem
 * ano abre «Editar variante». Nenhum dos dois caminhos apaga ou recria o registro.
 */
@Composable
public fun IncompleteEntriesScreen(
    onSelectModel: (modelId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IncompleteEntriesViewModel = hiltViewModel(),
    onEditModel: (modelId: String) -> Unit = {},
    onEditVariant: (variantId: String) -> Unit = {},
    message: String? = null,
    onMessageShown: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    IncompleteEntriesContent(
        state = state,
        onSelectModel = onSelectModel,
        onEditModel = onEditModel,
        onEditVariant = onEditVariant,
        message = message,
        onMessageShown = onMessageShown,
        modifier = modifier,
    )
}

@Composable
internal fun IncompleteEntriesContent(
    state: IncompleteEntriesUiState,
    onSelectModel: (modelId: String) -> Unit,
    modifier: Modifier = Modifier,
    onEditModel: (modelId: String) -> Unit = {},
    onEditVariant: (variantId: String) -> Unit = {},
    message: String? = null,
    onMessageShown: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // A confirmação volta pela navegação: a tela de edição sai no mesmo instante em que
    // salva, então é aqui que o usuário vê que o registro saiu da lista (Art. IV.4).
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.incomplete_entries_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            when {
                state.isEmpty -> Text(
                    text = stringResource(R.string.incomplete_entries_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.entries, key = { it.key }) { entry ->
                        IncompleteModelCard(
                            entry = entry,
                            onClick = { onSelectModel(entry.modelId) },
                            onEditModel = { onEditModel(entry.modelId) },
                            onEditVariant = { entry.variantId?.let(onEditVariant) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MissingField.labelRes(): Int = when (this) {
    MissingField.PRIMARY_COLOR -> R.string.incomplete_missing_color
    MissingField.RELEASE_YEAR -> R.string.incomplete_missing_year
}

@Composable
private fun IncompleteModelCard(
    entry: IncompleteEntry,
    onClick: () -> Unit,
    onEditModel: () -> Unit,
    onEditVariant: () -> Unit,
) {
    // Só o modelo sem variante tem para onde voltar. Um cartão que parece clicável e não
    // faz nada seria pior que um cartão sem toque.
    val click = if (entry.hasVariant) Modifier else Modifier.clickable(onClick = onClick)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(click),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.modelName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.incomplete_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Text(
                text = "${entry.manufacturerName} · ${entry.scale}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            val missing = entry.missingFields.map { field -> stringResource(field.labelRes()) }
            Text(
                text = if (missing.isEmpty()) {
                    stringResource(R.string.incomplete_missing_variant)
                } else {
                    stringResource(R.string.incomplete_missing_fields, missing.joinToString(", "))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (!entry.hasVariant) {
                TextButton(
                    onClick = onEditModel,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Text(stringResource(R.string.incomplete_edit_model))
                }
            } else if (entry.variantId != null) {
                TextButton(
                    onClick = onEditVariant,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Text(stringResource(R.string.incomplete_edit_variant))
                }
            }
        }
    }
}
