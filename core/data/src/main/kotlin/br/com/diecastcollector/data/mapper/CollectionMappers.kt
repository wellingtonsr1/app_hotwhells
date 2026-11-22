package br.com.diecastcollector.data.mapper

import br.com.diecastcollector.data.db.dao.CollectionItemRow
import br.com.diecastcollector.domain.model.CollectionItemStatus

/**
 * Linha de `collection_item` já com rótulo e possuídos → modelo de leitura.
 *
 * `CollectionEntity`/`CollectionItemEntity` já têm mapeador em `SystemMappers.kt`; aqui
 * fica só a projeção de progresso, que não é entidade.
 */
internal fun CollectionItemRow.toDomain(): CollectionItemStatus = CollectionItemStatus(
    itemId = id,
    collectionId = collectionId,
    variantId = variantId,
    modelId = modelId,
    label = label,
    expected = expected,
    nonCollectible = nonCollectible != 0,
    ownedExemplars = owned,
    photoPath = photoPath,
)
