package br.com.hotwheelscollector.domain.usecase

import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.CarModel
import br.com.hotwheelscollector.domain.model.Manufacturer
import br.com.hotwheelscollector.domain.repository.CarModelRepository
import br.com.hotwheelscollector.domain.repository.ManufacturerRepository

/** O que a primeira tela da miniatura precisa saber: fabricante, modelo e escala. */
public data class NewCatalogEntry(
    val manufacturerName: String,
    val modelName: String,
    val scale: String = "1:64",
)

/**
 * Cria (ou reaproveita) fabricante e modelo a partir do que o usuário digitou.
 *
 * A variante **não** nasce aqui: ela é o que distingue uma miniatura de outra (cor, rodas,
 * base, decoração) e precisa dos campos que a tela de variante coleta — inclusive das
 * fotos, que pertencem ao exemplar criado junto com a variante (FR-020). Manter as duas
 * criações no mesmo passo fazia o cadastro mínimo produzir uma variante que ninguém
 * conseguia completar sem duplicar o modelo.
 *
 * A identidade do modelo é (fabricante, nome, escala), então digitar o mesmo modelo duas
 * vezes reaproveita o registro — FR-035.
 */
public class CreateCatalogEntryUseCase(
    private val manufacturers: ManufacturerRepository,
    private val models: CarModelRepository,
) {

    public suspend operator fun invoke(input: NewCatalogEntry): DomainResult<CarModel> {
        val manufacturerName = input.manufacturerName.trim()
        val modelName = input.modelName.trim()
        if (manufacturerName.isEmpty()) {
            return Result.failure(DomainError.Validation("Informe o fabricante."))
        }
        if (modelName.isEmpty()) {
            return Result.failure(DomainError.Validation("Informe o nome do modelo."))
        }
        val scale = input.scale.trim()
        if (!SCALE_PATTERN.matches(scale)) {
            return Result.failure(DomainError.Validation("Escala deve estar no formato 1:N, como 1:64."))
        }

        val manufacturer = resolveManufacturer(manufacturerName)
            .getOrElse { return Result.failure(it) }
        return resolveModel(manufacturer, modelName, scale)
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

    private companion object {
        val SCALE_PATTERN = Regex("""^1:[0-9]{1,3}$""")
    }
}
