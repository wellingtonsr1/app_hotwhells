package br.com.hotwheelscollector.domain.usecase

import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.CarModel
import br.com.hotwheelscollector.domain.repository.CarModelRepository
import br.com.hotwheelscollector.domain.repository.ManufacturerRepository
import br.com.hotwheelscollector.domain.validation.CatalogValidation

/** Parâmetros de entrada para criação ou resolução de modelo (T041 - US2, FR-025, FR-033–FR-036). */
public data class CreateModelInput(
    val manufacturerId: String,
    val name: String,
    val scale: String = "1:64",
    val brandId: String? = null,
    val realVehicleId: String? = null,
    val forceCreate: Boolean = false,
)

/**
 * Resultado da criação do modelo: o modelo obtido e indicador de já existência.
 * Permite que a UI alerte o usuário e ofereça reutilizar o existente (FR-025).
 */
public data class CreateModelOutcome(
    val model: CarModel,
    val alreadyExisted: Boolean,
)

/**
 * Caso de uso para criar modelo com aviso de mesmo nome+fabricante (FR-025).
 */
public class CreateModelUseCase(
    private val manufacturers: ManufacturerRepository,
    private val models: CarModelRepository,
) {

    public suspend operator fun invoke(input: CreateModelInput): DomainResult<CreateModelOutcome> {
        val nameResult = CatalogValidation.validateRequiredName(input.name, "nome do modelo")
        val cleanName = nameResult.getOrElse { return Result.failure(it) }

        val scaleResult = CatalogValidation.validateScale(input.scale)
        val cleanScale = scaleResult.getOrElse { return Result.failure(it) }

        val manufacturer = manufacturers.findById(input.manufacturerId)
            ?: return Result.failure(DomainError.Validation("Fabricante não encontrado."))

        // FR-025: detecta modelo com mesmo nome e fabricante e escala
        val existing = models.findByIdentity(manufacturer.id, cleanName, cleanScale)
        if (existing != null && !input.forceCreate) {
            return Result.success(CreateModelOutcome(model = existing, alreadyExisted = true))
        }

        val created = models.add(
            manufacturerId = manufacturer.id,
            name = cleanName,
            scale = cleanScale,
            brandId = input.brandId,
            realVehicleId = input.realVehicleId,
        ).getOrElse { return Result.failure(it) }

        return Result.success(CreateModelOutcome(model = created, alreadyExisted = false))
    }
}
