package br.com.diecastcollector.data.importexport

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Test

/**
 * T152 / SC-007 — gerar PDF de 1.000 exemplares em menos de 10 s, com totais iguais aos do
 * painel.
 *
 * Gera o PDF de verdade com `PdfDocument` (Robolectric) e confere tempo, número de páginas,
 * total de exemplares (o mesmo que o painel conta) e que os bytes são um PDF válido.
 */
class PdfPerformanceTest {

    @Test
    fun `gera PDF de 1000 exemplares dentro do tempo e com total correto`() {
        val rows = buildRows(1000)

        val start = System.nanoTime()
        val result = PdfReportWriter.write(rows)
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)

        // Total desenhado é o total do painel para o mesmo conjunto.
        assertThat(result.totalExemplares).isEqualTo(1000)
        // 1.000 linhas não cabem em uma página A4.
        assertThat(result.pageCount).isGreaterThan(1)
        // PDF válido começa com a assinatura "%PDF".
        assertThat(result.bytes).isNotEmpty()
        assertThat(result.sizeBytes).isGreaterThan(0)
        assertThat(String(result.bytes, 0, 4, Charsets.ISO_8859_1)).isEqualTo("%PDF")

        // SC-007: menos de 10 s.
        assertThat(elapsedMs).isLessThan(10_000L)
    }

    @Test
    fun `PDF de poucas linhas cabe em uma pagina`() {
        val result = PdfReportWriter.write(buildRows(10))

        assertThat(result.totalExemplares).isEqualTo(10)
        assertThat(result.pageCount).isEqualTo(1)
    }

    private fun buildRows(count: Int): List<PdfReportRow> = List(count) { index ->
        val n = index + 1
        PdfReportRow(
            internalCode = "HW-" + n.toString().padStart(6, '0'),
            model = "Modelo $n",
            variant = "Variante $n",
            condition = if (n % 3 == 0) "solto" else "lacrado",
            photos = n % 4,
        )
    }
}
