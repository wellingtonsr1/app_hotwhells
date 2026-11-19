package br.com.hotwheelscollector.domain.model

/**
 * Resumo financeiro do acervo, calculado na leitura (FR-077–FR-086).
 *
 * Tudo em centavos e `Long` cru: são somas de muitos exemplares e podem passar do limite
 * de um item (FR-076). O custo médio considera só exemplares com preço informado; os sem
 * valor aparecem em [exemplarsWithoutValue] para a tela poder dizer quantos ficaram fora.
 */
public data class FinanceSummary(
    val investedCents: Long = 0L,
    val estimatedCents: Long = 0L,
    val ownedExemplars: Int = 0,
    val exemplarsWithValue: Int = 0,
    val exemplarsWithoutValuation: Int = 0,
    val latestValuationDate: String? = null,
) {
    /** Estimado − investido; pode ser negativo e a apresentação é que cuida do sinal. */
    public val differenceCents: Long get() = estimatedCents - investedCents

    public val exemplarsWithoutValue: Int
        get() = (ownedExemplars - exemplarsWithValue).coerceAtLeast(0)

    /** `null` quando nenhum exemplar tem valor: média de zero itens não é R$ 0,00. */
    public val averageCostCents: Long?
        get() = if (exemplarsWithValue <= 0) null else investedCents / exemplarsWithValue
}

/** Gastos agrupados por fabricante, série ou coleção (FR-079–FR-080). */
public data class SpendingSlice(
    /** `null` quando a variante não tem série, por exemplo; a tela escolhe o rótulo. */
    val label: String?,
    val cents: Long,
)

/** Gastos de um mês (`AAAA-MM`), já em ordem decrescente (FR-078). */
public data class PeriodSpending(
    val yearMonth: String,
    val cents: Long,
)

/** Intervalo livre de gastos; pontas nulas significam "sem limite" (FR-078). */
public data class FinancePeriod(
    val from: IsoDate? = null,
    val to: IsoDate? = null,
) {
    public val isEmpty: Boolean get() = from == null && to == null
}
