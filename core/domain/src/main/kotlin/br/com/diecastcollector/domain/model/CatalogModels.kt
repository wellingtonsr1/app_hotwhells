package br.com.diecastcollector.domain.model

import java.time.Instant

/** Fabricante da miniatura (FR-047). `country` nulo = desconhecido, nunca sentinela. */
public data class Manufacturer(
    val id: String,
    val name: String,
    val country: String? = null,
    val archivedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(name.isNotBlank()) { "Nome do fabricante é obrigatório." }
        require(name.trim().length <= MAX_NAME_LENGTH) { "Nome do fabricante excede 120 caracteres." }
    }
}

/** Marca impressa na miniatura (FR-014). */
public data class Brand(
    val id: String,
    val manufacturerId: String,
    val name: String,
    val archivedAt: Instant? = null,
)

/** Veículo do mundo real que a miniatura representa (FR-014). */
public data class RealVehicle(
    val id: String,
    val make: String,
    val model: String,
    val year: Int? = null,
    val country: String? = null,
)

/** Cor do catálogo; `hex` é opcional porque o nome é o dado primário (FR-017). */
public data class ColorItem(
    val id: String,
    val name: String,
    val hex: String? = null,
)

/** Modelo: o carro em si, independente de pintura e série (Art. I.1–I.2). */
public data class CarModel(
    val id: String,
    val manufacturerId: String,
    val name: String,
    val scale: String = DEFAULT_SCALE,
    val brandId: String? = null,
    val realVehicleId: String? = null,
    val bodyType: String? = null,
    val material: String? = null,
    val countryOfManufacture: String? = null,
    val notes: String? = null,
    val archivedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(name.isNotBlank()) { "Nome do modelo é obrigatório." }
        require(name.trim().length <= MAX_NAME_LENGTH) { "Nome do modelo excede 120 caracteres." }
        require(SCALE_PATTERN.matches(scale)) { "Escala deve estar no formato 1:N." }
    }
}

/**
 * Campo que o cadastro pode deixar em branco e que a lista de incompletos cobra (FR-029).
 *
 * A lista é curta de propósito: campo desconhecido é legítimo (FR-014 admite `NULL`), então
 * só entra aqui o que impede o colecionador de reconhecer a própria miniatura — a cor
 * principal e o ano. Anotar tudo o que é opcional transformaria o acervo inteiro em
 * pendência.
 */
public enum class MissingField {
    PRIMARY_COLOR,
    RELEASE_YEAR,
}

/**
 * Cadastro incompleto, listável para complementação posterior (FR-029).
 *
 * Não é uma entidade — é projeção de leitura, montada por duas consultas: modelo ativo sem
 * variante ativa (`variantId` nulo) e variante ativa sem cor principal ou sem ano
 * (`missingFields` preenchido). Enquanto não houver variante, o exemplar não pode existir.
 */
public data class IncompleteEntry(
    val modelId: String,
    val modelName: String,
    val manufacturerName: String,
    val scale: String,
    /** Preenchido quando o incompleto é a variante; `null` quando é o modelo sem variante. */
    val variantId: String? = null,
    val missingFields: List<MissingField> = emptyList(),
) {
    /** Chave de lista: um modelo sem variante e cada variante incompleta são linhas distintas. */
    public val key: String get() = variantId ?: modelId

    /** Modelo sem variante é o único caso que ainda pode ser retomado pela tela de cadastro. */
    public val hasVariant: Boolean get() = variantId != null
}

/** Série ou subcoleção declarada pelo fabricante (FR-048–FR-049). */
public data class Series(
    val id: String,
    val manufacturerId: String,
    val name: String,
    val parentId: String? = null,
    val kind: String? = null,
    val year: Int? = null,
    val archivedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * Variante: a combinação que o colecionador distingue — cor, rodas, base, decoração.
 * `fingerprint` é o hash estável que garante FR-039/FR-040 sem proibir variantes legítimas.
 */
public data class Variant(
    val id: String,
    val modelId: String,
    val fingerprint: String,
    val seriesId: String? = null,
    val primaryColorId: String? = null,
    val secondaryColorIds: List<String> = emptyList(),
    val wheelsType: String? = null,
    val baseType: String? = null,
    val decoration: String? = null,
    val releaseYear: Int? = null,
    val collectionNumber: String? = null,
    val seriesNumber: String? = null,
    val edition: String? = null,
    val manufacturerCode: String? = null,
    val packagingType: String? = null,
    val archivedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(fingerprint.isNotBlank()) { "Variante exige fingerprint." }
    }
}

/** Exemplar: a unidade física que existe na estante (Art. I.1). */
public data class Exemplar(
    val id: String,
    val variantId: String,
    val internalCode: String,
    val condition: Condition,
    val status: ExemplarStatus = ExemplarStatus.OWNED,
    val packagingCondition: PackagingCondition? = null,
    val locationId: String? = null,
    val notes: String? = null,
    val rowVersion: Long = 0L,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(internalCode.isNotBlank()) { "Código interno é obrigatório." }
        require(rowVersion >= 0L) { "row_version não pode ser negativo." }
    }
}

/**
 * Fotografia guardada em arquivo privado; o banco só conhece o caminho e o checksum (R.2).
 *
 * [path] é sempre o arquivo **exibido** — a derivada quando a foto foi editada, o próprio
 * original quando não foi. [originalPath] guarda o arquivo intocado para restauração: é o
 * que torna a edição não destrutiva sem acumular versões (FR-214, FR-215).
 */
public data class Photo(
    val id: String,
    val exemplarId: String,
    val angle: PhotoAngle,
    val path: String,
    val checksum: String,
    val isPrimary: Boolean = false,
    val sortOrder: Int = 0,
    val width: Int? = null,
    val height: Int? = null,
    val bytes: Long? = null,
    val capturedAt: Instant? = null,
    val originalPath: String? = null,
    val editedAt: Instant? = null,
    val editParams: String? = null,
) {
    init {
        require(path.isNotBlank()) { "Caminho da foto é obrigatório." }
        require(checksum.isNotBlank()) { "Checksum da foto é obrigatório." }
        require(sortOrder >= 0) { "Ordem da foto não pode ser negativa." }
    }

    /** Houve edição: existe original preservado e o caminho exibido é a derivada. */
    public val isEdited: Boolean get() = originalPath != null
}

internal const val MAX_NAME_LENGTH: Int = 120
internal const val DEFAULT_SCALE: String = "1:64"
internal val SCALE_PATTERN: Regex = Regex("""^1:[0-9]{1,3}$""")
