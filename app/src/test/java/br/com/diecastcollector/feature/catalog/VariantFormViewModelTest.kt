package br.com.diecastcollector.feature.catalog

import br.com.diecastcollector.R
import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.CarModel
import br.com.diecastcollector.domain.model.ColorItem
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.Exemplar
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.domain.model.IncompleteEntry
import br.com.diecastcollector.domain.model.PackagingCondition
import br.com.diecastcollector.domain.model.Photo
import br.com.diecastcollector.domain.model.PhotoAngle
import br.com.diecastcollector.domain.model.Series
import br.com.diecastcollector.domain.model.Variant
import br.com.diecastcollector.domain.model.VariantSummary
import br.com.diecastcollector.domain.repository.AddExemplarOutcome
import br.com.diecastcollector.domain.repository.CarModelRepository
import br.com.diecastcollector.domain.repository.ColorRepository
import br.com.diecastcollector.domain.repository.ExemplarDependents
import br.com.diecastcollector.domain.repository.ExemplarRepository
import br.com.diecastcollector.domain.repository.NewExemplar
import br.com.diecastcollector.domain.repository.PhotoRepository
import br.com.diecastcollector.domain.repository.SeriesRepository
import br.com.diecastcollector.domain.repository.VariantRepository
import br.com.diecastcollector.domain.usecase.AddExemplarUseCase
import br.com.diecastcollector.domain.usecase.CreateVariantUseCase
import br.com.diecastcollector.domain.usecase.MergeVariantsUseCase
import br.com.diecastcollector.domain.usecase.SplitVariantUseCase
import br.com.diecastcollector.domain.usecase.UpdateVariantUseCase
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Testes do cadastro completo de variante: os casos de uso são os reais, o banco é falso.
 *
 * O que se verifica aqui é justamente a ordem que a tela não pode errar: variante →
 * exemplar → fotos (a foto pertence ao exemplar, FR-020), sem duplicar nada quando a
 * gravação de uma foto falha e o usuário tenta de novo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VariantFormViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC)

    private val models = FakeCarModelRepository()
    private val variants = FakeVariantRepository()
    private val exemplars = FakeExemplarRepository()
    private val photos = FakePhotoRepository()
    private val colors = FakeColorRepository()
    private val series = FakeSeriesRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): VariantFormViewModel = VariantFormViewModel(
        createVariant = CreateVariantUseCase(models, variants, fixedClock),
        addExemplar = AddExemplarUseCase(exemplars, fixedClock),
        photoRepository = photos,
        models = models,
        colors = colors,
        seriesRepository = series,
        variants = variants,
        updateVariant = UpdateVariantUseCase(variants, fixedClock),
        exemplarRepository = exemplars,
        mergeVariants = MergeVariantsUseCase(variants),
        splitVariant = SplitVariantUseCase(variants, exemplars),
    )

    @Test
    fun `salvar cria variante e exemplar com cor e condicao escolhidas`() = runTest {
        val model = viewModel()
        model.onPrimaryColorChange("Vermelho")
        model.onConditionChange(Condition.DAMAGED)

        model.onSave(MODEL_ID)

        val created = variants.lastCreated
        assertThat(created).isNotNull()
        assertThat(created!!.primaryColorId).isEqualTo("color-vermelho")
        assertThat(exemplars.lastDraft?.variantId).isEqualTo(created.id)
        assertThat(exemplars.lastDraft?.condition).isEqualTo(Condition.DAMAGED)
        assertThat(model.state.value.savedVariantId).isEqualTo(created.id)
        assertThat(model.state.value.exemplarCode).isEqualTo("HW-000001")
    }

    @Test
    fun `fotos pendentes vao para o exemplar criado e a primeira vira principal`() = runTest {
        val model = viewModel()
        model.onAddPhoto(byteArrayOf(1, 2, 3), PhotoAngle.FRONT)
        model.onAddPhoto(byteArrayOf(4, 5, 6), PhotoAngle.BACK)

        model.onSave(MODEL_ID)

        assertThat(photos.added.map { it.second }).containsExactly(PhotoAngle.FRONT, PhotoAngle.BACK)
        assertThat(photos.added.map { it.first }).containsExactly("exemplar-1", "exemplar-1")
        assertThat(photos.primaryCalls).containsExactly("exemplar-1" to "photo-1")
        assertThat(model.state.value.photos).isEmpty()
        assertThat(model.state.value.savedVariantId).isNotNull()
    }

    @Test
    fun `remover a foto principal promove a proxima`() = runTest {
        val model = viewModel()
        model.onAddPhoto(byteArrayOf(1), PhotoAngle.FRONT)
        model.onAddPhoto(byteArrayOf(2), PhotoAngle.BACK)
        val firstId = model.state.value.photos.first().id

        model.onRemovePhoto(firstId)

        assertThat(model.state.value.photos).hasSize(1)
        assertThat(model.state.value.photos.single().isPrimary).isTrue()
    }

    @Test
    fun `ano invalido nao cria variante`() = runTest {
        val model = viewModel()
        model.onReleaseYearChange("abc")

        model.onSave(MODEL_ID)

        assertThat(model.state.value.message?.resId).isEqualTo(R.string.variant_invalid_year)
        assertThat(variants.createCalls).isEqualTo(0)
        assertThat(exemplars.addCalls).isEqualTo(0)
    }

    @Test
    fun `variante identica e reaproveitada e o exemplar ainda e criado`() = runTest {
        variants.existingFingerprintVariant = variantOf("variante-existente")
        val model = viewModel()

        model.onSave(MODEL_ID)

        // Fingerprint repetido não cria variante nova: reaproveita a existente.
        assertThat(variants.createCalls).isEqualTo(0)
        assertThat(exemplars.addCalls).isEqualTo(1)
        assertThat(exemplars.lastDraft?.variantId).isEqualTo("variante-existente")
        assertThat(model.state.value.savedVariantId).isEqualTo("variante-existente")
    }

    @Test
    fun `serie digitada e cadastrada no fabricante do modelo`() = runTest {
        val model = viewModel()
        model.onSeriesChange("HW Racing")

        model.onSave(MODEL_ID)

        assertThat(series.lastManufacturerId).isEqualTo("mattel")
        assertThat(series.lastName).isEqualTo("HW Racing")
        assertThat(variants.lastCreated?.seriesId).isEqualTo("series-1")
    }

    @Test
    fun `falha de foto mantem a pendente e nao duplica exemplar na nova tentativa`() = runTest {
        photos.failure = DomainError.Storage("sem espaço")
        val model = viewModel()
        model.onAddPhoto(byteArrayOf(9), PhotoAngle.DETAIL)

        model.onSave(MODEL_ID)

        assertThat(model.state.value.savedVariantId).isNull()
        assertThat(model.state.value.photos).hasSize(1)
        assertThat(model.state.value.message?.resId).isEqualTo(R.string.variant_failed_photos)
        assertThat(model.state.value.message?.args).contains(1)
        assertThat(exemplars.addCalls).isEqualTo(1)

        photos.failure = null
        model.onSave(MODEL_ID)

        assertThat(model.state.value.savedVariantId).isNotNull()
        assertThat(model.state.value.photos).isEmpty()
        assertThat(variants.createCalls).isEqualTo(1)
        assertThat(exemplars.addCalls).isEqualTo(1)
    }

    @Test
    fun `edicao carrega os campos da variante sem tocar em exemplares`() = runTest {
        variants.editable = variantOf("variante-1").copy(
            primaryColorId = "color-vermelho",
            secondaryColorIds = listOf("color-azul"),
            releaseYear = 2023,
            wheelsType = "5SP",
            manufacturerCode = "HXY12",
        )
        colors.all += ColorItem(id = "color-vermelho", name = "Vermelho")
        colors.all += ColorItem(id = "color-azul", name = "Azul")
        val model = viewModel()

        model.loadForEdit("variante-1")

        val state = model.state.value
        assertThat(state.editing).isTrue()
        assertThat(state.loading).isFalse()
        assertThat(state.primaryColor).isEqualTo("Vermelho")
        assertThat(state.secondaryColors).isEqualTo("Azul")
        assertThat(state.releaseYearText).isEqualTo("2023")
        assertThat(state.wheelsType).isEqualTo("5SP")
        assertThat(state.manufacturerCode).isEqualTo("HXY12")
    }

    @Test
    fun `salvar em edicao atualiza a mesma variante e nao cria exemplar nem variante`() = runTest {
        variants.editable = variantOf("variante-1")
        colors.all += ColorItem(id = "color-vermelho", name = "Vermelho")
        val model = viewModel()
        model.loadForEdit("variante-1")
        model.onPrimaryColorChange("Vermelho")
        model.onReleaseYearChange("2024")

        model.onSave(modelId = "")

        assertThat(variants.createCalls).isEqualTo(0)
        assertThat(exemplars.addCalls).isEqualTo(0)
        val updated = variants.updated
        assertThat(updated).isNotNull()
        assertThat(updated!!.id).isEqualTo("variante-1")
        assertThat(updated.primaryColorId).isEqualTo("color-vermelho")
        assertThat(updated.releaseYear).isEqualTo(2024)
        assertThat(model.state.value.savedVariantId).isEqualTo("variante-1")
        assertThat(model.state.value.message?.resId)
            .isEqualTo(R.string.variant_success_update)
    }

    @Test
    fun `edicao de variante inexistente mostra mensagem e nao salva`() = runTest {
        val model = viewModel()

        model.loadForEdit("sumiu")

        assertThat(model.state.value.message).isNotNull()
        assertThat(model.state.value.editing).isTrue()
    }

    @Test
    fun `codigo lido preenche o codigo do fabricante quando o campo esta vazio`() = runTest {
        val model = viewModel()

        model.prefillManufacturerCode("HKC25")

        assertThat(model.state.value.manufacturerCode).isEqualTo("HKC25")
    }

    @Test
    fun `codigo lido nao sobrescreve o que o usuario ja digitou`() = runTest {
        val model = viewModel()
        model.onManufacturerCodeChange("HXY12")

        model.prefillManufacturerCode("HKC25")

        assertThat(model.state.value.manufacturerCode).isEqualTo("HXY12")
    }

    @Test
    fun `codigo em branco nao preenche o campo`() = runTest {
        val model = viewModel()

        model.prefillManufacturerCode("  ")

        assertThat(model.state.value.manufacturerCode).isEmpty()
    }

    private companion object {
        const val MODEL_ID = "model-1"
    }
    @Test
    fun `arquivar variante fecha a tela e preserva os exemplares`() = runTest {
        variants.editable = variantOf("variante-1")
        colors.all += ColorItem(id = "color-vermelho", name = "Vermelho")
        val model = viewModel()
        model.loadForEdit("variante-1")

        model.onArchive()

        assertThat(variants.archiveCalls).isEqualTo(1)
        assertThat(model.state.value.variantClosed).isTrue()
        assertThat(model.state.value.message?.resId).isEqualTo(R.string.variant_archive_done)
    }

    @Test
    fun `unir variante usa a variante de destino e fecha a tela`() = runTest {
        variants.editable = variantOf("variante-1")
        variants.sibling = variantOf("variante-2")
        colors.all += ColorItem(id = "color-vermelho", name = "Vermelho")
        val model = viewModel()
        model.loadForEdit("variante-1")

        model.onMergeInto("variante-2")

        assertThat(variants.mergeCalls).isEqualTo(1)
        assertThat(variants.lastMerge).isEqualTo("variante-2" to "variante-1")
        assertThat(model.state.value.variantClosed).isTrue()
        assertThat(model.state.value.message?.resId).isEqualTo(R.string.variant_merge_done)
    }

    @Test
    fun `separar exemplares move apenas os marcados`() = runTest {
        val variant = variantOf("variante-1")
        variants.editable = variant
        variants.sibling = variantOf("variante-2")
        exemplars.found = Exemplar(
            id = "exemplar-1",
            variantId = variant.id,
            internalCode = "HW-000001",
            condition = Condition.LOOSE,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        colors.all += ColorItem(id = "color-vermelho", name = "Vermelho")
        val model = viewModel()
        model.loadForEdit("variante-1")

        model.onToggleSplitExemplar("exemplar-1")
        model.onSplitInto("variante-2")

        assertThat(exemplars.reassignCalls).isEqualTo(1)
        assertThat(exemplars.lastReassign).isEqualTo("exemplar-1" to "variante-2")
        assertThat(model.state.value.message?.resId).isEqualTo(R.string.variant_split_done)
    }

    @Test
    fun `foto repetida avisa sem bloquear e entra no formulario`() = runTest {
        val model = viewModel()
        val bytes = byteArrayOf(1, 2, 3)

        model.onAddPhoto(bytes, PhotoAngle.FRONT)
        model.onAddPhoto(bytes, PhotoAngle.BACK)

        assertThat(model.state.value.photos).hasSize(2)
        assertThat(model.state.value.message?.resId).isEqualTo(R.string.photo_duplicate_warning)
    }

}

private fun variantOf(id: String): Variant = Variant(
    id = id,
    modelId = "model-1",
    fingerprint = "fingerprint",
    createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
)

private class FakeCarModelRepository : CarModelRepository {

    override fun observeActive(): Flow<List<CarModel>> = flowOf(listOf(model()))

    override fun observeByManufacturer(manufacturerId: String): Flow<List<CarModel>> =
        flowOf(listOf(model()))

    override suspend fun findById(id: String): CarModel? = if (id == "model-1") model() else null

    override suspend fun findByIdentity(
        manufacturerId: String,
        name: String,
        scale: String,
    ): CarModel? = null

    override suspend fun add(
        manufacturerId: String,
        name: String,
        scale: String,
        brandId: String?,
        realVehicleId: String?,
    ): DomainResult<CarModel> = Result.failure(DomainError.NotFound)

    override suspend fun update(
        id: String,
        name: String,
        scale: String,
        bodyType: String?,
        material: String?,
        countryOfManufacture: String?,
        notes: String?,
        realVehicleId: String?,
    ): DomainResult<CarModel> = Result.failure(DomainError.NotFound)

    override suspend fun archive(id: String): DomainResult<Unit> = Result.success(Unit)

    override fun observeIncomplete(): Flow<List<IncompleteEntry>> = flowOf(emptyList())

    private fun model(): CarModel = CarModel(
        id = "model-1",
        manufacturerId = "mattel",
        name = "Twin Mill",
        scale = "1:64",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
}

private class FakeVariantRepository : VariantRepository {

    var createCalls: Int = 0
    var lastCreated: Variant? = null
    var existingFingerprintVariant: Variant? = null
    var editable: Variant? = null
    var sibling: Variant? = null
    var updated: Variant? = null
    var archiveCalls: Int = 0
    var mergeCalls: Int = 0
    var lastMerge: Pair<String, String>? = null

    override fun observeSummaries(): Flow<List<VariantSummary>> = flowOf(emptyList())

    override fun observeByModel(modelId: String): Flow<List<Variant>> = flowOf(emptyList())

    override suspend fun findById(id: String): Variant? =
        editable?.takeIf { it.id == id }
            ?: sibling?.takeIf { it.id == id }
            ?: lastCreated?.takeIf { it.id == id }

    override suspend fun findActiveByFingerprint(fingerprint: String): Variant? =
        existingFingerprintVariant

    override suspend fun create(variant: Variant): DomainResult<Variant> {
        createCalls++
        lastCreated = variant
        return Result.success(variant)
    }

    override suspend fun update(variant: Variant): DomainResult<Variant> {
        updated = variant
        return Result.success(variant)
    }

    override suspend fun archive(id: String): DomainResult<Unit> {
        archiveCalls++
        return Result.success(Unit)
    }

    override suspend fun mergeVariants(targetId: String, absorbedId: String): DomainResult<Unit> {
        mergeCalls++
        lastMerge = targetId to absorbedId
        return Result.success(Unit)
    }
}

private class FakeExemplarRepository : ExemplarRepository {

    var addCalls: Int = 0
    var lastDraft: NewExemplar? = null
    var found: Exemplar? = null
    var reassignCalls: Int = 0
    var lastReassign: Pair<String, String>? = null

    override fun observeOwned(): Flow<List<Exemplar>> = flowOf(emptyList())

    override fun observeByVariant(variantId: String): Flow<List<Exemplar>> = flowOf(emptyList())

    override suspend fun findById(id: String): Exemplar? = found?.takeIf { it.id == id }

    override suspend fun add(draft: NewExemplar): DomainResult<AddExemplarOutcome> {
        addCalls++
        lastDraft = draft
        val now = Instant.parse("2026-11-16T12:00:00Z")
        return Result.success(
            AddExemplarOutcome(
                exemplar = Exemplar(
                    id = "exemplar-1",
                    variantId = draft.variantId,
                    internalCode = "HW-000001",
                    condition = draft.condition,
                    status = ExemplarStatus.OWNED,
                    createdAt = now,
                    updatedAt = now,
                ),
                sameVariantCount = 1,
            ),
        )
    }

    override suspend fun updateDetails(
        id: String,
        condition: Condition,
        packagingCondition: PackagingCondition?,
        notes: String?,
        expectedRowVersion: Long,
    ): DomainResult<Exemplar> = Result.failure(DomainError.NotFound)

    override suspend fun move(id: String, toLocationId: String?, note: String?): DomainResult<Unit> =
        Result.success(Unit)

    override suspend fun reassignVariant(id: String, toVariantId: String): DomainResult<Unit> {
        reassignCalls++
        lastReassign = id to toVariantId
        return Result.success(Unit)
    }

    override suspend fun countByVariant(variantId: String): Int = 0

    override suspend fun archive(id: String): DomainResult<Unit> = Result.success(Unit)

    override suspend fun dependents(id: String): ExemplarDependents = ExemplarDependents()

    override suspend fun delete(id: String): DomainResult<Unit> = Result.success(Unit)
}

private class FakePhotoRepository : PhotoRepository {

    val added: MutableList<Pair<String, PhotoAngle>> = mutableListOf()
    val primaryCalls: MutableList<Pair<String, String>> = mutableListOf()
    var failure: DomainError? = null
    private var counter: Int = 0

    override fun observeByExemplar(exemplarId: String): Flow<List<Photo>> = flowOf(emptyList())

    override suspend fun add(
        exemplarId: String,
        angle: PhotoAngle,
        source: ByteArray,
    ): DomainResult<Photo> {
        failure?.let { return Result.failure(it) }
        counter++
        added += exemplarId to angle
        return Result.success(
            Photo(
                id = "photo-$counter",
                exemplarId = exemplarId,
                angle = angle,
                path = "images/$exemplarId/photo-$counter.jpg",
                checksum = "checksum-$counter",
                isPrimary = counter == 1,
            ),
        )
    }

    override suspend fun setPrimary(exemplarId: String, photoId: String): DomainResult<Unit> {
        primaryCalls += exemplarId to photoId
        return Result.success(Unit)
    }

    override suspend fun remove(photoId: String): DomainResult<Unit> = Result.success(Unit)
}

private class FakeColorRepository : ColorRepository {

    val all: MutableList<ColorItem> = mutableListOf()

    override fun observeAll(): Flow<List<ColorItem>> = flowOf(all.toList())

    override suspend fun findOrCreate(name: String, hex: String?): DomainResult<ColorItem> =
        Result.success(
            ColorItem(
                id = "color-${name.trim().lowercase()}",
                name = name.trim(),
                hex = hex,
            ),
        )
}

private class FakeSeriesRepository : SeriesRepository {

    var lastManufacturerId: String? = null
    var lastName: String? = null

    override fun observeByManufacturer(manufacturerId: String): Flow<List<Series>> =
        flowOf(emptyList())

    override suspend fun findById(id: String): Series? = null

    override suspend fun rename(id: String, name: String): DomainResult<Unit> =
        Result.success(Unit)

    override suspend fun archive(id: String): DomainResult<Unit> = Result.success(Unit)

    override suspend fun add(
        manufacturerId: String,
        name: String,
        parentId: String?,
    ): DomainResult<Series> {
        lastManufacturerId = manufacturerId
        lastName = name
        return Result.success(
            Series(
                id = "series-1",
                manufacturerId = manufacturerId,
                name = name,
                parentId = parentId,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
    }
}
