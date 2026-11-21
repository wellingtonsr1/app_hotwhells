package br.com.hotwheelscollector.domain.usecase

import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.Variant
import br.com.hotwheelscollector.domain.repository.ExemplarRepository
import br.com.hotwheelscollector.domain.repository.VariantRepository

/** Une duas variantes: move todos os exemplares da absorvida para a target (FR-041).
 * Não apaga nada - apenas reorienta todos os dados para a variante alvo. */
public class MergeVariantsUseCase(
    private val variants: VariantRepository,
    private val exemplars: ExemplarRepository,
) {

    public suspend fun merge(targetId: String, absorbedId: String): DomainResult<Unit> {
        // 1. Busca as duas variantes
        val target = variants.findById(targetId)
            ?: return DomainResult.failure(DomainError.Validation("Variante target não encontrada"))
        val absorbed = variants.findById(absorbedId)
            ?: return DomainResult.failure(DomainError.Validation("Variante absorvida não encontrada"))

        if (targetId == absorbedId) {
            return DomainResult.failure(DomainError.Validation("Não é possível unir uma variante consigo mesma"))
        }

        // 2. Move todos os exemplares da absorvida para a target
        val moveResult = exemplars.move(absorbedId, null, "Fundida para $targetId")
        if (moveResult.isFailure) {
            return DomainResult.failure(moveResult.exceptionOrNull()!!)
        }

        // 3. Arquiva a variante absorvida (define archivedAt)
        val archiveResult = variants.archive(absorbedId)
        if (archiveResult.isFailure) {
            return DomainResult.failure(archiveResult.exceptionOrNull()!!)
        }

        return DomainResult.success(Unit)
    }
}