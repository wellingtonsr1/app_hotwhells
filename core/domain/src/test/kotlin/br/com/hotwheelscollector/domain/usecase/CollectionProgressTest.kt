package br.com.hotwheelscollector.domain.usecase

import br.com.hotwheelscollector.domain.model.Collection
import br.com.hotwheelscollector.domain.model.CollectionItemStatus
import br.com.hotwheelscollector.domain.model.CollectionProgress
import br.com.hotwheelscollector.domain.model.CollectionProgressCalculator
import br.com.hotwheelscollector.domain.repository.CollectionRepository
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Progresso e faltantes (T065, FR-052–FR-054).
 *
 * O Independent Test da fase é 10 itens conhecidos com 6 possuídos → 60% e 4 faltantes.
 * Os casos extras cobrem o que esse exemplo não pega: item não colecionável, quantidade
 * esperada maior que 1, coleção vazia e exemplar possuído além do esperado.
 */
public class CollectionProgressTest {

    private val collection = Collection(
        id = "c1",
        name = "Car Culture",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    @Test
    public fun `dez itens com seis possuidos mostram 60 por cento e quatro faltantes`() {
        val items = (1..10).map { index ->
            item(id = "i$index", owned = if (index <= 6) 1 else 0)
        }

        val progress = CollectionProgressCalculator.calculate(collection, items)

        assertThat(progress.known).isEqualTo(10)
        assertThat(progress.owned).isEqualTo(6)
        assertThat(progress.percent).isEqualTo(60)
        assertThat(progress.missing).hasSize(4)
        assertThat(progress.missing.map { it.itemId })
            .containsExactly("i7", "i8", "i9", "i10")
        assertThat(progress.isComplete).isFalse()
    }

    @Test
    public fun `item nao colecionavel sai do denominador e nunca esta faltando`() {
        val items = listOf(
            item("i1", owned = 1),
            item("i2", owned = 0),
            item("i3", owned = 0, nonCollectible = true),
        )

        val progress = CollectionProgressCalculator.calculate(collection, items)

        assertThat(progress.known).isEqualTo(2)
        assertThat(progress.owned).isEqualTo(1)
        assertThat(progress.percent).isEqualTo(50)
        assertThat(progress.itemCount).isEqualTo(3)
        assertThat(progress.missing.map { it.itemId }).containsExactly("i2")
    }

    @Test
    public fun `quantidade esperada maior que um cobra as unidades que faltam`() {
        val items = listOf(
            item("i1", owned = 1, expected = 2),
            item("i2", owned = 3, expected = 1),
        )

        val progress = CollectionProgressCalculator.calculate(collection, items)

        // 1 de 2 no primeiro item + 1 de 1 no segundo: 2 unidades de 3.
        assertThat(progress.known).isEqualTo(3)
        assertThat(progress.owned).isEqualTo(2)
        assertThat(progress.percent).isEqualTo(66)
        val missing = progress.missing.single()
        assertThat(missing.itemId).isEqualTo("i1")
        assertThat(missing.missingUnits).isEqualTo(1)
    }

    @Test
    public fun `colecao vazia nao e zero por cento nem esta completa`() {
        val progress = CollectionProgressCalculator.calculate(collection, emptyList())

        assertThat(progress.percent).isEqualTo(0)
        assertThat(progress.hasKnownItems).isFalse()
        assertThat(progress.isComplete).isFalse()
        assertThat(progress.missing).isEmpty()
    }

    @Test
    public fun `todos os itens conhecidos possuidos fecham a colecao em cem por cento`() {
        val items = listOf(item("i1", owned = 1), item("i2", owned = 2, expected = 1))

        val progress = CollectionProgressCalculator.calculate(collection, items)

        assertThat(progress.percent).isEqualTo(100)
        assertThat(progress.isComplete).isTrue()
        assertThat(progress.missing).isEmpty()
    }

    @Test
    public fun `faltantes ignoram o que ja foi possuido e vem ordenados pelo rotulo`() {
        val items = listOf(
            item("i1", owned = 0, label = "Mattel · Bone Shaker"),
            item("i2", owned = 1, label = "Mattel · Camaro"),
            item("i3", owned = 0, label = "Mattel · Aero Sculpt"),
        )

        val progress = CollectionProgressCalculator.calculate(collection, items)

        assertThat(progress.missing.map { it.label })
            .containsExactly("Mattel · Aero Sculpt", "Mattel · Bone Shaker")
            .inOrder()
    }

    @Test
    public fun `use case de faltantes filtra o que falta do repositorio`() = runTest {
        val items = listOf(
            item("i1", owned = 1),
            item("i2", owned = 0),
            item("i3", owned = 0, nonCollectible = true),
        )
        val useCase = MissingItemsUseCase(FakeCollectionRepository(items))

        val missing: List<CollectionItemStatus> = useCase.observe("c1").first()

        assertThat(missing.map { it.itemId }).containsExactly("i2")
    }

    private fun item(
        id: String,
        owned: Int,
        expected: Int = 1,
        nonCollectible: Boolean = false,
        label: String = "Mattel · $id",
    ): CollectionItemStatus = CollectionItemStatus(
        itemId = id,
        collectionId = "c1",
        variantId = id,
        label = label,
        expected = expected,
        nonCollectible = nonCollectible,
        ownedExemplars = owned,
    )

    private class FakeCollectionRepository(
        private val items: List<CollectionItemStatus>,
    ) : CollectionRepository {
        override fun observeAll(): Flow<List<CollectionProgress>> = flowOf(emptyList())

        override fun observeItems(collectionId: String): Flow<List<CollectionItemStatus>> =
            flowOf(items.filter { it.collectionId == collectionId })

        override suspend fun findById(id: String): Collection? = null
        override suspend fun create(
            name: String,
            description: String?,
            parentId: String?,
            manufacturerId: String?,
        ) = error("não usado")

        override suspend fun update(
            id: String,
            name: String,
            description: String?,
            parentId: String?,
            manufacturerId: String?,
        ) = error("não usado")

        override suspend fun archive(id: String) = error("não usado")
        override suspend fun addVariant(collectionId: String, variantId: String, expected: Int) =
            error("não usado")

        override suspend fun addModel(collectionId: String, modelId: String, expected: Int) =
            error("não usado")

        override suspend fun setNonCollectible(itemId: String, nonCollectible: Boolean) =
            error("não usado")

        override suspend fun removeItem(itemId: String) = error("não usado")
    }
}
