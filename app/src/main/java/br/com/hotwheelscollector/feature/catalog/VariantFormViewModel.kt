package br.com.hotwheelscollector.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.PhotoAngle
import br.com.hotwheelscollector.domain.repository.CarModelRepository
import br.com.hotwheelscollector.domain.repository.ColorRepository
import br.com.hotwheelscollector.domain.repository.NewExemplar
import br.com.hotwheelscollector.domain.repository.PhotoRepository
import br.com.hotwheelscollector.domain.repository.SeriesRepository
import br.com.hotwheelscollector.domain.usecase.AddExemplarUseCase
import br.com.hotwheelscollector.domain.usecase.CreateVariantInput
import br.com.hotwheelscollector.domain.usecase.CreateVariantUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Captura que ainda não tem exemplar para receber o arquivo, guardada só em memória. */
public data class PendingPhoto(
    val id: String,
    val angle: PhotoAngle,
    val bytes: ByteArray,
    val isPrimary: Boolean = false,
)

public data class VariantFormUiState(
    val primaryColor: String = "",
    val secondaryColors: String = "",
    val wheelsType: String = "",
    val baseType: String = "",
    val decoration: String = "",
    val seriesName: String = "",
    val releaseYearText: String = "",
    val manufacturerCode: String = "",
    val condition: Condition = Condition.SEALED,
    val photos: List<PendingPhoto> = emptyList(),
    val saving: Boolean = false,
    val message: String? = null,
    /** Variante já gravada numa tentativa anterior: não criar outra ao tentar de novo. */
    val createdVariantId: String? = null,
    /** Exemplar já gravado numa tentativa anterior: não duplicar ao tentar de novo. */
    val createdExemplarId: String? = null,
    val exemplarCode: String? = null,
    /** Preenchido só quando variante, exemplar e fotos terminaram: a tela navega. */
    val savedVariantId: String? = null,
)

/**
 * Cadastro completo de miniatura (US2): cria a variante e o primeiro exemplar — que é
 * quem recebe as fotos, porque no schema `photo` pertence a `exemplar` (FR-020).
 *
 * A ordem não é arbitrária: o `PhotoRepository` exige um `exemplarId`, então as capturas
 * ficam em memória até o exemplar existir. Se alguma foto falhar, o que já foi gravado
 * permanece e só as pendentes ficam para a próxima tentativa; a variante e o exemplar
 * **não** são recriados, porque ficam registrados no estado.
 */
@HiltViewModel
public class VariantFormViewModel @Inject constructor(
    private val createVariant: CreateVariantUseCase,
    private val addExemplar: AddExemplarUseCase,
    private val photoRepository: PhotoRepository,
    private val models: CarModelRepository,
    private val colors: ColorRepository,
    private val seriesRepository: SeriesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(VariantFormUiState())
    public val state: StateFlow<VariantFormUiState> = _state.asStateFlow()

    public fun onPrimaryColorChange(value: String) = _state.update { it.copy(primaryColor = value) }

    public fun onSecondaryColorsChange(value: String) = _state.update { it.copy(secondaryColors = value) }

    public fun onWheelsTypeChange(value: String) = _state.update { it.copy(wheelsType = value) }

    public fun onBaseTypeChange(value: String) = _state.update { it.copy(baseType = value) }

    public fun onDecorationChange(value: String) = _state.update { it.copy(decoration = value) }

    public fun onSeriesChange(value: String) = _state.update { it.copy(seriesName = value) }

    public fun onReleaseYearChange(value: String) = _state.update { it.copy(releaseYearText = value) }

    public fun onManufacturerCodeChange(value: String) = _state.update { it.copy(manufacturerCode = value) }

    public fun onConditionChange(condition: Condition) = _state.update { it.copy(condition = condition) }

    /** Captura nova entra na lista; a primeira é principal, como no repositório. */
    public fun onAddPhoto(bytes: ByteArray, angle: PhotoAngle) {
        if (bytes.isEmpty()) return
        _state.update { current ->
            current.copy(
                photos = current.photos + PendingPhoto(
                    id = UUID.randomUUID().toString(),
                    angle = angle,
                    bytes = bytes,
                    isPrimary = current.photos.isEmpty(),
                ),
            )
        }
    }

    public fun onSetPrimary(photoId: String) = _state.update { current ->
        current.copy(photos = current.photos.map { it.copy(isPrimary = it.id == photoId) })
    }

    /** Remover a principal promove a próxima: o exemplar nunca fica sem foto principal. */
    public fun onRemovePhoto(photoId: String) = _state.update { current ->
        val remaining = current.photos.filterNot { it.id == photoId }
        val hadPrimary = remaining.size != current.photos.size &&
            current.photos.firstOrNull { it.id == photoId }?.isPrimary == true
        current.copy(
            photos = if (hadPrimary && remaining.isNotEmpty()) {
                remaining.mapIndexed { index, photo -> photo.copy(isPrimary = index == 0) }
            } else {
                remaining
            },
        )
    }

    public fun onMessageShown() = _state.update { it.copy(message = null) }

    public fun onSave(modelId: String) {
        val current = _state.value
        if (current.saving) return

        val year = current.releaseYearText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        if (current.releaseYearText.isNotBlank() && year == null) {
            _state.update { it.copy(message = "Ano inválido. Use quatro dígitos, ex.: 2024.") }
            return
        }

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val variantId = current.createdVariantId ?: createVariantFor(modelId, current, year)
                    .also { id -> _state.update { it.copy(createdVariantId = id) } }

                val exemplarId = current.createdExemplarId ?: run {
                    val outcome = addExemplar(
                        NewExemplar(variantId = variantId, condition = current.condition),
                    ).getOrThrow()
                    _state.update {
                        it.copy(
                            createdExemplarId = outcome.exemplar.id,
                            exemplarCode = outcome.exemplar.internalCode,
                            message = buildString {
                                append("Exemplar ${outcome.exemplar.internalCode} cadastrado.")
                                if (outcome.sameVariantCount > 1) {
                                    append(" ${outcome.sameVariantCount}º exemplar desta variante.")
                                }
                            },
                        )
                    }
                    outcome.exemplar.id
                }

                val failedPhotos = flushPhotos(exemplarId)

                if (failedPhotos == 0) {
                    _state.update { it.copy(saving = false, savedVariantId = variantId) }
                } else {
                    _state.update {
                        it.copy(
                            saving = false,
                            message = "$failedPhotos foto(s) não puderam ser gravadas. " +
                                "Toque em salvar para tentar de novo.",
                        )
                    }
                }
            } catch (error: Throwable) {
                _state.update { it.copy(saving = false, message = error.toMessage()) }
            }
        }
    }

    private suspend fun createVariantFor(modelId: String, current: VariantFormUiState, year: Int?): String {
        val outcome = createVariant(
            CreateVariantInput(
                modelId = modelId,
                seriesId = resolveSeries(modelId, current.seriesName),
                primaryColorId = resolveColor(current.primaryColor),
                secondaryColorIds = current.secondaryColors
                    .split(",", ";")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { name -> colors.findOrCreate(name).getOrThrow().id },
                wheelsType = current.wheelsType,
                baseType = current.baseType,
                decoration = current.decoration,
                releaseYear = year,
                manufacturerCode = current.manufacturerCode,
            ),
        ).getOrThrow()

        if (outcome.alreadyExisted) {
            _state.update {
                it.copy(message = "Variante idêntica já existia e foi reaproveitada.")
            }
        }
        return outcome.variant.id
    }

    private suspend fun resolveColor(name: String): String? =
        name.trim().takeIf { it.isNotEmpty() }?.let { colors.findOrCreate(it).getOrThrow().id }

    /** Série é resolvida por nome dentro do fabricante do modelo; não se presume taxonomia. */
    private suspend fun resolveSeries(modelId: String, name: String): String? {
        val trimmed = name.trim().takeIf { it.isNotEmpty() } ?: return null
        val manufacturerId = models.findById(modelId)?.manufacturerId
            ?: throw DomainError.Validation("Modelo não encontrado.")
        val existing = seriesRepository.observeByManufacturer(manufacturerId).first()
            .firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        return existing?.id ?: seriesRepository.add(manufacturerId, trimmed).getOrThrow().id
    }

    /** Grava as pendentes e as remove do estado conforme dão certo; devolve quantas falharam. */
    private suspend fun flushPhotos(exemplarId: String): Int {
        var failed = 0
        _state.value.photos.forEach { pending ->
            photoRepository.add(exemplarId, pending.angle, pending.bytes).fold(
                onSuccess = { saved ->
                    if (pending.isPrimary) {
                        photoRepository.setPrimary(exemplarId, saved.id)
                    }
                    _state.update { current ->
                        current.copy(photos = current.photos.filterNot { it.id == pending.id })
                    }
                },
                onFailure = { failed++ },
            )
        }
        return failed
    }

    private fun Throwable.toMessage(): String = when (this) {
        is DomainError.Validation -> reason
        DomainError.Duplicate -> "Já existe uma variante com estes atributos."
        is DomainError.Blocked -> reason
        is DomainError.Storage -> "Falha ao gravar: ${reason}"
        else -> message ?: "Não foi possível salvar."
    }
}
