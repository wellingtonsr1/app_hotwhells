package br.com.hotwheelscollector.domain.model

/** Estado físico do exemplar (FR-018). Persistido pelo nome do enum. */
public enum class Condition { SEALED, LOOSE, DAMAGED, RESTORED, INCOMPLETE }

/** Estado da embalagem, quando existe (FR-019). */
public enum class PackagingCondition { SEALED, OPENED, DAMAGED, NONE }

/** Ângulos de fotografia previstos (FR-020, FR-097). */
public enum class PhotoAngle { FRONT, BACK, LEFT, RIGHT, TOP, BASE, PACKAGING, DETAIL }

/** Situação do exemplar no acervo (Art. I.1). */
public enum class ExemplarStatus { OWNED, SOLD, TRADED, ARCHIVED }

/**
 * Nível de localização (FR-088–FR-089).
 * O rótulo pt-BR vive só na apresentação: ENVIRONMENT = Ambiente, RACK = Estante etc.
 */
public enum class LocationType {
    ENVIRONMENT, CABINET, RACK, SHELF, CRATE, DRAWER, DISPLAY, COMPARTMENT, CUSTOM
}

public enum class NegotiationType { SALE, TRADE }

public enum class NegotiationStatus { OPEN, RESERVED, CLOSED, CANCELLED }

public enum class WishlistStatus { WANTED, ACQUIRED, DISMISSED }

/**
 * Origem do reconhecimento assistido (FR-102) e da correspondência no catálogo (FR-110).
 *
 * `CATALOG` não é leitura nova: marca a correspondência encontrada no acervo local, para
 * a tela poder mostrar de onde veio o dado.
 */
public enum class RecognitionSource { PHOTO, OCR, BARCODE, CATALOG }
