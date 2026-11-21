/** T145: união e separação de variantes com múltiplos exemplares (Art. I.3, V.3). */
package br.com.hotwheelscollector.domain.usecase

import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.Exemplar
import br.com.hotwheelscollector.domain.model.ExemplarStatus
import br.com.hotwheelscollector.domain.model.PackagingCondition
import br.com.hotwheelscollector.domain.model.Variant
import br.com.hotwheelscollector.domain.model.VariantSummary
import br.com.hotwheelscollector.domain.repository.AddExemplarOutcome
import br.com.hotwheelscollector.domain.repository.ExemplarDependents
import br.com.hotwheelscollector.domain.repository.ExemplarRepository
import br.com.hotwheelscollector.domain.repository.NewExemplar
import br.com.hotwheelscollector.domain.repository.VariantRepository
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class VariantMergeSplitTest {

    private val now = Instant.parse("2026-11-21T12:00:00Z")

    private fun variant(id: String, modelId: String = "model-1", archived: Boolean = false) = Variant(
        id = id,
        modelId = modelId,
        fingerprint = "fp-$id",
        archivedAt = if (archived) now else null,
        createdAt = now,
        updatedAt = now,
    )

    private fun exemplar(id: String, variantId: String) = Exemplar(
        id = id,
        variantId = variantId,
        internalCode = "HW-$id",
        condition = Condition.LOOSE,
        createdAt = now,
        updatedAt = now,
    )

    @Test
    fun `uniao recusa a mesma variante`() = runTest {
        val repo = MergeSplitVariantRepository(listOf(variant("v1")))
        val result = MergeVariantsUseCase(repo).merge("v1", "v1")

        assertThat(result.exceptionOrNull()).isInstanceOf(DomainError.Validation::class.java)
        assertThat(repo.mergeCalls).isEqualTo(0)
    }

    @Test
    fun `uniao recusa variantes de modelos diferentes`() = runTest {
        val repo = MergeSplitVariantRepository(listOf(variant("v1"), variant("v2", modelId = "model-2")))
        val result = MergeVariantsUseCase(repo).merge("v1", "v2")

        assertThat(result.exceptionOrNull()).isInstanceOf(DomainError.Validation::class.java)
        assertThat(repo.mergeCalls).isEqualTo(0)
    }

    @Test
    fun `uniao válida delega a escrita transacional do repositório`() = runTest {
        val repo = MergeSplitVariantRepository(listOf(variant("v1"), variant("v2")))
        val result = MergeVariantsUseCase(repo).merge("v1", "v2")

        assertThat(result.isSuccess).isTrue()
        assertThat(repo.mergeCalls).isEqualTo(1)
        assertThat(repo.lastMerge).isEqualTo("v1" to "v2")
    }

    @Test
    fun `separacao move apenas os exemplares escolhidos`() = runTest {
        val variants = MergeSplitVariantRepository(listOf(variant("v1"), variant("v2")))
        val exemplars = MergeSplitExemplarRepository(
            listOf(exemplar("e1", "v1"), exemplar("e2", "v1"), exemplar("e3", "v1")),
        )

        val result = SplitVariantUseCase(variants, exemplars).split("v1", "v2", listOf("e1", "e3"))

        assertThat(result.isSuccess).isTrue()
        assertThat(exemplars.reassigned).containsExactly("e1" to "v2", "e3" to "v2")
        assertThat(exemplars.findById("e2")!!.variantId).isEqualTo("v1")
    }

    @Test
    fun `separacao recusa exemplar de outra variante sem mover nada`() = runTest {
        val variants = MergeSplitVariantRepository(listOf(variant("v1"), variant("v2")))
        val exemplars = MergeSplitExemplarRepository(
            listOf(exemplar("e1", "v1"), exemplar("e2", "outra")),
        )

        val result = SplitVariantUseCase(variants, exemplars).split("v1", "v2", listOf("e1", "e2"))

        assertThat(result.exceptionOrNull()).isInstanceOf(DomainError.Validation::class.java)
        assertThat(exemplars.reassigned).isEmpty()
    }

    @Test
    fun `separacao sem exemplares é recusada`() = runTest {
        val variants = MergeSplitVariantRepository(listOf(variant("v1"), variant("v2")))
        val result = SplitVariantUseCase(variants, MergeSplitExemplarRepository(emptyList()))
            .split("v1", "v2", emptyList())

        assertThat(result.exceptionOrNull()).isInstanceOf(DomainError.Validation::class.java)
    }
}

private class MergeSplitVariantRepository(initial: List<Variant>) : VariantRepository {

    private val variants = initial.associateBy { it.id }.toMutableMap()
    var mergeCalls: Int = 0
    var lastMerge: Pair<String, String>? = null

    override fun observeSummaries(): Flow<List<VariantSummary>> = flowOf(emptyList())
    override fun observeByModel(modelId: String): Flow<List<Variant>> =
        flowOf(variants.values.filter { it.modelId == modelId })

    override suspend fun findById(id: String): Variant? = variants[id]
    override suspend fun findActiveByFingerprint(fingerprint: String): Variant? =
        variants.values.firstOrNull { it.fingerprint == fingerprint && it.archivedAt == null }

    override suspend fun create(variant: Variant): DomainResult<Variant> {
        variants[variant.id] = variant
        return Result.success(variant)
    }

    override suspend fun update(variant: Variant): DomainResult<Variant> {
        variants[variant.id] = variant
        return Result.success(variant)
    }

    override suspend fun archive(id: String): DomainResult<Unit> = Result.success(Unit)

    override suspend fun mergeVariants(targetId: String, absorbedId: String): DomainResult<Unit> {
        mergeCalls++
        lastMerge = targetId to absorbedId
        return Result.success(Unit)
    }
}

private class MergeSplitExemplarRepository(initial: List<Exemplar>) : ExemplarRepository {

    private val byId: MutableMap<String, Exemplar> = initial.associateBy { it.id }.toMutableMap()
    val reassigned: MutableList<Pair<String, String>> = mutableListOf()

    override fun observeOwned(): Flow<List<Exemplar>> = flowOf(byId.values.toList())
    override fun observeByVariant(variantId: String): Flow<List<Exemplar>> =
        flowOf(byId.values.filter { it.variantId == variantId })

    override suspend fun findById(id: String): Exemplar? = byId[id]

    override suspend fun add(draft: NewExemplar): DomainResult<AddExemplarOutcome> =
        error("não usado")

    override suspend fun updateDetails(
        id: String,
        condition: Condition,
        packagingCondition: PackagingCondition?,
        notes: String?,
        expectedRowVersion: Long,
    ): DomainResult<Exemplar> = error("não usado")

    override suspend fun move(id: String, toLocationId: String?, note: String?): DomainResult<Unit> =
        error("não usado")

    override suspend fun reassignVariant(id: String, toVariantId: String): DomainResult<Unit> {
        val current = byId[id] ?: return Result.failure(DomainError.NotFound)
        byId[id] = current.copy(variantId = toVariantId)
        reassigned += id to toVariantId
        return Result.success(Unit)
    }

    override suspend fun countByVariant(variantId: String): Int {
        var total = 0
        for (exemplar in byId.values) {
            if (exemplar.variantId == variantId && exemplar.status == ExemplarStatus.OWNED) total++
        }
        return total
    }

    override suspend fun archive(id: String): DomainResult<Unit> = error("não usado")
    override suspend fun dependents(id: String): ExemplarDependents = ExemplarDependents()
    override suspend fun delete(id: String): DomainResult<Unit> = error("não usado")
}
