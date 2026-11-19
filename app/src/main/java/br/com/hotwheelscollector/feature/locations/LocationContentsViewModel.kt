package br.com.hotwheelscollector.feature.locations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.domain.model.InventoryItem
import br.com.hotwheelscollector.domain.repository.LocationRepository
import br.com.hotwheelscollector.navigation.RouteArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

public data class LocationContentsUiState(
    val loading: Boolean = true,
    val locationId: String = "",
    val name: String = "",
    val items: List<InventoryItem> = emptyList(),
)

/**
 * Inventário por localização (T106, FR-094–FR-095).
 *
 * A consulta já desce a hierarquia: abrir a estante mostra o que está em todas as
 * prateleiras dela, que é exatamente a pergunta "o que tem aqui?".
 */
@HiltViewModel
public class LocationContentsViewModel @Inject constructor(
    locations: LocationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val locationId: String = savedStateHandle
        .get<String>(RouteArgs.LOCATION_ID)
        .orEmpty()

    public val state: StateFlow<LocationContentsUiState> = combine(
        locations.observeActive(),
        locations.observeContents(locationId),
    ) { active, items ->
        LocationContentsUiState(
            loading = false,
            locationId = locationId,
            name = active.firstOrNull { it.id == locationId }?.name.orEmpty(),
            items = items,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = LocationContentsUiState(locationId = locationId),
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
