package br.com.diecastcollector.feature.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.DuplicateAvailability
import br.com.diecastcollector.domain.model.DuplicateExemplar
import br.com.diecastcollector.domain.model.DuplicateGroup
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.repository.DuplicateRepository
import br.com.diecastcollector.domain.usecase.GetDuplicatesUseCase
import br.com.diecastcollector.ui.UiMessage
import br.com.diecastcollector.ui.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public data class DuplicatesUiState(
    val groups: List<DuplicateGroup> = emptyList(),
    val loading: Boolean = true,
    val selected: DuplicateExemplar? = null,
    val saving: Boolean = false,
    val message: UiMessage? = null,
)

/**
 * Lista de duplicados e marcação de disponibilidade (US7, FR-065–FR-067).
 *
 * O grupo é sempre por variante; o estado guarda o exemplar selecionado para que a folha
 * de disponibilidade não dependa de procurar de novo na lista.
 */
@HiltViewModel
public class DuplicatesViewModel @Inject constructor(
    getDuplicates: GetDuplicatesUseCase,
    private val duplicates: DuplicateRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DuplicatesUiState())
    public val state: StateFlow<DuplicatesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getDuplicates.observeGroups().collect { groups ->
                _state.update { current ->
                    val refreshed = current.selected?.let { selected ->
                        groups.asSequence()
                            .flatMap { it.exemplars.asSequence() }
                            .firstOrNull { it.exemplar.id == selected.exemplar.id }
                    }
                    current.copy(groups = groups, loading = false, selected = refreshed)
                }
            }
        }
    }

    public fun onSelect(exemplarId: String) = _state.update { current ->
        val selected = current.groups.asSequence()
            .flatMap { it.exemplars.asSequence() }
            .firstOrNull { it.exemplar.id == exemplarId }
        current.copy(selected = selected)
    }

    public fun onDismissSheet() = _state.update { it.copy(selected = null) }

    public fun onMessageShown() = _state.update { it.copy(message = null) }

    public fun onSaveOffer(
        availability: DuplicateAvailability,
        condition: Condition?,
        priceText: String,
        notes: String,
    ) {
        val selected = _state.value.selected ?: return
        if (_state.value.saving) return

        val askingPrice = if (priceText.isBlank()) {
            null
        } else {
            MoneyCents.parsePtBrOrNull(priceText)
                ?: return _state.update {
                    it.copy(message = UiMessage(R.string.error_invalid_money))
                }
        }

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            duplicates.setOffer(
                exemplarId = selected.exemplar.id,
                availability = availability,
                condition = condition,
                askingPrice = askingPrice,
                notes = notes,
            ).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            saving = false,
                            selected = null,
                            message = UiMessage(R.string.availability_updated),
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            saving = false,
                            message = error.toUiMessage(R.string.duplicates_error_save),
                        )
                    }
                },
            )
        }
    }

}

