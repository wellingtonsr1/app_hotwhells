package br.com.diecastcollector.data.repository

import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.TransactionRunner
import br.com.diecastcollector.data.db.dao.WishlistRow
import br.com.diecastcollector.data.db.entity.WishlistItemEntity
import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.model.WishlistItem
import br.com.diecastcollector.domain.model.WishlistStatus
import br.com.diecastcollector.domain.repository.AddExemplarOutcome
import br.com.diecastcollector.domain.repository.ExemplarRepository
import br.com.diecastcollector.domain.repository.NewExemplar
import br.com.diecastcollector.domain.repository.WishlistEntry
import br.com.diecastcollector.domain.repository.WishlistRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Lista de desejos (US6).
 *
 * O desejo nasce com data de criação e prioridade; a conversão só muda status e vínculo,
 * nunca valores ou data, para que o histórico da intenção de compra continue legível.
 */
@Singleton
public class WishlistRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val exemplarRepository: ExemplarRepository,
    private val transactionRunner: TransactionRunner,
    private val clock: Clock,
) : WishlistRepository {

    override fun observeAll(): Flow<List<WishlistEntry>> =
        database.wishlistDao().observeAllWithTitle().map { rows ->
            rows.map { it.toDomain() }
        }

    override suspend fun findById(id: String): WishlistItem? =
        database.wishlistDao().findById(id)?.toDomain()

    override suspend fun add(item: WishlistItem): DomainResult<WishlistItem> {
        if (item.modelId == null && item.variantId == null && item.freeText.isNullOrBlank()) {
            return Result.failure(
                DomainError.Validation("Informe o modelo ou descreva o que você procura."),
            )
        }
        val now = Instant.now(clock).toEpochMilli()
        val entity = item.toEntity(
            id = item.id.ifBlank { UUID.randomUUID().toString() },
            createdAt = now,
            updatedAt = now,
        )
        return try {
            database.wishlistDao().insert(entity)
            Result.success(entity.toDomain())
        } catch (error: Throwable) {
            Result.failure(DomainError.Storage(error.message ?: "Falha ao salvar desejo."))
        }
    }

    override suspend fun update(item: WishlistItem): DomainResult<WishlistItem> {
        val current = database.wishlistDao().findById(item.id)
            ?: return Result.failure(DomainError.NotFound)
        // Editar o formulário não mexe no ciclo de vida: status e vínculo com o exemplar
        // só mudam na conversão ou na desistência, nunca por sobrescrita de campos.
        val updated = item.toEntity(
            id = current.id,
            createdAt = current.createdAt,
            updatedAt = Instant.now(clock).toEpochMilli(),
        ).copy(
            status = current.status,
            convertedExemplarId = current.convertedExemplarId,
        )
        return try {
            database.wishlistDao().update(updated)
            Result.success(updated.toDomain())
        } catch (error: Throwable) {
            Result.failure(DomainError.Storage(error.message ?: "Falha ao salvar desejo."))
        }
    }

    override suspend fun dismiss(id: String): DomainResult<Unit> =
        changeStatus(id) { it.copy(status = WishlistStatus.DISMISSED.name) }

    /**
     * A transação externa envolve a interna de `ExemplarRepository.add` (o Room participa
     * da mesma transação): se gravar o vínculo falhar, exemplar, aquisição, movimentação
     * e auditoria voltam atrás junto.
     */
    override suspend fun convertToAcquisition(
        id: String,
        draft: NewExemplar?,
    ): DomainResult<AddExemplarOutcome?> = try {
        val outcome = transactionRunner.inTransaction {
            val current = database.wishlistDao().findById(id)
                ?: throw DomainError.NotFound
            if (current.status == WishlistStatus.ACQUIRED.name) {
                throw DomainError.Validation("Este desejo já foi convertido em aquisição.")
            }

            val created = draft?.let { exemplarRepository.add(it).getOrThrow() }
            database.wishlistDao().update(
                current.copy(
                    status = WishlistStatus.ACQUIRED.name,
                    convertedExemplarId = created?.exemplar?.id,
                    updatedAt = Instant.now(clock).toEpochMilli(),
                ),
            )
            created
        }
        Result.success(outcome)
    } catch (error: Throwable) {
        Result.failure(
            when (error) {
                is DomainError -> error
                else -> DomainError.Storage(error.message ?: "Falha ao converter desejo.")
            },
        )
    }

    private suspend fun changeStatus(
        id: String,
        change: (WishlistItemEntity) -> WishlistItemEntity,
    ): DomainResult<Unit> {
        val current = database.wishlistDao().findById(id)
            ?: return Result.failure(DomainError.NotFound)
        return try {
            database.wishlistDao().update(
                change(current).copy(updatedAt = Instant.now(clock).toEpochMilli()),
            )
            Result.success(Unit)
        } catch (error: Throwable) {
            Result.failure(DomainError.Storage(error.message ?: "Falha ao atualizar desejo."))
        }
    }

    private fun WishlistRow.toDomain(): WishlistEntry =
        WishlistEntry(item = item.toDomain(), title = title.orEmpty())

    private fun WishlistItemEntity.toDomain(): WishlistItem = WishlistItem(
        id = id,
        priority = priority,
        status = WishlistStatus.valueOf(status),
        modelId = modelId,
        variantId = variantId,
        freeText = freeText,
        maxPrice = maxPriceCents?.let(::MoneyCents),
        foundPrice = foundPriceCents?.let(::MoneyCents),
        store = store,
        referenceUrl = referenceUrl,
        notes = notes,
        convertedExemplarId = convertedExemplarId,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

    private fun WishlistItem.toEntity(
        id: String,
        createdAt: Long,
        updatedAt: Long,
    ): WishlistItemEntity = WishlistItemEntity(
        id = id,
        modelId = modelId,
        variantId = variantId,
        freeText = freeText?.trim()?.takeIf { it.isNotEmpty() },
        priority = priority,
        maxPriceCents = maxPrice?.cents,
        foundPriceCents = foundPrice?.cents,
        store = store?.trim()?.takeIf { it.isNotEmpty() },
        referenceUrl = referenceUrl?.trim()?.takeIf { it.isNotEmpty() },
        notes = notes?.trim()?.takeIf { it.isNotEmpty() },
        status = status.name,
        convertedExemplarId = convertedExemplarId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
