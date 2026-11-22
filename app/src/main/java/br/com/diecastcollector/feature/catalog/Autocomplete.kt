package br.com.diecastcollector.feature.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.CarModel
import br.com.diecastcollector.domain.model.Manufacturer

/** Quantas sugestões cabem sem empurrar o formulário para fora da tela. */
private const val MAX_SUGGESTIONS = 5

/**
 * Fabricantes existentes que combinam com o que está sendo digitado.
 *
 * A comparação ignora caixa e separadores, para que "HotWheels", "hot wheels" e
 * "HOT-WHEELS" caiam todas em "Hot Wheels". O nome equivalente exato não vira sugestão:
 * se o usuário já digitou a marca, repeti-la na lista só ocupa espaço.
 */
internal fun manufacturerSuggestions(
    query: String,
    manufacturers: List<Manufacturer>,
): List<Manufacturer> {
    val needle = normalizeForSearch(query)
    if (needle.length < 2) return emptyList()
    return manufacturers
        .filter { manufacturer ->
            val candidate = normalizeForSearch(manufacturer.name)
            candidate.contains(needle) && candidate != needle
        }
        .take(MAX_SUGGESTIONS)
}

/** Forma comparável do nome: sem caixa, sem espaço e sem pontuação. */
private fun normalizeForSearch(value: String): String =
    value.lowercase().filter { it.isLetterOrDigit() }

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
            }.take(MAX_SUGGESTIONS)
        }
    }

    SuggestionCard(
        title = stringResource(R.string.autocomplete_model_suggestion),
        visible = suggestions.isNotEmpty(),
        modifier = modifier,
    ) {
        suggestions.forEach { model ->
            SuggestionRow(onClick = { onSelectModel(model) }) {
                Text(text = model.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = stringResource(R.string.autocomplete_model_scale, model.scale),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Sugestão de fabricante já cadastrado (FR-024 aplicado ao fabricante).
 *
 * Sem isso, "Hot Wheels", "hot wheels" e "HotWheels" viram três fabricantes distintos e
 * nenhum filtro por fabricante fecha a conta. Escolher a sugestão reaproveita o registro;
 * ignorar a lista continua criando um nome novo, porque a base aceita qualquer marca.
 */
@Composable
public fun ManufacturerAutocomplete(
    query: String,
    availableManufacturers: List<Manufacturer>,
    onSelectManufacturer: (Manufacturer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestions = remember(query, availableManufacturers) {
        manufacturerSuggestions(query, availableManufacturers)
    }

    SuggestionCard(
        title = stringResource(R.string.autocomplete_manufacturer_suggestion),
        visible = suggestions.isNotEmpty(),
        modifier = modifier,
    ) {
        suggestions.forEach { manufacturer ->
            SuggestionRow(onClick = { onSelectManufacturer(manufacturer) }) {
                Text(text = manufacturer.name, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    title: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
                content()
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        content = content,
    )
}
