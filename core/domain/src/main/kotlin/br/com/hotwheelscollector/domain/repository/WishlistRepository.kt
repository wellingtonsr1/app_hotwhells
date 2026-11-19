package br.com.hotwheelscollector.domain.repository

import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.WishlistItem
import kotlinx.coroutines.flow.Flow

/**
 * Desejo com o rótulo já resolvido para a lista.
 *
 * O título vem do vínculo (fabricante e modelo da variante) ou do texto livre; resolvê-lo
 * no banco evita uma consulta por linha e mantém a tela livre de regra de apresentação.
 */
public data class WishlistEntry(
    val item: WishlistItem,
    val title: String,
)

/** Lista de desejos: intenção de compra que não conta como acervo (FR-057–FR-064). */
public interface WishlistRepository {

    /** Em aberto primeiro, depois por prioridade e data de criação. */
    public fun observeAll(): Flow<List<WishlistEntry>>

    public suspend fun findById(id: String): WishlistItem?

    public suspend fun add(item: WishlistItem): DomainResult<WishlistItem>

    public suspend fun update(item: WishlistItem): DomainResult<WishlistItem>

    /** Desistência explícita: o desejo continua no histórico, fora da lista em aberto. */
    public suspend fun dismiss(id: String): DomainResult<Unit>

    /**
     * Conversão em aquisição. O exemplar é criado por quem chama (quando o desejo aponta
     * para uma variante) e apenas vinculado aqui; prioridade, valores e data de criação
     * do desejo são preservados.
     */
    public suspend fun markAcquired(id: String, exemplarId: String?): DomainResult<Unit>
}
