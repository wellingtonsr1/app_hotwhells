package br.com.hotwheelscollector.domain.usecase

import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.CarModel
import br.com.hotwheelscollector.domain.model.Manufacturer
import br.com.hotwheelscollector.domain.model.Variant
import br.com.hotwheelscollector.domain.repository.CarModelRepository
import br.com.hotwheelscollector.domain.repository.ManufacturerRepository
import br.com.hotwheelscollector.domain.repository.VariantRepository
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.Locale
import java.util.UUID

/** O mínimo para a primeira miniatura existir: fabricante, modelo e variante. */
public data class NewCatalogEntry(
    val manufacturerName: String,
    val modelName: String,
    val scale: String = "1:64",
    val colorName: String? = null,
    val releaseYear: Int? = null,
    val manufacturerCode: String? = null,
)

/**
 * Cria fabricante/modelo/variante reaproveitando o que já existe.
 *
 * Sem isso não há variante nenhuma para receber exemplar, e a tela de US1 fica
 * inalcançável. A regra de unicidade de variante (FR-040) é do repositório, via
 * `fingerprint`; aqui só se calcula esse fingerprint de forma estável.
 */
public class CreateCatalogEntryUseCase(
    private val manufacturers: ManufacturerRepository,
    private val models: CarModelRepository,
    private val variants: VariantRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    public suspend operator fun invoke(input: NewCatalogEntry): DomainResult<Variant> {
        val manufacturerName = input.manufacturerName.trim()
        val modelName = input.modelName.trim()
        if (manufacturerName.isEmpty()) {
            return Result.failure(DomainError.Validation("Informe o fabricante."))
        }
        if (modelName.isEmpty()) {
            return Result.failure(DomainError.Validation("Informe o nome do modelo."))
        }
        if (!SCALE_PATTERN.matches(input.scale.trim())) {
            return Result.failure(DomainError.Validation("Escala deve estar no formato 1:N, como 1:64."))
        }

        val manufacturer = resolveManufacturer(manufacturerName)
            .getOrElse { return Result.failure(it) }
        val model = resolveModel(manufacturer, modelName, input.scale.trim())
            .getOrElse { return Result.failure(it) }

        val now = Instant.now(clock)
        val variant = Variant(
            id = UUID.randomUUID().toString(),
            modelId = model.id,
            fingerprint = fingerprint(
                modelId = model.id,
                colorName = input.colorName,
                releaseYear = input.releaseYear,
                manufacturerCode = input.manufacturerCode,
            ),
            releaseYear = input.releaseYear,
            manufacturerCode = input.manufacturerCode?.trim()?.takeIf { it.isNotEmpty() },
            createdAt = now,
            updatedAt = now,
        )
        return variants.create(variant)
    }

    private suspend fun resolveManufacturer(name: String): DomainResult<Manufacturer> =
        manufacturers.findByName(name)?.let { Result.success(it) }
            ?: manufacturers.add(name, country = null)

    private suspend fun resolveModel(
        manufacturer: Manufacturer,
        name: String,
        scale: String,
    ): DomainResult<CarModel> =
        models.findByIdentity(manufacturer.id, name, scale)?.let { Result.success(it) }
            ?: models.add(manufacturerId = manufacturer.id, name = name, scale = scale)

    /**
     * Hash estável dos atributos que diferenciam variantes: modelo, cor, ano e código do
     * fabricante. Minúsculas e sem espaços nas pontas, para "Vermelho" e "vermelho "
     * darem o mesmo fingerprint e a duplicata ser detectada.
     */
    private fun fingerprint(
        modelId: String,
        colorName: String?,
        releaseYear: Int?,
        manufacturerCode: String?,
    ): String {
        val normalized = listOf(
            modelId,
            colorName?.trim()?.lowercase(Locale.ROOT).orEmpty(),
            releaseYear?.toString().orEmpty(),
            manufacturerCode?.trim()?.lowercase(Locale.ROOT).orEmpty(),
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val SCALE_PATTERN = Regex("""^1:[0-9]{1,3}$""")
    }
}
