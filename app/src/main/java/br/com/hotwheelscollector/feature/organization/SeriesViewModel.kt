package br.com.hotwheelscollector.feature.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.model.Manufacturer
import br.com.hotwheelscollector.domain.model.Series
import br.com.hotwheelscollector.domain.repository.ManufacturerRepository
import br.com.hotwheelscollector.domain.repository.SeriesRepository
import br.com.hotwheelscollector.ui.UiMessage
import br.com.hotwheelscollector.ui.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

public data class SeriesUiState(
    val manufacturers: List<Manufacturer> = emptyList(),
    val selectedManufacturerId: String? = null,
    val items: List<Series> = emptyList(),
    val loading: Boolean = true,
    val message: UiMessage? = null,
)

/**
 * Séries por fabricante (T070, FR-048–FR-049).
 *
 * A série sempre pertence a um fabricante — a mesma taxonomia não é presumida entre
 * fabricantes diferentes (Art. VI.2), então a tela pede o fabricante antes de listar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
public class SeriesViewModel @Inject constructor(
    private val manufacturers: ManufacturerRepository,
    private val series: SeriesRepository,
) : ViewModel() {

    private val selected = MutableStateFlow<String?>(null)
    private val feedback = MutableStateFlow<UiMessage?>(null)

    public val state: StateFlow<SeriesUiState> = combine(
        manufacturers.observeActive(),
        selected,
        feedback,
    ) { all, chosen, message ->
        Triple(all, chosen ?: all.firstOrNull()?.id, message)
    }.flatMapLatest { (all, effective, message) ->
        if (effective == null) {
            flowOf(SeriesUiState(manufacturers = all, loading = false, message = message))
        } else {
            series.observeByManufacturer(effective).map { items ->
                SeriesUiState(
                    manufacturers = all,
                    selectedManufacturerId = effective,
                    items = items,
                    loading = false,
                    message = message,
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SeriesUiState(),
    )

    public fun onSelectManufacturer(id: String) {
        selected.value = id
    }

    public fun onAdd(name: String, parentId: String?) {
        val manufacturerId = state.value.selectedManufacturerId
        if (manufacturerId == null) {
            feedback.value = NO_MANUFACTURER
            return
        }
        viewModelScope.launch {
            feedback.value = series.add(manufacturerId, name, parentId)
                .fold(
                    onSuccess = { ADDED },
                    onFailure = { it.toUiMessage() },
                )
        }
    }

    public fun onRename(id: String, name: String) {
        viewModelScope.launch {
            feedback.value = series.rename(id, name).fold(
                onSuccess = { RENAMED },
                onFailure = { it.toUiMessage() },
            )
        }
    }

    public fun onArchive(id: String) {
        viewModelScope.launch {
            feedback.value = series.archive(id).fold(
                onSuccess = { ARCHIVED },
                onFailure = { it.toUiMessage() },
            )
        }
    }

    public fun onMessageShown() {
        feedback.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        val NO_MANUFACTURER: UiMessage = UiMessage(R.string.series_no_manufacturer)
        val ADDED: UiMessage = UiMessage(R.string.series_added)
        val RENAMED: UiMessage = UiMessage(R.string.series_renamed)
        val ARCHIVED: UiMessage = UiMessage(R.string.series_archived)
    }
}
