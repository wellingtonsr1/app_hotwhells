package br.com.diecastcollector.domain.model

import java.time.Instant

/** Coleção criada pelo usuário, com hierarquia livre (FR-049–FR-056). */
public data class Collection(
    val id: String,
    val name: String,
    val description: String? = null,
    val parentId: String? = null,
    val manufacturerId: String? = null,
    val archivedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(name.isNotBlank()) { "Nome da coleção é obrigatório." }
    }
}

/**
 * Item previsto (ou possuído) em uma coleção.
 * Exatamente um entre `variantId` e `modelId` é preenchido (FR-051).
 */
public data class CollectionItem(
    val id: String,
    val collectionId: String,
    val variantId: String? = null,
    val modelId: String? = null,
    val expected: Int = 1,
    val nonCollectible: Int = 0,
    val notes: String? = null,
) {
    init {
        require((variantId == null) != (modelId == null)) {
            "Item de coleção deve apontar para uma variante ou para um modelo, nunca ambos."
        }
        require(expected >= 0) { "Quantidade esperada não pode ser negativa." }
        require(nonCollectible == 0 || nonCollectible == 1) { "non_collectible aceita apenas 0 ou 1." }
    }
}

/** Desejo de compra (FR-057–FR-064). */
public data class WishlistItem(
    val id: String,
    val priority: Int = 2,
    val status: WishlistStatus = WishlistStatus.WANTED,
    val modelId: String? = null,
    val variantId: String? = null,
    val freeText: String? = null,
    val maxPrice: MoneyCents? = null,
    val foundPrice: MoneyCents? = null,
    val store: String? = null,
    val referenceUrl: String? = null,
    val notes: String? = null,
    /** Foto anexada ao desejo; o exemplar desejado ainda não existe para ter foto própria. */
    val photoPath: String? = null,
    val convertedExemplarId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(priority in 1..3) { "Prioridade deve estar entre 1 e 3." }
        require(modelId != null || variantId != null || !freeText.isNullOrBlank()) {
            "Desejo precisa de modelo, variante ou texto livre."
        }
        require(referenceUrl == null || referenceUrl.startsWith("http://") || referenceUrl.startsWith("https://")) {
            "URL de referência deve começar com http:// ou https://."
        }
    }
}

/** Nível físico onde os exemplares ficam (FR-088–FR-095). */
public data class Location(
    val id: String,
    val type: LocationType,
    val name: String,
    val parentId: String? = null,
    val archivedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(name.isNotBlank()) { "Nome da localização é obrigatório." }
        require(parentId != id) { "Localização não pode ser pai de si mesma." }
    }
}

/** Movimentação entre localizações; imutável por definição (FR-091). */
public data class Movement(
    val id: String,
    val exemplarId: String,
    val at: Instant,
    val fromLocationId: String? = null,
    val toLocationId: String? = null,
    val note: String? = null,
)
