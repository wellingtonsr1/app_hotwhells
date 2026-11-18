package br.com.hotwheelscollector.feature.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.ExemplarStatus
import br.com.hotwheelscollector.domain.model.Location
import br.com.hotwheelscollector.domain.model.Movement
import br.com.hotwheelscollector.domain.model.PackagingCondition
import br.com.hotwheelscollector.domain.repository.ExemplarDependents
import br.com.hotwheelscollector.domain.repository.ExemplarRepository
import br.com.hotwheelscollector.domain.repository.LocationRepository
import br.com.hotwheelscollector.navigation.RouteArgs
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val loading: Boolean = true,
    val saving: Boolean = false,
    val notFound: Boolean = false,
    val message: String? = null,
    val finished: Boolean = false,
) {
    public val isArchived: Boolean get() = status == ExemplarStatus.ARCHIVED

    /** O botão de excluir só habilita sem vínculos; com qualquer um, a tela oferece arquivar. */
    public val canDelete: Boolean get() = !loading && !notFound && !dependents.hasAny
}

@HiltViewModel
public class ExemplarDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val exemplarRepository: ExemplarRepository,
    locationRepository: LocationRepository,
) : ViewModel() {

    private val exemplarId: String = savedStateHandle[RouteArgs.EXEMPLAR_ID] ?: ""

    private val _state = MutableStateFlow(ExemplarDetailUiState(exemplarId = exemplarId))
    public val state: StateFlow<ExemplarDetailUiState> = _state.asStateFlow()

    init {
        refresh()
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
    }

    public fun onConditionChange(condition: Condition) = _state.update { it.copy(condition = condition) }

    public fun onPackagingChange(packaging: PackagingCondition?) =
        _state.update { it.copy(packagingCondition = packaging) }

    public fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    public fun onTargetLocationChange(locationId: String?) =
        _state.update { it.copy(targetLocationId = locationId) }

    public fun onMoveNoteChange(value: String) = _state.update { it.copy(moveNote = value) }

    public fun onMessageShown() = _state.update { it.copy(message = null) }

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
                            message = "Alterações salvas.",
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
            _state.update { it.copy(message = "Escolha outro local ou escreva uma observação.") }
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
                    _state.update { it.copy(saving = false, moveNote = "", message = "Localização atualizada.") }
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
                        it.copy(saving = false, finished = true, message = "Exemplar arquivado.")
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
                        it.copy(saving = false, finished = true, message = "Exemplar excluído.")
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
                    dependents = dependents,
                    loading = false,
                    notFound = false,
                )
            }
        }
    }

    private fun Throwable.toMessage(): String = when (this) {
        is DomainError.Validation -> reason
        DomainError.Duplicate -> "Já existe um registro igual."
        DomainError.StaleWrite -> "Registro alterado em outra tela. Recarregue."
        is DomainError.Blocked -> reason
        is DomainError.Storage -> "Falha ao gravar: $reason"
        DomainError.NotFound -> "Exemplar não encontrado."
        else -> message ?: "Não foi possível concluir."
    }
}
