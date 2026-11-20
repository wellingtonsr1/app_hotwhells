package br.com.hotwheelscollector.domain.repository

import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.Brand
import br.com.hotwheelscollector.domain.model.CarModel
import br.com.hotwheelscollector.domain.model.ColorItem
import br.com.hotwheelscollector.domain.model.IncompleteEntry
import br.com.hotwheelscollector.domain.model.Manufacturer
import br.com.hotwheelscollector.domain.model.RealVehicle
import br.com.hotwheelscollector.domain.model.Series
import br.com.hotwheelscollector.domain.model.Variant
import br.com.hotwheelscollector.domain.model.VariantSummary
import kotlinx.coroutines.flow.Flow

/** Catálogo de fabricantes (FR-047). */
public interface ManufacturerRepository {
    public fun observeActive(): Flow<List<Manufacturer>>
    public suspend fun findById(id: String): Manufacturer?
    public suspend fun findByName(name: String): Manufacturer?
    public suspend fun add(name: String, country: String?): DomainResult<Manufacturer>
    public suspend fun rename(id: String, name: String): DomainResult<Unit>
    public suspend fun archive(id: String): DomainResult<Unit>
}

public interface BrandRepository {
    public fun observeByManufacturer(manufacturerId: String): Flow<List<Brand>>
    public suspend fun add(manufacturerId: String, name: String): DomainResult<Brand>
}

public interface RealVehicleRepository {
    public fun observeAll(): Flow<List<RealVehicle>>
    public suspend fun add(make: String, model: String, year: Int?, country: String?): DomainResult<RealVehicle>
}

public interface ColorRepository {
    public fun observeAll(): Flow<List<ColorItem>>
    public suspend fun findOrCreate(name: String, hex: String? = null): DomainResult<ColorItem>
}

/** Modelo e escala (FR-033–FR-036). */
public interface CarModelRepository {
    public fun observeActive(): Flow<List<CarModel>>
    public fun observeByManufacturer(manufacturerId: String): Flow<List<CarModel>>
    public suspend fun findById(id: String): CarModel?

    /** Mesmo nome e mesma escala do mesmo fabricante = mesmo modelo. */
    public suspend fun findByIdentity(manufacturerId: String, name: String, scale: String): CarModel?

    public suspend fun add(
        manufacturerId: String,
        name: String,
        scale: String = "1:64",
        brandId: String? = null,
        realVehicleId: String? = null,
    ): DomainResult<CarModel>

    /**
     * Completa ou corrige o modelo (FR-014), inclusive o vínculo com o veículo real.
     * Recusa nome/escala que colidam com outro modelo do mesmo fabricante (FR-035).
     */
    public suspend fun update(
        id: String,
        name: String,
        scale: String,
        bodyType: String? = null,
        material: String? = null,
        countryOfManufacture: String? = null,
        notes: String? = null,
        realVehicleId: String? = null,
    ): DomainResult<CarModel>

    public suspend fun archive(id: String): DomainResult<Unit>

    /**
     * Cadastros incompletos (FR-029): modelo ativo sem variante ativa, e variante ativa sem
     * cor principal ou sem ano. Nada é armazenado: ao preencher o campo, a linha sai sozinha.
     */
    public fun observeIncomplete(): Flow<List<IncompleteEntry>>
}

public interface SeriesRepository {
    public fun observeByManufacturer(manufacturerId: String): Flow<List<Series>>
    public suspend fun findById(id: String): Series?
    public suspend fun add(manufacturerId: String, name: String, parentId: String? = null): DomainResult<Series>

    /** Renomeia a série sem tocar nas variantes que a apontam (FR-049). */
    public suspend fun rename(id: String, name: String): DomainResult<Unit>

    /** Arquiva a série; as variantes continuam existindo, só perdem o agrupamento. */
    public suspend fun archive(id: String): DomainResult<Unit>
}

/**
 * Variante: a unidade que o colecionador distingue (FR-039–FR-042).
 * `create` recusa fingerprint repetido entre variantes ativas.
 */
public interface VariantRepository {
    /** Lista de leitura: modelo, fabricante, cor, ano e contagem de exemplares. */
    public fun observeSummaries(): Flow<List<VariantSummary>>

    public fun observeByModel(modelId: String): Flow<List<Variant>>
    public suspend fun findById(id: String): Variant?
    public suspend fun findActiveByFingerprint(fingerprint: String): Variant?
    public suspend fun create(variant: Variant): DomainResult<Variant>

    /**
     * Completa ou corrige uma variante existente sem tocá-la em outra coisa (FR-029).
     *
     * O `id` e o `createdAt` são os do registro atual — os exemplares apontam para eles —
     * e a checagem de fingerprint continua sendo feita aqui, não na tela (Art. I.3).
     */
    public suspend fun update(variant: Variant): DomainResult<Variant>
    public suspend fun archive(id: String): DomainResult<Unit>
    public suspend fun mergeVariants(targetId: String, absorbedId: String): DomainResult<Unit>
}
