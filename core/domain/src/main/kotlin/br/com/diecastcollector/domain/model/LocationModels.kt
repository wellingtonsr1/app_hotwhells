package br.com.diecastcollector.domain.model

/**
 * Nó da árvore de localizações (FR-088–FR-089).
 *
 * `exemplarCount` é quantos exemplares possuídos estão **nesta** localização; o total da
 * subárvore é derivado, não armazenado, para não existir contador que divirja do banco.
 */
public data class LocationNode(
    val location: Location,
    val exemplarCount: Int = 0,
    val children: List<LocationNode> = emptyList(),
) {
    public val subtreeExemplarCount: Int
        get() = exemplarCount + children.sumOf { it.subtreeExemplarCount }

    /** Profundidade na árvore, usada pela tela para indentar sem consulta extra. */
    public val id: String get() = location.id
}

/**
 * Montagem da árvore a partir da lista plana.
 *
 * Hierarquia é dado do usuário: pais arquivados deixam filhos sem raiz, e um ciclo só
 * poderia existir por escrita externa. Os dois casos viram raiz em vez de sumir da tela —
 * esconder uma localização que ainda guarda exemplares seria pior do que mostrá-la solta.
 */
public object LocationTree {

    public fun build(
        locations: List<Location>,
        counts: Map<String, Int> = emptyMap(),
    ): List<LocationNode> {
        val known = locations.map { it.id }.toSet()
        val byParent = locations.groupBy { it.parentId }
        val rendered = mutableSetOf<String>()

        fun render(location: Location): LocationNode {
            rendered += location.id
            val children = byParent[location.id].orEmpty()
                .filter { it.id !in rendered }
                .sortedBy { it.name.lowercase() }
                .map { render(it) }
            return LocationNode(
                location = location,
                exemplarCount = counts[location.id] ?: 0,
                children = children,
            )
        }

        val roots = locations
            .filter { it.parentId == null || it.parentId !in known }
            .sortedBy { it.name.lowercase() }
            .map { render(it) }
            .toMutableList()

        // Sobras (ciclo ou pai arquivado no meio da cadeia) entram como raiz. O filtro é
        // relido a cada iteração: renderizar uma sobra pode renderizar as outras junto.
        locations.sortedBy { it.name.lowercase() }
            .forEach { location ->
                if (location.id !in rendered) roots += render(location)
            }

        return roots
    }
}

/**
 * O que fazer com os exemplares ao excluir a localização (FR-093).
 *
 * Não existe cascata: ou o colecionador escolhe um destino, ou manda arquivar os
 * exemplares, ou a exclusão é recusada. `None` só é aceito quando não há exemplar.
 */
public sealed interface LocationArchivePolicy {
    public data object None : LocationArchivePolicy
    public data class MoveTo(val targetLocationId: String) : LocationArchivePolicy
    public data object ArchiveExemplars : LocationArchivePolicy
}

/** Linha do inventário por localização (FR-094–FR-095). */
public data class InventoryItem(
    val exemplarId: String,
    val variantId: String,
    val title: String,
    val internalCode: String,
    val condition: Condition,
    val locationName: String?,
)
