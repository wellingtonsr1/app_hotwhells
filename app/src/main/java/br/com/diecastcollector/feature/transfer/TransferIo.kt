package br.com.diecastcollector.feature.transfer

import android.content.Context
import android.net.Uri
import br.com.diecastcollector.data.importexport.XlsxReader
import br.com.diecastcollector.domain.backup.BackupException
import br.com.diecastcollector.domain.backup.BackupFailure
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Leitura dos arquivos escolhidos no seletor (SAF), compartilhada entre tela e worker. */
internal suspend fun copyUriToCache(
    context: Context,
    source: Uri,
    prefix: String,
): File = withContext(Dispatchers.IO) {
    val copy = File.createTempFile(prefix, ".bin", context.cacheDir)
    context.contentResolver.openInputStream(source)?.use { input ->
        copy.outputStream().use { output -> input.copyTo(output) }
    } ?: throw BackupException(
        BackupFailure.NOT_A_PACKAGE,
        "Não foi possível ler o arquivo escolhido.",
    )
    copy
}

/** Aceita CSV ou XLSX, como a tela de importação já fazia (T113). */
internal suspend fun readImportText(context: Context, source: Uri): String =
    withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() }
            ?: throw IllegalStateException("Não foi possível ler o arquivo escolhido.")
        val mime = context.contentResolver.getType(source)
        val zipSignature =
            bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
        if (zipSignature || mime?.contains("spreadsheet") == true) {
            XlsxReader.toCsv(bytes).csv
        } else {
            String(bytes, Charsets.UTF_8)
        }
    }
