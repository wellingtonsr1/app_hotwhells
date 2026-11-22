package br.com.diecastcollector.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.entity.CarModelEntity
import br.com.diecastcollector.data.db.entity.ExemplarEntity
import br.com.diecastcollector.data.db.entity.ManufacturerEntity
import br.com.diecastcollector.data.db.entity.PhotoEntity
import br.com.diecastcollector.data.db.entity.VariantEntity
import br.com.diecastcollector.data.importexport.ReportFormat
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** T118 — inventário lido do banco e PDF gerado com o total correto. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReportsRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ReportsRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.CALLBACK)
            .allowMainThreadQueries()
            .build()
        repository = ReportsRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `inventario traz modelo condicao e contagem de fotos por exemplar`(): Unit = runBlocking {
        seed()

        val rows = repository.inventoryRows()

        assertThat(rows).hasSize(2)
        val byCode = rows.associateBy { it.internalCode }
        assertThat(byCode.getValue("HW-1").modelName).isEqualTo("Twin Mill")
        assertThat(byCode.getValue("HW-1").condition).isEqualTo("SEALED")
        assertThat(byCode.getValue("HW-1").photoCount).isEqualTo(2)
        // Sem cor principal na variante, o rótulo fica nulo e o relatório usa "não informado".
        assertThat(byCode.getValue("HW-1").variantLabel).isNull()
        assertThat(byCode.getValue("HW-2").photoCount).isEqualTo(0)
    }

    @Test
    fun `gera PDF do inventario com o total do painel`(): Unit = runBlocking {
        seed()

        val result = repository.buildInventoryReport(ReportFormat.PDF)

        assertThat(result.totalExemplares).isEqualTo(2)
        assertThat(result.pageCount).isEqualTo(1)
        assertThat(result.bytes).isNotEmpty()
        assertThat(String(result.bytes, 0, 4, Charsets.ISO_8859_1)).isEqualTo("%PDF")
        assertThat(result.fileName).startsWith("colecao-inventario-")
        assertThat(result.fileName).endsWith(".pdf")
    }

    @Test
    fun `gera CSV e XLSX com o inventario e nome datado`(): Unit = runBlocking {
        seed()

        val csv = repository.buildInventoryReport(ReportFormat.CSV)
        val csvText = csv.bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        assertThat(csv.totalExemplares).isEqualTo(2)
        assertThat(csv.fileName).endsWith(".csv")
        assertThat(csvText.lines().first()).isEqualTo("Código,Modelo,Variante,Condição,Fotos")
        assertThat(csvText).contains("HW-1,Twin Mill,não informado,SEALED,2")

        val xlsx = repository.buildInventoryReport(ReportFormat.XLSX)
        assertThat(xlsx.totalExemplares).isEqualTo(2)
        assertThat(xlsx.fileName).endsWith(".xlsx")
        assertThat(xlsx.bytes.copyOfRange(0, 2).toString(Charsets.ISO_8859_1)).isEqualTo("PK")
    }

    private suspend fun seed() {
        val now = 0L
        database.manufacturerDao().insert(
            ManufacturerEntity("m1", "Mattel", null, null, now, now),
        )
        database.carModelDao().insert(
            CarModelEntity(
                id = "model1",
                manufacturerId = "m1",
                name = "Twin Mill",
                scale = "1:64",
                brandId = null,
                realVehicleId = null,
                bodyType = null,
                material = null,
                countryOfManufacture = null,
                notes = null,
                archivedAt = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        database.variantDao().insert(
            VariantEntity(
                id = "variant1",
                modelId = "model1",
                fingerprint = "fp-twin",
                seriesId = null,
                primaryColorId = null,
                wheelsType = null,
                baseType = null,
                decoration = null,
                releaseYear = 2024,
                collectionNumber = null,
                seriesNumber = null,
                edition = null,
                manufacturerCode = null,
                packagingType = null,
                archivedAt = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        database.exemplarDao().insert(exemplar("e1", "HW-1"))
        database.exemplarDao().insert(exemplar("e2", "HW-2"))
        database.photoDao().insert(photo("p1", "e1", primary = true, order = 0))
        database.photoDao().insert(photo("p2", "e1", primary = false, order = 1))
    }

    private fun exemplar(id: String, code: String) = ExemplarEntity(
        id = id,
        variantId = "variant1",
        internalCode = code,
        condition = "SEALED",
        packagingCondition = null,
        locationId = null,
        status = "OWNED",
        notes = null,
        rowVersion = 0,
        createdAt = 0,
        updatedAt = 0,
    )

    private fun photo(id: String, exemplarId: String, primary: Boolean, order: Int) = PhotoEntity(
        id = id,
        exemplarId = exemplarId,
        angle = "FRONT",
        path = "images/$exemplarId/$id.jpg",
        checksum = "checksum-$id",
        isPrimary = primary,
        sortOrder = order,
        width = 100,
        height = 100,
        bytes = 1024,
        capturedAt = 0,
    )
}
