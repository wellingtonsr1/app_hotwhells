package br.com.diecastcollector.domain.model

/**
 * Item de coleção já resolvido para leitura: rótulo pronto (fabricante · modelo · cor) e
 * quantos exemplares possuídos existem para o alvo (a variante ou o modelo inteiro).
 *
 * `expected` é quantas unidades a coleção espera daquele item; `nonCollectible` marca o
 * item que existe no catálogo mas não entra na conta (FR-055). As duas colunas existem
 * desde o schema v1 e são o que dá sentido a "possuídos / esperados".
 */
public data class CollectionItemStatus(
    val itemId: String,
    val collectionId: String,
    val variantId: String? = null,
    val modelId: String? = null,
    val label: String,
    val expected: Int = 1,
    val nonCollectible: Boolean = false,
    val ownedExemplars: Int = 0,
    /** Foto de capa de um exemplar possuído do alvo, quando existe alguma. */
    val photoPath: String? = null,
) {
    init {
        require((variantId == null) != (modelId == null)) {
            "Item de coleção deve apontar para uma variante ou para um modelo, nunca ambos."
        }
        require(expected >= 0) { "Quantidade esperada não pode ser negativa." }
        require(ownedExemplars >= 0) { "Contagem de exemplares não pode ser negativa." }
    }

    /** Unidades que entram no denominador; item não colecionável não entra. */
    public val units: Int get() = if (nonCollectible) 0 else expected

    /** Unidades atendidas: não passa do esperado, porque sobra não é progresso. */
    public val ownedUnits: Int get() = if (nonCollectible) 0 else minOf(ownedExemplars, expected)

    public val missingUnits: Int get() = (units - ownedUnits).coerceAtLeast(0)

    public val isMissing: Boolean get() = missingUnits > 0

    public val isOwned: Boolean get() = ownedUnits > 0
}

/**
 * Regra única de progresso, usada pela lista de coleções, pela tela de detalhe e pelo
 * painel. Ter uma implementação só evita o caso clássico de dois números diferentes para
 * a mesma coleção em duas telas.
 */
public object CollectionProgressCalculator {

    public fun calculate(
        collection: Collection,
        items: List<CollectionItemStatus>,
    ): CollectionProgress {
        val relevant = items.filterNot { it.nonCollectible }
        return CollectionProgress(
            id = collection.id,
            name = collection.name,
            owned = relevant.sumOf { it.ownedUnits },
            known = relevant.sumOf { it.units },
            parentId = collection.parentId,
            itemCount = items.size,
            missing = relevant
                .filter { it.isMissing }
                .sortedBy { it.label.lowercase() },
            // A capa da coleção é a foto de um item possuído; sem nenhuma, um item
            // ainda faltante serve de referência visual.
            photoPath = items.firstOrNull { it.isOwned && it.photoPath != null }?.photoPath
                ?: items.firstNotNullOfOrNull { it.photoPath },
        )
    }
}
