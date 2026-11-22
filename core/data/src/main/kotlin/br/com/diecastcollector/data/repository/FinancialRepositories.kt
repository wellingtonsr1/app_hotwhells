package br.com.diecastcollector.data.repository

import br.com.diecastcollector.data.db.AuditLogWriter
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.TransactionRunner
import br.com.diecastcollector.data.mapper.toDomain
import br.com.diecastcollector.data.mapper.toEntity
import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.Acquisition
import br.com.diecastcollector.domain.model.Valuation
import br.com.diecastcollector.domain.repository.AcquisitionRepository
import br.com.diecastcollector.domain.repository.ValuationRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Aquisição 1:1 com o exemplar (FR-074–FR-076).
 *
 * `save` é upsert por `exemplar_id`: a primeira gravação cria, as seguintes atualizam a
 * mesma linha. É o que permite corrigir preço/data digitados errado sem inventar uma
 * segunda aquisição para a mesma unidade física.
 */
@Singleton
public class AcquisitionRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val transactionRunner: TransactionRunner,
    private val auditLogWriter: AuditLogWriter,
    private val clock: Clock,
) : AcquisitionRepository {

    override suspend fun findByExemplar(exemplarId: String): Acquisition? =
        database.acquisitionDao().findByExemplar(exemplarId)?.toDomain()

    override suspend fun save(acquisition: Acquisition): DomainResult<Acquisition> = try {
        val stored = transactionRunner.inTransaction {
            val existing = database.acquisitionDao().findByExemplar(acquisition.exemplarId)
            val now = Instant.now(clock)
            val toWrite = acquisition.copy(
                // O id e a data de criação pertencem ao registro existente, não à tela.
                id = existing?.id ?: acquisition.id,
                createdAt = existing?.let { Instant.ofEpochMilli(it.createdAt) }
                    ?: acquisition.createdAt,
                updatedAt = now,
            ).toEntity()

            if (existing == null) {
                database.acquisitionDao().insert(toWrite)
            } else {
                database.acquisitionDao().update(toWrite)
            }
            auditLogWriter.record(
                entityType = "acquisition",
                entityId = toWrite.id,
                field = if (existing == null) "created" else "updated",
                oldValue = existing?.priceCents?.toString(),
                newValue = toWrite.priceCents?.toString(),
                source = "app",
                at = now,
            )
            database.acquisitionDao().findByExemplar(acquisition.exemplarId)
                ?: error("Aquisição sumiu após gravação.")
        }
        Result.success(stored.toDomain())
    } catch (error: Throwable) {
        Result.failure(error.toDomainError("Falha ao salvar a aquisição."))
    }

    override fun observeTotalInvested(): Flow<Long> =
        database.acquisitionDao().observeTotalInvestedCents()
}

/**
 * Avaliações append-only (FR-082–FR-084): cada registro é um retrato do valor na data.
 * O valor atual é sempre a avaliação mais recente; o histórico não é sobrescrito.
 */
@Singleton
public class ValuationRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val auditLogWriter: AuditLogWriter,
    private val clock: Clock,
) : ValuationRepository {

    override fun observeByExemplar(exemplarId: String): Flow<List<Valuation>> =
        database.valuationDao().observeByExemplar(exemplarId)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(valuation: Valuation): DomainResult<Valuation> = try {
        database.valuationDao().insert(valuation.toEntity())
        auditLogWriter.record(
            entityType = "valuation",
            entityId = valuation.id,
            field = "created",
            oldValue = null,
            newValue = valuation.value.formatPtBr(),
            source = "app",
            at = Instant.now(clock),
        )
        Result.success(valuation)
    } catch (error: Throwable) {
        Result.failure(error.toDomainError("Falha ao registrar a avaliação."))
    }

    override fun observeCurrentEstimated(): Flow<Long> =
        database.valuationDao().observeCurrentEstimatedCents()
}

private fun Throwable.toDomainError(fallback: String): Throwable =
    if (this is DomainError) this else DomainError.Storage(message ?: fallback)
