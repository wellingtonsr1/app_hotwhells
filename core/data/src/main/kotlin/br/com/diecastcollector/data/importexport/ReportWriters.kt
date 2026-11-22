package br.com.diecastcollector.data.importexport

import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Formatos de relatório oferecidos ao usuário (FR-124–FR-126). */
public enum class ReportFormat(
    public val extension: String,
    public val mimeType: String,
) {
    PDF("pdf", "application/pdf"),
    CSV("csv", "text/csv"),
    XLSX(
        "xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    ),
}

/** Uma tabela de relatório: nome da aba, cabeçalhos e linhas já formatadas em texto. */
public data class ReportTable(
    val name: String,
    val headers: List<String>,
    val rows: List<List<String>>,
)

/**
 * Escritores CSV e XLSX dos relatórios (T116, FR-124/FR-125/FR-131).
 *
 * O CSV sai em UTF-8 com BOM (abre direto no Excel) e usa aspas apenas quando o campo
 * precisa. O XLSX é OOXML mínimo escrito com [ZipOutputStream]: workbook, relações e uma
 * planilha por tabela, com todo o conteúdo em `inlineStr` — sem `sharedStrings`, sem
 * biblioteca externa e sem arquivo intermediário em disco.
 *
 * O nome do arquivo carrega o tipo do relatório e a data de geração (FR-131).
 */
public object ReportWriters {

    private const val BOM = "\uFEFF"
    private const val XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
    private const val SPREADSHEET_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val RELATIONSHIPS_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val PACKAGE_RELATIONSHIPS_NS =
        "http://schemas.openxmlformats.org/package/2006/relationships"
    private const val WORKSHEET_REL_TYPE =
        "http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
    private const val WORKBOOK_REL_TYPE =
        "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"

    private val FILE_STAMP: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT)

    /** `inventario-20261121-011500.csv`, com data/hora local de geração (FR-131). */
    public fun fileName(
        reportSlug: String,
        format: ReportFormat,
        generatedAt: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val stamp = FILE_STAMP.withZone(zone).format(generatedAt)
        val slug = java.text.Normalizer.normalize(reportSlug.trim(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9-]+"), "-")
            .trim('-')
            .ifEmpty { "relatorio" }
        return "$slug-$stamp.${format.extension}"
    }

    /** CSV em UTF-8 com BOM, uma linha por registro. */
    public fun writeCsv(table: ReportTable): ByteArray {
        val builder = StringBuilder(BOM)
        builder.append(table.headers.joinToString(",") { escapeCsv(it) }).append("\r\n")
        table.rows.forEach { row ->
            builder.append(row.joinToString(",") { escapeCsv(it) }).append("\r\n")
        }
        return builder.toString().toByteArray(Charsets.UTF_8)
    }

    /** Planilha OOXML com uma aba por tabela. */
    public fun writeXlsx(tables: List<ReportTable>): ByteArray {
        val sheets = tables.ifEmpty { listOf(ReportTable("Relatório", emptyList(), emptyList())) }
            .mapIndexed { index, table ->
                Sheet(index + 1, sanitizeSheetName(table.name, index + 1), table)
            }

        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putText("[Content_Types].xml", contentTypes(sheets))
            zip.putText("_rels/.rels", packageRelationships())
            zip.putText("xl/workbook.xml", workbook(sheets))
            zip.putText("xl/_rels/workbook.xml.rels", workbookRelationships(sheets))
            sheets.forEach { sheet ->
                zip.putText("xl/worksheets/sheet${sheet.number}.xml", worksheet(sheet.table))
            }
        }
        return output.toByteArray()
    }

    private data class Sheet(val number: Int, val name: String, val table: ReportTable)

    private fun contentTypes(sheets: List<Sheet>): String {
        val overrides = buildString {
            append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-")
            append("officedocument.spreadsheetml.sheet.main+xml\"/>")
            sheets.forEach { sheet ->
                append("<Override PartName=\"/xl/worksheets/sheet${sheet.number}.xml\" ContentType=\"")
                append("application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
            }
        }
        return "$XML_DECLARATION<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            overrides +
            "</Types>"
    }

    private fun packageRelationships(): String =
        "$XML_DECLARATION<Relationships xmlns=\"$PACKAGE_RELATIONSHIPS_NS\">" +
            "<Relationship Id=\"rId1\" Type=\"$WORKBOOK_REL_TYPE\" Target=\"xl/workbook.xml\"/>" +
            "</Relationships>"

    private fun workbook(sheets: List<Sheet>): String {
        val entries = sheets.joinToString("") { sheet ->
            "<sheet name=\"${escapeXml(sheet.name)}\" sheetId=\"${sheet.number}\" r:id=\"rId${sheet.number}\"/>"
        }
        return "$XML_DECLARATION<workbook xmlns=\"$SPREADSHEET_NS\" xmlns:r=\"$RELATIONSHIPS_NS\">" +
            "<sheets>$entries</sheets></workbook>"
    }

    private fun workbookRelationships(sheets: List<Sheet>): String {
        val entries = sheets.joinToString("") { sheet ->
            "<Relationship Id=\"rId${sheet.number}\" Type=\"$WORKSHEET_REL_TYPE\" " +
                "Target=\"worksheets/sheet${sheet.number}.xml\"/>"
        }
        return "$XML_DECLARATION<Relationships xmlns=\"$PACKAGE_RELATIONSHIPS_NS\">$entries</Relationships>"
    }

    private fun worksheet(table: ReportTable): String {
        val rows = StringBuilder()
        var rowNumber = 1

        if (table.headers.isNotEmpty()) {
            rows.append(row(rowNumber, table.headers))
            rowNumber++
        }
        table.rows.forEach { values ->
            rows.append(row(rowNumber, values))
            rowNumber++
        }

        return "$XML_DECLARATION<worksheet xmlns=\"$SPREADSHEET_NS\"><sheetData>$rows</sheetData></worksheet>"
    }

    private fun row(rowNumber: Int, values: List<String>): String {
        val cells = values.mapIndexed { index, value ->
            "<c r=\"${columnName(index)}$rowNumber\" t=\"inlineStr\"><is><t xml:space=\"preserve\">" +
                "${escapeXml(value)}</t></is></c>"
        }.joinToString("")
        return "<row r=\"$rowNumber\">$cells</row>"
    }

    /** A, B, …, Z, AA, AB, … como no Excel. */
    private fun columnName(index: Int): String {
        var value = index
        val builder = StringBuilder()
        do {
            builder.insert(0, ('A' + value % 26))
            value = value / 26 - 1
        } while (value >= 0)
        return builder.toString()
    }

    /** Nome de aba seguro: até 31 caracteres, sem `[]:*?/\`. */
    private fun sanitizeSheetName(raw: String, fallbackNumber: Int): String {
        val cleaned = raw.replace(Regex("[\\[\\]:*?/\\\\]"), " ").trim().take(31)
        return cleaned.ifEmpty { "Relatório $fallbackNumber" }
    }

    private fun escapeCsv(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private fun escapeXml(value: String): String = buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                // Controle inválido em XML 1.0 é descartado em vez de corromper o arquivo.
                else -> if (char >= ' ' || char == '\t' || char == '\n' || char == '\r') append(char)
            }
        }
    }

    private fun ZipOutputStream.putText(path: String, content: String) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }
}
