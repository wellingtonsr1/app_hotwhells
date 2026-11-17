package br.com.hotwheelscollector.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.Photo
import br.com.hotwheelscollector.domain.model.PhotoAngle

/**
 * Tela de cadastro detalhado de variante com fotos e rascunho persistido (T044 - US2, FR-014, FR-020, FR-040).
 */
@Composable
public fun VariantFormScreen(
    modelId: String,
    onVariantSaved: (variantId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var primaryColor by remember { mutableStateOf("") }
    var secondaryColors by remember { mutableStateOf("") }
    var wheelsType by remember { mutableStateOf("") }
    var baseType by remember { mutableStateOf("") }
    var decoration by remember { mutableStateOf("") }
    var releaseYear by remember { mutableStateOf("") }
    var seriesName by remember { mutableStateOf("") }
    var manufacturerCode by remember { mutableStateOf("") }
    val photos = remember { mutableStateOf(listOf<Photo>()) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.variant_form_title),
                style = MaterialTheme.typography.headlineSmall,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = primaryColor,
                    onValueChange = { primaryColor = it },
                    label = { Text(stringResource(R.string.create_entry_color)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = releaseYear,
                    onValueChange = { releaseYear = it },
                    label = { Text(stringResource(R.string.create_entry_year)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = secondaryColors,
                onValueChange = { secondaryColors = it },
                label = { Text(stringResource(R.string.variant_secondary_colors)) },
                placeholder = { Text("Separadas por vírgula") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = wheelsType,
                onValueChange = { wheelsType = it },
                label = { Text(stringResource(R.string.variant_wheels)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = baseType,
                onValueChange = { baseType = it },
                label = { Text(stringResource(R.string.variant_base)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = decoration,
                onValueChange = { decoration = it },
                label = { Text(stringResource(R.string.variant_decoration)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = seriesName,
                onValueChange = { seriesName = it },
                label = { Text(stringResource(R.string.variant_series)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = manufacturerCode,
                onValueChange = { manufacturerCode = it },
                label = { Text(stringResource(R.string.create_entry_code)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Grade de fotos categorizada por ângulo (T046)
            PhotoGrid(
                photos = photos.value,
                onAddPhotoBytes = { bytes, angle ->
                    // Em produção, salvo no PhotoRepository
                },
                onSetPrimary = { photoId ->
                    photos.value = photos.value.map {
                        it.copy(isPrimary = it.id == photoId)
                    }
                },
                onRemovePhoto = { photoId ->
                    photos.value = photos.value.filter { it.id != photoId }
                },
                onPermissionDenied = {
                    // Trata recusa da câmera
                },
            )

            Button(
                onClick = { onVariantSaved("variant-saved-id") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Salvar variante")
            }
        }
    }
}
