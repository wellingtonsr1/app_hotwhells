package br.com.hotwheelscollector.feature.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.CarModel

/**
 * Busca-assistida e sugestão de modelo ao digitar (T049 - US2, FR-024).
 *
 * Exibe modelos já cadastrados cujo nome coincide parcialmente com o que está
 * sendo digitado, evitando que o colecionador cadastre o mesmo modelo repetidas vezes.
 */
@Composable
public fun ModelAutocomplete(
    query: String,
    availableModels: List<CarModel>,
    onSelectModel: (CarModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestions = remember(query, availableModels) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            emptyList()
        } else {
            availableModels.filter {
                it.name.contains(trimmed, ignoreCase = true)
            }.take(5)
        }
    }

    AnimatedVisibility(
        visible = suggestions.isNotEmpty(),
        modifier = modifier,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = stringResource(R.string.autocomplete_model_suggestion),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
                suggestions.forEach { model ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectModel(model) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = model.name,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Escala ${model.scale}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
