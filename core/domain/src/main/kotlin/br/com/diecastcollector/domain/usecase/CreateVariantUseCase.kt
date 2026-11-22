package br.com.diecastcollector.domain.usecase

import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.Variant
import br.com.diecastcollector.domain.repository.CarModelRepository
import br.com.diecastcollector.domain.repository.VariantRepository
import br.com.diecastcollector.domain.validation.CatalogValidation
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.Locale
import java.util.UUID

/** Parâmetros de entrada para criar ou resolver uma variante (T042 - US2, FR-035, FR-039, FR-040). */
public data class CreateVariantInput(
    val modelId: String,
    val seriesId: String? = null,
    val primaryColorId: String? = null,
    val secondaryColorIds: List<String> = emptyList(),
    val wheelsType: String? = null,
    val baseType: String? = null,
    val decoration: String? = null,
    val releaseYear: Int? = null,
    val collectionNumber: String? = null,
    val seriesNumber: String? = null,
    val edition: String? = null,
    val manufacturerCode: String? = null,
    val packagingType: String? = null,
)

/**
 * Resultado da criação de variante: a variante obtida e indicador de já existência (FR-040).
 */
public data class CreateVariantOutcome(
    val variant: Variant,
    val alreadyExisted: Boolean,
)

/**
 * Caso de uso para criar variante com cálculo de fingerprint estável e aviso de variante idêntica (FR-040).
 */
public class CreateVariantUseCase(
    private val models: CarModelRepository,
    private val variants: VariantRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    public suspend operator fun invoke(input: CreateVariantInput): DomainResult<CreateVariantOutcome> {
        val model = models.findById(input.modelId)
            ?: return Result.failure(DomainError.Validation("Modelo não encontrado."))

        val yearValidation = CatalogValidation.validateYear(input.releaseYear)
        val validYear = yearValidation.getOrElse { return Result.failure(it) }

        val cleanWheels = CatalogValidation.cleanOptionalString(input.wheelsType)
        val cleanBase = CatalogValidation.cleanOptionalString(input.baseType)
        val cleanDeco = CatalogValidation.cleanOptionalString(input.decoration)
        val cleanEdition = CatalogValidation.cleanOptionalString(input.edition)
        val cleanCode = CatalogValidation.cleanOptionalString(input.manufacturerCode)
        val cleanCollectionNum = CatalogValidation.cleanOptionalString(input.collectionNumber)
        val cleanSeriesNum = CatalogValidation.cleanOptionalString(input.seriesNumber)
        val cleanPackaging = CatalogValidation.cleanOptionalString(input.packagingType)

        val fingerprint = calculateFingerprint(
            modelId = model.id,
            seriesId = input.seriesId,
            primaryColorId = input.primaryColorId,
            secondaryColorIds = input.secondaryColorIds,
            wheelsType = cleanWheels,
            baseType = cleanBase,
            decoration = cleanDeco,
            releaseYear = validYear,
            edition = cleanEdition,
            manufacturerCode = cleanCode,
        )

        // FR-040: se variante ativa idêntica já existe, avisa e oferece reutilizar
        val existing = variants.findActiveByFingerprint(fingerprint)
        if (existing != null) {
            return Result.success(CreateVariantOutcome(variant = existing, alreadyExisted = true))
        }

        val now = Instant.now(clock)
        val newVariant = Variant(
            id = UUID.randomUUID().toString(),
            modelId = model.id,
            fingerprint = fingerprint,
            seriesId = input.seriesId,
            primaryColorId = input.primaryColorId,
            secondaryColorIds = input.secondaryColorIds,
            wheelsType = cleanWheels,
            baseType = cleanBase,
            decoration = cleanDeco,
            releaseYear = validYear,
            collectionNumber = cleanCollectionNum,
            seriesNumber = cleanSeriesNum,
            edition = cleanEdition,
            manufacturerCode = cleanCode,
            packagingType = cleanPackaging,
            archivedAt = null,
            createdAt = now,
            updatedAt = now,
        )

        val created = variants.create(newVariant).getOrElse { return Result.failure(it) }
        return Result.success(CreateVariantOutcome(variant = created, alreadyExisted = false))
    }

    public companion object {
        /**
         * Hash SHA-256 estável dos atributos que distinguem variantes (FR-035, FR-040).
         * Texto normalizado (minúsculas, sem espaços) para evitar duplicatas por formatação.
         */
        public fun calculateFingerprint(
            modelId: String,
            seriesId: String?,
            primaryColorId: String?,
            secondaryColorIds: List<String>,
            wheelsType: String?,
            baseType: String?,
            decoration: String?,
            releaseYear: Int?,
            edition: String?,
            manufacturerCode: String?,
        ): String {
            val sortedSecondary = secondaryColorIds.map { it.trim().lowercase(Locale.ROOT) }.sorted()
            val normalized = listOf(
                modelId.trim().lowercase(Locale.ROOT),
                seriesId?.trim()?.lowercase(Locale.ROOT).orEmpty(),
                primaryColorId?.trim()?.lowercase(Locale.ROOT).orEmpty(),
                sortedSecondary.joinToString(","),
                wheelsType?.trim()?.lowercase(Locale.ROOT).orEmpty(),
                baseType?.trim()?.lowercase(Locale.ROOT).orEmpty(),
                decoration?.trim()?.lowercase(Locale.ROOT).orEmpty(),
                releaseYear?.toString().orEmpty(),
                edition?.trim()?.lowercase(Locale.ROOT).orEmpty(),
                manufacturerCode?.trim()?.lowercase(Locale.ROOT).orEmpty(),
            ).joinToString("|")

            val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
