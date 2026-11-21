package br.com.hotwheelscollector.data.importexport

import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Uma linha do relatório de inventário, já pronta para desenho. */
public data class PdfReportRow(
    val internalCode: String,
    val model: String,
    val variant: String,
    val condition: String,
    val photos: Int,
)

/** Filtros aplicados ao relatório; aparecem no cabeçalho (FR-127). */
public data class PdfReportFilters(
    val modelName: String? = null,
    val variantName: String? = null,
    val condition: String? = null,
)

/**
 * Resultado da geração: bytes do PDF, número de páginas e total de exemplares desenhado.
 * O total é o mesmo que o painel usa, o que permite conferir SC-007 sem reler o PDF.
 */
public data class PdfReportResult(
    val bytes: ByteArray,
    val pageCount: Int,
    val totalExemplares: Int,
) {
    public val sizeBytes: Int get() = bytes.size
}

/**
 * Gerador de PDF do relatório de inventário (T117, FR-126–FR-128).
 *
 * Escreve um PDF 1.4 válido diretamente, sem dependência de plataforma nem de biblioteca
 * externa: o arquivo abre no aparelho e também é verificável na suíte de testes JVM (o
 * `android.graphics.pdf.PdfDocument` depende de código nativo e não roda no Robolectric).
 * O conteúdo é o exigido por FR-127: título, data/hora de geração, filtros aplicados,
 * cabeçalho de colunas, paginação com número de página e totais no fim.
 *
 * Acentos usam WinAnsiEncoding (Latin-1) com as fontes base Helvetica, presentes em todo
 * leitor de PDF — nada é baixado nem embutido.
 */
public object PdfReportWriter {

    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGIN = 36f
    private const val ROW_HEIGHT = 16f
    private const val TABLE_HEADER_HEIGHT = 22f
    private const val TITLE_TOP = 42f
    private const val SUBTITLE_TOP = 58f

    private val COLUMNS = arrayOf("Código", "Modelo", "Variante", "Condição", "Fotos")
    private val COLUMN_WIDTHS = floatArrayOf(0.22f, 0.30f, 0.22f, 0.15f, 0.11f)

    public fun write(
        rows: List<PdfReportRow>,
        filters: PdfReportFilters = PdfReportFilters(),
        generatedAt: Date = Date(),
        title: String = "Inventário de Miniaturas",
    ): PdfReportResult {
        val dateText = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(generatedAt)

        val pages = ArrayList<String>()
        var content = StringBuilder()
        var pageNumber = 1

        var topY = drawPageHeader(content, title, dateText, filters, pageNumber)
        topY = drawTableHeader(content, topY)

        rows.forEachIndexed { index, row ->
            if (topY + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) {
                pages.add(content.toString())
                pageNumber++
                content = StringBuilder()
                topY = drawPageHeader(content, title, dateText, filters, pageNumber)
                topY = drawTableHeader(content, topY)
            }
            drawRow(content, topY, row, shaded = index % 2 == 0)
            topY += ROW_HEIGHT
        }

        drawTotals(content, topY + 18f, rows.size)
        pages.add(content.toString())

        val bytes = assemblePdf(pages)
        return PdfReportResult(bytes = bytes, pageCount = pageNumber, totalExemplares = rows.size)
    }

    private fun drawPageHeader(
        content: StringBuilder,
        title: String,
        dateText: String,
        filters: PdfReportFilters,
        pageNumber: Int,
    ): Float {
        text(content, MARGIN, TITLE_TOP, 18, bold = true, value = title)
        text(content, MARGIN, SUBTITLE_TOP, 11, bold = false, value = "Gerado em: $dateText")

        var top = SUBTITLE_TOP + 14f
        filters.modelName?.let {
            text(content, MARGIN, top, 11, bold = false, value = "Modelo: $it"); top += 13f
        }
        filters.variantName?.let {
            text(content, MARGIN, top, 11, bold = false, value = "Variante: $it"); top += 13f
        }
        filters.condition?.let {
            text(content, MARGIN, top, 11, bold = false, value = "Condição: $it"); top += 13f
        }

        val pageLabel = "Página $pageNumber"
        val approxWidth = pageLabel.length * 11 * 0.5f
        text(content, PAGE_WIDTH - MARGIN - approxWidth, TITLE_TOP, 11, bold = false, value = pageLabel)

        return top + 6f
    }

    private fun drawTableHeader(content: StringBuilder, top: Float): Float {
        fillRect(content, MARGIN, top, PAGE_WIDTH - 2 * MARGIN, TABLE_HEADER_HEIGHT, gray = 0.85f)

        val usableWidth = PAGE_WIDTH - 2 * MARGIN
        var x = MARGIN
        for (index in COLUMNS.indices) {
            text(content, x + 4f, top + 15f, 11, bold = true, value = COLUMNS[index])
            x += usableWidth * COLUMN_WIDTHS[index]
        }
        return top + TABLE_HEADER_HEIGHT
    }

    private fun drawRow(content: StringBuilder, top: Float, row: PdfReportRow, shaded: Boolean) {
        if (shaded) {
            fillRect(content, MARGIN, top, PAGE_WIDTH - 2 * MARGIN, ROW_HEIGHT, gray = 0.96f)
        }
        val values = arrayOf(
            row.internalCode,
            row.model,
            row.variant,
            row.condition,
            row.photos.toString(),
        )
        val usableWidth = PAGE_WIDTH - 2 * MARGIN
        var x = MARGIN
        for (index in values.indices) {
            val cellWidth = usableWidth * COLUMN_WIDTHS[index]
            text(content, x + 4f, top + 12f, 10, bold = false, value = ellipsize(values[index], cellWidth - 8f))
            x += cellWidth
        }
    }

    private fun drawTotals(content: StringBuilder, top: Float, total: Int) {
        text(content, MARGIN, top, 12, bold = true, value = "Total de exemplares: $total")
    }

    private fun text(
        content: StringBuilder,
        x: Float,
        topBaseline: Float,
        size: Int,
        bold: Boolean,
        value: String,
    ) {
        val y = PAGE_HEIGHT - topBaseline
        content.append("BT /").append(if (bold) "F2" else "F1").append(' ').append(size)
            .append(" Tf 1 0 0 1 ").append(fmt(x)).append(' ').append(fmt(y))
            .append(" Tm (").append(escape(value)).append(") Tj ET\n")
    }

    private fun fillRect(content: StringBuilder, x: Float, top: Float, width: Float, height: Float, gray: Float) {
        val lowerLeftY = PAGE_HEIGHT - (top + height)
        content.append("q ").append(gray).append(" g ").append(fmt(x)).append(' ')
            .append(fmt(lowerLeftY)).append(' ').append(fmt(width)).append(' ')
            .append(fmt(height)).append(" re f Q\n")
    }

    private fun fmt(value: Float): String = String.format(Locale.ROOT, "%.2f", value)

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")

    private fun ellipsize(value: String, maxWidth: Float): String {
        // Aproximação de Helvetica: ~0,5 * tamanho por caractere, tamanho 10.
        val approxCharWidth = 10 * 0.5f
        val maxChars = (maxWidth / approxCharWidth).toInt()
        if (maxChars <= 0) return ""
        if (value.length <= maxChars) return value
        return if (maxChars <= 3) "..." else value.substring(0, maxChars - 3) + "..."
    }

    private fun assemblePdf(pageContents: List<String>): ByteArray {
        val objects = ArrayList<ByteArray>()
        val pageCount = pageContents.size

        // 1: catálogo, 2: páginas, 3: F1, 4: F2; depois pares (page, content).
        objects.add("<< /Type /Catalog /Pages 2 0 R >>".toIso())
        val kids = (0 until pageCount).joinToString(" ") { "${5 + it * 2} 0 R" }
        objects.add("<< /Type /Pages /Kids [$kids] /Count $pageCount >>".toIso())
        objects.add(
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>".toIso(),
        )
        objects.add(
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>".toIso(),
        )

        pageContents.forEach { content ->
            val contentNumber = objects.size + 2
            objects.add(
                ("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${fmt(PAGE_WIDTH)} ${fmt(PAGE_HEIGHT)}] " +
                    "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> " +
                    "/Contents $contentNumber 0 R >>").toIso(),
            )
            val contentBytes = content.toByteArray(Charsets.ISO_8859_1)
            val stream = ByteArrayOutputStream().apply {
                write("<< /Length ${contentBytes.size} >>\nstream\n".toByteArray(Charsets.ISO_8859_1))
                write(contentBytes)
                write("\nendstream".toByteArray(Charsets.ISO_8859_1))
            }.toByteArray()
            objects.add(stream)
        }

        val output = ByteArrayOutputStream()
        output.write("%PDF-1.4\n".toByteArray(Charsets.ISO_8859_1))

        val offsets = IntArray(objects.size)
        objects.forEachIndexed { index, body ->
            offsets[index] = output.size()
            output.write("${index + 1} 0 obj\n".toByteArray(Charsets.ISO_8859_1))
            output.write(body)
            output.write("\nendobj\n".toByteArray(Charsets.ISO_8859_1))
        }

        val xrefOffset = output.size()
        val total = objects.size + 1
        output.write("xref\n0 $total\n".toByteArray(Charsets.ISO_8859_1))
        output.write("0000000000 65535 f \n".toByteArray(Charsets.ISO_8859_1))
        for (offset in offsets) {
            val entry = String.format(Locale.ROOT, "%010d 00000 n \n", offset)
            output.write(entry.toByteArray(Charsets.ISO_8859_1))
        }
        output.write(
            ("trailer\n<< /Size $total /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF")
                .toByteArray(Charsets.ISO_8859_1),
        )

        return output.toByteArray()
    }

    private fun String.toIso(): ByteArray = toByteArray(Charsets.ISO_8859_1)
}
