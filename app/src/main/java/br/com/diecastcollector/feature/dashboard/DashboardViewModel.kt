package br.com.diecastcollector.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.diecastcollector.domain.model.DashboardSnapshot
import br.com.diecastcollector.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Estados do painel: o resumo é leitura, então não há evento de entrada. */
public sealed interface DashboardUiState {
    public data object Loading : DashboardUiState
    public data class Ready(val snapshot: DashboardSnapshot) : DashboardUiState
    public data class Error(val message: String?) : DashboardUiState
}

/**
 * Painel do estado da coleção (US3).
 *
 * Não guarda cópia do acervo: cada mudança nas tabelas gera nova emissão do repositório e
 * o painel recalcula. Falha de leitura vira `Error` visível — nunca zero, que pareceria
 * "coleção vazia" em vez de "não consegui ler".
 */
@HiltViewModel
public class DashboardViewModel @Inject constructor(
    dashboardRepository: DashboardRepository,
) : ViewModel() {

    public val state: StateFlow<DashboardUiState> = dashboardRepository.observeSnapshot()
        .map<DashboardSnapshot, DashboardUiState> { DashboardUiState.Ready(it) }
        .catch { error -> emit(DashboardUiState.Error(error.message)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = DashboardUiState.Loading,
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
