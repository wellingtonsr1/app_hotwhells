package br.com.hotwheelscollector.data.importexport

/** Códigos de erro do relatório de importação (contrato, seção "Relatório de erros"). */
public enum class ImportErrorCode(public val code: String) {
    MANDATORY_EMPTY("E001"),
    INVALID_NUMBER("E002"),
    INVALID_DATE("E003"),
    ENUM_OUT_OF_DOMAIN("E004"),
    DUPLICATE("E005"),
    TEXT_TOO_LONG("E006"),
    INVALID_LOCATION_HIERARCHY("E007"),
    NEGATIVE_MONEY("E008"),
}

/** Um problema de validação de uma linha, com o código e o valor que o originou. */
public data class ImportProblem(
    val line: Int,
    val field: String,
    val code: ImportErrorCode,
    val message: String,
    val value: String,
)

/**
 * Relatório de erros por linha (T114).
 *
 * Gera o CSV com cabeçalho `linha,campo,codigo,mensagem,valor_informado`, exatamente como o
 * contrato de importação define. Campos são escapados quando contêm vírgula, aspas ou quebra
 * de linha, para que o arquivo continue abrindo em qualquer leitor.
 */
public object ImportErrorReport {

    public const val HEADER: String = "linha,campo,codigo,mensagem,valor_informado"

    public fun toCsv(problems: List<ImportProblem>): String {
        val builder = StringBuilder()
        builder.append(HEADER).append('\n')
        for (problem in problems) {
            builder.append(problem.line).append(',')
                .append(escape(problem.field)).append(',')
                .append(problem.code.code).append(',')
                .append(escape(problem.message)).append(',')
                .append(escape(problem.value)).append('\n')
        }
        return builder.toString()
    }

    private fun escape(value: String): String {
        val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuoting) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
