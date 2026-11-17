package br.com.hotwheelscollector.data.repository

import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.entity.CarModelEntity
import br.com.hotwheelscollector.data.db.entity.ManufacturerEntity
import br.com.hotwheelscollector.data.mapper.toDomain
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.CarModel
import br.com.hotwheelscollector.domain.model.Manufacturer
import br.com.hotwheelscollector.domain.repository.CarModelRepository
import br.com.hotwheelscollector.domain.repository.ManufacturerRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
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

    override suspend fun archive(id: String): DomainResult<Unit> {
        database.carModelDao().archive(id, Instant.now(clock).toEpochMilli())
        return Result.success(Unit)
    }

    private companion object {
        val SCALE_PATTERN = Regex("""^1:[0-9]{1,3}$""")
    }
}
