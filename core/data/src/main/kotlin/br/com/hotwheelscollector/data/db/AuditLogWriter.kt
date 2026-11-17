package br.com.hotwheelscollector.data.db

import androidx.room.withTransaction
import br.com.hotwheelscollector.data.db.dao.HistoryDao
import br.com.hotwheelscollector.data.db.entity.HistoryEntryEntity
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Grava a trilha de auditoria (Art. I.8, FR-027).
 *
 * A trilha é appende-only: este é o único ponto de escrita, e não existe operação
 * de edição ou remoção. `old_value`/`new_value` guardam o texto como o usuário viu.
 */
@Singleton
public class AuditLogWriter @Inject constructor(
    private val historyDao: HistoryDao,
) {

    public suspend fun record(
        entityType: String,
        entityId: String,
        field: String,
        oldValue: String?,
        newValue: String?,
        source: String? = null,
        at: Instant = Instant.now(),
    ) {
        historyDao.insert(
            HistoryEntryEntity(
                id = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                field = field,
                oldValue = oldValue,
                newValue = newValue,
                at = at.toEpochMilli(),
                source = source,
            ),
        )
    }

    /** Registra várias alterações da mesma operação em um único insert. */
    public suspend fun recordAll(entries: List<PendingAuditEntry>) {
        if (entries.isEmpty()) return
        val at = Instant.now().toEpochMilli()
        historyDao.insertAll(
            entries.map { entry ->
                HistoryEntryEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = entry.entityType,
                    entityId = entry.entityId,
                    field = entry.field,
                    oldValue = entry.oldValue,
                    newValue = entry.newValue,
                    at = at,
                    source = entry.source,
                )
            },
        )
    }
}

/** Alteração pendente de auditoria, para operações que gravam várias linhas juntas. */
public data class PendingAuditEntry(
    val entityType: String,
    val entityId: String,
    val field: String,
    val oldValue: String?,
    val newValue: String?,
    val source: String? = null,
)

/**
 * Executa blocos dentro de uma transação do Room.
 *
 * Existe para que as regras transacionais de `data-model.md` (mover exemplar, definir
 * foto principal, converter desejo em aquisição) escrevam tudo ou nada — sem espalhar
 * `withTransaction` cru pela camada de repositório.
 */
@Singleton
public class TransactionRunner @Inject constructor(
    private val database: AppDatabase,
) {

    public suspend fun <T> inTransaction(block: suspend () -> T): T =
        database.withTransaction { block() }
}
