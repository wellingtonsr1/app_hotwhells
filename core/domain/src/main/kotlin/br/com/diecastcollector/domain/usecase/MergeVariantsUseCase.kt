package br.com.diecastcollector.domain.usecase

import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.repository.VariantRepository

/**
 * Une duas variantes do mesmo modelo (FR-041): os exemplares da absorvida passam para a
 * de destino e a origem é arquivada, sem apagar nada.
 *
 * A escrita fica no repositório, numa transação só — mover exemplares, arquivar a origem
 * e registrar a decisão acontecem juntos ou não acontecem.
 */
public class MergeVariantsUseCase(
    private val variants: VariantRepository,
) {

    public suspend fun merge(targetId: String, absorbedId: String): DomainResult<Unit> {
        val target = variants.findById(targetId)
            ?: return DomainResult.failure(DomainError.Validation("Variante de destino não encontrada."))
        val absorbed = variants.findById(absorbedId)
            ?: return DomainResult.failure(DomainError.Validation("Variante a unir não encontrada."))

        if (targetId == absorbedId) {
            return DomainResult.failure(
                DomainError.Validation("Escolha duas variantes diferentes."),
            )
        }
        if (target.modelId != absorbed.modelId) {
            return DomainResult.failure(
                DomainError.Validation("Só é possível unir variantes do mesmo modelo."),
            )
        }
        if (target.archivedAt != null || absorbed.archivedAt != null) {
            return DomainResult.failure(DomainError.Validation("Variante arquivada não pode ser unida."))
        }

        return variants.mergeVariants(targetId = targetId, absorbedId = absorbedId)
    }
}
