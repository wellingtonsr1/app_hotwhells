package br.com.hotwheelscollector.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.AuditLogWriter
import br.com.hotwheelscollector.data.db.TransactionRunner
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.model.MissingField
import br.com.hotwheelscollector.domain.model.Variant
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
 * Testes do CarModelRepository (T039 - US2, FR-024, FR-025, FR-033–FR-036).
 *
 * Cobre:
 * - Localização de modelo por identidade (fabricante, nome, escala).
 * - Distinção entre escalas diferentes do mesmo modelo (1:64 vs 1:43).
 * - Detecção de modelo com mesmo nome/fabricante para exibir aviso.
 * - Arquivamento de modelo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class CarModelRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var modelRepository: CarModelRepositoryImpl
    private lateinit var manufacturerRepository: ManufacturerRepositoryImpl
    private val clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).addCallback(AppDatabase.CALLBACK).allowMainThreadQueries().build()

        modelRepository = CarModelRepositoryImpl(database, clock)
        manufacturerRepository = ManufacturerRepositoryImpl(database, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `findByIdentity localiza modelo existente com mesmo fabricante, nome e escala`() = runBlocking {
        val manufacturer = manufacturerRepository.add("Mattel", "EUA").getOrThrow()
        val created = modelRepository.add(
            manufacturerId = manufacturer.id,
            name = "Twin Mill",
            scale = "1:64",
        ).getOrThrow()

        val found = modelRepository.findByIdentity(
            manufacturerId = manufacturer.id,
            name = "Twin Mill",
            scale = "1:64",
        )

        assertThat(found).isNotNull()
        assertThat(found?.id).isEqualTo(created.id)
        assertThat(found?.name).isEqualTo("Twin Mill")
        assertThat(found?.scale).isEqualTo("1:64")
    }

    @Test
    fun `findByIdentity com escala diferente retorna nulo`() = runBlocking {
        val manufacturer = manufacturerRepository.add("Mattel", "EUA").getOrThrow()
        modelRepository.add(
            manufacturerId = manufacturer.id,
            name = "Twin Mill",
            scale = "1:64",
        ).getOrThrow()

        val found118 = modelRepository.findByIdentity(
            manufacturerId = manufacturer.id,
            name = "Twin Mill",
            scale = "1:18",
        )

        assertThat(found118).isNull()
    }

    @Test
    fun `recusa adicionar modelo com nome vazio ou escala invalida`() = runBlocking {
        val manufacturer = manufacturerRepository.add("Hot Wheels", null).getOrThrow()

        val emptyName = modelRepository.add(manufacturer.id, "   ", "1:64")
        assertThat(emptyName.isFailure).isTrue()
        assertThat(emptyName.exceptionOrNull()).isInstanceOf(DomainError.Validation::class.java)

        val invalidScale = modelRepository.add(manufacturer.id, "Bone Shaker", "64")
        assertThat(invalidScale.isFailure).isTrue()
        assertThat(invalidScale.exceptionOrNull()).isInstanceOf(DomainError.Validation::class.java)
    }

    @Test
    fun `observeIncomplete lista modelo sem variante e o remove quando a variante e criada`() =
        runBlocking {
            val manufacturer = manufacturerRepository.add("Mattel", "EUA").getOrThrow()
            val model = modelRepository.add(manufacturer.id, "Twin Mill", "1:64").getOrThrow()
            val variants = VariantRepositoryImpl(
                database = database,
                transactionRunner = TransactionRunner(database),
                auditLogWriter = AuditLogWriter(database.historyDao()),
                clock = clock,
            )

            val before = modelRepository.observeIncomplete().first()
            assertThat(before.map { it.modelId }).containsExactly(model.id)
            assertThat(before.single().manufacturerName).isEqualTo("Mattel")

            variants.create(
                Variant(
                    id = "variant-1",
                    modelId = model.id,
                    fingerprint = "fp-1",
                    createdAt = Instant.parse("2026-11-16T12:00:00Z"),
                    updatedAt = Instant.parse("2026-11-16T12:00:00Z"),
                ),
            ).getOrThrow()

            // Com variante ativa o modelo deixa de ser "sem variante", sem flag armazenada.
            // A variante criada aqui não tem cor nem ano, então o que muda é o motivo: a
            // linha continua, agora apontada para a variante (ver o teste seguinte).
            val after = modelRepository.observeIncomplete().first()
            assertThat(after.map { it.variantId }).containsExactly("variant-1")
            assertThat(after.single().modelId).isEqualTo(model.id)
        }

    @Test
    fun `observeIncomplete lista variante sem cor ou sem ano e ignora a completa`() = runBlocking {
        val manufacturer = manufacturerRepository.add("Mattel", "EUA").getOrThrow()
        val model = modelRepository.add(manufacturer.id, "Twin Mill", "1:64").getOrThrow()
        val red = ColorRepositoryImpl(database).findOrCreate("Vermelho", null).getOrThrow()
        val variants = VariantRepositoryImpl(
            database = database,
            transactionRunner = TransactionRunner(database),
            auditLogWriter = AuditLogWriter(database.historyDao()),
            clock = clock,
        )
        val now = Instant.parse("2026-11-16T12:00:00Z")

        variants.create(
            Variant("v-sem-nada", model.id, "fp-1", createdAt = now, updatedAt = now),
        ).getOrThrow()
        variants.create(
            Variant(
                id = "v-sem-ano",
                modelId = model.id,
                fingerprint = "fp-2",
                primaryColorId = red.id,
                createdAt = now,
                updatedAt = now,
            ),
        ).getOrThrow()
        variants.create(
            Variant(
                id = "v-completa",
                modelId = model.id,
                fingerprint = "fp-3",
                primaryColorId = red.id,
                releaseYear = 2024,
                createdAt = now,
                updatedAt = now,
            ),
        ).getOrThrow()

        // O modelo já tem variante: nenhuma linha é "modelo sem variante".
        val missing = modelRepository.observeIncomplete().first()
            .associate { it.key to it.missingFields }

        assertThat(missing).hasSize(2)
        assertThat(missing["v-sem-nada"]).containsExactly(
            MissingField.PRIMARY_COLOR,
            MissingField.RELEASE_YEAR,
        )
        assertThat(missing["v-sem-ano"]).containsExactly(MissingField.RELEASE_YEAR)
        assertThat(missing).doesNotContainKey("v-completa")
    }

    @Test
    fun `arquivar modelo remove da lista de ativos`() = runBlocking {
        val manufacturer = manufacturerRepository.add("Matchbox", "Reino Unido").getOrThrow()
        val model = modelRepository.add(manufacturer.id, "Route Master", "1:64").getOrThrow()

        val before = modelRepository.observeActive().first()
        assertThat(before.map { it.id }).contains(model.id)

        modelRepository.archive(model.id)

        val after = modelRepository.observeActive().first()
        assertThat(after.map { it.id }).doesNotContain(model.id)
    }
}
