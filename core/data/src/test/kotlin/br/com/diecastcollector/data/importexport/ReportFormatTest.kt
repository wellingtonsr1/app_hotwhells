package br.com.diecastcollector.data.importexport

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.ZoneId
import java.util.zip.ZipInputStream
import org.junit.Test

/**
 * Formato dos relatórios (T109, FR-124–FR-131).
 *
 * Confere o PDF gerado (páginas, cabeçalho, filtros e totais) e lê o XLSX de volta com um
 * leitor independente do escritor, garantindo que o pacote OOXML está íntegro. O CSV é
 * conferido como texto, com BOM e cabeçalho do contrato.
 */
class ReportFormatTest {

    private val rows = (1..60).map { index ->
        PdfReportRow(
            internalCode = "HW-%04d".format(index),
            model = "Modelo $index",
            variant = "Variante $index",
            condition = "SEALED",
            photos = index % 3,
        )
    }

    @Test
    fun `PDF tem titulo data filtros paginacao e total`() {
        val result = PdfReportWriter.write(
            rows = rows,
            filters = PdfReportFilters(modelName = "Twin Mill", condition = "SEALED"),
            generatedAt = java.util.Date.from(Instant.parse("2026-11-21T10:00:00Z")),
        )

        assertThat(result.totalExemplares).isEqualTo(60)
        assertThat(result.pageCount).isAtLeast(2)

        val text = String(result.bytes, Charsets.ISO_8859_1)
        assertThat(text).startsWith("%PDF-1.4")
        assertThat(text).contains("Inventário de Miniaturas")
        assertThat(text).contains("Gerado em:")
        assertThat(text).contains("Modelo: Twin Mill")
        assertThat(text).contains("Condição: SEALED")
        assertThat(text).contains("Código")
        assertThat(text).contains("Página 1")
        assertThat(text).contains("Página ${result.pageCount}")
        assertThat(text).contains("Total de exemplares: 60")
        assertThat(text).contains("%%EOF")
    }

    @Test
    fun `PDF com lista vazia ainda tem cabecalho e total zero`() {
        val result = PdfReportWriter.write(rows = emptyList())

        assertThat(result.pageCount).isEqualTo(1)
        assertThat(result.totalExemplares).isEqualTo(0)
        assertThat(String(result.bytes, Charsets.ISO_8859_1))
            .contains("Total de exemplares: 0")
    }

    @Test
    fun `XLSX e lido de volta por leitor independente`() {
        val table = ReportTable(
            name = "Inventário",
            headers = listOf("Código", "Modelo", "Observação"),
            rows = listOf(
                listOf("HW-1", "Twin Mill", "com \"aspas\" & símbolos <ok>"),
                listOf("HW-2", "Bone Shaker", "linha, com vírgula"),
            ),
        )

        val bytes = ReportWriters.writeXlsx(listOf(table))

        val parts = entryNames(bytes)
        assertThat(parts).containsAtLeast(
            "[Content_Types].xml",
            "_rels/.rels",
            "xl/workbook.xml",
            "xl/_rels/workbook.xml.rels",
            "xl/worksheets/sheet1.xml",
        )

        val read = XlsxReader.toCsv(bytes)
        assertThat(read.ignoredSheetCount).isEqualTo(0)
        val lines = read.csv.trim().lines()
        assertThat(lines[0]).isEqualTo("Código,Modelo,Observação")
        assertThat(lines[1]).isEqualTo("HW-1,Twin Mill,\"com \"\"aspas\"\" & símbolos <ok>\"")
        assertThat(lines[2]).isEqualTo("HW-2,Bone Shaker,\"linha, com vírgula\"")
    }

    @Test
    fun `XLSX aceita varias abas e nomes invalidos`() {
        val bytes = ReportWriters.writeXlsx(
            listOf(
                ReportTable("Coleções/Séries", listOf("Nome"), listOf(listOf("Premium"))),
                ReportTable("", listOf("Total"), listOf(listOf("1"))),
            ),
        )

        val read = XlsxReader.toCsv(bytes)
        assertThat(read.ignoredSheetCount).isEqualTo(1)
        assertThat(read.csv.lines().first()).isEqualTo("Nome")
        assertThat(read.csv).contains("Premium")
    }

    @Test
    fun `CSV sai em UTF-8 com BOM e aspas somente quando precisa`() {
        val bytes = ReportWriters.writeCsv(
            ReportTable(
                name = "Inventário",
                headers = listOf("Código", "Variante"),
                rows = listOf(
                    listOf("HW-1", "não informado"),
                    listOf("HW-2", "azul, com faixa"),
                ),
            ),
        )

        assertThat(bytes.copyOfRange(0, 3)).isEqualTo(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        assertThat(text.lines().first()).isEqualTo("Código,Variante")
        assertThat(text).contains("HW-1,não informado")
        assertThat(text).contains("HW-2,\"azul, com faixa\"")
    }

    @Test
    fun `nome do arquivo traz tipo e data de geracao`() {
        val name = ReportWriters.fileName(
            reportSlug = "colecao-inventario",
            format = ReportFormat.XLSX,
            generatedAt = Instant.parse("2026-11-21T10:11:12Z"),
            zone = ZoneId.of("UTC"),
        )

        assertThat(name).isEqualTo("colecao-inventario-20261121-101112.xlsx")
        assertThat(
            ReportWriters.fileName("Coleção Inventário", ReportFormat.CSV, Instant.EPOCH, ZoneId.of("UTC")),
        ).isEqualTo("colecao-inventario-19700101-000000.csv")
    }

    private fun entryNames(bytes: ByteArray): Set<String> {
        val names = mutableSetOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) names += entry.name
            }
        }
        return names
    }
}
