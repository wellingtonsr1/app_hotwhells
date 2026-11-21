package br.com.hotwheelscollector.data.importexport

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

/** Resultado da leitura de um XLSX: a primeira aba convertida em CSV e o que foi ignorado. */
public data class XlsxReadResult(
    val csv: String,
    val ignoredSheetCount: Int,
    val warnings: List<String>,
)

/**
 * Leitor XLSX do contrato de importação (T112, FR-134–FR-139).
 *
 * Abre o pacote OOXML (ZIP) e lê **apenas a primeira aba**, convertendo-a em CSV para
 * reaproveitar o mesmo pipeline de validação do CSV. Abas adicionais são ignoradas e
 * reportadas como aviso. Sem dependência externa: usa `ZipInputStream` e o parser XML da
 * plataforma.
 */
public object XlsxReader {

    private val SHEET_PATTERN = Regex("""xl/worksheets/sheet(\d+)\.xml""")

    public fun toCsv(bytes: ByteArray): XlsxReadResult {
        val entries = readEntries(bytes)

        val sheets = entries.keys
            .filter { SHEET_PATTERN.matches(it) }
            .sortedBy { SHEET_PATTERN.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() ?: 0 }
        if (sheets.isEmpty()) throw CsvFormatException("Planilha XLSX sem aba de dados.")

        val sharedStrings = entries["xl/sharedStrings.xml"]?.let(::parseSharedStrings).orEmpty()
        val rows = parseSheet(entries.getValue(sheets.first()), sharedStrings)

        val ignored = sheets.size - 1
        val warnings = if (ignored > 0) listOf("Abas adicionais ignoradas: $ignored") else emptyList()
        return XlsxReadResult(csv = toCsvText(rows), ignoredSheetCount = ignored, warnings = warnings)
    }

    private fun readEntries(bytes: ByteArray): Map<String, ByteArray> {
        val result = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    result[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
            }
        }
        return result
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val document = newDocument(bytes)
        val nodes = document.getElementsByTagName("si")
        return buildList(nodes.length) {
            for (index in 0 until nodes.length) {
                val text = collectText(nodes.item(index))
                add(unescapeXml(text))
            }
        }
    }

    private fun parseSheet(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val document = newDocument(bytes)
        val rowNodes = document.getElementsByTagName("row")
        val rows = ArrayList<List<String>>()

        for (rowIndex in 0 until rowNodes.length) {
            val rowElement = rowNodes.item(rowIndex) as? Element ?: continue
            val cellNodes = rowElement.childElements()
            val cells = sortedMapOf<Int, String>()
            for (cell in cellNodes) {
                if (cell.tagName != "c") continue
                val ref = cell.getAttribute("r")
                val column = columnIndex(ref)
                val type = cell.getAttribute("t")
                val value = when (type) {
                    "s" -> {
                        val index = childText(cell, "v")?.toIntOrNull()
                        sharedStrings.getOrNull(index ?: -1).orEmpty()
                    }
                    "inlineStr" -> childElement(cell, "is")?.let(::collectText).orEmpty()
                    else -> childText(cell, "v").orEmpty()
                }
                cells[column] = unescapeXml(value)
            }
            if (cells.isEmpty()) continue
            val width = (cells.keys.maxOrNull() ?: 0) + 1
            rows.add(List(width) { cells[it].orEmpty() })
        }
        return rows
    }

    private fun toCsvText(rows: List<List<String>>): String {
        if (rows.isEmpty()) throw CsvFormatException("Planilha XLSX sem linhas.")
        val builder = StringBuilder()
        rows.forEach { row ->
            builder.append(row.joinToString(",") { escapeCsv(it) }).append('\n')
        }
        return builder.toString()
    }

    private fun escapeCsv(value: String): String {
        val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuoting) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private fun columnIndex(reference: String): Int {
        var column = 0
        var index = 0
        while (index < reference.length && reference[index].isLetter()) {
            column = column * 26 + (reference[index].uppercaseChar() - 'A' + 1)
            index++
        }
        return if (column == 0) 0 else column - 1
    }

    private fun newDocument(bytes: ByteArray): org.w3c.dom.Document =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
        }.newDocumentBuilder().parse(ByteArrayInputStream(bytes))

    private fun Element.childElements(): List<Element> {
        val result = ArrayList<Element>()
        val children = childNodes
        for (index in 0 until children.length) {
            (children.item(index) as? Element)?.let(result::add)
        }
        return result
    }

    private fun childElement(parent: Element, name: String): Element? {
        val children = parent.childNodes
        for (index in 0 until children.length) {
            val node = children.item(index)
            if (node is Element && node.tagName == name) return node
        }
        return null
    }

    private fun childText(parent: Element, name: String): String? =
        childElement(parent, name)?.textContent

    private fun collectText(node: Node): String {
        if (node is Element && (node.tagName == "t" || node.tagName == "v" || node.tagName == "si" || node.tagName == "is")) {
            return node.textContent.orEmpty()
        }
        val builder = StringBuilder()
        val children = node.childNodes
        for (index in 0 until children.length) {
            val child = children.item(index)
            if (child is Element && child.tagName == "t") {
                builder.append(child.textContent.orEmpty())
            }
        }
        return builder.toString()
    }

    private fun unescapeXml(value: String): String = value
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
}
