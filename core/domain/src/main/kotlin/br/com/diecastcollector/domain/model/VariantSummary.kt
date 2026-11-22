package br.com.diecastcollector.domain.model

/**
 * Linha da lista de variantes.
 *
 * É uma projeção de leitura, não uma entidade: junta o que a lista precisa mostrar
 * (modelo, fabricante, cor, ano) e a contagem de exemplares — contada na consulta,
 * nunca armazenada (Art. I.2).
 *
 * Não tem método de formatação de propósito: texto e plural são da camada de
 * apresentação, que é quem tem acesso a `strings.xml` (Art. IV). O domínio expõe os dados.
 */
public data class VariantSummary(
    val variantId: String,
    val modelName: String,
    val manufacturerName: String,
    val colorName: String?,
    val releaseYear: Int?,
    val exemplarCount: Int,
    /** Caminho relativo da foto principal de um dos exemplares, para a miniatura da lista. */
    val primaryPhotoPath: String? = null,
)
