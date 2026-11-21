package br.com.hotwheelscollector.domain.usecase

import br.com.hotwheelscollector.domain.model.RecognitionSource
import br.com.hotwheelscollector.domain.model.SearchFilters
import br.com.hotwheelscollector.domain.model.SearchResultItem
import br.com.hotwheelscollector.domain.recognition.RecognitionField
import br.com.hotwheelscollector.domain.recognition.RecognitionProposal
import br.com.hotwheelscollector.domain.repository.SearchRepository
import java.text.Normalizer
import kotlinx.coroutines.flow.first

/**
 * Uma correspondência do catálogo local, com a confiança calculada e a origem (FR-103/
 * FR-110). É a variante que importa — vários exemplares do mesmo carro são o mesmo item.
 */
public data class CatalogMatch(
    val variantId: String,
    val exemplarId: String,
    val modelName: String,
    val manufacturerName: String,
    val seriesName: String?,
    val releaseYear: Int?,
    val manufacturerCode: String?,
    val confidence: Double,
    val matchedFields: List<RecognitionField>,
    val source: RecognitionSource = RecognitionSource.CATALOG,
)

/**
 * Busca até cinco correspondências no catálogo local a partir de uma proposta (T126,
 * FR-103/FR-110).
 *
 * Cada valor sugerido consulta o índice textual; os candidatos são então pontuados pela
 * semelhança entre a sugestão e o campo correspondente do item. A ordem é por confiança
 * decrescente. A busca inclui exemplares fora do acervo ativo (`OWNED` é só o padrão da
 * tela): o código lido pode pertencer a um item vendido, e a tela ainda precisa avisar.
 *
 * Não grava nada: devolver a lista é o fim do caminho até o aceite do usuário (FR-104).
 */
public class FindCatalogMatchesUseCase(
    private val searchRepository: SearchRepository,
) {

    public suspend operator fun invoke(
        proposal: RecognitionProposal,
        limit: Int = MAX_MATCHES,
    ): List<CatalogMatch> {
        if (proposal.suggestions.isEmpty()) return emptyList()

        val variantIds = LinkedHashSet<String>()
        for (suggestion in proposal.suggestions) {
            if (variantIds.size >= MAX_CANDIDATES) break
            val found = searchRepository.variantIds(
                query = suggestion.value,
                limit = CANDIDATES_PER_QUERY,
            )
            found.forEach { variantIds += it }
        }
        if (variantIds.isEmpty()) return emptyList()

        val items = searchRepository.observeResults(
            SearchFilters(variantIds = variantIds, statuses = emptySet()),
        ).first()

        return items
            .distinctBy { it.variantId }
            .mapNotNull { item -> score(item, proposal) }
            .filter { it.confidence > 0.0 }
            .sortedWith(
                compareByDescending<CatalogMatch> { it.confidence }
                    .thenBy { it.modelName.lowercase() },
            )
            .take(limit.coerceIn(1, MAX_MATCHES))
    }

    private fun score(item: SearchResultItem, proposal: RecognitionProposal): CatalogMatch? {
        val matched = proposal.suggestions.mapNotNull { suggestion ->
            val similarity = similarity(suggestion.value, targetOf(item, suggestion.field))
                ?: return@mapNotNull null
            suggestion to similarity
        }
        if (matched.isEmpty()) return null

        // Divide por todas as sugestões: quem casa mais campos (e com mais confiança) sobe.
        val confidence = (matched.sumOf { (suggestion, similarity) ->
            suggestion.confidence * similarity
        } / proposal.suggestions.size).coerceIn(0.0, 1.0)

        return CatalogMatch(
            variantId = item.variantId,
            exemplarId = item.exemplarId,
            modelName = item.modelName,
            manufacturerName = item.manufacturerName,
            seriesName = item.seriesName,
            releaseYear = item.releaseYear,
            manufacturerCode = item.manufacturerCode,
            confidence = confidence,
            matchedFields = matched.map { it.first.field }.distinct(),
        )
    }

    private fun targetOf(item: SearchResultItem, field: RecognitionField): String? = when (field) {
        RecognitionField.MANUFACTURER -> item.manufacturerName
        RecognitionField.MODEL -> item.modelName
        RecognitionField.SERIES -> item.seriesName
        RecognitionField.YEAR -> item.releaseYear?.toString()
        RecognitionField.VARIANT -> item.decoration
        RecognitionField.CODE -> item.manufacturerCode
    }

    /** Semelhança em 0..1; `null` quando os textos não têm nada em comum. */
    private fun similarity(suggested: String, candidate: String?): Double? {
        if (candidate.isNullOrBlank()) return null
        val left = normalize(suggested)
        val right = normalize(candidate)
        if (left.isEmpty() || right.isEmpty()) return null
        if (left == right) return 1.0
        if (left in right || right in left) return 0.8

        val leftTokens = left.split(' ').filter { it.isNotEmpty() }.toSet()
        val rightTokens = right.split(' ').filter { it.isNotEmpty() }.toSet()
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return null
        val overlap = leftTokens.intersect(rightTokens).size.toDouble() /
            leftTokens.union(rightTokens).size
        return if (overlap >= MIN_TOKEN_OVERLAP) 0.5 + 0.3 * overlap else null
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    private companion object {
        const val MAX_MATCHES = 5
        const val MAX_CANDIDATES = 50
        const val CANDIDATES_PER_QUERY = 20
        const val MIN_TOKEN_OVERLAP = 0.34
    }
}
