package br.com.hotwheelscollector.feature.transfer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.com.hotwheelscollector.domain.backup.BackupFailure
import br.com.hotwheelscollector.domain.backup.BackupSummary
import br.com.hotwheelscollector.domain.backup.RestoreMode
import br.com.hotwheelscollector.domain.backup.ValidatedBackup
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backup manual (T119/T120) — estado da tela.
 *
 * As operações rodam no `TransferWorker` (T134): continuam com a tela fechada e o resultado
 * é recuperado do WorkManager quando a tela volta. O texto fica em `strings.xml`; aqui só
 * trafegam dados e o motivo da falha.
 *
 * A restauração fecha o banco: depois de concluída, o app precisa ser reaberto.
 */
@HiltViewModel
public class BackupViewModel @Inject constructor(
    private val workManager: WorkManager,
) : ViewModel() {

    public data class State(
        val busy: Boolean = false,
        val created: BackupSummary? = null,
        val pending: ValidatedBackup? = null,
        val restored: BackupSummary? = null,
        val merged: BackupSummary? = null,
        val failure: BackupFailure? = null,
        val failureDetail: String? = null,
    )

    private val _state: MutableStateFlow<State> = MutableStateFlow(State())
    public val state: StateFlow<State> = _state.asStateFlow()

    private var restoreMode: RestoreMode = RestoreMode.REPLACE

    init {
        observeBackup()
        observeInspection()
        observeRestore()
    }

    /** Escreve o pacote direto no destino escolhido no seletor de arquivos (SAF). */
    public fun create(target: Uri) {
        if (_state.value.busy) return
        _state.update {
            it.copy(busy = true, created = null, failure = null, failureDetail = null)
        }
        val request = OneTimeWorkRequestBuilder<TransferWorker>().apply {
            setInputData(
                workDataOf(
                    TransferOutput.ACTION to TransferOutput.ACTION_BACKUP,
                    TransferOutput.URI to target.toString(),
                ),
            )
        }.build()
        workManager.enqueueUniqueWork(
            UNIQUE_BACKUP,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    /** Lê o pacote escolhido e mostra o resumo; nada é gravado ainda. */
    public fun inspect(source: Uri) {
        if (_state.value.busy) return
        _state.update {
            it.copy(
                busy = true,
                pending = null,
                restored = null,
                merged = null,
                failure = null,
                failureDetail = null,
            )
        }
        val request = OneTimeWorkRequestBuilder<TransferWorker>().apply {
            setInputData(
                workDataOf(
                    TransferOutput.ACTION to TransferOutput.ACTION_INSPECT,
                    TransferOutput.URI to source.toString(),
                ),
            )
        }.build()
        workManager.enqueueUniqueWork(UNIQUE_INSPECT, ExistingWorkPolicy.KEEP, request)
    }

    /** Só depois da confirmação explícita (FR-163): substitui tudo pelo backup. */
    public fun confirmRestore() {
        restore(RestoreMode.REPLACE)
    }

    /** Mescla os dois lados sem sobrescrever nada (FR-144). */
    public fun confirmMerge() {
        restore(RestoreMode.MERGE)
    }

    private fun restore(mode: RestoreMode) {
        val pending = _state.value.pending ?: return
        if (_state.value.busy) return
        restoreMode = mode
        _state.update { it.copy(busy = true, pending = null) }
        val request = OneTimeWorkRequestBuilder<TransferWorker>().apply {
            setInputData(
                workDataOf(
                    TransferOutput.ACTION to TransferOutput.ACTION_RESTORE,
                    TransferOutput.FILE to pending.file.absolutePath,
                    TransferOutput.MODE to mode.name,
                ),
            )
        }.build()
        workManager.enqueueUniqueWork(UNIQUE_RESTORE, ExistingWorkPolicy.KEEP, request)
    }

    public fun dismissPending() {
        _state.update { it.copy(pending = null) }
    }

    public fun clearMessage() {
        _state.update {
            it.copy(failure = null, failureDetail = null, created = null, merged = null)
        }
    }

    private fun observeBackup() {
        viewModelScope.launch {
            var active: UUID? = null
            workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_BACKUP).collect { infos ->
                val info = infos.lastOrNull() ?: return@collect
                if (info.state.isActive) {
                    active = info.id
                    _state.update { it.copy(busy = true) }
                    return@collect
                }
                if (info.id != active) return@collect
                active = null
                when (info.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val summary = TransferOutput.readSummary(info.outputData)
                        _state.update {
                            it.copy(
                                busy = false,
                                created = summary,
                                failure = if (summary == null) BackupFailure.WRITE_FAILED else null,
                            )
                        }
                    }
                    WorkInfo.State.FAILED -> _state.update {
                        it.copy(busy = false).fail(info.outputData)
                    }
                    else -> _state.update { it.copy(busy = false) }
                }
            }
        }
    }

    private fun observeInspection() {
        viewModelScope.launch {
            var active: UUID? = null
            workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_INSPECT).collect { infos ->
                val info = infos.lastOrNull() ?: return@collect
                if (info.state.isActive) {
                    active = info.id
                    _state.update { it.copy(busy = true) }
                    return@collect
                }
                if (info.id != active) return@collect
                active = null
                _state.update { current ->
                    if (info.state == WorkInfo.State.SUCCEEDED) {
                        val summary = TransferOutput.readSummary(info.outputData)
                        val path = info.outputData.getString(TransferOutput.FILE)
                        if (summary != null && path != null) {
                            current.copy(
                                busy = false,
                                pending = ValidatedBackup(File(path), summary, emptyList()),
                            )
                        } else {
                            current.copy(busy = false, failure = BackupFailure.WRITE_FAILED)
                        }
                    } else if (info.state == WorkInfo.State.FAILED) {
                        current.copy(busy = false).fail(info.outputData)
                    } else {
                        current.copy(busy = false)
                    }
                }
            }
        }
    }

    private fun observeRestore() {
        viewModelScope.launch {
            var active: UUID? = null
            var mode = RestoreMode.REPLACE
            workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_RESTORE).collect { infos ->
                val info = infos.lastOrNull() ?: return@collect
                if (info.state.isActive) {
                    active = info.id
                    mode = restoreMode
                    _state.update { it.copy(busy = true) }
                    return@collect
                }
                if (info.id != active) return@collect
                active = null
                _state.update { current ->
                    if (info.state == WorkInfo.State.SUCCEEDED) {
                        val summary = TransferOutput.readSummary(info.outputData)
                        when {
                            summary == null -> current.copy(busy = false, failure = BackupFailure.WRITE_FAILED)
                            mode == RestoreMode.MERGE -> current.copy(busy = false, merged = summary)
                            else -> current.copy(busy = false, restored = summary)
                        }
                    } else if (info.state == WorkInfo.State.FAILED) {
                        current.copy(busy = false).fail(info.outputData)
                    } else {
                        current.copy(busy = false)
                    }
                }
            }
        }
    }

    private fun State.fail(data: androidx.work.Data): State = copy(
        busy = false,
        failure = TransferOutput.readFailure(data) ?: BackupFailure.WRITE_FAILED,
        failureDetail = TransferOutput.readDetail(data),
    )

    private companion object {
        const val UNIQUE_BACKUP = "transfer-backup"
        const val UNIQUE_INSPECT = "transfer-inspect"
        const val UNIQUE_RESTORE = "transfer-restore"
    }
}

private val WorkInfo.State.isActive: Boolean
    get() = this == WorkInfo.State.ENQUEUED ||
        this == WorkInfo.State.RUNNING ||
        this == WorkInfo.State.BLOCKED
