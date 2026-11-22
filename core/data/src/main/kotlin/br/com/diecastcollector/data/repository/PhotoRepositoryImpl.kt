package br.com.diecastcollector.data.repository

import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.AuditLogWriter
import br.com.diecastcollector.data.db.TransactionRunner
import br.com.diecastcollector.data.db.entity.PhotoEntity
import br.com.diecastcollector.data.mapper.toDomain
import br.com.diecastcollector.data.storage.ImageStore
import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.Photo
import br.com.diecastcollector.domain.model.PhotoAngle
import br.com.diecastcollector.domain.repository.PhotoRepository
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repositório de fotografias (T045, T046 - US2, FR-020, FR-021).
 *
 * Garante que:
 * 1. O arquivo binário é comprimido e armazenado pelo ImageStore fora do banco (R.2).
 * 2. O registro no Room grava metadados (checksum, dimensões, ângulo, ordem).
 * 3. A primeira foto adicionada torna-se automaticamente a principal (isPrimary = true).
 * 4. A alteração de foto principal desmarca a anterior em transação atômica.
 * 5. A remoção apaga o registro no banco e o arquivo do armazenamento interno.
 */
@Singleton
public class PhotoRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val imageStore: ImageStore,
    private val transactionRunner: TransactionRunner,
    private val auditLogWriter: AuditLogWriter,
    private val clock: Clock,
) : PhotoRepository {

    override fun observeByExemplar(exemplarId: String): Flow<List<Photo>> =
        database.photoDao().observeByExemplar(exemplarId).map { rows ->
            rows.map { it.toDomain() }
        }

    override suspend fun add(
        exemplarId: String,
        angle: PhotoAngle,
        source: ByteArray,
    ): DomainResult<Photo> = try {
        val stored = imageStore.save(
            exemplarId = exemplarId,
            source = ByteArrayInputStream(source),
            angle = angle.name,
        )

        val photo = transactionRunner.inTransaction {
            val count = database.photoDao().countByExemplar(exemplarId)
            val isFirst = count == 0
            val entity = PhotoEntity(
                id = UUID.randomUUID().toString(),
                exemplarId = exemplarId,
                angle = angle.name,
                path = stored.relativePath,
                checksum = stored.checksum,
                isPrimary = isFirst,
                sortOrder = count,
                width = stored.width,
                height = stored.height,
                bytes = stored.bytes,
                capturedAt = Instant.now(clock).toEpochMilli(),
            )
            database.photoDao().insert(entity)
            auditLogWriter.record("photo", entity.id, "created", null, entity.path, "app")
            entity.toDomain()
        }
        Result.success(photo)
    } catch (error: Throwable) {
        Result.failure(if (error is DomainError) error else DomainError.Storage(error.message ?: "Falha ao gravar foto."))
    }

    override suspend fun setPrimary(exemplarId: String, photoId: String): DomainResult<Unit> = try {
        transactionRunner.inTransaction {
            database.photoDao().clearPrimary(exemplarId)
            database.photoDao().markPrimary(photoId)
            auditLogWriter.record("photo", photoId, "set_primary", null, exemplarId, "app")
        }
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(if (error is DomainError) error else DomainError.Storage(error.message ?: "Falha ao definir foto principal."))
    }

    override suspend fun remove(photoId: String): DomainResult<Unit> = try {
        val photo = database.photoDao().findById(photoId)
            ?: return Result.failure(DomainError.NotFound)

        transactionRunner.inTransaction {
            database.photoDao().delete(photoId)
            auditLogWriter.record("photo", photoId, "deleted", photo.path, null, "app")
        }
        imageStore.delete(photo.path)
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(if (error is DomainError) error else DomainError.Storage(error.message ?: "Falha ao excluir foto."))
    }
}
