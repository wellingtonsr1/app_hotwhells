package br.com.diecastcollector.feature.wishlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.Variant
import br.com.diecastcollector.domain.model.VariantSummary
import br.com.diecastcollector.domain.model.WishlistItem
import br.com.diecastcollector.domain.repository.AddExemplarOutcome
import br.com.diecastcollector.domain.repository.NewExemplar
import br.com.diecastcollector.domain.repository.VariantRepository
import br.com.diecastcollector.domain.repository.WishlistEntry
import br.com.diecastcollector.domain.repository.WishlistRepository
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Foto do desejo presa ao formulário (FR-057, FR-153).
 *
 * O que o teste protege é o par de riscos do arquivo gravado antes do insert: sair sem salvar
 * não pode deixar foto órfã em `images/wish-<id>/`, e salvar a edição não pode apagar a foto
 * que passou a ser a do desejo. A saída do formulário é exercitada pelo caminho real — o
 * `ViewModelStore` limpa o ViewModel, que roda `onCleared` —, não por um método de teste.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class WishlistFormViewModelTest {

    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-03-01T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val repository = FakeWishlistRepository()
    private val variants = FakeVariantRepository()
    private val scope = CoroutineScope(UnconfinedTestDispatcher())
    private val stores = mutableMapOf<WishlistFormViewModel, ViewModelStore>()

    @Before
    public fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    public fun tearDown() {
        stores.values.forEach { store -> store.clear() }
        Dispatchers.resetMain()
    }

    @Test
    public fun `cancelar depois de escolher a foto apaga o rascunho`(): Unit = runBlocking {
        val viewModel = form()
        viewModel.onTitleChange("Camaro")
        viewModel.onPhotoSelected(ByteArray(8))
        val gravada = awaitPhoto(viewModel)

        leave(viewModel)

        assertThat(repository.deleted).containsExactly(gravada)
        assertThat(repository.items).isEmpty()
    }

    @Test
    public fun `salvar mantem a foto que o desejo passou a usar`(): Unit = runBlocking {
        val viewModel = form()
        viewModel.onTitleChange("Camaro")
        viewModel.onPhotoSelected(ByteArray(8))
        val gravada = awaitPhoto(viewModel)

        viewModel.onSave()
        awaitState(viewModel) { it.saved }
        leave(viewModel)

        // Nada foi apagado: o arquivo agora é a foto do desejo, não um rascunho.
        assertThat(repository.deleted).isEmpty()
        assertThat(repository.items.values.single().photoPath).isEqualTo(gravada)
    }

    @Test
    public fun `cancelar a edicao apaga so a foto nova e guarda a antiga`(): Unit = runBlocking {
        repository.seed(desejo(id = "w1", photoPath = FOTO_ANTIGA))
        val viewModel = form(itemId = "w1")
        awaitState(viewModel) { it.photoPath == FOTO_ANTIGA }

        viewModel.onPhotoSelected(ByteArray(8))
        val nova = awaitState(viewModel) { it.photoPath == FOTO_NOVA }.photoPath

        leave(viewModel)

        assertThat(repository.deleted).containsExactly(nova)
        assertThat(repository.items.getValue("w1").photoPath).isEqualTo(FOTO_ANTIGA)
    }

    @Test
    public fun `salvar a edicao troca a foto e so entao apaga a antiga`(): Unit = runBlocking {
        repository.seed(desejo(id = "w1", photoPath = FOTO_ANTIGA))
        val viewModel = form(itemId = "w1")
        awaitState(viewModel) { it.photoPath == FOTO_ANTIGA }

        viewModel.onPhotoSelected(ByteArray(8))
        awaitState(viewModel) { it.photoPath == FOTO_NOVA }
        // Escolher a foto nova não mexe no disco da anterior: desistir ainda é possível.
        assertThat(repository.deleted).isEmpty()

        viewModel.onSave()
        awaitState(viewModel) { it.saved }

        assertThat(repository.deleted).containsExactly(FOTO_ANTIGA)
        assertThat(repository.items.getValue("w1").photoPath).isEqualTo(FOTO_NOVA)

        leave(viewModel)
        // A foto nova continua no disco depois de sair: agora ela é do desejo.
        assertThat(repository.deleted).containsExactly(FOTO_ANTIGA)
    }

    private fun form(itemId: String? = null): WishlistFormViewModel {
        val savedStateHandle = SavedStateHandle(
            if (itemId == null) {
                emptyMap()
            } else {
                mapOf(WishlistFormViewModel.ARG_ITEM_ID to itemId)
            },
        )
        val viewModel = WishlistFormViewModel(
            savedStateHandle = savedStateHandle,
            wishlistRepository = repository,
            variantRepository = variants,
            clock = clock,
            applicationScope = scope,
        )
        stores[viewModel] = ViewModelStore().apply { put("form", viewModel) }
        return viewModel
    }

    /** Sair do formulário é o `ViewModelStore` limpando o ViewModel, como faz a navegação. */
    private fun leave(viewModel: WishlistFormViewModel) {
        stores.getValue(viewModel).clear()
    }

    private suspend fun awaitState(
        viewModel: WishlistFormViewModel,
        predicate: (WishlistFormUiState) -> Boolean,
    ): WishlistFormUiState = withTimeout(5_000) { viewModel.state.first(predicate) }

    private suspend fun awaitPhoto(viewModel: WishlistFormViewModel): String =
        requireNotNull(awaitState(viewModel) { it.photoPath != null }.photoPath)

    private fun desejo(id: String, photoPath: String? = null) = WishlistItem(
        id = id,
        freeText = "Camaro",
        photoPath = photoPath,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private companion object {
        const val FOTO_ANTIGA: String = "images/wish-w1/front_antiga.jpg"
        const val FOTO_NOVA: String = "images/wish-w1/front_1.jpg"
    }
}

private class FakeWishlistRepository : WishlistRepository {

    val items: MutableMap<String, WishlistItem> = linkedMapOf()
    val deleted: MutableList<String> = mutableListOf()
    private var photos: Int = 0

    fun seed(item: WishlistItem) {
        items[item.id] = item
    }

    override fun observeAll(): Flow<List<WishlistEntry>> = flowOf(emptyList())

    override suspend fun findById(id: String): WishlistItem? = items[id]

    override suspend fun add(item: WishlistItem): DomainResult<WishlistItem> {
        items[item.id] = item
        return Result.success(item)
    }

    override suspend fun update(item: WishlistItem): DomainResult<WishlistItem> {
        items[item.id] = item
        return Result.success(item)
    }

    override suspend fun dismiss(id: String): DomainResult<Unit> = Result.success(Unit)

    override suspend fun storePhoto(wishId: String, source: ByteArray): DomainResult<String> {
        photos += 1
        return Result.success("images/wish-$wishId/front_$photos.jpg")
    }

    override suspend fun deletePhoto(relativePath: String) {
        deleted.add(relativePath)
    }

    override suspend fun convertToAcquisition(
        id: String,
        draft: NewExemplar?,
    ): DomainResult<AddExemplarOutcome?> = Result.success(null)
}

private class FakeVariantRepository : VariantRepository {

    override fun observeSummaries(): Flow<List<VariantSummary>> = flowOf(emptyList())

    override fun observeByModel(modelId: String): Flow<List<Variant>> = flowOf(emptyList())

    override suspend fun findById(id: String): Variant? = null

    override suspend fun findActiveByFingerprint(fingerprint: String): Variant? = null

    override suspend fun create(variant: Variant): DomainResult<Variant> = Result.success(variant)

    override suspend fun update(variant: Variant): DomainResult<Variant> = Result.success(variant)

    override suspend fun archive(id: String): DomainResult<Unit> = Result.success(Unit)

    override suspend fun mergeVariants(targetId: String, absorbedId: String): DomainResult<Unit> =
        Result.success(Unit)
}
