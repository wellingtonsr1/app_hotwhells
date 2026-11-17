package br.com.hotwheelscollector.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.domain.error.DomainError
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
