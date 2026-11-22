package br.com.diecastcollector.feature.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.model.Acquisition
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.domain.model.IsoDate
import br.com.diecastcollector.domain.model.Location
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.model.Movement
import br.com.diecastcollector.domain.model.PackagingCondition
import br.com.diecastcollector.domain.model.Valuation
import br.com.diecastcollector.domain.repository.AcquisitionRepository
import br.com.diecastcollector.domain.repository.ExemplarDependents
import br.com.diecastcollector.domain.repository.ExemplarRepository
import br.com.diecastcollector.domain.repository.LocationRepository
import br.com.diecastcollector.domain.repository.ValuationRepository
import br.com.diecastcollector.navigation.RouteArgs
import br.com.diecastcollector.ui.UiMessage
import br.com.diecastcollector.ui.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado da edição de um exemplar.
 *
 * `dependents` existe na tela antes de qualquer tentativa de exclusão: o botão já aparece
 * explicando por que está bloqueado, em vez de o usuário tocar e receber um erro depois
 * (FR-028, Art. I.6).
 */
public data class ExemplarDetailUiState(
    val exemplarId: String = "",
    val internalCode: String = "",
    val status: ExemplarStatus? = null,
    val condition: Condition = Condition.SEALED,
    val packagingCondition: PackagingCondition? = null,
    val notes: String = "",
    val locations: List<Location> = emptyList(),
    val currentLocationId: String? = null,
    val targetLocationId: String? = null,
    val moveNote: String = "",
    val movements: List<Movement> = emptyList(),
    val dependents: ExemplarDependents = ExemplarDependents(),
    val rowVersion: Long = 0L,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val notFound: Boolean = false,
    val message: UiMessage? = null,
    val finished: Boolean = false,

    // Aquisição (FR-074–FR-076): editável aqui, uma linha por exemplar.
    val acquisition: Acquisition? = null,
    val acquisitionPrice: String = "",
    val acquisitionShipping: String = "",
    val acquisitionDiscount: String = "",
    val acquisitionDate: String = "",
    val acquisitionStore: String = "",
    val acquisitionSeller: String = "",
    val acquisitionMethod: String = "",
    val acquisitionNotes: String = "",
    val savingAcquisition: Boolean = false,

    // Avaliações (FR-082–FR-084): append-only, mais recente primeiro.
    val valuations: List<Valuation> = emptyList(),
    val valuationValue: String = "",
    val valuationDate: String = "",
    val valuationSource: String = "",
    val valuationNotes: String = "",
    val savingValuation: Boolean = false,
) {
    public val isArchived: Boolean get() = status == ExemplarStatus.ARCHIVED

    /** O botão de excluir só habilita sem vínculos; com qualquer um, a tela oferece arquivar. */
    public val canDelete: Boolean get() = !loading && !notFound && !dependents.hasAny

    /** Avaliação mais recente = valor atual (FR-083). */
    public val currentValuation: Valuation? get() = valuations.firstOrNull()
}

@HiltViewModel
public class ExemplarDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exemplarRepository: ExemplarRepository,
    locationRepository: LocationRepository,
    private val acquisitionRepository: AcquisitionRepository,
    private val valuationRepository: ValuationRepository,
    private val clock: Clock,
) : ViewModel() {

    private val exemplarId: String = savedStateHandle[RouteArgs.EXEMPLAR_ID] ?: ""
    private val today: String = IsoDate.today(clock).formatPtBr()

    private val _state = MutableStateFlow(
        ExemplarDetailUiState(
            exemplarId = exemplarId,
            acquisitionDate = today,
            valuationDate = today,
        ),
    )
    public val state: StateFlow<ExemplarDetailUiState> = _state.asStateFlow()

    init {
        refresh()
        loadAcquisition()
        viewModelScope.launch {
            locationRepository.observeActive().collect { locations ->
                _state.update { it.copy(locations = locations) }
            }
        }
        viewModelScope.launch {
            locationRepository.movements(exemplarId).collect { movements ->
                _state.update { it.copy(movements = movements) }
            }
        }
        viewModelScope.launch {
            valuationRepository.observeByExemplar(exemplarId).collect { valuations ->
                _state.update { it.copy(valuations = valuations) }
            }
        }
    }

    public fun onConditionChange(condition: Condition) = _state.update { it.copy(condition = condition) }

    public fun onPackagingChange(packaging: PackagingCondition?) =
        _state.update { it.copy(packagingCondition = packaging) }

    public fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    public fun onTargetLocationChange(locationId: String?) =
        _state.update { it.copy(targetLocationId = locationId) }

    public fun onMoveNoteChange(value: String) = _state.update { it.copy(moveNote = value) }

    public fun onMessageShown() = _state.update { it.copy(message = null) }

    // ---- Aquisição ----------------------------------------------------------------

    public fun onAcquisitionPriceChange(value: String) =
        _state.update { it.copy(acquisitionPrice = value) }

    public fun onAcquisitionShippingChange(value: String) =
        _state.update { it.copy(acquisitionShipping = value) }

    public fun onAcquisitionDiscountChange(value: String) =
        _state.update { it.copy(acquisitionDiscount = value) }

    public fun onAcquisitionDateChange(value: String) =
        _state.update { it.copy(acquisitionDate = value) }

    public fun onAcquisitionStoreChange(value: String) =
        _state.update { it.copy(acquisitionStore = value) }

    public fun onAcquisitionSellerChange(value: String) =
        _state.update { it.copy(acquisitionSeller = value) }

    public fun onAcquisitionMethodChange(value: String) =
        _state.update { it.copy(acquisitionMethod = value) }

    public fun onAcquisitionNotesChange(value: String) =
        _state.update { it.copy(acquisitionNotes = value) }

    public fun onSaveAcquisition() {
        val current = _state.value
        if (current.savingAcquisition || current.loading || current.notFound) return

        val price = parseMoney(current.acquisitionPrice)
        val shipping = parseMoney(current.acquisitionShipping)
        val discount = parseMoney(current.acquisitionDiscount)
        if (price.isInvalid || shipping.isInvalid || discount.isInvalid) {
            _state.update { it.copy(message = UiMessage(R.string.error_invalid_money)) }
            return
        }
        val date = IsoDate.parsePtBrOrNull(current.acquisitionDate)
        if (date == null) {
            _state.update { it.copy(message = UiMessage(R.string.error_invalid_date)) }
            return
        }

        val existing = current.acquisition
        val now = Instant.now(clock)
        val acquisition = Acquisition(
            id = existing?.id ?: UUID.randomUUID().toString(),
            exemplarId = exemplarId,
            date = date,
            price = price.value,
            shipping = shipping.value,
            discount = discount.value,
            store = current.acquisitionStore.trim().ifBlank { null },
            seller = current.acquisitionSeller.trim().ifBlank { null },
            method = current.acquisitionMethod.trim().ifBlank { null },
            notes = current.acquisitionNotes.trim().ifBlank { null },
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )

        _state.update { it.copy(savingAcquisition = true) }
        viewModelScope.launch {
            acquisitionRepository.save(acquisition).fold(
                onSuccess = { saved ->
                    _state.update {
                        it.copy(
                            savingAcquisition = false,
                            message = UiMessage(R.string.exemplar_acquisition_saved),
                        )
                    }
                    applyAcquisition(saved)
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(savingAcquisition = false, message = error.toMessage())
                    }
                },
            )
        }
    }

    // ---- Avaliações ---------------------------------------------------------------

    public fun onValuationValueChange(value: String) =
        _state.update { it.copy(valuationValue = value) }

    public fun onValuationDateChange(value: String) =
        _state.update { it.copy(valuationDate = value) }

    public fun onValuationSourceChange(value: String) =
        _state.update { it.copy(valuationSource = value) }

    public fun onValuationNotesChange(value: String) =
        _state.update { it.copy(valuationNotes = value) }

    public fun onAddValuation() {
        val current = _state.value
        if (current.savingValuation || current.loading || current.notFound) return

        val value = parseMoney(current.valuationValue)
        if (value.isInvalid || value.value == null || value.value.cents <= 0L) {
            _state.update {
                it.copy(message = UiMessage(R.string.exemplar_valuation_value_required))
            }
            return
        }
        val date = IsoDate.parsePtBrOrNull(current.valuationDate)
        if (date == null) {
            _state.update { it.copy(message = UiMessage(R.string.error_invalid_date)) }
            return
        }

        val valuation = Valuation(
            id = UUID.randomUUID().toString(),
            exemplarId = exemplarId,
            date = date,
            value = value.value,
            source = current.valuationSource.trim().ifBlank { null },
            notes = current.valuationNotes.trim().ifBlank { null },
            createdAt = Instant.now(clock),
        )

        _state.update { it.copy(savingValuation = true) }
        viewModelScope.launch {
            valuationRepository.add(valuation).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            savingValuation = false,
                            valuationValue = "",
                            valuationSource = "",
                            valuationNotes = "",
                            valuationDate = today,
                            message = UiMessage(R.string.exemplar_valuation_registered),
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(savingValuation = false, message = error.toMessage()) }
                },
            )
        }
    }

    // ---- Exemplar, movimentação e arquivamento ------------------------------------

    public fun onSaveDetails() {
        val current = _state.value
        if (current.saving || current.loading || current.notFound) return

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = exemplarRepository.updateDetails(
                id = exemplarId,
                condition = current.condition,
                packagingCondition = current.packagingCondition,
                notes = current.notes.trim().ifBlank { null },
                expectedRowVersion = current.rowVersion,
            )
            result.fold(
                onSuccess = { exemplar ->
                    _state.update {
                        it.copy(
                            saving = false,
                            status = exemplar.status,
                            condition = exemplar.condition,
                            packagingCondition = exemplar.packagingCondition,
                            notes = exemplar.notes.orEmpty(),
                            rowVersion = exemplar.rowVersion,
                            message = UiMessage(R.string.exemplar_changes_saved),
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(saving = false, message = error.toMessage()) }
                },
            )
        }
    }

    /** Mover grava localização e movimentação juntas; o histórico recarrega pelo `Flow`. */
    public fun onMove() {
        val current = _state.value
        if (current.saving || current.loading || current.notFound) return
        if (current.targetLocationId == current.currentLocationId && current.moveNote.isBlank()) {
            _state.update {
                it.copy(message = UiMessage(R.string.exemplar_move_note_required))
            }
            return
        }

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = exemplarRepository.move(
                id = exemplarId,
                toLocationId = current.targetLocationId,
                note = current.moveNote.trim().ifBlank { null },
            )
            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            saving = false,
                            moveNote = "",
                            message = UiMessage(R.string.exemplar_location_updated),
                        )
                    }
                    refresh(showLoading = false)
                },
                onFailure = { error ->
                    _state.update { it.copy(saving = false, message = error.toMessage()) }
                },
            )
        }
    }

    public fun onArchive() {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            exemplarRepository.archive(exemplarId).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            saving = false,
                            finished = true,
                            message = UiMessage(R.string.exemplar_archived),
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { it.copy(saving = false, message = error.toMessage()) }
                },
            )
        }
    }

    public fun onDelete() {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            exemplarRepository.delete(exemplarId).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            saving = false,
                            finished = true,
                            message = UiMessage(R.string.exemplar_deleted),
                        )
                    }
                },
                onFailure = { error ->
                    // Bloqueio por vínculo: recarrega os dependentes e deixa a tela explicar.
                    _state.update { it.copy(saving = false, message = error.toMessage()) }
                    refresh(showLoading = false)
                },
            )
        }
    }

    private fun refresh(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) _state.update { it.copy(loading = true) }
            val exemplar = exemplarRepository.findById(exemplarId)
            val dependents = exemplarRepository.dependents(exemplarId)
            if (exemplar == null) {
                _state.update { it.copy(loading = false, notFound = true, dependents = dependents) }
                return@launch
            }
            _state.update {
                it.copy(
                    internalCode = exemplar.internalCode,
                    status = exemplar.status,
                    condition = exemplar.condition,
                    packagingCondition = exemplar.packagingCondition,
                    notes = exemplar.notes.orEmpty(),
                    currentLocationId = exemplar.locationId,
                    targetLocationId = exemplar.locationId,
                    rowVersion = exemplar.rowVersion,
                    createdAt = exemplar.createdAt,
                    updatedAt = exemplar.updatedAt,
                    dependents = dependents,
                    loading = false,
                    notFound = false,
                )
            }
        }
    }

    private fun loadAcquisition() {
        viewModelScope.launch {
            applyAcquisition(acquisitionRepository.findByExemplar(exemplarId))
        }
    }

    private fun applyAcquisition(acquisition: Acquisition?) {
        _state.update {
            it.copy(
                acquisition = acquisition,
                acquisitionPrice = acquisition?.price?.formatPtBr().orEmpty(),
                acquisitionShipping = acquisition?.shipping?.formatPtBr().orEmpty(),
                acquisitionDiscount = acquisition?.discount?.formatPtBr().orEmpty(),
                acquisitionDate = acquisition?.date?.formatPtBr() ?: today,
                acquisitionStore = acquisition?.store.orEmpty(),
                acquisitionSeller = acquisition?.seller.orEmpty(),
                acquisitionMethod = acquisition?.method.orEmpty(),
                acquisitionNotes = acquisition?.notes.orEmpty(),
            )
        }
    }

    private fun parseMoney(text: String): ParsedMoney {
        if (text.isBlank()) return ParsedMoney(null, isInvalid = false)
        val value = MoneyCents.parsePtBrOrNull(text)
        return ParsedMoney(value, isInvalid = value == null)
    }

    private fun Throwable.toMessage(): UiMessage = when (this) {
        DomainError.Duplicate -> UiMessage(R.string.error_duplicate_record)
        DomainError.NotFound -> UiMessage(R.string.error_exemplar_not_found)
        else -> toUiMessage()
    }

    private data class ParsedMoney(val value: MoneyCents?, val isInvalid: Boolean)
}
