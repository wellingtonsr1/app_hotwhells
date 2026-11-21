package br.com.hotwheelscollector.feature.locations

import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.InventoryItem
import br.com.hotwheelscollector.domain.model.Location
import br.com.hotwheelscollector.domain.model.LocationArchivePolicy
import br.com.hotwheelscollector.domain.model.LocationNode
import br.com.hotwheelscollector.domain.model.LocationType
import br.com.hotwheelscollector.domain.model.Movement
import br.com.hotwheelscollector.domain.repository.LocationRepository
import br.com.hotwheelscollector.ui.UiMessage
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Mensagem da exclusão de localização.
 *
 * O número anunciado tem de ser o que o repositório tratou, e não o que a linha da árvore
 * exibia: a linha conta possuídos da subárvore, o repositório trata os possuídos daquela
 * localização. O fake devolve números deliberadamente diferentes para provar de onde a
 * mensagem tira o valor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class LocationsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    public fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    public fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    public fun `mensagem usa a contagem devolvida pelo repositorio`(): Unit = runTest {
        val repository = FakeLocationRepository(handled = 5, advertisedOnScreen = 99)
        val viewModel = LocationsViewModel(repository)
        val collector = launch(dispatcher) { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onArchive("box", LocationArchivePolicy.MoveTo("dest"))
        advanceUntilIdle()

        assertThat(viewModel.state.value.message)
            .isEqualTo(LocationsMessage.MovedExemplars(count = 5))
        collector.cancel()
    }

    @Test
    public fun `arquivar exemplares informa a contagem tratada`(): Unit = runTest {
        val repository = FakeLocationRepository(handled = 3, advertisedOnScreen = 5)
        val viewModel = LocationsViewModel(repository)
        val collector = launch(dispatcher) { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onArchive("box", LocationArchivePolicy.ArchiveExemplars)
        advanceUntilIdle()

        assertThat(viewModel.state.value.message)
            .isEqualTo(LocationsMessage.ArchivedExemplars(count = 3))
        collector.cancel()
    }

    @Test
    public fun `falha do repositorio vira a mensagem da tela`(): Unit = runTest {
        val repository = FakeLocationRepository(
            handled = 0,
            advertisedOnScreen = 2,
            failure = "Localização bloqueada.",
        )
        val viewModel = LocationsViewModel(repository)
        val collector = launch(dispatcher) { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.onArchive("box", LocationArchivePolicy.None)
        advanceUntilIdle()

        assertThat(viewModel.state.value.message)
            .isEqualTo(
                // Falha fora do domínio não expõe texto cru: cai no genérico (T131).
                LocationsMessage.Failure(UiMessage(R.string.error_generic)),
            )
        collector.cancel()
    }

    private class FakeLocationRepository(
        private val handled: Int,
        advertisedOnScreen: Int,
        private val failure: String? = null,
    ) : LocationRepository {

        private val tree = listOf(
            LocationNode(
                location = Location(
                    id = "box",
                    type = LocationType.CRATE,
                    name = "Caixa 12",
                    createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                    updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
                ),
                exemplarCount = advertisedOnScreen,
            ),
        )

        override fun observeActive(): Flow<List<Location>> =
            flowOf(tree.map { it.location })

        override fun observeTree(): Flow<List<LocationNode>> = flowOf(tree)

        override suspend fun children(parentId: String): List<Location> = emptyList()

        override suspend fun findById(id: String): Location? =
            tree.firstOrNull { it.location.id == id }?.location

        override suspend fun add(
            type: LocationType,
            name: String,
            parentId: String?,
        ): DomainResult<Location> = Result.failure(UnsupportedOperationException())

        override suspend fun rename(id: String, name: String): DomainResult<Unit> =
            Result.success(Unit)

        override suspend fun archive(
            id: String,
            policy: LocationArchivePolicy,
        ): DomainResult<Int> = when {
            failure != null -> Result.failure(IllegalStateException(failure))
            else -> Result.success(handled)
        }

        override suspend fun movements(exemplarId: String): Flow<List<Movement>> = flowOf(emptyList())

        override fun observeContents(locationId: String): Flow<List<InventoryItem>> =
            flowOf(emptyList())
    }
}
