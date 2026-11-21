package br.com.hotwheelscollector.feature.transfer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.data.importexport.CsvImportService
import br.com.hotwheelscollector.data.importexport.ImportApplyResult
import br.com.hotwheelscollector.data.importexport.ImportPreview
import br.com.hotwheelscollector.data.importexport.XlsxReader
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
 * Importação de CSV (T115): lê o arquivo escolhido, mostra a pré-visualização com o
 * relatório de erros e, só com ação explícita, aplica as linhas válidas no banco (FR-136).
 */
@HiltViewModel
public class ImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importService: CsvImportService,
) : ViewModel() {

    public data class State(
        val busy: Boolean = false,
        val preview: ImportPreview? = null,
        val imported: ImportApplyResult? = null,
        val failure: String? = null,
    )

    private val _state: MutableStateFlow<State> = MutableStateFlow(State())
    public val state: StateFlow<State> = _state.asStateFlow()

    private var pendingText: String? = null

    /** Lê o CSV escolhido e monta a pré-visualização; nada é gravado (FR-136). */
    public fun load(source: Uri) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.value = State(busy = true)
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val bytes = context.contentResolver.openInputStream(source)?.use { input ->
                        input.readBytes()
                    } ?: throw IllegalStateException("Não foi possível ler o arquivo escolhido.")
                    val mime = context.contentResolver.getType(source)
                    val text = if (isXlsx(bytes, mime)) {
                        XlsxReader.toCsv(bytes).csv
                    } else {
                        String(bytes, Charsets.UTF_8)
                    }
                    pendingText = text
                    importService.preview(text)
                }
            }
            _state.value = result.fold(
                onSuccess = { State(preview = it) },
                onFailure = { State(failure = it.message) },
            )
        }
    }

    /** Aplica as linhas válidas numa transação; duplicatas são puladas (FR-138). */
    public fun import() {
        val text = pendingText ?: return
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, imported = null, failure = null)
            val result = runCatching { importService.apply(text) }
            _state.value = result.fold(
                onSuccess = { _state.value.copy(busy = false, imported = it) },
                onFailure = { _state.value.copy(busy = false, failure = it.message) },
            )
        }
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

    private fun isXlsx(bytes: ByteArray, mime: String?): Boolean {
        val zipSignature = bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
        val spreadsheetMime = mime?.contains("spreadsheet") == true
        return zipSignature || spreadsheetMime
    }
}
