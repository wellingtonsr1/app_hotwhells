package br.com.diecastcollector.data.repository

import br.com.diecastcollector.data.db.AuditLogWriter
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.TransactionRunner
import br.com.diecastcollector.data.db.entity.DuplicateOfferEntity
import br.com.diecastcollector.data.db.entity.NegotiationEventEntity
import br.com.diecastcollector.data.mapper.toDomain
import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.DuplicateAvailability
import br.com.diecastcollector.domain.model.DuplicateExemplar
import br.com.diecastcollector.domain.model.DuplicateGroup
import br.com.diecastcollector.domain.model.DuplicateOffer
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.domain.model.IsoDate
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.model.Negotiation
import br.com.diecastcollector.domain.model.NegotiationEvent
import br.com.diecastcollector.domain.model.NegotiationStatus
import br.com.diecastcollector.domain.model.NegotiationType
import br.com.diecastcollector.domain.repository.DuplicateRepository
import br.com.diecastcollector.domain.repository.NegotiationRepository
import br.com.diecastcollector.domain.repository.NewNegotiation
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Duplicados por variante (FR-065–FR-067).
 *
 * A consulta já devolve oferta e negociação ativa de cada exemplar; aqui só se agrupa
 * em memória, preservando a ordem do SQL.
 */
@Singleton
public class DuplicateRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val transactionRunner: TransactionRunner,
    private val clock: Clock,
) : DuplicateRepository {

    override fun observeGroups(): Flow<List<DuplicateGroup>> =
        database.duplicateDao().observeDuplicates().map { rows ->
            rows.groupBy { it.exemplar.variantId }.map { (variantId, groupRows) ->
                val first = groupRows.first()
                DuplicateGroup(
                    variantId = variantId,
                    manufacturerName = first.manufacturerName,
                    modelName = first.modelName,
                    colorName = first.colorName,
                    exemplars = groupRows.map { row ->
                        DuplicateExemplar(
                            exemplar = row.exemplar.toDomain(),
                            manufacturerName = row.manufacturerName,
                            modelName = row.modelName,
                            colorName = row.colorName,
                            offer = row.offer?.toDomain(),
                            activeNegotiation = row.activeNegotiation?.toDomain(),
                        )
                    },
                )
            }
        }

    override suspend fun offerFor(exemplarId: String): DuplicateOffer? =
        database.duplicateDao().findOffer(exemplarId)?.toDomain()

    override fun observeOffer(exemplarId: String): Flow<DuplicateOffer?> =
        database.duplicateDao().observeOffer(exemplarId).map { it?.toDomain() }

    override fun observeOffersByVariant(
        variantId: String,
    ): Flow<Map<String, DuplicateOffer>> =
        database.duplicateDao().observeOffersByVariant(variantId).map { rows ->
            rows.map { it.toDomain() }.associateBy { it.exemplarId }
        }

    override suspend fun setOffer(
        exemplarId: String,
        availability: DuplicateAvailability,
        condition: Condition?,
        askingPrice: MoneyCents?,
        notes: String?,
    ): DomainResult<DuplicateOffer> = try {
        val stored = transactionRunner.inTransaction {
            val exemplar = database.exemplarDao().findById(exemplarId)
                ?: throw DomainError.NotFound
            if (exemplar.status != ExemplarStatus.OWNED.name) {
                throw DomainError.Validation(
                    "Só exemplar no acervo pode ser marcado como disponível.",
                )
            }
            val now = Instant.now(clock).toEpochMilli()
            val current = database.duplicateDao().findOffer(exemplarId)
            val entity = DuplicateOfferEntity(
                id = current?.id ?: UUID.randomUUID().toString(),
                exemplarId = exemplarId,
                availability = availability.name,
                condition = condition?.name,
                askingPriceCents = askingPrice?.cents,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
                createdAt = current?.createdAt ?: now,
                updatedAt = now,
            )
            if (current == null) {
                database.duplicateDao().insertOffer(entity)
            } else {
                database.duplicateDao().updateOffer(entity)
            }
            entity
        }
        Result.success(stored.toDomain())
    } catch (error: Throwable) {
        Result.failure(mapError(error))
    }

    private fun mapError(error: Throwable): Throwable = when (error) {
        is DomainError -> error
        else -> DomainError.Storage(error.message ?: "Falha ao salvar a disponibilidade.")
    }
}

/**
 * Negociação (FR-068–FR-073).
 *
 * Toda mudança de estado — abrir, concluir, cancelar — acontece numa transação com o
 * evento correspondente. A unicidade de negociação ativa é verificada aqui dentro,
 * porque o SQLite não expressa o índice parcial único de `OPEN`/`RESERVED` pelo Room.
 */
@Singleton
public class NegotiationRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val transactionRunner: TransactionRunner,
    private val auditLogWriter: AuditLogWriter,
    private val clock: Clock,
) : NegotiationRepository {

    override fun observeByExemplar(exemplarId: String): Flow<List<Negotiation>> =
        database.negotiationDao().observeByExemplar(exemplarId)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeActiveByVariant(
        variantId: String,
    ): Flow<Map<String, Negotiation>> =
        database.negotiationDao().observeActiveByVariant(variantId)
            .map { rows -> rows.map { it.toDomain() }.associateBy { it.exemplarId } }

    override fun observeEventsForExemplar(exemplarId: String): Flow<List<NegotiationEvent>> =
        database.negotiationEventDao().observeByExemplar(exemplarId)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun open(draft: NewNegotiation): DomainResult<Negotiation> = try {
        val stored = transactionRunner.inTransaction {
            val exemplar = database.exemplarDao().findById(draft.exemplarId)
                ?: throw DomainError.NotFound
            if (exemplar.status != ExemplarStatus.OWNED.name) {
                throw DomainError.Validation(
                    "Só exemplar no acervo pode entrar em negociação.",
                )
            }
            val active = database.negotiationDao().findActiveForExemplar(draft.exemplarId)
            if (active.isNotEmpty()) {
                throw DomainError.Blocked(
                    "Este exemplar já está em negociação ativa. " +
                        "Conclua ou cancele a negociação atual antes de abrir outra.",
                )
            }

            val now = Instant.now(clock)
            val millis = now.toEpochMilli()
            val id = UUID.randomUUID().toString()
            val entity = br.com.diecastcollector.data.db.entity.NegotiationEntity(
                id = id,
                exemplarId = draft.exemplarId,
                type = draft.type.name,
                counterparty = draft.counterparty?.trim()?.takeIf { it.isNotEmpty() },
                channel = draft.channel?.trim()?.takeIf { it.isNotEmpty() },
                status = if (draft.reserve) {
                    NegotiationStatus.RESERVED.name
                } else {
                    NegotiationStatus.OPEN.name
                },
                startedAt = millis,
                closedAt = null,
                amountCents = draft.amount?.cents,
                currency = draft.amount?.let { CURRENCY },
                notes = draft.notes?.trim()?.takeIf { it.isNotEmpty() },
                receivedExemplarId = null,
            )
            database.negotiationDao().insert(entity)
            database.negotiationEventDao().insert(
                NegotiationEventEntity(
                    id = UUID.randomUUID().toString(),
                    negotiationId = id,
                    at = millis,
                    description = if (draft.reserve) "Reservado para negociação" else "Negociação aberta",
                    amountCents = draft.amount?.cents,
                ),
            )
            auditLogWriter.record(
                entityType = EXEMPLAR_TYPE,
                entityId = draft.exemplarId,
                field = "negotiation",
                oldValue = null,
                newValue = id,
                source = "app",
                at = now,
            )
            entity
        }
        Result.success(stored.toDomain())
    } catch (error: Throwable) {
        Result.failure(mapError(error))
    }

    override suspend fun closeSale(
        negotiationId: String,
        amount: MoneyCents,
        date: IsoDate?,
        counterparty: String?,
    ): DomainResult<Unit> = try {
        transactionRunner.inTransaction {
            val current = activeNegotiation(negotiationId)
            if (current.type != NegotiationType.SALE.name) {
                throw DomainError.Validation("Esta negociação é uma troca, não uma venda.")
            }
            val now = Instant.now(clock)
            val closedAt = date?.value
                ?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli()
                ?: now.toEpochMilli()
            database.negotiationDao().update(
                current.copy(
                    status = NegotiationStatus.CLOSED.name,
                    closedAt = closedAt,
                    amountCents = amount.cents,
                    currency = CURRENCY,
                    counterparty = counterparty?.trim()?.takeIf { it.isNotEmpty() }
                        ?: current.counterparty,
                ),
            )
            database.negotiationEventDao().insert(
                NegotiationEventEntity(
                    id = UUID.randomUUID().toString(),
                    negotiationId = negotiationId,
                    at = closedAt,
                    description = "Venda concluída",
                    amountCents = amount.cents,
                ),
            )
            database.exemplarDao().setStatus(
                current.exemplarId,
                ExemplarStatus.SOLD.name,
                now.toEpochMilli(),
            )
            auditLogWriter.record(
                entityType = EXEMPLAR_TYPE,
                entityId = current.exemplarId,
                field = "status",
                oldValue = ExemplarStatus.OWNED.name,
                newValue = ExemplarStatus.SOLD.name,
                source = "app",
                at = now,
            )
        }
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(mapError(error))
    }

    override suspend fun closeTrade(
        negotiationId: String,
        receivedExemplarId: String?,
        note: String?,
    ): DomainResult<Unit> = try {
        transactionRunner.inTransaction {
            val current = activeNegotiation(negotiationId)
            if (current.type != NegotiationType.TRADE.name) {
                throw DomainError.Validation("Esta negociação é uma venda, não uma troca.")
            }

            val received = receivedExemplarId?.let { id ->
                if (id == current.exemplarId) {
                    throw DomainError.Validation(
                        "O exemplar recebido precisa ser diferente do negociado.",
                    )
                }
                val entity = database.exemplarDao().findById(id)
                    ?: throw DomainError.NotFound
                if (entity.status != ExemplarStatus.OWNED.name) {
                    throw DomainError.Validation("O exemplar recebido precisa estar no acervo.")
                }
                entity
            }

            val now = Instant.now(clock)
            val millis = now.toEpochMilli()
            database.negotiationDao().update(
                current.copy(
                    status = NegotiationStatus.CLOSED.name,
                    closedAt = millis,
                    receivedExemplarId = received?.id,
                ),
            )
            val description = buildString {
                append("Troca concluída")
                received?.let { append(" · recebido ${it.internalCode}") }
                note?.trim()?.takeIf { it.isNotEmpty() }?.let { append(" · $it") }
            }
            database.negotiationEventDao().insert(
                NegotiationEventEntity(
                    id = UUID.randomUUID().toString(),
                    negotiationId = negotiationId,
                    at = millis,
                    description = description,
                    amountCents = current.amountCents,
                ),
            )
            database.exemplarDao().setStatus(
                current.exemplarId,
                ExemplarStatus.TRADED.name,
                millis,
            )
            auditLogWriter.record(
                entityType = EXEMPLAR_TYPE,
                entityId = current.exemplarId,
                field = "status",
                oldValue = ExemplarStatus.OWNED.name,
                newValue = ExemplarStatus.TRADED.name,
                source = "app",
                at = now,
            )
            received?.let {
                auditLogWriter.record(
                    entityType = EXEMPLAR_TYPE,
                    entityId = it.id,
                    field = "received_in_trade",
                    oldValue = null,
                    newValue = negotiationId,
                    source = "app",
                    at = now,
                )
            }
        }
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(mapError(error))
    }

    /**
     * Cancelar não mexe no `exemplar.status`: a reserva vivia na negociação, então o
     * exemplar volta sozinho ao estado anterior (FR-072). O evento de cancelamento fica.
     */
    override suspend fun cancel(negotiationId: String): DomainResult<Unit> = try {
        transactionRunner.inTransaction {
            val current = activeNegotiation(negotiationId)
            val now = Instant.now(clock)
            val millis = now.toEpochMilli()
            database.negotiationDao().update(
                current.copy(
                    status = NegotiationStatus.CANCELLED.name,
                    closedAt = millis,
                ),
            )
            database.negotiationEventDao().insert(
                NegotiationEventEntity(
                    id = UUID.randomUUID().toString(),
                    negotiationId = negotiationId,
                    at = millis,
                    description = "Negociação cancelada",
                    amountCents = null,
                ),
            )
            auditLogWriter.record(
                entityType = EXEMPLAR_TYPE,
                entityId = current.exemplarId,
                field = "negotiation_status",
                oldValue = current.status,
                newValue = NegotiationStatus.CANCELLED.name,
                source = "app",
                at = now,
            )
        }
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(mapError(error))
    }

    private suspend fun activeNegotiation(
        negotiationId: String,
    ): br.com.diecastcollector.data.db.entity.NegotiationEntity {
        val current = database.negotiationDao().findById(negotiationId)
            ?: throw DomainError.NotFound
        if (current.status !in ACTIVE_STATUSES) {
            throw DomainError.Validation(
                "Esta negociação já foi concluída ou cancelada.",
            )
        }
        return current
    }

    private fun mapError(error: Throwable): Throwable = when (error) {
        is DomainError -> error
        else -> DomainError.Storage(error.message ?: "Falha ao gravar a negociação.")
    }

    private companion object {
        const val EXEMPLAR_TYPE = "exemplar"
        const val CURRENCY = "BRL"
        val ACTIVE_STATUSES = setOf(
            NegotiationStatus.OPEN.name,
            NegotiationStatus.RESERVED.name,
        )
    }
}
