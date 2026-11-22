package br.com.diecastcollector.data.repository

import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.storage.ImageStore
import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.repository.DataControlRepository
import br.com.diecastcollector.domain.repository.UserDataCounts
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Exclusão total dos dados (T146, FR-157, Art. III.7).
 *
 * `clearAllTables` é do Room e roda numa transação só: as FKs em `RESTRICT` não impedem a
 * limpeza completa (as chaves são adiadas até o fim), e as tabelas de histórico saem junto
 * — não sobra registro órfão nem contador divergente. O índice FTS é limpo como tabela; os
 * gatilhos e o índice parcial continuam instalados para a próxima escrita.
 *
 * A ordem importa: primeiro o banco (fonte da verdade), depois as fotos. Se apagar as
 * imagens falhar, o acervo já não aponta para elas e uma nova tentativa remove o resto —
 * o contrário deixaria linhas de `photo` apontando para arquivos que não existem.
 */
@Singleton
public class DataControlRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val imageStore: ImageStore,
) : DataControlRepository {

    override fun observeCounts(): Flow<UserDataCounts> =
        database.dataControlDao().observeCounts().map { row ->
            UserDataCounts(
                exemplars = row.exemplars,
                models = row.models,
                photos = row.photos,
                collections = row.collections,
                wishlistItems = row.wishlistItems,
                locations = row.locations,
            )
        }

    override suspend fun deleteAll(): DomainResult<Unit> = withContext(Dispatchers.IO) {
        try {
            database.clearAllTables()
            imageStore.deleteAll()
            Result.success(Unit)
        } catch (error: Throwable) {
            Result.failure(
                DomainError.Storage(error.message ?: "Falha ao excluir os dados do acervo."),
            )
        }
    }
}
