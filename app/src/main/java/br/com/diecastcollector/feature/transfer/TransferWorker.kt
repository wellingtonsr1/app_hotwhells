package br.com.diecastcollector.feature.transfer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import br.com.diecastcollector.R
import br.com.diecastcollector.data.importexport.CsvImportService
import br.com.diecastcollector.domain.backup.BackupService
import br.com.diecastcollector.domain.backup.RestoreMode
import br.com.diecastcollector.feature.settings.BackupReminderStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

/**
 * Operações longas de transferência (T134, FR-150).
 *
 * Backup, validação de pacote, restauração e importação rodam fora do ViewModel: continuam
 * se a tela fechar ou o processo for recriado, e o resultado fica no WorkManager até a tela
 * voltar a observar o trabalho. Cada ação usa um nome único, então tocar duas vezes não
 * duplica a operação.
 *
 * A restauração continua fechando o banco: quem recebe o sucesso precisa reabrir o app.
 */
@HiltWorker
public class TransferWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupService: BackupService,
    private val importService: CsvImportService,
    private val reminderStore: BackupReminderStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val action = inputData.getString(TransferOutput.ACTION)
            ?: return fail(IllegalStateException("Ação de transferência ausente."))
        val title = when (action) {
            TransferOutput.ACTION_BACKUP -> R.string.transfer_notification_backup
            TransferOutput.ACTION_INSPECT -> R.string.transfer_notification_inspect
            TransferOutput.ACTION_RESTORE -> R.string.transfer_notification_restore
            TransferOutput.ACTION_IMPORT -> R.string.transfer_notification_import
            else -> return fail(IllegalStateException("Ação de transferência desconhecida."))
        }
        return runCatching {
            showProgress(title)
            when (action) {
                TransferOutput.ACTION_BACKUP -> backup()
                TransferOutput.ACTION_INSPECT -> inspect()
                TransferOutput.ACTION_RESTORE -> restore()
                else -> import()
            }
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { fail(it) },
        )
    }

    private suspend fun backup(): Data {
        val uri = (inputData.getString(TransferOutput.URI)
            ?: error("Destino do backup ausente.")).toUri()
        persist(uri, write = true)
        val output = applicationContext.contentResolver.openOutputStream(uri)
            ?: error("Não foi possível abrir o destino escolhido.")
        val summary = output.use { stream ->
            backupService.create(stream, appVersion(applicationContext), deviceName())
        }
        // Só um pacote gravado com sucesso conta como backup feito (T120).
        reminderStore.markBackup()
        return TransferOutput.summary(summary)
    }

    private suspend fun inspect(): Data {
        val uri = (inputData.getString(TransferOutput.URI)
            ?: error("Arquivo de backup ausente.")).toUri()
        persist(uri, write = false)
        val copy = copyUriToCache(applicationContext, uri, "backup-restore-")
        val validated = backupService.inspect(copy)
        return Data.Builder()
            .putAll(TransferOutput.summary(validated.summary))
            .putString(TransferOutput.FILE, validated.file.absolutePath)
            .build()
    }

    private suspend fun restore(): Data {
        val path = inputData.getString(TransferOutput.FILE) ?: error("Pacote validado ausente.")
        val file = File(path)
        val cache = applicationContext.cacheDir.canonicalFile
        require(file.canonicalFile.parentFile == cache || file.canonicalPath.startsWith("${cache.path}/")) {
            "O pacote validado precisa estar no cache do aplicativo."
        }
        val mode = inputData.getString(TransferOutput.MODE)
            ?.let { name -> RestoreMode.entries.firstOrNull { it.name == name } }
            ?: RestoreMode.REPLACE
        val summary = backupService.restore(file, mode)
        return TransferOutput.summary(summary)
    }

    private suspend fun import(): Data {
        val uri = (inputData.getString(TransferOutput.URI)
            ?: error("Arquivo de importação ausente.")).toUri()
        persist(uri, write = false)
        val text = readImportText(applicationContext, uri)
        return TransferOutput.imported(importService.apply(text))
    }

    private fun persist(uri: Uri, write: Boolean) {
        // Nem todo contrato do SAF concede permissão persistente; sem ela o trabalho ainda
        // roda no mesmo processo, e a falha de acesso vira erro visível na tela.
        runCatching {
            applicationContext.contentResolver.takePersistableUriPermission(
                uri,
                if (write) {
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                } else {
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                },
            )
        }
    }

    private suspend fun showProgress(titleRes: Int) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    applicationContext.getString(R.string.transfer_notification_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
        val launch = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        val pending = launch?.let {
            PendingIntent.getActivity(
                applicationContext,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_transfer)
            .setContentTitle(applicationContext.getString(titleRes))
            .setContentText(applicationContext.getString(R.string.transfer_notification_running))
            .setOngoing(true)
            .setProgress(0, 0, true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .apply { pending?.let(::setContentIntent) }
            .build()
        setForeground(
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                },
            ),
        )
        setProgress(workDataOf(TransferOutput.STAGE to "running"))
    }

    private fun fail(error: Throwable): Result =
        Result.failure(TransferOutput.failure(error))

    private companion object {
        const val CHANNEL_ID = "transfer"
        const val NOTIFICATION_ID = 0x5452
    }
}
