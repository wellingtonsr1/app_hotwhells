package br.com.hotwheelscollector.data.repository

import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.AuditLogWriter
import br.com.hotwheelscollector.data.db.TransactionRunner
import br.com.hotwheelscollector.data.db.entity.CollectionEntity
import br.com.hotwheelscollector.data.db.entity.CollectionItemEntity
import br.com.hotwheelscollector.data.mapper.toDomain
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.Collection
import br.com.hotwheelscollector.domain.model.CollectionItem
import br.com.hotwheelscollector.domain.model.CollectionItemStatus
import br.com.hotwheelscollector.domain.model.CollectionProgress
import br.com.hotwheelscollector.domain.model.CollectionProgressCalculator
import br.com.hotwheelscollector.domain.repository.CollectionRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Coleções e itens (FR-049–FR-056).
 *
 * O progresso é calculado em Kotlin pelo `CollectionProgressCalculator`, com uma consulta
 * única para todos os itens. A alternativa — somar no SQL — repetiria a regra em dois
 * lugares e deixaria a lista de coleções e o painel discordarem sobre o mesmo número.
 *
 * A hierarquia é verificada aqui porque o SQLite não expressa "sem ciclo" em CHECK: subir
 * a cadeia de pais antes de gravar é o que impede uma coleção de virar descendente de si.
 */
@Singleton
public class CollectionRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val transactionRunner: TransactionRunner,
    private val auditLogWriter: AuditLogWriter,
    private val clock: Clock,
) : CollectionRepository {

    override fun observeAll(): Flow<List<CollectionProgress>> = combine(
        database.collectionDao().observeActive(),
        database.collectionItemDao().observeAllWithOwned(),
    ) { collections, rows ->
        val byCollection = rows.map { it.toDomain() }.groupBy { it.collectionId }
        collections.map { entity ->
            val collection = entity.toDomain()
            CollectionProgressCalculator.calculate(
                collection = collection,
                items = byCollection[collection.id].orEmpty(),
            )
        }
    }

    override fun observeItems(collectionId: String): Flow<List<CollectionItemStatus>> =
        database.collectionItemDao().observeAllWithOwned().map { rows ->
            rows.asSequence()
                .filter { it.collectionId == collectionId }
                .map { it.toDomain() }
                .toList()
        }

    override suspend fun findById(id: String): Collection? =
        database.collectionDao().findById(id)?.toDomain()

    override suspend fun create(
        name: String,
        description: String?,
        parentId: String?,
        manufacturerId: String?,
    ): DomainResult<Collection> = try {
        val trimmed = requireName(name)
        val stored = transactionRunner.inTransaction {
            parentId?.let { requireParentExists(it) }
            manufacturerId?.let {
                if (database.manufacturerDao().findById(it) == null) throw DomainError.NotFound
            }
            val now = Instant.now(clock)
            val entity = CollectionEntity(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                description = description.cleanOptional(),
                parentId = parentId,
                manufacturerId = manufacturerId,
                archivedAt = null,
                createdAt = now.toEpochMilli(),
                updatedAt = now.toEpochMilli(),
            )
            database.collectionDao().insert(entity)
            auditLogWriter.record(ENTITY_TYPE, entity.id, "created", null, trimmed, "app", now)
            entity.toDomain()
        }
        Result.success(stored)
    } catch (error: Throwable) {
        Result.failure(mapError(error, "Falha ao criar coleção."))
    }

    override suspend fun update(
        id: String,
        name: String,
        description: String?,
        parentId: String?,
        manufacturerId: String?,
    ): DomainResult<Unit> = try {
        val trimmed = requireName(name)
        transactionRunner.inTransaction {
            val current = database.collectionDao().findById(id) ?: throw DomainError.NotFound
            if (parentId != null) {
                requireParentExists(parentId)
                requireNoCycle(id, parentId)
            }
            manufacturerId?.let {
                if (database.manufacturerDao().findById(it) == null) throw DomainError.NotFound
            }
            val now = Instant.now(clock)
            database.collectionDao().update(
                current.copy(
                    name = trimmed,
                    description = description.cleanOptional(),
                    parentId = parentId,
                    manufacturerId = manufacturerId,
                    updatedAt = now.toEpochMilli(),
                ),
            )
            if (current.name != trimmed) {
                auditLogWriter.record(ENTITY_TYPE, id, "name", current.name, trimmed, "app", now)
            }
            if (current.parentId != parentId) {
                auditLogWriter.record(
                    entityType = ENTITY_TYPE,
                    entityId = id,
                    field = "parent_id",
                    oldValue = current.parentId,
                    newValue = parentId,
                    source = "app",
                    at = now,
                )
            }
        }
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(mapError(error, "Falha ao salvar coleção."))
    }

    override suspend fun archive(id: String): DomainResult<Unit> = try {
        transactionRunner.inTransaction {
            val current = database.collectionDao().findById(id) ?: throw DomainError.NotFound
            if (current.archivedAt != null) return@inTransaction
            val children = database.collectionDao().countActiveChildren(id)
            if (children > 0) {
                throw DomainError.Blocked(
                    "Esta coleção tem $children subcoleção(ões) ativa(s). " +
                        "Reorganize ou arquive as subcoleções antes.",
                )
            }
            val now = Instant.now(clock)
            database.collectionDao().update(
                current.copy(archivedAt = now.toEpochMilli(), updatedAt = now.toEpochMilli()),
            )
            auditLogWriter.record(ENTITY_TYPE, id, "archived", null, current.name, "app", now)
        }
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(mapError(error, "Falha ao arquivar coleção."))
    }

    override suspend fun addVariant(
        collectionId: String,
        variantId: String,
        expected: Int,
    ): DomainResult<CollectionItem> = addItem(
        collectionId = collectionId,
        variantId = variantId,
        modelId = null,
        expected = expected,
    )

    override suspend fun addModel(
        collectionId: String,
        modelId: String,
        expected: Int,
    ): DomainResult<CollectionItem> = addItem(
        collectionId = collectionId,
        variantId = null,
        modelId = modelId,
        expected = expected,
    )

    private suspend fun addItem(
        collectionId: String,
        variantId: String?,
        modelId: String?,
        expected: Int,
    ): DomainResult<CollectionItem> = try {
        if (expected < 0) throw DomainError.Validation("Quantidade esperada não pode ser negativa.")
        val stored = transactionRunner.inTransaction {
            val collection = database.collectionDao().findById(collectionId)
                ?: throw DomainError.NotFound
            if (collection.archivedAt != null) {
                throw DomainError.Validation("Coleção arquivada não recebe itens novos.")
            }
            val duplicate = if (variantId != null) {
                database.collectionItemDao().findByVariant(collectionId, variantId)
            } else {
                database.collectionItemDao().findByModel(collectionId, modelId.orEmpty())
            }
            if (duplicate != null) throw DomainError.Duplicate

            val entity = CollectionItemEntity(
                id = UUID.randomUUID().toString(),
                collectionId = collectionId,
                variantId = variantId,
                modelId = modelId,
                expected = expected,
                nonCollectible = 0,
                notes = null,
            )
            database.collectionItemDao().insert(entity)
            auditLogWriter.record(
                entityType = "collection_item",
                entityId = entity.id,
                field = "added",
                oldValue = null,
                newValue = variantId?.let { "variante $it" } ?: "modelo $modelId",
                source = "app",
                at = Instant.now(clock),
            )
            entity.toDomain()
        }
        Result.success(stored)
    } catch (error: Throwable) {
        Result.failure(mapError(error, "Falha ao adicionar item à coleção."))
    }

    override suspend fun setNonCollectible(
        itemId: String,
        nonCollectible: Boolean,
    ): DomainResult<Unit> = try {
        transactionRunner.inTransaction {
            val current = database.collectionItemDao().findById(itemId)
                ?: throw DomainError.NotFound
            val flag = if (nonCollectible) 1 else 0
            if (current.nonCollectible == flag) return@inTransaction
            database.collectionItemDao().setNonCollectible(itemId, flag)
            auditLogWriter.record(
                entityType = "collection_item",
                entityId = itemId,
                field = "non_collectible",
                oldValue = current.nonCollectible.toString(),
                newValue = flag.toString(),
                source = "app",
                at = Instant.now(clock),
            )
        }
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(mapError(error, "Falha ao marcar o item."))
    }

    override suspend fun removeItem(itemId: String): DomainResult<Unit> = try {
        transactionRunner.inTransaction {
            val current = database.collectionItemDao().findById(itemId)
                ?: throw DomainError.NotFound
            database.collectionItemDao().deleteById(itemId)
            auditLogWriter.record(
                entityType = "collection_item",
                entityId = itemId,
                field = "removed",
                oldValue = current.variantId ?: current.modelId,
                newValue = null,
                source = "app",
                at = Instant.now(clock),
            )
        }
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(mapError(error, "Falha ao remover o item."))
    }

    private suspend fun requireParentExists(parentId: String) {
        if (database.collectionDao().findById(parentId) == null) throw DomainError.NotFound
    }

    /**
     * Sobe a cadeia de pais até a raiz. Se encontrar a própria coleção, o novo pai é
     * descendente dela e a gravação criaria um ciclo.
     */
    private suspend fun requireNoCycle(id: String, parentId: String) {
        if (parentId == id) {
            throw DomainError.Validation("Uma coleção não pode ficar dentro dela mesma.")
        }
        var cursor: String? = parentId
        var guard = 0
        while (cursor != null) {
            if (cursor == id) {
                throw DomainError.Validation(
                    "Esta coleção não pode ficar dentro de uma subcoleção dela mesma.",
                )
            }
            if (++guard > MAX_DEPTH) {
                throw DomainError.Validation("Hierarquia de coleções profunda demais.")
            }
            cursor = database.collectionDao().findById(cursor)?.parentId
        }
    }

    private fun requireName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw DomainError.Validation("Nome da coleção é obrigatório.")
        if (trimmed.length > MAX_NAME_LENGTH) {
            throw DomainError.Validation("Nome da coleção excede 120 caracteres.")
        }
        return trimmed
    }

    private fun String?.cleanOptional(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }

    private fun mapError(error: Throwable, fallback: String): Throwable = when (error) {
        is DomainError -> error
        else -> DomainError.Storage(error.message ?: fallback)
    }

    private companion object {
        const val ENTITY_TYPE = "collection"
        const val MAX_DEPTH = 100
        const val MAX_NAME_LENGTH = 120
    }
}
