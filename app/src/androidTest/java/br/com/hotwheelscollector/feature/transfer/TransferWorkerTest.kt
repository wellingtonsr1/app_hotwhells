package br.com.hotwheelscollector.feature.transfer

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.io.File
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * O backup precisa terminar mesmo com a tela fora do caminho (T134).
 *
 * Usa o WorkManager real do app, com a `HiltWorkerFactory` injetada, para provar que o
 * worker encontra o `BackupService` e grava o pacote no destino.
 */
@RunWith(AndroidJUnit4::class)
public class TransferWorkerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    public fun backupNoWorkerGravaPacoteEConclui(): Unit = runBlocking {
        val target = File(context.cacheDir, "worker-backup-${System.nanoTime()}.zip")
        val request = OneTimeWorkRequestBuilder<TransferWorker>()
            .setInputData(
                workDataOf(
                    TransferOutput.ACTION to TransferOutput.ACTION_BACKUP,
                    TransferOutput.URI to Uri.fromFile(target).toString(),
                ),
            )
            .build()
        val workManager = WorkManager.getInstance(context)

        workManager.enqueueUniqueWork("test-transfer-backup", ExistingWorkPolicy.REPLACE, request)
        val info = withTimeout(120_000) {
            workManager.getWorkInfoByIdFlow(request.id)
                .filterNotNull()
                .first { it.state.isFinished }
        }

        assertEquals(WorkInfo.State.SUCCEEDED, info.state)
        assertTrue("o worker precisa ter gravado o pacote", target.length() > 0)
        val signature = target.inputStream().use { input -> ByteArray(2).also { input.read(it) } }
        assertEquals("PK", String(signature, Charsets.US_ASCII))
        val summary = TransferOutput.readSummary(info.outputData)
        assertNotNull("o resumo precisa voltar pelo WorkManager", summary)
        assertTrue(summary!!.schemaVersion >= 1)
        assertTrue(summary.appVersion.isNotBlank())
        target.delete()
    }
}
