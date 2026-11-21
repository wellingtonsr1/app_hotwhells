package br.com.hotwheelscollector.domain.usecase

import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.Exemplar
import br.com.hotwheelscollector.domain.model.Variant
import br.com.hotwheelscollector.domain.repository.ExemplarRepository
import br.com.hotwheelscollector.domain.repository.VariantRepository

/** Separa uma variante em duas: move exemplares selecionados para a nova variante,
 * preservando aquisições, avaliações, fotos e movimentações (FR-042).
 * A variante origem mantém os exemplares restantes. */
public class SplitVariantUseCase(
    private val variants: VariantRepository,
    private val exemplars: ExemplarRepository,
) {

    public suspend fun split(
        sourceVariantId: String,
        targetVariantId: String,
        exemplarIds: List<String>,
    ): DomainResult<Unit> {
        // 1. Busca a variante fonte
        val source = variants.findById(sourceVariantId)
            ?: return DomainResult.failure(DomainError.Validation("Variante fonte não encontrada"))

        // 2. Busca ou cria a variante target
        val target = variants.findById(targetVariantId)
            ?: return DomainResult.failure(DomainError.Validation("Variante target não encontrada"))

        if (sourceVariantId == targetVariantId) {
            return DomainResult.failure(DomainError.Validation("Não é possível dividir uma variante consigo mesma"))
        }

        // 3. Move os exemplares selecionados da fonte para a target
        for (exemplarId in exemplarIds) {
            // Verifica se o exemplar pertence à variante fonte
            val exemplar = exemplars.findById(exemplarId)
                ?: return DomainResult.failure(DomainError.Validation("Exemplar $exemplarId não encontrado"))

            if (exemplar.variantId != sourceVariantId) {
                return DomainResult.failure(
                    DomainError.Validation("Exemplar $exemplarId não pertence à variante fonte")
                )
            }

            val moveResult = exemplars.move(exemplarId, targetVariantId, "Dividida para nova variante")
            if (moveResult.isFailure) {
                return DomainResult.failure(moveResult.exceptionOrNull()!!)
            }
        }

        // 4. Fim da divisão - variante fonte permanece com seus exemplares não movidos
        return DomainResult.success(Unit)
    }
}