package br.com.hotwheelscollector.feature.reports

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.data.importexport.ReportFormat
import br.com.hotwheelscollector.data.repository.InventoryReport
import br.com.hotwheelscollector.data.repository.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Relatórios (T118/T116): gera o inventário em PDF, CSV ou XLSX e grava no destino
 * escolhido via SAF.
 *
 * Mesmo padrão do backup: o arquivo só é escrito no `Uri` devolvido pelo seletor de
 * arquivos, em `Dispatchers.IO`, e a tela mostra progresso e resultado.
 */
@HiltViewModel
public class ReportsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reportsRepository: ReportsRepository,
) : ViewModel() {

    public data class State(
        val busy: Boolean = false,
        val created: InventoryReport? = null,
        val failure: String? = null,
    )

    private val _state: MutableStateFlow<State> = MutableStateFlow(State())
    public val state: StateFlow<State> = _state.asStateFlow()

    /** Gera o relatório no [format] pedido e escreve direto no destino escolhido (SAF). */
    public fun generate(target: Uri, format: ReportFormat) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.value = State(busy = true)
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val report = reportsRepository.buildInventoryReport(format)
                    val output = context.contentResolver.openOutputStream(target)
                        ?: throw IllegalStateException("Não foi possível abrir o destino escolhido.")
                    output.use { stream -> stream.write(report.bytes) }
                    report
                }
            }
            _state.value = result.fold(
                onSuccess = { State(created = it) },
                onFailure = { State(failure = it.message) },
            )
        }
    }

    public fun clearMessage() {
        _state.value = _state.value.copy(created = null, failure = null)
    }
}
