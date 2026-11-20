package br.com.hotwheelscollector.feature.transfer

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.domain.backup.BackupException
import br.com.hotwheelscollector.domain.backup.BackupFailure
import br.com.hotwheelscollector.domain.backup.BackupService
import br.com.hotwheelscollector.domain.backup.BackupSummary
import br.com.hotwheelscollector.domain.backup.ValidatedBackup
import br.com.hotwheelscollector.feature.settings.BackupReminderStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Backup manual (T119/T120) — estado da tela.
 *
 * O texto fica na tela (`strings.xml`); aqui só trafegam dados e o motivo da falha.
 * A restauração fecha o banco: depois de concluída, o app precisa ser reaberto.
 */
@HiltViewModel
public class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupService: BackupService,
    private val reminderStore: BackupReminderStore,
) : ViewModel() {

    public data class State(
        val busy: Boolean = false,
        val created: BackupSummary? = null,
        val pending: ValidatedBackup? = null,
        val restored: BackupSummary? = null,
        val failure: BackupFailure? = null,
        val failureDetail: String? = null,
    )

    private val _state: MutableStateFlow<State> = MutableStateFlow(State())
    public val state: StateFlow<State> = _state.asStateFlow()

    /** Escreve o pacote direto no destino escolhido no seletor de arquivos (SAF). */
    public fun create(target: Uri) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update {
                it.copy(busy = true, created = null, failure = null, failureDetail = null)
            }
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(target)
                        ?: throw BackupException(
                            BackupFailure.WRITE_FAILED,
                            "Não foi possível abrir o destino escolhido.",
                        )
                    output.use { stream ->
                        backupService.create(stream, appVersion(), deviceName())
                    }
                }
            }
            val summary = result.getOrNull()
            if (summary != null) {
                // Só um pacote gravado com sucesso conta como backup feito (T120).
                reminderStore.markBackup()
                _state.update { it.copy(busy = false, created = summary) }
            } else {
                _state.update { it.fail(result.exceptionOrNull() ?: IllegalStateException()) }
            }
        }
    }

    /** Lê o pacote escolhido e mostra o resumo; nada é gravado ainda. */
    public fun inspect(source: Uri) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update {
                it.copy(busy = true, pending = null, restored = null, failure = null, failureDetail = null)
            }
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    backupService.inspect(copyToCache(source))
                }
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(busy = false, pending = it) },
                    onFailure = { current.fail(it) },
                )
            }
        }
    }

    /** Só depois da confirmação explícita (FR-163). */
    public fun confirmRestore() {
        val pending = _state.value.pending ?: return
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, pending = null) }
            val result = runCatching {
                withContext(Dispatchers.IO) { backupService.restore(pending.file) }
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { current.copy(busy = false, restored = it) },
                    onFailure = { current.fail(it) },
                )
            }
        }
    }

    public fun dismissPending() {
        _state.update { it.copy(pending = null) }
    }

    public fun clearMessage() {
        _state.update { it.copy(failure = null, failureDetail = null, created = null) }
    }

    private fun copyToCache(source: Uri): File {
        val copy = File.createTempFile("backup-restore-", ".zip", context.cacheDir)
        context.contentResolver.openInputStream(source)?.use { input ->
            copy.outputStream().use { output -> input.copyTo(output) }
        } ?: throw BackupException(
            BackupFailure.NOT_A_PACKAGE,
            "Não foi possível ler o arquivo escolhido.",
        )
        return copy
    }

    private fun State.fail(failure: Throwable): State = copy(
        busy = false,
        failure = (failure as? BackupException)?.failure ?: BackupFailure.WRITE_FAILED,
        failureDetail = failure.message,
    )

    private fun appVersion(): String {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return "${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})"
    }

    private fun deviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
}
