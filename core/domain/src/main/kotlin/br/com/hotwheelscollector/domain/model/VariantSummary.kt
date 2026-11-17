package br.com.hotwheelscollector.domain.model

/**
 * Linha da lista de variantes.
 *
 * É uma projeção de leitura, não uma entidade: junta o que a lista precisa mostrar
 * (modelo, fabricante, cor, ano) e a contagem de exemplares — contada na consulta,
 * nunca armazenada (Art. I.2).
 */
public data class VariantSummary(
    val variantId: String,
    val modelName: String,
    val manufacturerName: String,
    val colorName: String?,
    val releaseYear: Int?,
    val exemplarCount: Int,
) {
    /** Texto curto para a segunda linha do cartão: "Hot Wheels · 2024 · 2 exemplares". */
    public fun subtitle(): String = buildList {
        add(manufacturerName)
        releaseYear?.let { add(it.toString()) }
        add(
            when (exemplarCount) {
                0 -> "nenhum exemplar"
                1 -> "1 exemplar"
                else -> "$exemplarCount exemplares"
            },
        )
    }.joinToString(" · ")
}
