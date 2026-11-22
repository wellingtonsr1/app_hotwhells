package br.com.diecastcollector.domain.usecase

import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.repository.ExemplarRepository
import br.com.diecastcollector.domain.repository.VariantRepository

/**
 * Separa exemplares de uma variante para outra já existente do mesmo modelo (FR-042).
 *
 * Antes de mover qualquer coisa, confere que todos os exemplares pertencem à origem; o
 * vínculo muda sem tocar em aquisição, avaliações, fotos ou movimentações.
 */
public class SplitVariantUseCase(
    private val variants: VariantRepository,
    private val exemplars: ExemplarRepository,
) {

    public suspend fun split(
        sourceVariantId: String,
        targetVariantId: String,
        exemplarIds: List<String>,
    ): DomainResult<Unit> {
        if (exemplarIds.isEmpty()) {
            return DomainResult.failure(DomainError.Validation("Escolha ao menos um exemplar."))
        }
        if (sourceVariantId == targetVariantId) {
            return DomainResult.failure(
                DomainError.Validation("Escolha uma variante de destino diferente."),
            )
        }

        val source = variants.findById(sourceVariantId)
            ?: return DomainResult.failure(DomainError.Validation("Variante de origem não encontrada."))
        val target = variants.findById(targetVariantId)
            ?: return DomainResult.failure(DomainError.Validation("Variante de destino não encontrada."))
        if (source.modelId != target.modelId) {
            return DomainResult.failure(
                DomainError.Validation("Só é possível separar para uma variante do mesmo modelo."),
            )
        }
        if (target.archivedAt != null) {
            return DomainResult.failure(DomainError.Validation("Variante de destino está arquivada."))
        }

        val selected = exemplarIds.distinct().map { id ->
            val exemplar = exemplars.findById(id)
                ?: return DomainResult.failure(DomainError.Validation("Exemplar $id não encontrado."))
            if (exemplar.variantId != sourceVariantId) {
                return DomainResult.failure(
                    DomainError.Validation("O exemplar ${exemplar.internalCode} não pertence a esta variante."),
                )
            }
            exemplar
        }

        for (exemplar in selected) {
            val moved = exemplars.reassignVariant(exemplar.id, targetVariantId)
            if (moved.isFailure) {
                return DomainResult.failure(moved.exceptionOrNull()!!)
            }
        }
        return DomainResult.success(Unit)
    }
}
