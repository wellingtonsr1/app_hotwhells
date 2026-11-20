package br.com.hotwheelscollector.data.repository

import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.entity.RealVehicleEntity
import br.com.hotwheelscollector.data.mapper.toDomain
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.RealVehicle
import br.com.hotwheelscollector.domain.repository.RealVehicleRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Veículo real (FR-014): o carro do mundo que a miniatura representa.
 *
 * Existe para o modelo poder apontar para ele — é catálogo de apoio, não uma exigência do
 * cadastro: o colecionador pode salvar a miniatura sem saber qual carro ela retrata.
 */
@Singleton
public class RealVehicleRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
) : RealVehicleRepository {

    override fun observeAll(): Flow<List<RealVehicle>> =
        database.realVehicleDao().observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(
        make: String,
        model: String,
        year: Int?,
        country: String?,
    ): DomainResult<RealVehicle> {
        val trimmedMake = make.trim()
        val trimmedModel = model.trim()
        if (trimmedMake.isEmpty() || trimmedModel.isEmpty()) {
            return Result.failure(
                DomainError.Validation("Marca e modelo do veículo real são obrigatórios."),
            )
        }
        if (year != null && year !in FIRST_CAR_YEAR..CURRENT_YEAR_LIMIT) {
            return Result.failure(DomainError.Validation("Ano do veículo real inválido."))
        }

        val entity = RealVehicleEntity(
            id = UUID.randomUUID().toString(),
            make = trimmedMake,
            model = trimmedModel,
            year = year,
            country = country?.trim()?.takeIf { it.isNotEmpty() },
        )
        return try {
            database.realVehicleDao().insert(entity)
            Result.success(entity.toDomain())
        } catch (error: Throwable) {
            Result.failure(DomainError.Storage(error.message ?: "Falha ao salvar veículo real."))
        }
    }

    private companion object {
        /** Primeiro automóvel fabricado; qualquer coisa antes disso é erro de digitação. */
        const val FIRST_CAR_YEAR = 1886
        const val CURRENT_YEAR_LIMIT = 2100
    }
}
