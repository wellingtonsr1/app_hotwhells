package br.com.diecastcollector.domain.usecase

import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.Variant
import br.com.diecastcollector.domain.repository.VariantRepository
import br.com.diecastcollector.domain.validation.CatalogValidation
import java.time.Clock
import java.time.Instant

/**
 * Campos que a tela de edição controla. O que não está aqui (números de coleção e de
 * série, edição, tipo de embalagem) é preservado do registro atual, não apagado.
 */
public data class UpdateVariantInput(
    val seriesId: String? = null,
    val primaryColorId: String? = null,
    val secondaryColorIds: List<String> = emptyList(),
    val wheelsType: String? = null,
    val baseType: String? = null,
    val decoration: String? = null,
    val releaseYear: Int? = null,
    val manufacturerCode: String? = null,
)

/**
 * Completa ou corrige uma variante já cadastrada — o caminho para sair dos cadastros
 * incompletos (FR-029) sem tocar no banco à mão.
 *
 * O `fingerprint` é recalculado pela **mesma** função da criação (FR-035, FR-040): com
 * cor ou ano novos, a variante pode passar a coincidir com outra ativa, e nesse caso a
 * edição é recusada em vez de gravar duas variantes idênticas. `id`, `createdAt` e os
 * exemplares permanecem intactos (Art. I.4).
 */
public class UpdateVariantUseCase(
    private val variants: VariantRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    public suspend operator fun invoke(
        variantId: String,
        input: UpdateVariantInput,
    ): DomainResult<Variant> {
        val current = variants.findById(variantId)
            ?: return Result.failure(DomainError.NotFound)
        if (current.archivedAt != null) {
            return Result.failure(DomainError.Validation("Variante arquivada não pode ser editada."))
        }

        val validYear = CatalogValidation.validateYear(input.releaseYear)
            .getOrElse { return Result.failure(it) }

        val secondaryColorIds = input.secondaryColorIds
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val fingerprint = CreateVariantUseCase.calculateFingerprint(
            modelId = current.modelId,
            seriesId = input.seriesId,
            primaryColorId = input.primaryColorId,
            secondaryColorIds = secondaryColorIds,
            wheelsType = CatalogValidation.cleanOptionalString(input.wheelsType),
            baseType = CatalogValidation.cleanOptionalString(input.baseType),
            decoration = CatalogValidation.cleanOptionalString(input.decoration),
            releaseYear = validYear,
            edition = current.edition,
            manufacturerCode = CatalogValidation.cleanOptionalString(input.manufacturerCode),
        )

        val duplicate = variants.findActiveByFingerprint(fingerprint)
        if (duplicate != null && duplicate.id != current.id) {
            return Result.failure(DomainError.Duplicate)
        }

        val updated = current.copy(
            fingerprint = fingerprint,
            seriesId = input.seriesId,
            primaryColorId = input.primaryColorId,
            secondaryColorIds = secondaryColorIds,
            wheelsType = CatalogValidation.cleanOptionalString(input.wheelsType),
            baseType = CatalogValidation.cleanOptionalString(input.baseType),
            decoration = CatalogValidation.cleanOptionalString(input.decoration),
            releaseYear = validYear,
            manufacturerCode = CatalogValidation.cleanOptionalString(input.manufacturerCode),
            updatedAt = Instant.now(clock),
        )
        return variants.update(updated)
    }
}
