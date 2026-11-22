package br.com.diecastcollector.domain.usecase

import br.com.diecastcollector.domain.model.CollectionItemStatus
import br.com.diecastcollector.domain.model.CollectionProgress
import br.com.diecastcollector.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Progresso das coleções (T069, FR-052–FR-054).
 *
 * O cálculo em si vive em `CollectionProgressCalculator`, para que a mesma regra valha
 * para a lista de coleções e para o painel; aqui fica só a exposição como fluxo.
 */
public class CollectionProgressUseCase(
    private val collections: CollectionRepository,
) {
    public fun observe(): Flow<List<CollectionProgress>> = collections.observeAll()
}

/**
 * Itens que faltam em uma coleção (T069, FR-054).
 *
 * "Faltar" é `ownedUnits < expected`: um item com 1 de 2 unidades ainda falta. Item
 * marcado como não colecionável nunca aparece, porque não é dívida.
 */
public class MissingItemsUseCase(
    private val collections: CollectionRepository,
) {
    public fun observe(collectionId: String): Flow<List<CollectionItemStatus>> =
        collections.observeItems(collectionId)
            .map { items -> items.filter { it.isMissing } }
}
