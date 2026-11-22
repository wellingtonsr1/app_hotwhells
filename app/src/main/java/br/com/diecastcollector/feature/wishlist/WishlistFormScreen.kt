package br.com.diecastcollector.feature.wishlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.VariantSummary

/** Formulário de desejo (T076): o que se procura, por quanto e onde. */
@Composable
public fun WishlistFormScreen(
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WishlistFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val messageText = state.message?.text()

    LaunchedEffect(state.message) {
        messageText?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }
    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        WishlistFormContent(
            state = state,
            modifier = Modifier.padding(padding),
            actions = WishlistFormActions(
                onTitleChange = viewModel::onTitleChange,
                onVariantQueryChange = viewModel::onVariantQueryChange,
                onSelectVariant = viewModel::onSelectVariant,
                onClearVariant = viewModel::onClearVariant,
                onPriorityChange = viewModel::onPriorityChange,
                onMaxPriceChange = viewModel::onMaxPriceChange,
                onFoundPriceChange = viewModel::onFoundPriceChange,
                onStoreChange = viewModel::onStoreChange,
                onReferenceUrlChange = viewModel::onReferenceUrlChange,
                onNotesChange = viewModel::onNotesChange,
                onSave = viewModel::onSave,
            ),
        )
    }
}

internal data class WishlistFormActions(
    val onTitleChange: (String) -> Unit = {},
    val onVariantQueryChange: (String) -> Unit = {},
    val onSelectVariant: (VariantSummary) -> Unit = {},
    val onClearVariant: () -> Unit = {},
    val onPriorityChange: (Int) -> Unit = {},
    val onMaxPriceChange: (String) -> Unit = {},
    val onFoundPriceChange: (String) -> Unit = {},
    val onStoreChange: (String) -> Unit = {},
    val onReferenceUrlChange: (String) -> Unit = {},
    val onNotesChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
)

@Composable
internal fun WishlistFormContent(
    state: WishlistFormUiState,
    actions: WishlistFormActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(
                if (state.editing) R.string.wishlist_form_edit_title
                else R.string.wishlist_form_title,
            ),
            style = MaterialTheme.typography.headlineSmall,
        )

        OutlinedTextField(
            value = state.title,
            onValueChange = actions.onTitleChange,
            label = { Text(stringResource(R.string.wishlist_form_free_text)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.variantQuery,
            onValueChange = actions.onVariantQueryChange,
            label = { Text(stringResource(R.string.wishlist_form_variant_search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.variantLabel != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.wishlist_form_variant_selected, state.variantLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = actions.onClearVariant) {
                    Text(stringResource(R.string.wishlist_form_clear_variant))
                }
            }
        } else {
            val suggestions = state.variants
                .filter { summary ->
                    state.variantQuery.length >= 2 && (
                        summary.modelName.contains(state.variantQuery, ignoreCase = true) ||
                            summary.manufacturerName.contains(state.variantQuery, ignoreCase = true)
                        )
                }
                .take(MAX_SUGGESTIONS)
            if (suggestions.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        suggestions.forEach { summary ->
                            Text(
                                text = variantLabel(summary),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { actions.onSelectVariant(summary) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.wishlist_form_priority),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.priority == 1,
                onClick = { actions.onPriorityChange(1) },
                label = { Text(stringResource(R.string.wishlist_priority_high)) },
            )
            FilterChip(
                selected = state.priority == 2,
                onClick = { actions.onPriorityChange(2) },
                label = { Text(stringResource(R.string.wishlist_priority_medium)) },
            )
            FilterChip(
                selected = state.priority == 3,
                onClick = { actions.onPriorityChange(3) },
                label = { Text(stringResource(R.string.wishlist_priority_low)) },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.maxPriceText,
                onValueChange = actions.onMaxPriceChange,
                label = { Text(stringResource(R.string.wishlist_form_max_price)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.foundPriceText,
                onValueChange = actions.onFoundPriceChange,
                label = { Text(stringResource(R.string.wishlist_form_found_price)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }

        OutlinedTextField(
            value = state.store,
            onValueChange = actions.onStoreChange,
            label = { Text(stringResource(R.string.wishlist_form_store)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.referenceUrl,
            onValueChange = actions.onReferenceUrlChange,
            label = { Text(stringResource(R.string.wishlist_form_link)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.notes,
            onValueChange = actions.onNotesChange,
            label = { Text(stringResource(R.string.wishlist_form_notes)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = actions.onSave,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.wishlist_form_save))
        }
    }
}

private const val MAX_SUGGESTIONS = 5

private fun variantLabel(summary: VariantSummary): String = buildString {
    append(summary.manufacturerName)
    append(" · ")
    append(summary.modelName)
    summary.releaseYear?.let {
        append(" · ")
        append(it)
    }
}
