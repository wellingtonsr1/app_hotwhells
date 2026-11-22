package br.com.diecastcollector.data.importexport

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.data.db.AppDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** T113 (fase 7) — aplicação transacional da importação e idempotência (FR-136–FR-139). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CsvImportServiceTest {

    private lateinit var database: AppDatabase
    private lateinit var service: CsvImportService

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.CALLBACK)
            .allowMainThreadQueries()
            .build()
        service = CsvImportService(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `aplica linhas validas e reaproveita variante e fabricante`(): Unit = runBlocking {
        val result = service.apply(CSV)

        assertThat(result.importedExemplars).isEqualTo(2)
        assertThat(result.skippedDuplicates).isEqualTo(1)
        assertThat(result.invalidRows).isEqualTo(2) // linha inválida + duplicata
        assertThat(result.createdManufacturers).isEqualTo(1)
        assertThat(result.createdModels).isEqualTo(1)
        assertThat(result.createdVariants).isEqualTo(1) // as duas linhas caem na mesma variante
        assertThat(result.createdColors).isEqualTo(1)

        assertThat(database.exemplarDao().countAll()).isEqualTo(2)
        assertThat(database.exemplarDao().allInternalCodes().toSet())
            .containsExactly("HW-0001", "HW-0002")
    }

    @Test
    fun `reimportar nao duplica exemplares`(): Unit = runBlocking {
        service.apply(CSV)

        val again = service.apply(CSV)

        assertThat(again.importedExemplars).isEqualTo(0)
        assertThat(again.skippedDuplicates).isEqualTo(3)
        assertThat(database.exemplarDao().countAll()).isEqualTo(2)
    }

    private companion object {
        // Duas linhas válidas (mesma variante), uma inválida (fabricante vazio)
        // e uma com código interno repetido no próprio arquivo.
        val CSV: String = buildString {
            appendLine("fabricante,modelo,escala,condicao,codigo_interno,cor_principal,ano_lancamento")
            appendLine("Mattel,Skyline,1:64,lacrado,HW-0001,azul,2024")
            appendLine("Mattel,Skyline,1:64,solto,HW-0002,azul,2024")
            appendLine(",Modelo Sem Fabricante,1:64,solto,HW-0003,azul,2024")
            appendLine("Mattel,Skyline,1:64,lacrado,HW-0001,azul,2024")
        }
    }
}
