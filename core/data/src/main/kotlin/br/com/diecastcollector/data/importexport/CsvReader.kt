package br.com.diecastcollector.data.importexport

import java.text.Normalizer

/** Arquivo CSV que não pôde ser lido (vazio, sem cabeçalho ou malformado). */
public class CsvFormatException(message: String) : Exception(message)

/** Uma linha de dados do CSV, já associada ao número de linha física do arquivo. */
public data class CsvRow(
    val lineNumber: Int,
    val values: Map<String, String>,
)

/** Resultado da leitura de um CSV, antes de qualquer validação de domínio. */
public data class CsvDocument(
    val headers: List<String>,
    val rows: List<CsvRow>,
    val delimiter: Char,
    val hadBom: Boolean,
)

/**
 * Leitor CSV do contrato de importação (T111, FR-134–FR-139).
 *
 * Regras cobertas: UTF-8 com ou sem BOM, delimitador `,` ou `;` detectado pela primeira
 * linha, cabeçalho obrigatório, campos entre aspas (com `""` escapando aspas) e linhas em
 * branco ignoradas. Não grava nada e não conhece o domínio — só entrega texto estruturado.
 */
public object CsvReader {

    public fun read(text: String): CsvDocument {
        var content = text
        val hadBom = content.startsWith('\uFEFF')
        if (hadBom) content = content.substring(1)
        if (content.isBlank()) throw CsvFormatException("Arquivo CSV vazio.")

        val delimiter = detectDelimiter(content)
        val records = parseRecords(content, delimiter)
        if (records.isEmpty()) throw CsvFormatException("Cabeçalho ausente.")

        val headerRecord = records.first()
        if (headerRecord.fields.all { it.isBlank() }) {
            throw CsvFormatException("Cabeçalho ausente.")
        }
        val headers = headerRecord.fields.map(::normalizeHeader)

        val rows = ArrayList<CsvRow>(records.size - 1)
        for (record in records.drop(1)) {
            if (record.fields.all { it.isBlank() }) continue
            val values = LinkedHashMap<String, String>()
            headers.forEachIndexed { index, header ->
                if (header.isNotEmpty()) {
                    values[header] = record.fields.getOrElse(index) { "" }.trim()
                }
            }
            rows.add(CsvRow(record.startLine, values))
        }

        return CsvDocument(headers = headers, rows = rows, delimiter = delimiter, hadBom = hadBom)
    }

    /**
     * Minúsculas, sem acentos e com espaços vira sublinhado (contrato, seção "Colunas"):
     * `Ano Lançamento` e `ano_lancamento` chegam ao mesmo campo.
     */
    public fun normalizeHeader(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val withoutAccents = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return withoutAccents.lowercase()
            .replace(Regex("\\s+"), "_")
            .trim('_')
    }

    private data class RawRecord(val startLine: Int, val fields: List<String>)

    private fun detectDelimiter(text: String): Char {
        var commas = 0
        var semicolons = 0
        var inQuotes = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (char == '"') {
                if (inQuotes && index + 1 < text.length && text[index + 1] == '"') {
                    index += 2
                    continue
                }
                inQuotes = !inQuotes
                index++
                continue
            }
            if (!inQuotes && (char == '\n' || char == '\r')) break
            if (!inQuotes) {
                if (char == ',') commas++ else if (char == ';') semicolons++
            }
            index++
        }
        return if (semicolons > commas) ';' else ','
    }

    private fun parseRecords(text: String, delimiter: Char): List<RawRecord> {
        val records = ArrayList<RawRecord>()
        val fields = ArrayList<String>()
        val builder = StringBuilder()
        var inQuotes = false
        var line = 1
        var recordStartLine = 1
        var index = 0

        while (index < text.length) {
            val char = text[index]
            if (inQuotes) {
                when {
                    char == '"' ->
                        if (index + 1 < text.length && text[index + 1] == '"') {
                            builder.append('"')
                            index += 2
                        } else {
                            inQuotes = false
                            index++
                        }
                    else -> {
                        if (char == '\n') line++
                        builder.append(char)
                        index++
                    }
                }
                continue
            }

            when (char) {
                '"' -> {
                    inQuotes = true
                    index++
                }
                delimiter -> {
                    fields.add(builder.toString())
                    builder.clear()
                    index++
                }
                '\r' -> index++
                '\n' -> {
                    fields.add(builder.toString())
                    builder.clear()
                    records.add(RawRecord(recordStartLine, fields.toList()))
                    fields.clear()
                    line++
                    recordStartLine = line
                    index++
                }
                else -> {
                    builder.append(char)
                    index++
                }
            }
        }

        if (builder.isNotEmpty() || fields.isNotEmpty()) {
            fields.add(builder.toString())
            records.add(RawRecord(recordStartLine, fields.toList()))
        }
        return records
    }
}
