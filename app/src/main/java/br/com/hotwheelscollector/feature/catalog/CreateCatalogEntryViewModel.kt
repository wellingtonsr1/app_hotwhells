package br.com.hotwheelscollector.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.model.CarModel
import br.com.hotwheelscollector.domain.repository.CarModelRepository
import br.com.hotwheelscollector.domain.repository.DraftRepository
import br.com.hotwheelscollector.domain.usecase.CreateCatalogEntryUseCase
import br.com.hotwheelscollector.domain.usecase.NewCatalogEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Cadastro de miniatura: fabricante, modelo, escala e o que diferencia a variante.
 *
 * Inclui rascunho recuperável após interrupção (FR-023) e sugestões de modelos existentes (FR-024).
 */
public data class CreateCatalogEntryUiState(
    val manufacturer: String = "",
    val model: String = "",
    val scale: String = "1:64",
    val color: String = "",
    val yearText: String = "",
    val manufacturerCode: String = "",
    val availableModels: List<CarModel> = emptyList(),
    val saving: Boolean = false,
    val message: String? = null,
    val createdVariantId: String? = null,
)

@HiltViewModel
public class CreateCatalogEntryViewModel @Inject constructor(
    private val createEntry: CreateCatalogEntryUseCase,
    private val carModelRepository: CarModelRepository,
    private val draftRepository: DraftRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateCatalogEntryUiState())
    public val state: StateFlow<CreateCatalogEntryUiState> = _state.asStateFlow()

    init {
        // Observa modelos para autocomplete (FR-024)
        viewModelScope.launch {
            carModelRepository.observeActive().collect { models ->
                _state.update { it.copy(availableModels = models) }
            }
        }

        // Recupera rascunho anterior se houver (FR-023)
        viewModelScope.launch {
            val draft = draftRepository.getDraft(DRAFT_KEY)
            if (draft != null) {
                parseDraft(draft)
            }
        }
    }

    public fun onManufacturerChange(value: String) {
        _state.update { it.copy(manufacturer = value) }
        saveDraftAsync()
    }

    public fun onModelChange(value: String) {
        _state.update { it.copy(model = value) }
        saveDraftAsync()
    }

    public fun onScaleChange(value: String) {
        _state.update { it.copy(scale = value) }
        saveDraftAsync()
    }

    public fun onColorChange(value: String) {
        _state.update { it.copy(color = value) }
        saveDraftAsync()
    }

    public fun onYearChange(value: String) {
        _state.update { it.copy(yearText = value) }
        saveDraftAsync()
    }

    public fun onManufacturerCodeChange(value: String) {
        _state.update { it.copy(manufacturerCode = value) }
        saveDraftAsync()
    }

    public fun onSelectModelSuggestion(model: CarModel) {
        _state.update {
            it.copy(
                model = model.name,
                scale = model.scale,
            )
        }
        saveDraftAsync()
    }

    public fun onMessageShown() = _state.update { it.copy(message = null) }

    public fun onSave() {
        val current = _state.value
        if (current.saving) return

        val year = current.yearText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        if (current.yearText.isNotBlank() && year == null) {
            _state.update { it.copy(message = "Ano inválido. Use quatro dígitos, ex.: 2024.") }
            return
        }

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            runCatching {
                createEntry(
                    NewCatalogEntry(
                        manufacturerName = current.manufacturer,
                        modelName = current.model,
                        scale = current.scale,
                        colorName = current.color,
                        releaseYear = year,
                        manufacturerCode = current.manufacturerCode,
                    ),
                )
            }.fold(
                onSuccess = { result ->
                    result.fold(
                        onSuccess = { variant ->
                            // Limpa rascunho ao concluir com sucesso
                            draftRepository.clearDraft(DRAFT_KEY)
                            _state.update {
                                it.copy(saving = false, createdVariantId = variant.id)
                            }
                        },
                        onFailure = { error ->
                            _state.update { it.copy(saving = false, message = error.toMessage()) }
                        },
                    )
                },
                onFailure = { error ->
                    _state.update { it.copy(saving = false, message = error.toMessage()) }
                },
            )
        }
    }

    private fun saveDraftAsync() {
        val current = _state.value
        viewModelScope.launch {
            val serialized = "${current.manufacturer}|${current.model}|${current.scale}|${current.color}|${current.yearText}|${current.manufacturerCode}"
            draftRepository.saveDraft(DRAFT_KEY, serialized)
        }
    }

    private fun parseDraft(raw: String) {
        val parts = raw.split("|")
        if (parts.size >= 6) {
            _state.update {
                it.copy(
                    manufacturer = parts[0],
                    model = parts[1],
                    scale = parts[2],
                    color = parts[3],
                    yearText = parts[4],
                    manufacturerCode = parts[5],
                )
            }
        }
    }

    private fun Throwable.toMessage(): String = when (this) {
        is DomainError.Validation -> reason
        is DomainError.Duplicate -> "Já existe uma variante com estes atributos."
        else -> message ?: "Erro ao salvar miniatura."
    }

    private companion object {
        const val DRAFT_KEY = "catalog_entry"
    }
}
