package br.com.hotwheelscollector.feature.transfer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.com.hotwheelscollector.data.importexport.CsvImportService
import br.com.hotwheelscollector.data.importexport.ImportApplyResult
import br.com.hotwheelscollector.data.importexport.ImportPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Importação de CSV (T115): lê o arquivo escolhido, mostra a pré-visualização com o
 * relatório de erros e, só com ação explícita, aplica as linhas válidas no banco (FR-136).
 *
 * A gravação roda no `TransferWorker` (T134), porque é a etapa longa e transacional; a
 * pré-visualização continua na tela por ser imediata e precisar das linhas para o relatório.
 */
@HiltViewModel
public class ImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importService: CsvImportService,
    private val workManager: WorkManager,
) : ViewModel() {

    public data class State(
        val busy: Boolean = false,
        val preview: ImportPreview? = null,
        val imported: ImportApplyResult? = null,
        val failure: String? = null,
    )

    private val _state: MutableStateFlow<State> = MutableStateFlow(State())
    public val state: StateFlow<State> = _state.asStateFlow()

    private var source: Uri? = null

    init {
        observeImport()
    }

    /** Lê o CSV escolhido e monta a pré-visualização; nada é gravado (FR-136). */
    public fun load(source: Uri) {
        if (_state.value.busy) return
        this.source = source
        viewModelScope.launch {
            _state.value = State(busy = true)
            val result = runCatching {
                val text = readImportText(context, source)
                importService.preview(text)
            }
            _state.value = result.fold(
                onSuccess = { State(preview = it) },
                onFailure = { State(failure = it.message) },
            )
        }
    }

    /** Aplica as linhas válidas numa transação; duplicatas são puladas (FR-138). */
    public fun import() {
        val source = source ?: return
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, imported = null, failure = null)
        val request = OneTimeWorkRequestBuilder<TransferWorker>().apply {
            setInputData(
                workDataOf(
                    TransferOutput.ACTION to TransferOutput.ACTION_IMPORT,
                    TransferOutput.URI to source.toString(),
                ),
            )
        }.build()
        workManager.enqueueUniqueWork(UNIQUE_IMPORT, ExistingWorkPolicy.KEEP, request)
    }

    /** Grava o relatório de erros no destino escolhido (SAF). */
    public fun saveErrorReport(target: Uri) {
        val csv = _state.value.preview?.errorReportCsv() ?: return
        viewModelScope.launch {
            val failure = runCatching {
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(target)
                        ?: throw IllegalStateException("Não foi possível abrir o destino escolhido.")
                    output.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                }
            }.exceptionOrNull()
            if (failure != null) {
                _state.value = _state.value.copy(failure = failure.message)
            }
        }
    }

    public fun clearMessage() {
        _state.value = _state.value.copy(imported = null, failure = null)
    }

    private fun observeImport() {
        viewModelScope.launch {
            var active: UUID? = null
            workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_IMPORT).collect { infos ->
                val info = infos.lastOrNull() ?: return@collect
                if (info.state == WorkInfo.State.ENQUEUED ||
                    info.state == WorkInfo.State.RUNNING ||
                    info.state == WorkInfo.State.BLOCKED
                ) {
                    active = info.id
                    _state.value = _state.value.copy(busy = true)
                    return@collect
                }
                if (info.id != active) return@collect
                active = null
                _state.value = when (info.state) {
                    WorkInfo.State.SUCCEEDED -> _state.value.copy(
                        busy = false,
                        imported = TransferOutput.readImported(info.outputData),
                        failure = if (TransferOutput.readImported(info.outputData) == null) {
                            "A importação terminou sem resultado."
                        } else {
                            null
                        },
                    )
                    WorkInfo.State.FAILED -> _state.value.copy(
                        busy = false,
                        failure = TransferOutput.readDetail(info.outputData),
                    )
                    else -> _state.value.copy(busy = false)
                }
            }
        }
    }

    private companion object {
        const val UNIQUE_IMPORT = "transfer-import"
    }
}
