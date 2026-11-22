package br.com.diecastcollector.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.model.CarModel
import br.com.diecastcollector.domain.model.Manufacturer
import br.com.diecastcollector.domain.repository.CarModelRepository
import br.com.diecastcollector.domain.repository.DraftRepository
import br.com.diecastcollector.domain.repository.ManufacturerRepository
import br.com.diecastcollector.domain.usecase.CreateCatalogEntryUseCase
import br.com.diecastcollector.domain.usecase.NewCatalogEntry
import br.com.diecastcollector.ui.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Primeiro passo da miniatura: fabricante, modelo e escala.
 *
 * Para no modelo de propósito — cor, rodas, base, decoração, série e fotos são da variante
 * e do exemplar, e são coletados na tela seguinte. Assim o cadastro mínimo não cria uma
 * variante que ninguém consegue completar, e não há duas formas de cadastrar a mesma coisa.
 *
 * Inclui rascunho recuperável após interrupção (FR-023) e sugestões de fabricantes e
 * modelos existentes (FR-024).
 */
public data class CreateCatalogEntryUiState(
    val manufacturer: String = "",
    val model: String = "",
    val scale: String = "1:64",
    val availableManufacturers: List<Manufacturer> = emptyList(),
    val availableModels: List<CarModel> = emptyList(),
    val saving: Boolean = false,
    val message: UiMessage? = null,
    val createdModelId: String? = null,
)

@HiltViewModel
public class CreateCatalogEntryViewModel @Inject constructor(
    private val createEntry: CreateCatalogEntryUseCase,
    private val carModelRepository: CarModelRepository,
    private val manufacturerRepository: ManufacturerRepository,
    private val draftRepository: DraftRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateCatalogEntryUiState())
    public val state: StateFlow<CreateCatalogEntryUiState> = _state.asStateFlow()

    init {
        // Observa fabricantes e modelos para autocomplete (FR-024)
        viewModelScope.launch {
            manufacturerRepository.observeActive().collect { manufacturers ->
                _state.update { it.copy(availableManufacturers = manufacturers) }
            }
        }
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

    public fun onSelectManufacturerSuggestion(manufacturer: Manufacturer) {
        // Só o fabricante muda: modelo e escala continuam o que o usuário digitou.
        _state.update { it.copy(manufacturer = manufacturer.name) }
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

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            createEntry(
                NewCatalogEntry(
                    manufacturerName = current.manufacturer,
                    modelName = current.model,
                    scale = current.scale,
                ),
            ).fold(
                onSuccess = { model ->
                    // Limpa rascunho ao concluir com sucesso
                    draftRepository.clearDraft(DRAFT_KEY)
                    _state.update { it.copy(saving = false, createdModelId = model.id) }
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
            val serialized = "${current.manufacturer}|${current.model}|${current.scale}"
            draftRepository.saveDraft(DRAFT_KEY, serialized)
        }
    }

    private fun parseDraft(raw: String) {
        val parts = raw.split("|")
        if (parts.size >= 3) {
            _state.update {
                it.copy(
                    manufacturer = parts[0],
                    model = parts[1],
                    scale = parts[2],
                )
            }
        }
    }

    private fun Throwable.toMessage(): UiMessage = when (this) {
        is DomainError.Validation -> UiMessage(R.string.error_validation, reason)
        is DomainError.Storage -> UiMessage(R.string.create_catalog_error_save, reason)
        else -> UiMessage(R.string.create_catalog_error_model)
    }

    private companion object {
        const val DRAFT_KEY = "catalog_entry"
    }
}
