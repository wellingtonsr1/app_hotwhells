package br.com.diecastcollector.data.importexport

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/** Resultado da validação de uma linha, com os problemas encontrados e a marca de duplicata. */
public data class ImportRowResult(
    val line: Int,
    val values: Map<String, String>,
    val problems: List<ImportProblem>,
    val duplicate: Boolean,
) {
    public val isValid: Boolean get() = problems.isEmpty()
    public val isInvalid: Boolean get() = problems.isNotEmpty()
}

/**
 * Pré-visualização da importação (fases 1–5 do contrato, SC-006).
 *
 * Nada é gravado: o pipeline lê, valida e classifica as linhas para que a tela mostre os
 * totais e o relatório de erros antes de qualquer confirmação (FR-136).
 */
public data class ImportPreview(
    val totalRows: Int,
    val validRows: Int,
    val invalidRows: Int,
    val duplicateRows: Int,
    val unknownColumns: List<String>,
    val rows: List<ImportRowResult>,
) {
    public val problems: List<ImportProblem> get() = rows.flatMap { it.problems }

    /** As 20 primeiras linhas, para a pré-visualização da tela. */
    public val firstRows: List<ImportRowResult> get() = rows.take(20)

    public fun errorReportCsv(): String = ImportErrorReport.toCsv(problems)
}

/**
 * Pipeline de leitura + validação da importação por CSV (T111/T113/T114).
 *
 * Cobre as fases de leitura, parse, validação, detecção de duplicata e pré-visualização do
 * contrato. A aplicação transacional no banco (fase 7) fica fora deste objeto: a tela confirma
 * a pré-visualização antes de gravar.
 */
public object CsvImportPipeline {

    private val KNOWN_COLUMNS: Set<String> = setOf(
        "fabricante", "marca", "modelo", "veiculo_real", "ano_veiculo_real", "escala", "serie",
        "colecao", "ano_lancamento", "numero_colecao", "codigo_fabricante", "cor_principal",
        "cores_secundarias", "tipo_carroceria", "material", "rodas", "base", "decoracao",
        "condicao", "condicao_embalagem", "codigo_interno", "data_aquisicao", "preco", "frete",
        "desconto", "moeda", "loja", "vendedor", "localizacao", "observacoes",
    )

    private val CONDITIONS: Set<String> =
        setOf("lacrado", "solto", "danificado", "restaurado", "incompleto")
    private val PACKAGING: Set<String> =
        setOf("lacrada", "aberta", "danificada", "sem embalagem")

    private val BR_DATE: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    private const val MAX_NAME_LENGTH = 120
    private const val MAX_NOTES_LENGTH = 2000

    /**
     * Lê e valida o CSV. [existingInternalCodes] permite classificar como duplicata um
     * `codigo_interno` que já existe no banco; códigos repetidos dentro do próprio arquivo
     * também contam como duplicata.
     */
    public fun preview(
        text: String,
        existingInternalCodes: Set<String> = emptySet(),
    ): ImportPreview {
        val document = CsvReader.read(text)
        val unknownColumns = document.headers.filter { it.isNotEmpty() && it !in KNOWN_COLUMNS }
        val seenCodes = HashSet<String>()

        val results = document.rows.map { row ->
            validateRow(row, existingInternalCodes, seenCodes)
        }

        return ImportPreview(
            totalRows = results.size,
            validRows = results.count { it.isValid },
            invalidRows = results.count { it.isInvalid },
            duplicateRows = results.count { it.duplicate },
            unknownColumns = unknownColumns,
            rows = results,
        )
    }

    private fun validateRow(
        row: CsvRow,
        existingInternalCodes: Set<String>,
        seenCodes: MutableSet<String>,
    ): ImportRowResult {
        // Colunas desconhecidas são avisadas na pré-visualização e ignoradas aqui (contrato).
        val values = row.values.filterKeys { it in KNOWN_COLUMNS }
        val problems = ArrayList<ImportProblem>()

        fun problem(field: String, code: ImportErrorCode, message: String, value: String = values[field].orEmpty()) {
            problems += ImportProblem(row.lineNumber, field, code, message, value)
        }

        if (values["fabricante"].isNullOrBlank()) {
            problem("fabricante", ImportErrorCode.MANDATORY_EMPTY, "Campo obrigatório vazio")
        }
        if (values["modelo"].isNullOrBlank()) {
            problem("modelo", ImportErrorCode.MANDATORY_EMPTY, "Campo obrigatório vazio")
        }

        listOf("fabricante", "marca", "modelo", "serie", "colecao").forEach { field ->
            val raw = values[field]
            if (!raw.isNullOrBlank() && raw.trim().length > MAX_NAME_LENGTH) {
                problem(field, ImportErrorCode.TEXT_TOO_LONG, "Texto acima de $MAX_NAME_LENGTH caracteres")
            }
        }
        values["observacoes"]?.let { raw ->
            if (raw.length > MAX_NOTES_LENGTH) {
                problem("observacoes", ImportErrorCode.TEXT_TOO_LONG, "Texto acima de $MAX_NOTES_LENGTH caracteres")
            }
        }

        values["ano_veiculo_real"]?.takeIf { it.isNotBlank() }?.let { raw ->
            val year = raw.trim().toIntOrNull()
            when {
                year == null -> problem("ano_veiculo_real", ImportErrorCode.INVALID_NUMBER, "Número inválido")
                year !in 1900..2100 -> problem(
                    "ano_veiculo_real",
                    ImportErrorCode.ENUM_OUT_OF_DOMAIN,
                    "Ano fora de 1900–2100",
                )
            }
        }
        values["ano_lancamento"]?.takeIf { it.isNotBlank() }?.let { raw ->
            if (raw.trim().toIntOrNull() == null) {
                problem("ano_lancamento", ImportErrorCode.INVALID_NUMBER, "Número inválido")
            }
        }

        listOf("preco", "frete", "desconto").forEach { field ->
            values[field]?.takeIf { it.isNotBlank() }?.let { raw ->
                val amount = parseDecimal(raw)
                when {
                    amount == null -> problem(field, ImportErrorCode.INVALID_NUMBER, "Número inválido")
                    amount < 0.0 -> problem(field, ImportErrorCode.NEGATIVE_MONEY, "Valor monetário negativo")
                }
            }
        }

        values["data_aquisicao"]?.takeIf { it.isNotBlank() }?.let { raw ->
            if (!isValidDate(raw.trim())) {
                problem("data_aquisicao", ImportErrorCode.INVALID_DATE, "Data inválida")
            }
        }

        values["condicao"]?.takeIf { it.isNotBlank() }?.let { raw ->
            if (raw.trim().lowercase() !in CONDITIONS) {
                problem("condicao", ImportErrorCode.ENUM_OUT_OF_DOMAIN, "Valor fora do domínio")
            }
        }
        values["condicao_embalagem"]?.takeIf { it.isNotBlank() }?.let { raw ->
            if (raw.trim().lowercase() !in PACKAGING) {
                problem("condicao_embalagem", ImportErrorCode.ENUM_OUT_OF_DOMAIN, "Valor fora do domínio")
            }
        }

        values["localizacao"]?.takeIf { it.isNotBlank() }?.let { raw ->
            val levels = raw.split('>').map { it.trim() }
            if (levels.any { it.isEmpty() }) {
                problem("localizacao", ImportErrorCode.INVALID_LOCATION_HIERARCHY, "Hierarquia inválida (use > entre níveis)")
            }
        }

        var duplicate = false
        val internalCode = values["codigo_interno"]?.trim().orEmpty()
        if (internalCode.isNotEmpty()) {
            val alreadyExists = internalCode in existingInternalCodes
            val repeatedInFile = !seenCodes.add(internalCode)
            if (alreadyExists || repeatedInFile) {
                duplicate = true
                problem(
                    "codigo_interno",
                    ImportErrorCode.DUPLICATE,
                    "Código interno duplicado",
                    internalCode,
                )
            }
        }

        return ImportRowResult(
            line = row.lineNumber,
            values = values,
            problems = problems,
            duplicate = duplicate,
        )
    }

    private fun parseDecimal(raw: String): Double? =
        raw.trim().replace(',', '.').toDoubleOrNull()

    private fun isValidDate(raw: String): Boolean {
        return try {
            LocalDate.parse(raw)
            true
        } catch (_: DateTimeParseException) {
            try {
                LocalDate.parse(raw, BR_DATE)
                true
            } catch (_: DateTimeParseException) {
                false
            }
        }
    }
}
