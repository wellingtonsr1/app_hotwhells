package br.com.hotwheelscollector.domain.usecase

import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.ExemplarStatus
import br.com.hotwheelscollector.domain.model.RecognitionSource
import br.com.hotwheelscollector.domain.model.SearchFilterOptions
import br.com.hotwheelscollector.domain.model.SearchFilters
import br.com.hotwheelscollector.domain.model.SearchResultItem
import br.com.hotwheelscollector.domain.recognition.RecognitionField
import br.com.hotwheelscollector.domain.recognition.RecognitionProposal
import br.com.hotwheelscollector.domain.recognition.RecognitionSuggestion
import br.com.hotwheelscollector.domain.repository.SearchRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** T126 — correspondências no catálogo local a partir das sugestões (FR-103/FR-110). */
class FindCatalogMatchesUseCaseTest {

    private val twinMill = item("e1", "v1", "Twin Mill", "Mattel")
    private val twinMillCustom = item("e2", "v2", "Twin Mill Custom", "Mattel")
    private val boneShaker = item("e3", "v3", "Bone Shaker", "Mattel")
    private val skyline = item("e4", "v4", "Skyline R34", "Hot Wheels")

    @Test
    fun `ordena por confianca e devolve no maximo cinco`() = runTest {
        val candidates = (1..7).map { index ->
            item("e$index", "v$index", "Twin Mill $index", "Mattel")
        }
        val search = FakeSearch(
            idsByQuery = mapOf("Twin Mill" to candidates.map { it.variantId }),
            items = candidates,
        )
        val useCase = FindCatalogMatchesUseCase(search)

        val matches = useCase(proposal("Twin Mill", RecognitionField.MODEL))

        assertThat(matches).hasSize(5)
        matches.zipWithNext().forEach { (first, second) ->
            assertThat(first.confidence).isAtLeast(second.confidence)
        }
        assertThat(matches.map { it.variantId }).containsNoDuplicates()
    }

    @Test
    fun `modelo exato fica acima de modelo apenas parecido`() = runTest {
        val search = FakeSearch(
            idsByQuery = mapOf(
                "Twin Mill" to listOf("v1", "v2"),
                "Mattel" to listOf("v1", "v2"),
            ),
            items = listOf(twinMill, twinMillCustom),
        )
        val useCase = FindCatalogMatchesUseCase(search)
        val proposal = RecognitionProposal.of(
            source = RecognitionSource.OCR,
            provider = "teste",
            suggestions = listOf(
                RecognitionSuggestion(RecognitionField.MODEL, "Twin Mill", 0.9),
                RecognitionSuggestion(RecognitionField.MANUFACTURER, "Mattel", 0.9),
            ),
        )

        val matches = useCase(proposal)

        assertThat(matches.first().variantId).isEqualTo("v1")
        assertThat(matches.first().matchedFields)
            .containsExactly(RecognitionField.MODEL, RecognitionField.MANUFACTURER)
        assertThat(matches.first().source).isEqualTo(RecognitionSource.CATALOG)
        assertThat(matches[1].confidence).isLessThan(matches[0].confidence)
    }

    @Test
    fun `sem sugestoes nao consulta o catalogo`() = runTest {
        val search = FakeSearch()
        val useCase = FindCatalogMatchesUseCase(search)

        val matches = useCase(
            RecognitionProposal.unidentified(RecognitionSource.OCR, "teste"),
        )

        assertThat(matches).isEmpty()
        assertThat(search.queries).isEmpty()
        assertThat(search.lastFilters).isNull()
    }

    @Test
    fun `busca inclui exemplares fora do acervo ativo`() = runTest {
        val sold = item("e9", "v9", "Twin Mill", "Mattel", status = ExemplarStatus.SOLD)
        val search = FakeSearch(
            idsByQuery = mapOf("Twin Mill" to listOf("v9")),
            items = listOf(sold),
        )
        val useCase = FindCatalogMatchesUseCase(search)

        val matches = useCase(proposal("Twin Mill", RecognitionField.MODEL))

        assertThat(matches).hasSize(1)
        assertThat(search.lastFilters?.statuses).isEmpty()
    }

    @Test
    fun `descarta candidato sem semelhanca com as sugestoes`() = runTest {
        val search = FakeSearch(
            idsByQuery = mapOf("Twin Mill" to listOf("v3")),
            items = listOf(boneShaker),
        )
        val useCase = FindCatalogMatchesUseCase(search)

        assertThat(useCase(proposal("Twin Mill", RecognitionField.MODEL))).isEmpty()
    }

    @Test
    fun `nao repete a mesma variante em varios exemplares`() = runTest {
        val anotherCopy = item("e5", "v1", "Twin Mill", "Mattel")
        val search = FakeSearch(
            idsByQuery = mapOf("Twin Mill" to listOf("v1")),
            items = listOf(twinMill, anotherCopy),
        )
        val useCase = FindCatalogMatchesUseCase(search)

        val matches = useCase(proposal("Twin Mill", RecognitionField.MODEL))

        assertThat(matches).hasSize(1)
        assertThat(matches.single().variantId).isEqualTo("v1")
    }

    @Test
    fun `codigo de barras casa pelo codigo do fabricante`() = runTest {
        val coded = item("e6", "v6", "Twin Mill", "Mattel", manufacturerCode = "HKC25")
        val search = FakeSearch(
            idsByQuery = mapOf("HKC25" to listOf("v6")),
            items = listOf(coded),
        )
        val useCase = FindCatalogMatchesUseCase(search)

        val matches = useCase(proposal("HKC25", RecognitionField.CODE))

        assertThat(matches.single().matchedFields).containsExactly(RecognitionField.CODE)
        assertThat(matches.single().confidence).isWithin(1e-9).of(0.9)
    }

    @Test
    fun `sugestao abaixo do limiar nao vira proposta`() {
        val proposal = RecognitionProposal.of(
            source = RecognitionSource.OCR,
            provider = "teste",
            suggestions = listOf(
                RecognitionSuggestion(RecognitionField.MODEL, "Twin Mill", 0.59),
                RecognitionSuggestion(RecognitionField.SERIES, "Car Culture", 0.8),
            ),
        )

        assertThat(proposal.suggestions.map { it.field })
            .containsExactly(RecognitionField.SERIES)
    }

    private fun proposal(value: String, field: RecognitionField): RecognitionProposal =
        RecognitionProposal.of(
            source = RecognitionSource.OCR,
            provider = "teste",
            suggestions = listOf(RecognitionSuggestion(field, value, 0.9)),
        )

    private fun item(
        exemplarId: String,
        variantId: String,
        model: String,
        manufacturer: String,
        status: ExemplarStatus = ExemplarStatus.OWNED,
        manufacturerCode: String? = null,
    ): SearchResultItem = SearchResultItem(
        exemplarId = exemplarId,
        variantId = variantId,
        internalCode = "HW-$exemplarId",
        modelName = model,
        manufacturerName = manufacturer,
        seriesName = null,
        collectionName = null,
        primaryColorName = null,
        decoration = null,
        realVehicleName = null,
        manufacturerCode = manufacturerCode,
        scale = "1:64",
        releaseYear = 2024,
        condition = Condition.SEALED,
        status = status,
        locationName = null,
        priceCents = null,
        acquisitionDate = null,
    )

    private class FakeSearch(
        private val idsByQuery: Map<String, List<String>> = emptyMap(),
        private val items: List<SearchResultItem> = emptyList(),
    ) : SearchRepository {

        val queries: MutableList<String> = mutableListOf()
        var lastFilters: SearchFilters? = null

        override fun observeResults(filters: SearchFilters): Flow<List<SearchResultItem>> {
            lastFilters = filters
            return flowOf(items.filter { it.variantId in filters.variantIds })
        }

        override fun observeOptions(): Flow<SearchFilterOptions> = flowOf(SearchFilterOptions())

        override suspend fun variantIds(query: String, limit: Int): List<String> {
            queries += query
            return idsByQuery[query].orEmpty().take(limit)
        }

        override suspend fun rebuild(): Result<Unit> = Result.success(Unit)
    }
}
