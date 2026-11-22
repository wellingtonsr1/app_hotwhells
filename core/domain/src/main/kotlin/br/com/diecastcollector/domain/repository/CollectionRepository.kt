package br.com.diecastcollector.domain.repository

import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.Collection
import br.com.diecastcollector.domain.model.CollectionItem
import br.com.diecastcollector.domain.model.CollectionItemStatus
import br.com.diecastcollector.domain.model.CollectionProgress
import kotlinx.coroutines.flow.Flow

/**
 * Coleções do colecionador (FR-049–FR-056).
 *
 * A hierarquia é livre (`parentId`) e protegida contra ciclo na escrita: uma coleção não
 * pode virar descendente de si mesma. Nada é apagado — arquivar mantém itens e vínculos.
 */
public interface CollectionRepository {

    /** Lista ativa com progresso já calculado, uma linha por coleção. */
    public fun observeAll(): Flow<List<CollectionProgress>>

    /** Itens de uma coleção, com rótulo resolvido e contagem de exemplares possuídos. */
    public fun observeItems(collectionId: String): Flow<List<CollectionItemStatus>>

    public suspend fun findById(id: String): Collection?

    public suspend fun create(
        name: String,
        description: String? = null,
        parentId: String? = null,
        manufacturerId: String? = null,
    ): DomainResult<Collection>

    /**
     * Renomeia/reorganiza sem perder vínculos (FR-056). Recusa `parentId` que criaria
     * ciclo, inclusive quando o ciclo passa por mais de um nível.
     */
    public suspend fun update(
        id: String,
        name: String,
        description: String? = null,
        parentId: String? = null,
        manufacturerId: String? = null,
    ): DomainResult<Unit>

    /** Arquiva a coleção; bloqueia quando há subcoleções ativas (FR-055). */
    public suspend fun archive(id: String): DomainResult<Unit>

    /** Item no nível da variante; recusa a mesma variante duas vezes na coleção. */
    public suspend fun addVariant(
        collectionId: String,
        variantId: String,
        expected: Int = 1,
    ): DomainResult<CollectionItem>

    /** Item no nível do modelo (o modelo inteiro), para coleções que não listam variantes. */
    public suspend fun addModel(
        collectionId: String,
        modelId: String,
        expected: Int = 1,
    ): DomainResult<CollectionItem>

    /** Marca/desmarca "não colecionável": o item sai do denominador, não da coleção. */
    public suspend fun setNonCollectible(itemId: String, nonCollectible: Boolean): DomainResult<Unit>

    /** Remove o item da coleção; a variante e o modelo continuam no catálogo. */
    public suspend fun removeItem(itemId: String): DomainResult<Unit>
}
