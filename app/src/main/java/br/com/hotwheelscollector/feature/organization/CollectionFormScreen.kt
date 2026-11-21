package br.com.hotwheelscollector.feature.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.hotwheelscollector.R

@Composable
public fun CollectionFormScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    CollectionFormContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onParentChange = viewModel::onParentChange,
        onSave = viewModel::onSave,
        onMessageShown = viewModel::onMessageShown,
        onCancel = onDone,
        modifier = modifier,
    )
}

@Composable
internal fun CollectionFormContent(
    state: CollectionFormUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onParentChange: (String?) -> Unit,
    onSave: () -> Unit,
    onMessageShown: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val messageText = state.message?.text()

    LaunchedEffect(state.message) {
        messageText?.let {
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
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    if (state.editing) {
                        R.string.collection_form_edit_title
                    } else {
                        R.string.collection_form_title
                    },
                ),
                style = MaterialTheme.typography.headlineSmall,
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.collection_form_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.collection_form_description)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            ParentPicker(
                state = state,
                onParentChange = onParentChange,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onSave,
                    enabled = !state.saving && state.name.isNotBlank(),
                ) {
                    Text(stringResource(R.string.collection_form_save))
                }
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        }
    }
}

@Composable
private fun ParentPicker(
    state: CollectionFormUiState,
    onParentChange: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = state.parents.firstOrNull { it.id == state.parentId }?.name
        ?: stringResource(R.string.collection_form_no_parent)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.collection_form_parent),
            style = MaterialTheme.typography.labelLarge,
        )
        Box {
            TextButton(onClick = { expanded = true }) { Text(selectedName) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.collection_form_no_parent)) },
                    onClick = {
                        onParentChange(null)
                        expanded = false
                    },
                )
                state.parents.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        onClick = {
                            onParentChange(option.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
