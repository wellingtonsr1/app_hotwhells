package br.com.diecastcollector.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.diecastcollector.domain.backup.BackupService
import br.com.diecastcollector.domain.repository.DataControlRepository
import br.com.diecastcollector.domain.repository.UserDataCounts
import br.com.diecastcollector.feature.transfer.appVersion
import br.com.diecastcollector.feature.transfer.deviceName
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Controle de dados (T146, FR-142, FR-157, FR-163).
 *
 * A exportação é o mesmo pacote do backup — banco, fotos, JSON e CSV —, então o usuário
 * tem sempre uma cópia aberta antes de decidir apagar. A exclusão só é chamada depois das
 * duas confirmações da tela: aqui não existe caminho que apague sem esse aceite.
 *
 * Depois de excluir, as consultas reativas do app passam a emitir vazio sozinhas; a tela
 * só precisa informar que terminou.
 */
@HiltViewModel
public class DataControlViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataControlRepository: DataControlRepository,
    private val backupService: BackupService,
) : ViewModel() {

    /** Motivo da falha; a tela escolhe o texto, este estado não carrega apresentação. */
    public enum class Failure { EXPORT, DELETE }

    public data class State(
        val counts: UserDataCounts = UserDataCounts(),
        val busy: Boolean = false,
        val exportedBytes: Long? = null,
        val deleted: Boolean = false,
        val failure: Failure? = null,
        val failureDetail: String? = null,
    )

    private val _state: MutableStateFlow<State> = MutableStateFlow(State())
    public val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            dataControlRepository.observeCounts().collect { counts ->
                _state.update { it.copy(counts = counts) }
            }
        }
    }

    /** Exporta para o destino escolhido no seletor de arquivos (SAF). */
    public fun exportTo(target: Uri) {
        runExport { context.contentResolver.openOutputStream(target) }
    }

    /** Mesma exportação, com o destino já aberto — é a borda que os testes usam. */
    internal fun exportTo(output: OutputStream) {
        runExport { output }
    }

    /** Chamado só pela segunda confirmação da tela. */
    public fun deleteAll() {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update {
                it.copy(busy = true, deleted = false, failure = null, failureDetail = null)
            }
            val result = withContext(Dispatchers.IO) { dataControlRepository.deleteAll() }
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(busy = false, deleted = true) },
                    onFailure = {
                        current.copy(
                            busy = false,
                            failure = Failure.DELETE,
                            failureDetail = it.message,
                        )
                    },
                )
            }
        }
    }

    public fun clearMessage() {
        _state.update {
            it.copy(exportedBytes = null, failure = null, failureDetail = null, deleted = false)
        }
    }

    private fun runExport(open: () -> OutputStream?) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update {
                it.copy(busy = true, exportedBytes = null, failure = null, failureDetail = null)
            }
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val output = open()
                        ?: error("Não foi possível abrir o destino escolhido.")
                    output.use { stream ->
                        backupService.create(stream, appVersion(context), deviceName())
                    }
                }
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(busy = false, exportedBytes = it.totalBytes) },
                    onFailure = {
                        current.copy(
                            busy = false,
                            failure = Failure.EXPORT,
                            failureDetail = it.message,
                        )
                    },
                )
            }
        }
    }
}
