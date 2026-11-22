package br.com.diecastcollector.feature.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.diecastcollector.domain.model.Exemplar
import br.com.diecastcollector.domain.model.Location
import br.com.diecastcollector.domain.repository.ExemplarRepository
import br.com.diecastcollector.domain.repository.LocationRepository
import br.com.diecastcollector.navigation.RouteArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Lista os exemplares possuídos de uma variante.
 *
 * É o degrau que faltava entre o cartão do catálogo e o exemplar: sem ela, editar ou mover
 * uma unidade exigiria saber o id de cor. Arquivados não aparecem (FR-044).
 */
public data class ExemplarListUiState(
    val variantId: String = "",
    val exemplars: List<Exemplar> = emptyList(),
    val locationNames: Map<String, String> = emptyMap(),
    val loading: Boolean = true,
) {
    public val isEmpty: Boolean get() = !loading && exemplars.isEmpty()
}

@HiltViewModel
public class ExemplarListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    exemplarRepository: ExemplarRepository,
    locationRepository: LocationRepository,
) : ViewModel() {

    private val variantId: String = savedStateHandle[RouteArgs.VARIANT_ID] ?: ""

    public val state: StateFlow<ExemplarListUiState> = combine(
        exemplarRepository.observeByVariant(variantId),
        locationRepository.observeActive(),
    ) { exemplars: List<Exemplar>, locations: List<Location> ->
        ExemplarListUiState(
            variantId = variantId,
            exemplars = exemplars,
            locationNames = locations.associate { it.id to it.name },
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ExemplarListUiState(variantId = variantId),
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
