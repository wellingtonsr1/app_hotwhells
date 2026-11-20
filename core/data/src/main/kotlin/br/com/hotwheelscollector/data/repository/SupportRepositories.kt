package br.com.hotwheelscollector.data.repository

import br.com.hotwheelscollector.data.db.AuditLogWriter
import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.TransactionRunner
import br.com.hotwheelscollector.data.db.entity.LocationEntity
import br.com.hotwheelscollector.data.db.entity.MovementEntity
import br.com.hotwheelscollector.data.db.entity.VariantEntity
import br.com.hotwheelscollector.data.db.entity.VariantSecondaryColorEntity
import br.com.hotwheelscollector.data.mapper.toDomain
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.ExemplarStatus
import br.com.hotwheelscollector.domain.model.HistoryEntry
import br.com.hotwheelscollector.domain.model.InventoryItem
import br.com.hotwheelscollector.domain.model.Location
import br.com.hotwheelscollector.domain.model.LocationArchivePolicy
import br.com.hotwheelscollector.domain.model.LocationNode
import br.com.hotwheelscollector.domain.model.LocationTree
import br.com.hotwheelscollector.domain.model.LocationType
import br.com.hotwheelscollector.domain.model.Movement
import br.com.hotwheelscollector.domain.model.Variant
import br.com.hotwheelscollector.domain.model.VariantSummary
import br.com.hotwheelscollector.domain.repository.HistoryRepository
import br.com.hotwheelscollector.domain.repository.LocationRepository
import br.com.hotwheelscollector.domain.repository.VariantRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Variantes: a checagem de fingerprint é a implementação real de FR-040. */
@Singleton
public class VariantRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val transactionRunner: TransactionRunner,
    private val auditLogWriter: AuditLogWriter,
    private val clock: Clock,
) : VariantRepository {

    override fun observeSummaries(): Flow<List<VariantSummary>> =
        database.variantDao().observeSummaries().map { rows ->
            rows.map {
                VariantSummary(
                    variantId = it.variantId,
                    modelName = it.modelName,
                    manufacturerName = it.manufacturerName,
                    colorName = it.colorName,
                    releaseYear = it.releaseYear,
                    exemplarCount = it.exemplarCount,
                )
            }
        }

    override fun observeByModel(modelId: String): Flow<List<Variant>> =
        database.variantDao().observeByModel(modelId).map { rows ->
            rows.map { variant ->
                variant.toDomain(database.variantSecondaryColorDao().findColorIds(variant.id))
            }
        }

    override suspend fun findById(id: String): Variant? {
        val entity = database.variantDao().findById(id) ?: return null
        return entity.toDomain(database.variantSecondaryColorDao().findColorIds(id))
    }

    override suspend fun findActiveByFingerprint(fingerprint: String): Variant? =
        database.variantDao().findActiveByFingerprint(fingerprint)?.toDomain()

    override suspend fun create(variant: Variant): DomainResult<Variant> = try {
        transactionRunner.inTransaction {
            database.variantDao().findActiveByFingerprint(variant.fingerprint)?.let {
                throw DomainError.Duplicate
            }
            database.variantDao().insert(
                VariantEntity(
                    id = variant.id,
                    modelId = variant.modelId,
                    fingerprint = variant.fingerprint,
                    seriesId = variant.seriesId,
                    primaryColorId = variant.primaryColorId,
                    wheelsType = variant.wheelsType,
                    baseType = variant.baseType,
                    decoration = variant.decoration,
                    releaseYear = variant.releaseYear,
                    collectionNumber = variant.collectionNumber,
                    seriesNumber = variant.seriesNumber,
                    edition = variant.edition,
                    manufacturerCode = variant.manufacturerCode,
                    packagingType = variant.packagingType,
                    archivedAt = null,
                    createdAt = variant.createdAt.toEpochMilli(),
                    updatedAt = variant.updatedAt.toEpochMilli(),
                ),
            )
            writeSecondaryColors(variant.id, variant.secondaryColorIds)
            auditLogWriter.record("variant", variant.id, "created", null, variant.fingerprint, "app")
        }
        Result.success(variant)
    } catch (error: Throwable) {
        Result.failure(if (error is DomainError) error else DomainError.Storage(error.message ?: "Falha ao criar variante."))
    }

    /**
     * Edição de variante (FR-029). O `createdAt` do registro atual é mantido — os
     * exemplares e fotos continuam apontando para o mesmo `id` — e o fingerprint é o que
     * o caso de uso calculou; a colisão com outra variante ativa é recusada aqui também,
     * porque a tela não é confiável para a regra (Art. I.3).
     */
    override suspend fun update(variant: Variant): DomainResult<Variant> = try {
        transactionRunner.inTransaction {
            val current = database.variantDao().findById(variant.id) ?: throw DomainError.NotFound
            database.variantDao().findActiveByFingerprint(variant.fingerprint)?.let {
                if (it.id != variant.id) throw DomainError.Duplicate
            }
            database.variantDao().update(
                VariantEntity(
                    id = variant.id,
                    modelId = current.modelId,
                    fingerprint = variant.fingerprint,
                    seriesId = variant.seriesId,
                    primaryColorId = variant.primaryColorId,
                    wheelsType = variant.wheelsType,
                    baseType = variant.baseType,
                    decoration = variant.decoration,
                    releaseYear = variant.releaseYear,
                    collectionNumber = variant.collectionNumber,
                    seriesNumber = variant.seriesNumber,
                    edition = variant.edition,
                    manufacturerCode = variant.manufacturerCode,
                    packagingType = variant.packagingType,
                    archivedAt = current.archivedAt,
                    createdAt = current.createdAt,
                    updatedAt = variant.updatedAt.toEpochMilli(),
                ),
            )
            writeSecondaryColors(variant.id, variant.secondaryColorIds)
            auditLogWriter.record(
                "variant",
                variant.id,
                "updated",
                current.fingerprint,
                variant.fingerprint,
                "app",
            )
        }
        Result.success(variant)
    } catch (error: Throwable) {
        Result.failure(if (error is DomainError) error else DomainError.Storage(error.message ?: "Falha ao atualizar variante."))
    }

    /** Cores secundárias são um conjunto: regravar é substituir, não somar. */
    private suspend fun writeSecondaryColors(variantId: String, colorIds: List<String>) {
        database.variantSecondaryColorDao().deleteByVariant(variantId)
        if (colorIds.isNotEmpty()) {
            database.variantSecondaryColorDao().insertAll(
                colorIds.map { VariantSecondaryColorEntity(variantId = variantId, colorId = it) },
            )
        }
    }

    override suspend fun archive(id: String): DomainResult<Unit> = try {
        database.variantDao().archive(id, Instant.now(clock).toEpochMilli())
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(DomainError.Storage(error.message ?: "Falha ao arquivar variante."))
    }

    /**
     * União de variantes (FR-041): move os exemplares, arquiva a absorvida e registra a
     * decisão. Nada é apagado — a variante absorvida continua rastreável no histórico.
     */
    override suspend fun mergeVariants(targetId: String, absorbedId: String): DomainResult<Unit> = try {
        if (targetId == absorbedId) throw DomainError.Validation("Escolha duas variantes diferentes.")
        transactionRunner.inTransaction {
            val now = Instant.now(clock)
            database.variantDao().findById(absorbedId) ?: throw DomainError.NotFound
            val moved = database.exemplarDao()
                .reassignVariant(absorbedId, targetId, now.toEpochMilli())
            database.variantDao().archive(absorbedId, now.toEpochMilli())
            auditLogWriter.record(
                entityType = "variant",
                entityId = targetId,
                field = "merged_from",
                oldValue = absorbedId,
                newValue = "$moved exemplares movidos",
                source = "app",
                at = now,
            )
        }
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(if (error is DomainError) error else DomainError.Storage(error.message ?: "Falha ao unir variantes."))
    }
}

/** Localizações: arquivar com exemplares exige decisão explícita, nunca cascata (FR-093). */
@Singleton
public class LocationRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val transactionRunner: TransactionRunner,
    private val auditLogWriter: AuditLogWriter,
    private val clock: Clock,
) : LocationRepository {

    override fun observeActive(): Flow<List<Location>> =
        database.locationDao().observeActive().map { rows -> rows.map { it.toDomain() } }

    override fun observeTree(): Flow<List<LocationNode>> = combine(
        database.locationDao().observeActive(),
        database.locationDao().observeOwnedCounts(),
    ) { rows, counts ->
        LocationTree.build(
            locations = rows.map { it.toDomain() },
            counts = counts.associate { it.locationId to it.total },
        )
    }

    override suspend fun children(parentId: String): List<Location> =
        database.locationDao().findChildren(parentId).map { it.toDomain() }

    override suspend fun findById(id: String): Location? =
        database.locationDao().findById(id)?.toDomain()

    override suspend fun add(
        type: LocationType,
        name: String,
        parentId: String?,
    ): DomainResult<Location> {
        if (name.isBlank()) {
            return Result.failure(DomainError.Validation("Nome da localização é obrigatório."))
        }
        if (parentId != null && database.locationDao().findById(parentId) == null) {
            return Result.failure(DomainError.NotFound)
        }
        val now = Instant.now(clock).toEpochMilli()
        val entity = LocationEntity(
            id = UUID.randomUUID().toString(),
            type = type.name,
            name = name.trim(),
            parentId = parentId,
            archivedAt = null,
            createdAt = now,
            updatedAt = now,
        )
        return try {
            database.locationDao().insert(entity)
            Result.success(entity.toDomain())
        } catch (error: Throwable) {
            Result.failure(DomainError.Storage(error.message ?: "Falha ao criar localização."))
        }
    }

    override suspend fun rename(id: String, name: String): DomainResult<Unit> {
        if (name.isBlank()) {
            return Result.failure(DomainError.Validation("Nome da localização é obrigatório."))
        }
        val current = database.locationDao().findById(id)
            ?: return Result.failure(DomainError.NotFound)
        val now = Instant.now(clock).toEpochMilli()
        return try {
            database.locationDao().update(
                current.copy(name = name.trim(), updatedAt = now),
            )
            auditLogWriter.record("location", id, "name", current.name, name.trim(), "app")
            Result.success(Unit)
        } catch (error: Throwable) {
            Result.failure(DomainError.Storage(error.message ?: "Falha ao renomear localização."))
        }
    }

    override suspend fun archive(
        id: String,
        policy: LocationArchivePolicy,
    ): DomainResult<Int> = try {
        val handled = transactionRunner.inTransaction {
            val current = database.locationDao().findById(id) ?: throw DomainError.NotFound
            if (current.archivedAt != null) return@inTransaction 0

            val children = database.locationDao().findChildren(id)
            if (children.isNotEmpty()) {
                throw DomainError.Blocked(
                    "Esta localização tem ${children.size} sublocalização(ões) ativa(s). " +
                        "Organize ou exclua as sublocalizações antes.",
                )
            }

            val exemplarIds = database.locationDao().findOwnedExemplarIdsAt(id)
            val now = Instant.now(clock)
            val millis = now.toEpochMilli()

            when (policy) {
                LocationArchivePolicy.None -> if (exemplarIds.isNotEmpty()) {
                    throw DomainError.Blocked(
                        "Esta localização tem ${exemplarIds.size} exemplar(es). " +
                            "Escolha mover para outra localização ou arquivar os exemplares.",
                    )
                }

                is LocationArchivePolicy.MoveTo -> {
                    if (policy.targetLocationId == id) {
                        throw DomainError.Validation("Escolha outra localização de destino.")
                    }
                    val target = database.locationDao().findById(policy.targetLocationId)
                        ?: throw DomainError.NotFound
                    if (target.archivedAt != null) {
                        throw DomainError.Validation("A localização de destino está arquivada.")
                    }
                    // Mover para dentro da própria localização deixaria os exemplares
                    // embaixo de uma raiz arquivada: o destino não pode ser descendente.
                    var ancestor: String? = target.parentId
                    var guard = 0
                    while (ancestor != null && ++guard <= MAX_DEPTH) {
                        if (ancestor == id) {
                            throw DomainError.Validation(
                                "Escolha um destino fora desta localização e das sublocalizações dela.",
                            )
                        }
                        ancestor = database.locationDao().findById(ancestor)?.parentId
                    }
                    exemplarIds.forEach { exemplarId ->
                        database.exemplarDao().setLocation(exemplarId, target.id, millis)
                        database.movementDao().insert(
                            MovementEntity(
                                id = UUID.randomUUID().toString(),
                                exemplarId = exemplarId,
                                at = millis,
                                fromLocationId = id,
                                toLocationId = target.id,
                                note = "Movimentação ao excluir a localização",
                            ),
                        )
                        auditLogWriter.record(
                            entityType = "exemplar",
                            entityId = exemplarId,
                            field = "location_id",
                            oldValue = id,
                            newValue = target.id,
                            source = "app",
                            at = now,
                        )
                    }
                }

                LocationArchivePolicy.ArchiveExemplars -> exemplarIds.forEach { exemplarId ->
                    database.exemplarDao().setStatus(
                        exemplarId,
                        ExemplarStatus.ARCHIVED.name,
                        millis,
                    )
                    database.movementDao().insert(
                        MovementEntity(
                            id = UUID.randomUUID().toString(),
                            exemplarId = exemplarId,
                            at = millis,
                            fromLocationId = id,
                            toLocationId = null,
                            note = "Exemplar arquivado com a localização",
                        ),
                    )
                    auditLogWriter.record(
                        entityType = "exemplar",
                        entityId = exemplarId,
                        field = "status",
                        oldValue = ExemplarStatus.OWNED.name,
                        newValue = ExemplarStatus.ARCHIVED.name,
                        source = "app",
                        at = now,
                    )
                }
            }

            database.locationDao().archive(id, millis)
            auditLogWriter.record(
                entityType = "location",
                entityId = id,
                field = "archived",
                oldValue = null,
                newValue = current.name,
                source = "app",
                at = now,
            )
            exemplarIds.size
        }
        Result.success(handled)
    } catch (error: Throwable) {
        Result.failure(if (error is DomainError) error else DomainError.Storage(error.message ?: "Falha ao excluir localização."))
    }

    override suspend fun movements(exemplarId: String): Flow<List<Movement>> =
        database.movementDao().observeByExemplar(exemplarId)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeContents(locationId: String): Flow<List<InventoryItem>> =
        database.locationDao().observeContents(locationId)
            .map { rows -> rows.map { it.toDomain() } }

    private companion object {
        const val MAX_DEPTH = 100
    }
}

/** Trilha de auditoria: leitura somente. */
@Singleton
public class HistoryRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
) : HistoryRepository {

    override fun observeFor(entityType: String, entityId: String): Flow<List<HistoryEntry>> =
        database.historyDao().observeFor(entityType, entityId)
            .map { rows -> rows.map { it.toDomain() } }
}
