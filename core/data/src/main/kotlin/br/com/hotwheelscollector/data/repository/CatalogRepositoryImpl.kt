package br.com.hotwheelscollector.data.repository

import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.dao.IncompleteModelRow
import br.com.hotwheelscollector.data.db.dao.IncompleteVariantRow
import br.com.hotwheelscollector.data.db.entity.CarModelEntity
import br.com.hotwheelscollector.data.db.entity.ManufacturerEntity
import br.com.hotwheelscollector.data.db.entity.SeriesEntity
import br.com.hotwheelscollector.data.mapper.toDomain
import br.com.hotwheelscollector.data.mapper.toEntity
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.CarModel
import br.com.hotwheelscollector.domain.model.ColorItem
import br.com.hotwheelscollector.domain.model.IncompleteEntry
import br.com.hotwheelscollector.domain.model.Manufacturer
import br.com.hotwheelscollector.domain.model.MissingField
import br.com.hotwheelscollector.domain.model.Series
import br.com.hotwheelscollector.domain.repository.CarModelRepository
import br.com.hotwheelscollector.domain.repository.ColorRepository
import br.com.hotwheelscollector.domain.repository.ManufacturerRepository
import br.com.hotwheelscollector.domain.repository.SeriesRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Fabricantes, com reaproveitamento por nome para não duplicar a Mattel. */
@Singleton
public class ManufacturerRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val clock: Clock,
) : ManufacturerRepository {

    override fun observeActive(): Flow<List<Manufacturer>> =
        database.manufacturerDao().observeActive().map { rows -> rows.map { it.toDomain() } }

    override suspend fun findById(id: String): Manufacturer? =
        database.manufacturerDao().findById(id)?.toDomain()

    override suspend fun findByName(name: String): Manufacturer? =
        database.manufacturerDao().findByName(name.trim())?.toDomain()

    override suspend fun add(name: String, country: String?): DomainResult<Manufacturer> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(DomainError.Validation("Nome do fabricante é obrigatório."))
        }
        val now = Instant.now(clock)
        val entity = ManufacturerEntity(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            country = country?.trim()?.takeIf { it.isNotEmpty() },
            archivedAt = null,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )
        return try {
            database.manufacturerDao().insert(entity)
            Result.success(entity.toDomain())
        } catch (error: Throwable) {
            Result.failure(DomainError.Storage(error.message ?: "Falha ao salvar fabricante."))
        }
    }

    override suspend fun rename(id: String, name: String): DomainResult<Unit> {
        val current = database.manufacturerDao().findById(id)
            ?: return Result.failure(DomainError.NotFound)
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(DomainError.Validation("Nome do fabricante é obrigatório."))
        }
        return try {
            database.manufacturerDao().update(
                current.copy(name = trimmed, updatedAt = Instant.now(clock).toEpochMilli()),
            )
            Result.success(Unit)
        } catch (error: Throwable) {
            Result.failure(DomainError.Storage(error.message ?: "Falha ao renomear fabricante."))
        }
    }

    override suspend fun archive(id: String): DomainResult<Unit> {
        database.manufacturerDao().archive(id, Instant.now(clock).toEpochMilli())
        return Result.success(Unit)
    }
}

/** Modelos; a identidade é (fabricante, nome, escala) — FR-035. */
@Singleton
public class CarModelRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val clock: Clock,
) : CarModelRepository {

    override fun observeActive(): Flow<List<CarModel>> =
        database.carModelDao().observeActive().map { rows -> rows.map { it.toDomain() } }

    override fun observeByManufacturer(manufacturerId: String): Flow<List<CarModel>> =
        database.carModelDao().observeByManufacturer(manufacturerId)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun findById(id: String): CarModel? =
        database.carModelDao().findById(id)?.toDomain()

    override suspend fun findByIdentity(
        manufacturerId: String,
        name: String,
        scale: String,
    ): CarModel? = database.carModelDao()
        .findByIdentity(manufacturerId, name.trim(), scale.trim())
        ?.toDomain()

    override suspend fun add(
        manufacturerId: String,
        name: String,
        scale: String,
        brandId: String?,
        realVehicleId: String?,
    ): DomainResult<CarModel> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(DomainError.Validation("Nome do modelo é obrigatório."))
        }
        if (!SCALE_PATTERN.matches(scale.trim())) {
            return Result.failure(DomainError.Validation("Escala deve estar no formato 1:N."))
        }
        val now = Instant.now(clock)
        val entity = CarModelEntity(
            id = UUID.randomUUID().toString(),
            manufacturerId = manufacturerId,
            name = trimmed,
            scale = scale.trim(),
            brandId = brandId,
            realVehicleId = realVehicleId,
            bodyType = null,
            material = null,
            countryOfManufacture = null,
            notes = null,
            archivedAt = null,
            createdAt = now.toEpochMilli(),
            updatedAt = now.toEpochMilli(),
        )
        return try {
            database.carModelDao().insert(entity)
            Result.success(entity.toDomain())
        } catch (error: Throwable) {
            Result.failure(DomainError.Storage(error.message ?: "Falha ao salvar modelo."))
        }
    }

    override suspend fun update(
        id: String,
        name: String,
        scale: String,
        bodyType: String?,
        material: String?,
        countryOfManufacture: String?,
        notes: String?,
        realVehicleId: String?,
    ): DomainResult<CarModel> {
        val current = database.carModelDao().findById(id)
            ?: return Result.failure(DomainError.NotFound)
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(DomainError.Validation("Nome do modelo é obrigatório."))
        }
        val normalizedScale = scale.trim()
        if (!SCALE_PATTERN.matches(normalizedScale)) {
            return Result.failure(DomainError.Validation("Escala deve estar no formato 1:N."))
        }
        val duplicate = database.carModelDao()
            .findByIdentity(current.manufacturerId, trimmed, normalizedScale)
        if (duplicate != null && duplicate.id != id) {
            return Result.failure(
                DomainError.Validation("Já existe um modelo com esse nome e escala."),
            )
        }

        val updated = current.copy(
            name = trimmed,
            scale = normalizedScale,
            bodyType = bodyType?.trim()?.takeIf { it.isNotEmpty() },
            material = material?.trim()?.takeIf { it.isNotEmpty() },
            countryOfManufacture = countryOfManufacture?.trim()?.takeIf { it.isNotEmpty() },
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            realVehicleId = realVehicleId,
            updatedAt = Instant.now(clock).toEpochMilli(),
        )
        return try {
            database.carModelDao().update(updated)
            Result.success(updated.toDomain())
        } catch (error: Throwable) {
            Result.failure(DomainError.Storage(error.message ?: "Falha ao salvar modelo."))
        }
    }

    override suspend fun archive(id: String): DomainResult<Unit> {
        database.carModelDao().archive(id, Instant.now(clock).toEpochMilli())
        return Result.success(Unit)
    }

    override fun observeIncomplete(): Flow<List<IncompleteEntry>> = combine(
        database.carModelDao().observeWithoutVariant(),
        database.variantDao().observeIncompleteVariants(),
    ) { models, variants ->
        models.map { it.toDomain() } + variants.map { it.toDomain() }
    }

    private fun IncompleteModelRow.toDomain(): IncompleteEntry = IncompleteEntry(
        modelId = modelId,
        modelName = modelName,
        manufacturerName = manufacturerName,
        scale = scale,
    )

    private fun IncompleteVariantRow.toDomain(): IncompleteEntry = IncompleteEntry(
        modelId = modelId,
        modelName = modelName,
        manufacturerName = manufacturerName,
        scale = scale,
        variantId = variantId,
        missingFields = buildList {
            if (missingPrimaryColor) add(MissingField.PRIMARY_COLOR)
            if (missingReleaseYear) add(MissingField.RELEASE_YEAR)
        },
    )

    private companion object {
        val SCALE_PATTERN = Regex("""^1:[0-9]{1,3}$""")
    }
}

/**
 * Cores do catálogo (FR-017): o nome é o dado primário e é único, então cadastrar a
 * mesma cor duas vezes reaproveita o registro em vez de criar sentinela duplicada.
 */
@Singleton
public class ColorRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
) : ColorRepository {

    override fun observeAll(): Flow<List<ColorItem>> =
        database.colorDao().observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun findOrCreate(name: String, hex: String?): DomainResult<ColorItem> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(DomainError.Validation("Informe o nome da cor."))
        }
        database.colorDao().findByName(trimmed)?.let { return Result.success(it.toDomain()) }

        val item = ColorItem(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            hex = hex?.trim()?.takeIf { it.isNotEmpty() },
        )
        return try {
            database.colorDao().insert(item.toEntity())
            Result.success(item)
        } catch (error: Throwable) {
            Result.failure(DomainError.Storage(error.message ?: "Falha ao salvar cor."))
        }
    }
}

/**
 * Séries declaradas pelo fabricante (FR-048–FR-049). A hierarquia é livre: `parentId`
 * só é usado quando quem chama sabe a série superior, nunca presumida aqui.
 */
@Singleton
public class SeriesRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val clock: Clock,
) : SeriesRepository {

    override fun observeByManufacturer(manufacturerId: String): Flow<List<Series>> =
        database.seriesDao().observeByManufacturer(manufacturerId)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun findById(id: String): Series? =
        database.seriesDao().findById(id)?.toDomain()

    override suspend fun rename(id: String, name: String): DomainResult<Unit> {
        val current = database.seriesDao().findById(id)
            ?: return Result.failure(DomainError.NotFound)
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(DomainError.Validation("Informe o nome da série."))
        }
        return try {
            database.seriesDao().update(
                current.copy(name = trimmed, updatedAt = Instant.now(clock).toEpochMilli()),
            )
            Result.success(Unit)
        } catch (error: Throwable) {
            Result.failure(DomainError.Storage(error.message ?: "Falha ao renomear série."))
        }
    }

    override suspend fun archive(id: String): DomainResult<Unit> {
        database.seriesDao().findById(id)
            ?: return Result.failure(DomainError.NotFound)
        return try {
            database.seriesDao().archive(id, Instant.now(clock).toEpochMilli())
            Result.success(Unit)
        } catch (error: Throwable) {
            Result.failure(DomainError.Storage(error.message ?: "Falha ao arquivar série."))
        }
    }

    override suspend fun add(
        manufacturerId: String,
        name: String,
        parentId: String?,
    ): DomainResult<Series> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(DomainError.Validation("Informe o nome da série."))
        }
        val now = Instant.now(clock).toEpochMilli()
        val entity = SeriesEntity(
            id = UUID.randomUUID().toString(),
            manufacturerId = manufacturerId,
            name = trimmed,
            parentId = parentId,
            kind = null,
            year = null,
            archivedAt = null,
            createdAt = now,
            updatedAt = now,
        )
        return try {
            database.seriesDao().insert(entity)
            Result.success(entity.toDomain())
        } catch (error: Throwable) {
            Result.failure(DomainError.Storage(error.message ?: "Falha ao salvar série."))
        }
    }
}
