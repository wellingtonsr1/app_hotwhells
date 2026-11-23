package br.com.diecastcollector.domain.repository

import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.WishlistItem
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
    /**
     * Foto a mostrar na lista: a que a pessoa anexou ao desejo ou, sem ela, a de capa de
     * um exemplar do mesmo alvo quando o acervo já tem algum.
     */
    val photoPath: String? = null,
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
     * Grava a foto anexada ao desejo e devolve o caminho relativo.
     *
     * Funciona antes de o desejo existir no banco: quem procura um carro que ainda não
     * tem não teria exemplar para pendurar a foto, e a imagem só precisa da pasta do id.
     */
    public suspend fun storePhoto(wishId: String, source: ByteArray): DomainResult<String>

    /** Apaga o arquivo de uma foto que o desejo não usa mais (melhor esforço). */
    public suspend fun deletePhoto(relativePath: String)

    /**
     * Conversão em aquisição em **uma transação**: o exemplar (quando [draft] é informado)
     * e o vínculo com o desejo nascem juntos ou não nascem. Prioridade, valores e data de
     * criação do desejo são preservados; o evento devolve o exemplar criado, se houver.
     *
     * Desejo sem variante é convertido com [draft] nulo: vira `ACQUIRED` sem exemplar, o
     * que a tela explica, em vez de inventar um vínculo.
     */
    public suspend fun convertToAcquisition(
        id: String,
        draft: NewExemplar?,
    ): DomainResult<AddExemplarOutcome?>
}
