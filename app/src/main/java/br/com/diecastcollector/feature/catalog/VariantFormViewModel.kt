package br.com.diecastcollector.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.PhotoAngle
import br.com.diecastcollector.domain.repository.CarModelRepository
import br.com.diecastcollector.domain.repository.ColorRepository
import br.com.diecastcollector.domain.repository.ExemplarRepository
import br.com.diecastcollector.domain.repository.NewExemplar
import br.com.diecastcollector.domain.repository.PhotoRepository
import br.com.diecastcollector.domain.repository.SeriesRepository
import br.com.diecastcollector.domain.repository.VariantRepository
import br.com.diecastcollector.domain.usecase.AddExemplarUseCase
import br.com.diecastcollector.domain.usecase.CreateVariantInput
import br.com.diecastcollector.domain.usecase.CreateVariantUseCase
import br.com.diecastcollector.domain.usecase.MergeVariantsUseCase
import br.com.diecastcollector.domain.usecase.SplitVariantUseCase
import br.com.diecastcollector.domain.usecase.UpdateVariantInput
import br.com.diecastcollector.domain.usecase.UpdateVariantUseCase
import br.com.diecastcollector.ui.UiMessage
import br.com.diecastcollector.ui.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
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

/** Outra variante ativa do mesmo modelo, para unir ou receber exemplares separados. */
public data class VariantOption(
    val id: String,
    val colorName: String? = null,
    val year: Int? = null,
    val manufacturerCode: String? = null,
)

/** Exemplar da variante em edição, com o código que o usuário vê na etiqueta. */
public data class VariantExemplarOption(
    val id: String,
    val internalCode: String,
    val condition: Condition,
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
    val message: UiMessage? = null,
    /** Variante já gravada numa tentativa anterior: não criar outra ao tentar de novo. */
    val createdVariantId: String? = null,
    /** Exemplar já gravado numa tentativa anterior: não duplicar ao tentar de novo. */
    val createdExemplarId: String? = null,
    val exemplarCode: String? = null,
    /** Preenchido só quando variante, exemplar e fotos terminaram: a tela navega. */
    val savedVariantId: String? = null,
    /** `true` quando a tela está completando uma variante existente, não criando outra. */
    val editing: Boolean = false,
    /** Edição: enquanto carrega, os campos vazios não podem ser salvos por engano. */
    val loading: Boolean = false,
    /** Data de criação/alteração da variante, exibida em edição (FR-032). */
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    /** Outras variantes ativas do mesmo modelo (FR-041/FR-042). */
    val siblingVariants: List<VariantOption> = emptyList(),
    /** Exemplares ativos da variante em edição. */
    val exemplars: List<VariantExemplarOption> = emptyList(),
    /** Seleção para separar exemplares. */
    val splitSelection: Set<String> = emptySet(),
    /** União ou arquivamento concluídos: a tela volta para a lista. */
    val variantClosed: Boolean = false,
    val runningAction: Boolean = false,
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
    private val variants: VariantRepository,
    private val updateVariant: UpdateVariantUseCase,
    private val exemplarRepository: ExemplarRepository,
    private val mergeVariants: MergeVariantsUseCase,
    private val splitVariant: SplitVariantUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(VariantFormUiState())
    public val state: StateFlow<VariantFormUiState> = _state.asStateFlow()

    private var editingVariantId: String? = null

    public fun onPrimaryColorChange(value: String) = _state.update { it.copy(primaryColor = value) }

    public fun onSecondaryColorsChange(value: String) = _state.update { it.copy(secondaryColors = value) }

    public fun onWheelsTypeChange(value: String) = _state.update { it.copy(wheelsType = value) }

    public fun onBaseTypeChange(value: String) = _state.update { it.copy(baseType = value) }

    public fun onDecorationChange(value: String) = _state.update { it.copy(decoration = value) }

    public fun onSeriesChange(value: String) = _state.update { it.copy(seriesName = value) }

    public fun onReleaseYearChange(value: String) = _state.update { it.copy(releaseYearText = value) }

    public fun onManufacturerCodeChange(value: String) = _state.update { it.copy(manufacturerCode = value) }

    /**
     * Código de barras lido no reconhecimento (FR-112).
     *
     * Preenche uma única vez e só quando o campo está vazio: a leitura nunca sobrescreve o
     * que o usuário já digitou nem entra no modo de edição de uma variante existente.
     */
    public fun prefillManufacturerCode(code: String?) {
        if (code.isNullOrBlank()) return
        _state.update { current ->
            if (current.editing || current.manufacturerCode.isNotBlank()) {
                current
            } else {
                current.copy(manufacturerCode = code)
            }
        }
    }

    public fun onConditionChange(condition: Condition) = _state.update { it.copy(condition = condition) }

    /** Captura nova entra na lista; a primeira é principal, como no repositório. */
    public fun onAddPhoto(bytes: ByteArray, angle: PhotoAngle) {
        if (bytes.isEmpty()) return
        _state.update { current ->
            val duplicate = current.photos.any { it.bytes.contentEquals(bytes) }
            current.copy(
                photos = current.photos + PendingPhoto(
                    id = UUID.randomUUID().toString(),
                    angle = angle,
                    bytes = bytes,
                    isPrimary = current.photos.isEmpty(),
                ),
                // FR-100: avisa sem bloquear; a foto entra do mesmo jeito.
                message = if (duplicate) {
                    UiMessage(R.string.photo_duplicate_warning)
                } else {
                    current.message
                },
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

    /** FR-028: arquiva a variante sem apagar exemplares, fotos nem histórico. */
    public fun onArchive() {
        val variantId = editingVariantId ?: return
        if (_state.value.runningAction) return
        _state.update { it.copy(runningAction = true) }
        viewModelScope.launch {
            variants.archive(variantId).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            runningAction = false,
                            variantClosed = true,
                            message = UiMessage(R.string.variant_archive_done),
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(runningAction = false, message = error.toMessage()) }
                },
            )
        }
    }

    /** FR-041: move os exemplares para [targetId] e arquiva esta variante. */
    public fun onMergeInto(targetId: String) {
        val variantId = editingVariantId ?: return
        if (_state.value.runningAction) return
        _state.update { it.copy(runningAction = true) }
        viewModelScope.launch {
            mergeVariants.merge(targetId = targetId, absorbedId = variantId).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            runningAction = false,
                            variantClosed = true,
                            message = UiMessage(R.string.variant_merge_done),
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(runningAction = false, message = error.toMessage()) }
                },
            )
        }
    }

    public fun onToggleSplitExemplar(exemplarId: String) = _state.update { current ->
        current.copy(
            splitSelection = if (exemplarId in current.splitSelection) {
                current.splitSelection - exemplarId
            } else {
                current.splitSelection + exemplarId
            },
        )
    }

    /** FR-042: leva os exemplares marcados para [targetId] sem tocar no histórico deles. */
    public fun onSplitInto(targetId: String) {
        val variantId = editingVariantId ?: return
        val current = _state.value
        if (current.runningAction) return
        _state.update { it.copy(runningAction = true) }
        viewModelScope.launch {
            splitVariant.split(
                sourceVariantId = variantId,
                targetVariantId = targetId,
                exemplarIds = current.splitSelection.toList(),
            ).fold(
                onSuccess = {
                    val remaining = exemplarRepository.observeByVariant(variantId).first()
                    _state.update {
                        it.copy(
                            runningAction = false,
                            splitSelection = emptySet(),
                            exemplars = remaining.map { exemplar ->
                                VariantExemplarOption(
                                    id = exemplar.id,
                                    internalCode = exemplar.internalCode,
                                    condition = exemplar.condition,
                                )
                            },
                            message = UiMessage(R.string.variant_split_done),
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(runningAction = false, message = error.toMessage()) }
                },
            )
        }
    }

    public fun onSave(modelId: String) {
        val current = _state.value
        if (current.saving) return

        val year = current.releaseYearText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        if (current.releaseYearText.isNotBlank() && year == null) {
            _state.update { it.copy(message = UiMessage(R.string.variant_invalid_year)) }
            return
        }

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                if (current.editing) {
                    updateExisting(current, year)
                    return@launch
                }
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
                            message = if (outcome.sameVariantCount > 1) {
                                UiMessage(
                                    R.string.add_exemplar_success_multiple,
                                    outcome.sameVariantCount,
                                )
                            } else {
                                UiMessage(
                                    R.string.add_exemplar_success_single,
                                    outcome.exemplar.internalCode,
                                )
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
                            message = UiMessage(R.string.variant_failed_photos, failedPhotos),
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
                it.copy(message = UiMessage(R.string.variant_already_existed))
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

    /**
     * Carrega a variante para edição (FR-029): mesmos campos do cadastro, preenchidos, e
     * sem o bloco de exemplar/fotos — completar cor e ano não cria uma segunda unidade.
     */
    public fun loadForEdit(variantId: String) {
        if (_state.value.editing || _state.value.loading) return
        _state.update { it.copy(editing = true, loading = true) }
        viewModelScope.launch {
            try {
                val variant = variants.findById(variantId)
                    ?: throw DomainError.NotFound
                if (variant.archivedAt != null) {
                    throw DomainError.Validation("Variante arquivada não pode ser editada.")
                }
                val colorNames = colors.observeAll().first().associateBy { it.id }
                val manufacturerId = models.findById(variant.modelId)?.manufacturerId
                val seriesName = manufacturerId?.let { id ->
                    seriesRepository.observeByManufacturer(id).first()
                        .firstOrNull { it.id == variant.seriesId }
                        ?.name
                }.orEmpty()
                val siblings = variants.observeByModel(variant.modelId).first()
                    .filter { it.id != variantId && it.archivedAt == null }
                    .map { sibling ->
                        VariantOption(
                            id = sibling.id,
                            colorName = sibling.primaryColorId?.let { id -> colorNames[id]?.name },
                            year = sibling.releaseYear,
                            manufacturerCode = sibling.manufacturerCode,
                        )
                    }
                val activeExemplars = exemplarRepository.observeByVariant(variantId).first()
                editingVariantId = variantId
                _state.update {
                    it.copy(
                        editing = true,
                        loading = false,
                        createdAt = variant.createdAt,
                        updatedAt = variant.updatedAt,
                        siblingVariants = siblings,
                        exemplars = activeExemplars.map { exemplar ->
                            VariantExemplarOption(
                                id = exemplar.id,
                                internalCode = exemplar.internalCode,
                                condition = exemplar.condition,
                            )
                        },
                        primaryColor = variant.primaryColorId
                            ?.let { id -> colorNames[id]?.name }
                            .orEmpty(),
                        secondaryColors = variant.secondaryColorIds
                            .mapNotNull { id -> colorNames[id]?.name }
                            .joinToString(", "),
                        wheelsType = variant.wheelsType.orEmpty(),
                        baseType = variant.baseType.orEmpty(),
                        decoration = variant.decoration.orEmpty(),
                        seriesName = seriesName,
                        releaseYearText = variant.releaseYear?.toString().orEmpty(),
                        manufacturerCode = variant.manufacturerCode.orEmpty(),
                    )
                }
            } catch (error: Throwable) {
                _state.update { it.copy(loading = false, message = error.toMessage()) }
            }
        }
    }

    private suspend fun updateExisting(current: VariantFormUiState, year: Int?) {
        val variantId = editingVariantId ?: throw DomainError.NotFound
        updateVariant(
            variantId,
            UpdateVariantInput(
                seriesId = resolveSeriesForVariant(current.seriesName),
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
        _state.update {
            it.copy(
                saving = false,
                savedVariantId = variantId,
                message = UiMessage(R.string.variant_success_update),
            )
        }
    }

    /** A série é resolvida no fabricante do modelo da variante, nunca presumindo taxonomia. */
    private suspend fun resolveSeriesForVariant(name: String): String? {
        val variantId = editingVariantId ?: throw DomainError.NotFound
        val modelId = variants.findById(variantId)?.modelId ?: throw DomainError.NotFound
        return resolveSeries(modelId, name)
    }

    private fun Throwable.toMessage(): UiMessage = when (this) {
        DomainError.Duplicate -> UiMessage(R.string.error_duplicate_variant)
        else -> toUiMessage(R.string.error_generic_save)
    }
}
