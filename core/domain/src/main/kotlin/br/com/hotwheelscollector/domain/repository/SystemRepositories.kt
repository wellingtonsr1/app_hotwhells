package br.com.hotwheelscollector.domain.repository

import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.AppSetting
import br.com.hotwheelscollector.domain.model.HistoryEntry
import br.com.hotwheelscollector.domain.model.SearchFilterOptions
import br.com.hotwheelscollector.domain.model.SearchFilters
import br.com.hotwheelscollector.domain.model.SearchResultItem
import kotlinx.coroutines.flow.Flow

/** Trilha de auditoria: leitura por entidade; escrita só pelo `AuditLogWriter`. */
public interface HistoryRepository {
    public fun observeFor(entityType: String, entityId: String): Flow<List<HistoryEntry>>
}

/** Preferências em chave/valor (FR-145, FR-171). */
public interface SettingRepository {
    public suspend fun value(key: String): String?
    public fun observeValue(key: String): Flow<String?>
    public suspend fun put(setting: AppSetting): DomainResult<Unit>
    public suspend fun remove(key: String): DomainResult<Unit>
}

/**
 * Busca do acervo sobre o índice FTS4, com diacríticos removidos (FR-114–FR-122).
 *
 * `observeResults` devolve exemplares já filtrados e ordenados; a apresentação só monta
 * os cartões. `variantIds`/`rebuild` são operações diretas no índice, usadas por testes e
 * por manutenção.
 */
public interface SearchRepository {
    public fun observeResults(filters: SearchFilters): Flow<List<SearchResultItem>>
    public fun observeOptions(): Flow<SearchFilterOptions>
    public suspend fun variantIds(query: String, limit: Int = 50): List<String>
    public suspend fun rebuild(): DomainResult<Unit>
}
