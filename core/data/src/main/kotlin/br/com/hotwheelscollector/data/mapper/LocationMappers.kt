package br.com.hotwheelscollector.data.mapper

import br.com.hotwheelscollector.data.db.dao.InventoryRow
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.InventoryItem

/**
 * Linha do inventário → modelo de leitura.
 *
 * Título é "Fabricante · Modelo", montado aqui porque a consulta já resolveu os nomes;
 * a apresentação só exibe o que veio. Condição desconhecida não derruba a lista: o item
 * continua no inventário com `LOOSE`, que é o estado físico mais comum — sumir com o
 * exemplar seria pior do que errar o rótulo.
 */
internal fun InventoryRow.toDomain(): InventoryItem = InventoryItem(
    exemplarId = exemplarId,
    variantId = variantId,
    title = listOf(manufacturerName, modelName).joinToString(" · "),
    internalCode = internalCode,
    condition = runCatching { Condition.valueOf(condition) }.getOrDefault(Condition.LOOSE),
    locationName = locationName,
)
