package br.com.diecastcollector.feature.transfer

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.entity.ManufacturerEntity
import br.com.diecastcollector.data.storage.FileBackupService
import java.io.File
import java.time.Clock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validação do pacote no **SQLite do Android**, não no do Robolectric.
 *
 * O banco real tem a tabela FTS4 `search_index`; `PRAGMA integrity_check` valida o índice
 * invertido e, em banco aberto como somente leitura, falha com «attempt to write a readonly
 * database». Este teste existe porque o Robolectric não reproduzia isso: é o teste que
 * corresponde ao aparelho do usuário.
 */
@RunWith(AndroidJUnit4::class)
public class BackupIntegrityTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: AppDatabase
    private lateinit var service: FileBackupService
    private val packageFile: File by lazy { File(context.cacheDir, "backup-integrity.zip") }

    @Before
    public fun setUp() {
        context.deleteDatabase(DB_NAME)
        database = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .addCallback(AppDatabase.CALLBACK)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        service = FileBackupService(context, database, Clock.systemUTC())
    }

    @After
    public fun tearDown() {
        runCatching { database.close() }
        context.deleteDatabase(DB_NAME)
        packageFile.delete()
    }

    @Test
    public fun pacoteValidaComIndiceFtsNoSQLiteReal() = runBlocking {
        database.manufacturerDao().insert(
            ManufacturerEntity("m1", "Hot Wheels", "EUA", null, 0, 0),
        )

        packageFile.outputStream().use { output ->
            service.create(output, "0.1.0 (1)", "Emulador")
        }

        val validated = service.inspect(packageFile)

        assertEquals(1, validated.summary.counts["manufacturer"])
        assertEquals(2, validated.summary.schemaVersion)
    }

    private companion object {
        const val DB_NAME = "backup-integrity-test.db"
    }
}
