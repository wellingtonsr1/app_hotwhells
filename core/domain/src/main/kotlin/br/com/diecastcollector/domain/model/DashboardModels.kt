package br.com.diecastcollector.domain.model

/**
 * Estado atual da coleção, calculado na leitura (FR-001–FR-011).
 *
 * Nenhum destes números é coluna: todos saem de consulta, para não existir contador que
 * possa divergir do que está gravado (Art. I.2). Valores em centavos, nunca `Double`.
 *
 * Os totais ficam em `Long` cru, e não em `MoneyCents`, porque `MoneyCents` representa o
 * valor de **um** item (limite de 9.999.999,99 por FR-076); a soma de muitos exemplares
 * pode passar disso, e o painel não pode estourar por causa da soma.
 */
public data class DashboardSnapshot(
    val ownedExemplars: Int = 0,
    val distinctModels: Int = 0,
    val distinctVariants: Int = 0,
    val distinctManufacturers: Int = 0,
    val distinctSeries: Int = 0,
    val activeCollections: Int = 0,
    val variantsWithDuplicates: Int = 0,
    val wantedItems: Int = 0,
    val investedCents: Long = 0L,
    val estimatedCents: Long = 0L,
    val exemplarsWithoutValuation: Int = 0,

    /** Data da avaliação mais recente entre os exemplares possuídos (FR-086). */
    val latestValuationDate: String? = null,
    val latestAcquisitions: List<DashboardEntry> = emptyList(),
    val latestEntries: List<DashboardEntry> = emptyList(),
    val collectionProgress: List<CollectionProgress> = emptyList(),
) {
    /** Estimado − investido, em centavos. Pode ser negativo; quem exibe formata o sinal. */
    val estimatedDifferenceCents: Long
        get() = estimatedCents - investedCents

    /** Coleção e lista de desejos vazias: não há o que resumir nem o que listar. */
    val isEmpty: Boolean
        get() = ownedExemplars == 0 && wantedItems == 0 && activeCollections == 0
}

/** Linha curta do painel: aquisição recente ou cadastro recente. */
public data class DashboardEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val date: String,
)

/**
 * Progresso de uma coleção: quantas unidades existem e quantas já estão na estante.
 *
 * `owned`/`known` são unidades, não linhas: um item com `expected = 2` pesa dois no
 * denominador. Item marcado como não colecionável não entra em nenhum dos dois.
 * `missing` só é preenchido quando a tela pediu o detalhe; o painel usa só os números.
 */
public data class CollectionProgress(
    val id: String,
    val name: String,
    val owned: Int,
    val known: Int,
    val parentId: String? = null,
    val itemCount: Int = 0,
    val missing: List<CollectionItemStatus> = emptyList(),
    /** Foto de capa vinda de um item da coleção, para o cartão da lista. */
    val photoPath: String? = null,
) {
    /** Percentual inteiro de 0 a 100; coleção sem itens não conta como completa. */
    val percent: Int
        get() = if (known <= 0) {
            0
        } else {
            ((owned.toLong() * 100L) / known.toLong()).toInt().coerceIn(0, 100)
        }

    val isComplete: Boolean
        get() = known > 0 && owned >= known

    /** Coleção sem itens conhecidos não é "0%": não há o que completar. */
    val hasKnownItems: Boolean
        get() = known > 0
}
