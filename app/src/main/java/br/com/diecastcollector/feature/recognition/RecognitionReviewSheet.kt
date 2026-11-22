package br.com.diecastcollector.feature.recognition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.RecognitionSource
import br.com.diecastcollector.domain.recognition.RecognitionField
import br.com.diecastcollector.domain.recognition.RecognitionProposal
import br.com.diecastcollector.domain.recognition.RecognitionSuggestion
import kotlin.math.roundToInt

/**
 * Revisão de sugestões com aceite campo a campo (T127, FR-104).
 *
 * A folha não grava nada: só devolve, em [onApply], os campos que o usuário marcou. Cada
 * sugestão nasce **desmarcada** — aceitar é um ato explícito por campo, não um efeito de
 * abrir a folha. Quando a leitura foi inconclusiva, a folha diz "não identificado" e não
 * oferece nenhum campo para aplicar (FR-105).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun RecognitionReviewSheet(
    proposal: RecognitionProposal,
    onApply: (Map<RecognitionField, String>) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var acceptedFields by remember { mutableStateOf(emptySet<RecognitionField>()) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.recognition_review_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.recognition_review_origin, originLabel(proposal.source)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!proposal.identified) {
                Text(
                    text = stringResource(R.string.recognition_review_unidentified_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(text = stringResource(R.string.recognition_review_unidentified_body))
                TextButton(onClick = onDismissRequest, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.recognition_review_dismiss))
                }
                return@Column
            }

            Text(
                text = stringResource(R.string.recognition_review_intro),
                style = MaterialTheme.typography.bodyMedium,
            )

            proposal.suggestions.forEachIndexed { index, suggestion ->
                if (index > 0) HorizontalDivider()
                ReviewRow(
                    suggestion = suggestion,
                    accepted = suggestion.field in acceptedFields,
                    onToggle = { checked ->
                        acceptedFields = if (checked) {
                            acceptedFields + suggestion.field
                        } else {
                            acceptedFields - suggestion.field
                        }
                    },
                )
            }

            if (proposal.suggestions.size > 1) {
                TextButton(
                    onClick = {
                        acceptedFields = if (acceptedFields.size == proposal.suggestions.size) {
                            emptySet()
                        } else {
                            proposal.suggestions.map { it.field }.toSet()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (acceptedFields.size == proposal.suggestions.size) {
                                R.string.recognition_review_clear
                            } else {
                                R.string.recognition_review_accept_all
                            },
                        ),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.recognition_review_dismiss))
                }
                Button(
                    onClick = {
                        onApply(
                            proposal.suggestions
                                .filter { it.field in acceptedFields }
                                .associate { it.field to it.value },
                        )
                    },
                    enabled = acceptedFields.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.recognition_review_apply))
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(
    suggestion: RecognitionSuggestion,
    accepted: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recognition-field-${suggestion.field.name}")
            .toggleable(
                value = accepted,
                role = Role.Checkbox,
                onValueChange = onToggle,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = accepted, onCheckedChange = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fieldLabel(suggestion.field),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = suggestion.value,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(
                    R.string.recognition_review_confidence,
                    (suggestion.confidence * 100).roundToInt(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun fieldLabel(field: RecognitionField): String = stringResource(
    when (field) {
        RecognitionField.MANUFACTURER -> R.string.recognition_field_manufacturer
        RecognitionField.MODEL -> R.string.recognition_field_model
        RecognitionField.SERIES -> R.string.recognition_field_series
        RecognitionField.YEAR -> R.string.recognition_field_year
        RecognitionField.VARIANT -> R.string.recognition_field_variant
        RecognitionField.CODE -> R.string.recognition_field_code
    },
)

@Composable
private fun originLabel(source: RecognitionSource): String = stringResource(
    when (source) {
        RecognitionSource.PHOTO -> R.string.recognition_source_photo
        RecognitionSource.OCR -> R.string.recognition_source_ocr
        RecognitionSource.BARCODE -> R.string.recognition_source_barcode
        RecognitionSource.CATALOG -> R.string.recognition_source_catalog
    },
)
