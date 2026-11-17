package br.com.hotwheelscollector.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.domain.error.DomainError
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
 * Cadastro mínimo: fabricante, modelo, escala e o que diferencia a variante.
 *
 * É o degrau que faltava para a US1 ser alcançável: sem variante, não há onde
 * pendurar exemplar. Campos desconhecidos ficam em branco e viram `null` (Art. I.9).
 */
public data class CreateCatalogEntryUiState(
    val manufacturer: String = "",
    val model: String = "",
    val scale: String = "1:64",
    val color: String = "",
    val yearText: String = "",
    val manufacturerCode: String = "",
    val saving: Boolean = false,
    val message: String? = null,
    val createdVariantId: String? = null,
)

@HiltViewModel
public class CreateCatalogEntryViewModel @Inject constructor(
    private val createEntry: CreateCatalogEntryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateCatalogEntryUiState())
    public val state: StateFlow<CreateCatalogEntryUiState> = _state.asStateFlow()

    public fun onManufacturerChange(value: String) = _state.update { it.copy(manufacturer = value) }

    public fun onModelChange(value: String) = _state.update { it.copy(model = value) }

    public fun onScaleChange(value: String) = _state.update { it.copy(scale = value) }

    public fun onColorChange(value: String) = _state.update { it.copy(color = value) }

    public fun onYearChange(value: String) = _state.update { it.copy(yearText = value) }

    public fun onManufacturerCodeChange(value: String) =
        _state.update { it.copy(manufacturerCode = value) }

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

    private fun Throwable.toMessage(): String = when (this) {
        is DomainError.Validation -> reason
        DomainError.Duplicate -> "Já existe uma variante igual a essa. Abra a existente."
        is DomainError.Storage -> "Falha ao salvar: ${reason}"
        else -> message ?: "Não foi possível salvar."
    }
}
