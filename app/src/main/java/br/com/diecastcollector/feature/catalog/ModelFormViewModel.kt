package br.com.diecastcollector.feature.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.model.CarModel
import br.com.diecastcollector.domain.model.RealVehicle
import br.com.diecastcollector.domain.repository.CarModelRepository
import br.com.diecastcollector.domain.repository.RealVehicleRepository
import br.com.diecastcollector.navigation.RouteArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Erros que a tela traduz em mensagem; o ViewModel não guarda texto. */
public enum class ModelFormError {
    REQUIRED_NAME,
    INVALID_SCALE,
    DUPLICATE,
    INVALID_YEAR,
    STORAGE,
}

public data class ModelFormUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val modelName: String = "",
    val scale: String = DEFAULT_SCALE,
    val bodyType: String = "",
    val material: String = "",
    val countryOfManufacture: String = "",
    val notes: String = "",
    val realVehicles: List<RealVehicle> = emptyList(),
    val realVehicleId: String? = null,
    val vehicleMake: String = "",
    val vehicleModel: String = "",
    val vehicleYear: String = "",
    val vehicleCountry: String = "",
    val error: ModelFormError? = null,
    val saved: Boolean = false,
)

/**
 * Edição do modelo (T043, FR-014) e cadastro do veículo real que faltava.
 *
 * O modelo nasce no fluxo do `+ Mini`; aqui o colecionador completa o que ficou de fora —
 * carroceria, material, país, observações — e, se quiser, aponta o carro do mundo real.
 * Um veículo novo digitado nos campos é criado e vinculado no mesmo salvamento.
 */
@HiltViewModel
public class ModelFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val carModelRepository: CarModelRepository,
    private val realVehicleRepository: RealVehicleRepository,
) : ViewModel() {

    private val modelId: String = checkNotNull(savedStateHandle[RouteArgs.MODEL_ID]) {
        "Rota do modelo sem modelId."
    }

    private val _state: MutableStateFlow<ModelFormUiState> = MutableStateFlow(ModelFormUiState())
    public val state: StateFlow<ModelFormUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val model = carModelRepository.findById(modelId)
            _state.update { current ->
                if (model == null) {
                    current.copy(loading = false, error = ModelFormError.STORAGE)
                } else {
                    current.copy(
                        loading = false,
                        modelName = model.name,
                        scale = model.scale,
                        bodyType = model.bodyType.orEmpty(),
                        material = model.material.orEmpty(),
                        countryOfManufacture = model.countryOfManufacture.orEmpty(),
                        notes = model.notes.orEmpty(),
                        realVehicleId = model.realVehicleId,
                    )
                }
            }
        }
        viewModelScope.launch {
            realVehicleRepository.observeAll().collect { vehicles ->
                _state.update { it.copy(realVehicles = vehicles) }
            }
        }
    }

    public fun setName(value: String) = _state.update { it.copy(modelName = value) }
    public fun setScale(value: String) = _state.update { it.copy(scale = value) }
    public fun setBodyType(value: String) = _state.update { it.copy(bodyType = value) }
    public fun setMaterial(value: String) = _state.update { it.copy(material = value) }
    public fun setCountryOfManufacture(value: String) =
        _state.update { it.copy(countryOfManufacture = value) }

    public fun setNotes(value: String) = _state.update { it.copy(notes = value) }
    public fun selectRealVehicle(id: String?) = _state.update { it.copy(realVehicleId = id) }
    public fun setVehicleMake(value: String) = _state.update { it.copy(vehicleMake = value) }
    public fun setVehicleModel(value: String) = _state.update { it.copy(vehicleModel = value) }
    public fun setVehicleYear(value: String) =
        _state.update { it.copy(vehicleYear = value.filter(Char::isDigit).take(4)) }

    public fun setVehicleCountry(value: String) = _state.update { it.copy(vehicleCountry = value) }
    public fun clearError() = _state.update { it.copy(error = null) }

    public fun save() {
        val current = _state.value
        if (current.saving || current.loading) return

        if (current.modelName.isBlank()) {
            _state.update { it.copy(error = ModelFormError.REQUIRED_NAME) }
            return
        }
        if (!SCALE_PATTERN.matches(current.scale.trim())) {
            _state.update { it.copy(error = ModelFormError.INVALID_SCALE) }
            return
        }
        val year = current.vehicleYear.toIntOrNull()
        if (current.vehicleYear.isNotBlank() && year == null) {
            _state.update { it.copy(error = ModelFormError.INVALID_YEAR) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            var realVehicleId = current.realVehicleId
            if (current.vehicleMake.isNotBlank() && current.vehicleModel.isNotBlank()) {
                val created = realVehicleRepository.add(
                    make = current.vehicleMake,
                    model = current.vehicleModel,
                    year = year,
                    country = current.vehicleCountry,
                )
                created.fold(
                    onSuccess = { realVehicleId = it.id },
                    onFailure = { failure ->
                        _state.update {
                            it.copy(
                                saving = false,
                                error = failure.toFormError(ModelFormError.INVALID_YEAR),
                            )
                        }
                        return@launch
                    },
                )
            }

            val result = carModelRepository.update(
                id = modelId,
                name = current.modelName,
                scale = current.scale,
                bodyType = current.bodyType,
                material = current.material,
                countryOfManufacture = current.countryOfManufacture,
                notes = current.notes,
                realVehicleId = realVehicleId,
            )
            result.fold(
                onSuccess = { saved ->
                    applySaved(saved, realVehicleId)
                },
                onFailure = { failure ->
                    _state.update {
                        it.copy(saving = false, error = failure.toFormError(ModelFormError.STORAGE))
                    }
                },
            )
        }
    }

    private fun applySaved(model: CarModel, realVehicleId: String?) {
        _state.update {
            it.copy(
                saving = false,
                saved = true,
                realVehicleId = realVehicleId,
                modelName = model.name,
                scale = model.scale,
            )
        }
    }

    private fun Throwable.toFormError(fallback: ModelFormError): ModelFormError = when (this) {
        is DomainError.Validation -> when {
            message?.contains("Escala", ignoreCase = true) == true -> ModelFormError.INVALID_SCALE
            message?.contains("Já existe", ignoreCase = true) == true -> ModelFormError.DUPLICATE
            message?.contains("Ano", ignoreCase = true) == true -> ModelFormError.INVALID_YEAR
            message?.contains("Nome", ignoreCase = true) == true -> ModelFormError.REQUIRED_NAME
            else -> fallback
        }
        else -> fallback
    }

    private companion object {
        val SCALE_PATTERN = Regex("""^1:[0-9]{1,3}$""")
    }
}

private const val DEFAULT_SCALE = "1:64"
