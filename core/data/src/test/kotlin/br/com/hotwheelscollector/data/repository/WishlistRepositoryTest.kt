package br.com.hotwheelscollector.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.AuditLogWriter
import br.com.hotwheelscollector.data.db.TransactionRunner
import br.com.hotwheelscollector.data.db.entity.CarModelEntity
import br.com.hotwheelscollector.data.db.entity.ManufacturerEntity
import br.com.hotwheelscollector.data.db.entity.VariantEntity
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.IsoDate
import br.com.hotwheelscollector.domain.model.MoneyCents
import br.com.hotwheelscollector.domain.model.WishlistItem
import br.com.hotwheelscollector.domain.model.WishlistStatus
import br.com.hotwheelscollector.domain.repository.NewAcquisition
import br.com.hotwheelscollector.domain.repository.NewExemplar
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lista de desejos (US6, FR-057–FR-064).
 *
 * Cobre o que o formulário e a lista dependem: título resolvido pelo vínculo, ordem por
 * status e prioridade, e conversão que preserva prioridade, valores e data de criação.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class WishlistRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: WishlistRepositoryImpl

    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-03-01T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @Before
    public fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.CALLBACK)
            .allowMainThreadQueries()
            .build()
        val transactionRunner = TransactionRunner(database)
        val exemplarRepository = ExemplarRepositoryImpl(
            database = database,
            transactionRunner = transactionRunner,
            auditLogWriter = AuditLogWriter(database.historyDao()),
            clock = clock,
        )
        repository = WishlistRepositoryImpl(database, exemplarRepository, transactionRunner, clock)
    }

    @After
    public fun tearDown() {
        database.close()
    }

    @Test
    public fun `lista resolve titulo pelo vinculo e ordena por prioridade`(): Unit = runBlocking {
        seedCatalog()

        repository.add(wish("w-livre", freeText = "Camaro preto", priority = 3))
        repository.add(wish("w-variante", variantId = "variant1", priority = 1))
        repository.add(wish("w-modelo", modelId = "model1", priority = 2))

        val entries = repository.observeAll().first()

        assertThat(entries.map { it.title })
            .containsExactly("Camaro preto", "Mattel · Twin Mill", "Mattel · Twin Mill")
        assertThat(entries.map { it.item.priority }).containsExactly(1, 2, 3)
    }

    @Test
    public fun `conversao preserva prioridade valores e data de criacao`(): Unit = runBlocking {
        seedCatalog()
        repository.add(
            wish(
                "w1",
                variantId = "variant1",
                priority = 1,
                maxPrice = 5_000,
                foundPrice = 4_500,
            ),
        )

        val before = repository.findById("w1")!!
        val result = repository.convertToAcquisition(
            "w1",
            NewExemplar(
                variantId = "variant1",
                condition = Condition.SEALED,
                acquisition = NewAcquisition(
                    date = IsoDate.parseIsoOrNull("2026-02-28")!!,
                    price = MoneyCents(4_500),
                    store = "Loja do centro",
                ),
            ),
        )
        val created = result.getOrThrow()!!
        val after = repository.findById("w1")!!

        assertThat(after.status).isEqualTo(WishlistStatus.ACQUIRED)
        assertThat(after.convertedExemplarId).isEqualTo(created.exemplar.id)
        assertThat(created.exemplar.variantId).isEqualTo("variant1")
        assertThat(after.priority).isEqualTo(before.priority)
        assertThat(after.maxPrice).isEqualTo(before.maxPrice)
        assertThat(after.foundPrice).isEqualTo(before.foundPrice)
        assertThat(after.createdAt).isEqualTo(before.createdAt)
    }

    @Test
    public fun `falha ao criar o exemplar desfaz a conversao inteira`(): Unit = runBlocking {
        // O desejo aponta para uma variante válida; o rascunho do exemplar é que falha
        // (variante inexistente viola a FK). Nada pode sobrar da conversão.
        seedCatalog()
        repository.add(wish("w1", variantId = "variant1", priority = 1))

        val result = repository.convertToAcquisition(
            "w1",
            NewExemplar(
                variantId = "variant-inexistente",
                condition = Condition.SEALED,
                acquisition = NewAcquisition(
                    date = IsoDate.parseIsoOrNull("2026-02-28")!!,
                    price = MoneyCents(4_500),
                ),
            ),
        )

        val item = repository.findById("w1")!!
        assertThat(result.isFailure).isTrue()
        assertThat(item.status).isEqualTo(WishlistStatus.WANTED)
        assertThat(item.convertedExemplarId).isNull()
        assertThat(database.exemplarDao().observeOwned().first()).isEmpty()
    }

    @Test
    public fun `editar o formulario nao reabre um desejo adquirido`(): Unit = runBlocking {
        repository.add(wish("w1", freeText = "Camaro", priority = 3))
        repository.convertToAcquisition("w1", null)

        val current = repository.findById("w1")!!
        repository.update(current.copy(priority = 1, notes = "achado usado"))

        val after = repository.findById("w1")!!
        assertThat(after.status).isEqualTo(WishlistStatus.ACQUIRED)
        assertThat(after.priority).isEqualTo(1)
        assertThat(after.notes).isEqualTo("achado usado")
    }

    @Test
    public fun `desistir mantem o desejo no historico com status proprio`(): Unit = runBlocking {
        repository.add(wish("w1", freeText = "Qualquer Porsche", priority = 2))

        repository.dismiss("w1")

        val item = repository.findById("w1")!!
        assertThat(item.status).isEqualTo(WishlistStatus.DISMISSED)
        assertThat(item.convertedExemplarId).isNull()
    }

    private fun wish(
        id: String,
        variantId: String? = null,
        modelId: String? = null,
        freeText: String? = null,
        priority: Int = 2,
        maxPrice: Long? = null,
        foundPrice: Long? = null,
    ) = WishlistItem(
        id = id,
        priority = priority,
        variantId = variantId,
        modelId = modelId,
        freeText = freeText,
        maxPrice = maxPrice?.let(::MoneyCents),
        foundPrice = foundPrice?.let(::MoneyCents),
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private suspend fun seedCatalog() {
        val t = 0L
        database.manufacturerDao().insert(ManufacturerEntity("m1", "Mattel", null, null, t, t))
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
                createdAt = t,
                updatedAt = t,
            ),
        )
        database.variantDao().insert(
            VariantEntity(
                id = "variant1",
                modelId = "model1",
                fingerprint = "fp-variant1",
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
                createdAt = t,
                updatedAt = t,
            ),
        )
    }
}
