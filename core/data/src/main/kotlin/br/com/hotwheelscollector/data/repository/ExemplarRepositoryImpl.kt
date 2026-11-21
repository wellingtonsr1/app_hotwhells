package br.com.hotwheelscollector.data.repository

import android.database.sqlite.SQLiteConstraintException
import br.com.hotwheelscollector.data.db.AuditLogWriter
import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.TransactionRunner
import br.com.hotwheelscollector.data.db.entity.AcquisitionEntity
import br.com.hotwheelscollector.data.db.entity.ExemplarEntity
import br.com.hotwheelscollector.data.db.entity.MovementEntity
import br.com.hotwheelscollector.data.mapper.toDomain
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.Exemplar
import br.com.hotwheelscollector.domain.model.ExemplarStatus
import br.com.hotwheelscollector.domain.model.PackagingCondition
import br.com.hotwheelscollector.domain.repository.AddExemplarOutcome
import br.com.hotwheelscollector.domain.repository.ExemplarDependents
import br.com.hotwheelscollector.domain.repository.ExemplarRepository
import br.com.hotwheelscollector.domain.repository.NewAcquisition
import br.com.hotwheelscollector.domain.repository.NewExemplar
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cadastro de exemplar em **uma transação** (regra 1 de `data-model.md`).
 *
 * O que a transação garante: ou nascem exemplar + aquisição + movimentação inicial +
 * trilha de auditoria, ou não nasce nada. Um cadastro com preço e sem exemplar (ou o
 * contrário) é impossível por construção.
 *
 * O código interno é gerado aqui, e não na tela, porque é aqui que existe acesso ao
 * banco para garantir unicidade (FR-038). A coluna tem índice único; se outra escrita
 * ganhar a corrida, o erro vira `Duplicate` em vez de estourar como SQL cru.
 */
@Singleton
public class ExemplarRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val transactionRunner: TransactionRunner,
    private val auditLogWriter: AuditLogWriter,
    private val clock: Clock,
) : ExemplarRepository {

    override fun observeOwned(): Flow<List<Exemplar>> =
        database.exemplarDao().observeOwned().map { rows -> rows.map { it.toDomain() } }

    override fun observeByVariant(variantId: String): Flow<List<Exemplar>> =
        database.exemplarDao().observeByVariant(variantId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun findById(id: String): Exemplar? =
        database.exemplarDao().findById(id)?.toDomain()

    override suspend fun add(draft: NewExemplar): DomainResult<AddExemplarOutcome> = try {
        Result.success(insertInTransaction(draft))
    } catch (error: Throwable) {
        Result.failure(mapError(error))
    }

    private suspend fun insertInTransaction(draft: NewExemplar): AddExemplarOutcome =
        transactionRunner.inTransaction {
            val now = Instant.now(clock)
            val millis = now.toEpochMilli()
            val exemplarDao = database.exemplarDao()
            val exemplarId = UUID.randomUUID().toString()

            exemplarDao.insert(
                ExemplarEntity(
                    id = exemplarId,
                    variantId = draft.variantId,
                    internalCode = nextInternalCode(),
                    condition = draft.condition.name,
                    packagingCondition = draft.packagingCondition?.name,
                    locationId = draft.locationId,
                    status = "OWNED",
                    notes = draft.notes,
                    rowVersion = 0L,
                    createdAt = millis,
                    updatedAt = millis,
                ),
            )

            // Movimentação inicial: mesmo já estando no lugar, o histórico começa aqui.
            if (draft.locationId != null) {
                database.movementDao().insert(
                    MovementEntity(
                        id = UUID.randomUUID().toString(),
                        exemplarId = exemplarId,
                        at = millis,
                        fromLocationId = null,
                        toLocationId = draft.locationId,
                        note = "Cadastro inicial",
                    ),
                )
            }

            draft.acquisition?.let { acquisition ->
                database.acquisitionDao().insert(acquisition.toEntity(exemplarId, millis))
            }

            auditLogWriter.record(
                entityType = ENTITY_TYPE,
                entityId = exemplarId,
                field = "created",
                oldValue = null,
                newValue = "exemplar da variante ${draft.variantId}",
                source = "app",
                at = now,
            )

            val stored = exemplarDao.findById(exemplarId) ?: error("Exemplar sumiu após inserção.")
            AddExemplarOutcome(
                exemplar = stored.toDomain(),
                sameVariantCount = exemplarDao.countByVariant(draft.variantId),
            )
        }

    override suspend fun updateDetails(
        id: String,
        condition: Condition,
        packagingCondition: PackagingCondition?,
        notes: String?,
        expectedRowVersion: Long,
    ): DomainResult<Exemplar> = try {
        val stored = transactionRunner.inTransaction {
            val current = database.exemplarDao().findById(id)
                ?: throw DomainError.NotFound
            val now = Instant.now(clock)
            val updated = database.exemplarDao().updateWithVersion(
                id = id,
                condition = condition.name,
                packagingCondition = packagingCondition?.name,
                notes = notes,
                updatedAt = now.toEpochMilli(),
                expectedVersion = expectedRowVersion,
            )
            if (updated == 0) throw DomainError.StaleWrite

            // Só o campo que mudou entra na trilha: auditoria de edição não é snapshot.
            if (current.condition != condition.name) {
                auditLogWriter.record(
                    entityType = ENTITY_TYPE,
                    entityId = id,
                    field = "condition",
                    oldValue = current.condition,
                    newValue = condition.name,
                    source = "app",
                    at = now,
                )
            }
            val newPackaging = packagingCondition?.name
            if (current.packagingCondition != newPackaging) {
                auditLogWriter.record(
                    entityType = ENTITY_TYPE,
                    entityId = id,
                    field = "packaging_condition",
                    oldValue = current.packagingCondition,
                    newValue = newPackaging,
                    source = "app",
                    at = now,
                )
            }
            if (current.notes != notes) {
                auditLogWriter.record(
                    entityType = ENTITY_TYPE,
                    entityId = id,
                    field = "notes",
                    oldValue = current.notes,
                    newValue = notes,
                    source = "app",
                    at = now,
                )
            }
            database.exemplarDao().findById(id) ?: throw DomainError.NotFound
        }
        Result.success(stored.toDomain())
    } catch (error: Throwable) {
        Result.failure(mapError(error))
    }

    override suspend fun move(id: String, toLocationId: String?, note: String?): DomainResult<Unit> = try {
        transactionRunner.inTransaction {
            val current = database.exemplarDao().findById(id)
                ?: throw DomainError.NotFound
            val now = Instant.now(clock)
            database.exemplarDao().setLocation(id, toLocationId, now.toEpochMilli())
            database.movementDao().insert(
                MovementEntity(
                    id = UUID.randomUUID().toString(),
                    exemplarId = id,
                    at = now.toEpochMilli(),
                    fromLocationId = current.locationId,
                    toLocationId = toLocationId,
                    note = note,
                ),
            )
            auditLogWriter.record(
                entityType = ENTITY_TYPE,
                entityId = id,
                field = "location_id",
                oldValue = current.locationId,
                newValue = toLocationId,
                source = "app",
                at = now,
            )
        }
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(mapError(error))
    }

    override suspend fun reassignVariant(id: String, toVariantId: String): DomainResult<Unit> = try {
        transactionRunner.inTransaction {
            val current = database.exemplarDao().findById(id)
                ?: throw DomainError.NotFound
            database.variantDao().findById(toVariantId)
                ?: throw DomainError.Validation("Variante de destino não encontrada.")
            val now = Instant.now(clock)
            val updated = database.exemplarDao().setVariant(
                id = id,
                variantId = toVariantId,
                updatedAt = now.toEpochMilli(),
                expectedVersion = current.rowVersion,
            )
            if (updated == 0) throw DomainError.StaleWrite
            auditLogWriter.record(
                entityType = ENTITY_TYPE,
                entityId = id,
                field = "variant_id",
                oldValue = current.variantId,
                newValue = toVariantId,
                source = "app",
                at = now,
            )
        }
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(mapError(error))
    }

    override suspend fun countByVariant(variantId: String): Int =
        database.exemplarDao().countByVariant(variantId)

    override suspend fun archive(id: String): DomainResult<Unit> = try {
        transactionRunner.inTransaction {
            val current = database.exemplarDao().findById(id)
                ?: throw DomainError.NotFound
            if (current.status == ExemplarStatus.ARCHIVED.name) return@inTransaction

            val now = Instant.now(clock)
            database.exemplarDao().setStatus(id, ExemplarStatus.ARCHIVED.name, now.toEpochMilli())
            auditLogWriter.record(
                entityType = ENTITY_TYPE,
                entityId = id,
                field = "status",
                oldValue = current.status,
                newValue = ExemplarStatus.ARCHIVED.name,
                source = "app",
                at = now,
            )
        }
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(mapError(error))
    }

    override suspend fun dependents(id: String): ExemplarDependents = ExemplarDependents(
        photos = database.photoDao().countByExemplar(id),
        acquisitions = database.acquisitionDao().countByExemplar(id),
        valuations = database.valuationDao().countByExemplar(id),
        movements = database.movementDao().countByExemplar(id),
        negotiations = database.negotiationDao().countByExemplar(id),
    )

    override suspend fun delete(id: String): DomainResult<Unit> = try {
        transactionRunner.inTransaction {
            database.exemplarDao().findById(id) ?: throw DomainError.NotFound
            val deps = dependents(id)
            if (deps.hasAny) throw DomainError.Blocked(blockedMessage(deps))

            database.exemplarDao().deleteById(id)
            auditLogWriter.record(
                entityType = ENTITY_TYPE,
                entityId = id,
                field = "deleted",
                oldValue = "exemplar sem vínculos",
                newValue = null,
                source = "app",
                at = Instant.now(clock),
            )
        }
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(mapError(error))
    }

    /** Explica o bloqueio em pt-BR com os vínculos que existem (FR-028). */
    private fun blockedMessage(deps: ExemplarDependents): String {
        val parts = buildList {
            if (deps.photos > 0) add("${deps.photos} foto(s)")
            if (deps.acquisitions > 0) add("${deps.acquisitions} aquisição(ões)")
            if (deps.valuations > 0) add("${deps.valuations} avaliação(ões)")
            if (deps.movements > 0) add("${deps.movements} movimentação(ões)")
            if (deps.negotiations > 0) add("${deps.negotiations} negociação(ões)")
        }
        return "Este exemplar tem ${parts.joinToString(", ")}. " +
            "Vínculos impedem a exclusão — arquive em vez de excluir."
    }

    /**
     * Código legível, sequencial na prática: `HW-000123`.
     * A tentativa extra existe porque duas telas podem cadastrar ao mesmo tempo; o
     * índice único é a garantia final, não o `COUNT`.
     */
    private suspend fun nextInternalCode(): String {
        val exemplarDao = database.exemplarDao()
        var candidate = exemplarDao.countAll() + 1
        repeat(MAX_CODE_ATTEMPTS) {
            val code = "HW-%06d".format(candidate)
            if (exemplarDao.findByInternalCode(code) == null) return code
            candidate++
        }
        return "HW-${UUID.randomUUID().toString().take(8).uppercase()}"
    }

    private fun mapError(error: Throwable): Throwable = when {
        error is DomainError -> error
        error is SQLiteConstraintException && error.message?.contains("internal_code") == true ->
            DomainError.Duplicate
        error is SQLiteConstraintException ->
            DomainError.Validation("Registro viola uma regra de integridade do banco.")
        error is IOException -> DomainError.Storage(error.message ?: "Falha de armazenamento.")
        else -> DomainError.Storage(error.message ?: "Falha ao salvar o exemplar.")
    }

    private companion object {
        const val ENTITY_TYPE = "exemplar"
        const val MAX_CODE_ATTEMPTS = 5
    }
}

private fun NewAcquisition.toEntity(exemplarId: String, millis: Long): AcquisitionEntity = AcquisitionEntity(
    id = UUID.randomUUID().toString(),
    exemplarId = exemplarId,
    date = date.toIsoString(),
    priceCents = price?.cents,
    shippingCents = shipping?.cents,
    discountCents = discount?.cents,
    currency = "BRL",
    store = store,
    seller = seller,
    method = method,
    notes = notes,
    createdAt = millis,
    updatedAt = millis,
)
