package br.com.diecastcollector.domain.model

/** Ordenações do acervo (FR-118). */
public enum class CatalogSort { NAME, YEAR, ACQUISITION_DATE, VALUE }

/**
 * Filtros de busca do acervo (FR-114–FR-122).
 *
 * Listas vazias significam "sem restrição"; a interseção é entre categorias e a união
 * dentro da mesma categoria (FR-117). O padrão mostra só o acervo ativo (`OWNED`) porque
 * a pergunta típica é "eu tenho esse?"; vendidos e arquivados entram quando o colecionador
 * escolhe o status.
 */
public data class SearchFilters(
    val query: String = "",
    val manufacturerIds: Set<String> = emptySet(),
    val modelIds: Set<String> = emptySet(),
    val seriesIds: Set<String> = emptySet(),
    val collectionIds: Set<String> = emptySet(),
    val years: Set<Int> = emptySet(),
    val colorIds: Set<String> = emptySet(),
    val scales: Set<String> = emptySet(),
    val conditions: Set<Condition> = emptySet(),
    val variantIds: Set<String> = emptySet(),
    val statuses: Set<ExemplarStatus> = setOf(ExemplarStatus.OWNED),
    val locationIds: Set<String> = emptySet(),
    val priceMinCents: Long? = null,
    val priceMaxCents: Long? = null,
    val sort: CatalogSort = CatalogSort.NAME,
) {
    public val hasActiveCriteria: Boolean
        get() = query.isNotBlank() ||
            manufacturerIds.isNotEmpty() ||
            modelIds.isNotEmpty() ||
            seriesIds.isNotEmpty() ||
            collectionIds.isNotEmpty() ||
            years.isNotEmpty() ||
            colorIds.isNotEmpty() ||
            scales.isNotEmpty() ||
            conditions.isNotEmpty() ||
            variantIds.isNotEmpty() ||
            locationIds.isNotEmpty() ||
            priceMinCents != null ||
            priceMaxCents != null ||
            statuses != setOf(ExemplarStatus.OWNED)
}

/** Uma linha de resultado: o exemplar, não a variante — é ele que se procura. */
public data class SearchResultItem(
    val exemplarId: String,
    val variantId: String,
    val internalCode: String,
    val modelName: String,
    val manufacturerName: String,
    val seriesName: String?,
    val collectionName: String?,
    val primaryColorName: String?,
    val decoration: String?,
    val realVehicleName: String?,
    val manufacturerCode: String?,
    val scale: String,
    val releaseYear: Int?,
    val condition: Condition,
    val status: ExemplarStatus,
    val locationName: String?,
    val priceCents: Long?,
    val acquisitionDate: IsoDate?,
    /** Caminho relativo da foto principal, para a visualização em galeria. */
    val primaryPhotoPath: String? = null,
)

/** Opção de filtro com rótulo pronto para exibição. */
public data class FilterOption(
    val id: String,
    val label: String,
)

/** Opções que a tela precisa conhecer para montar os filtros. */
public data class SearchFilterOptions(
    val manufacturers: List<FilterOption> = emptyList(),
    val models: List<FilterOption> = emptyList(),
    val series: List<FilterOption> = emptyList(),
    val collections: List<FilterOption> = emptyList(),
    val colors: List<FilterOption> = emptyList(),
    val locations: List<FilterOption> = emptyList(),
    val scales: List<String> = emptyList(),
    val years: List<Int> = emptyList(),
)
