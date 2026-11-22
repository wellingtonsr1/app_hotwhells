package br.com.diecastcollector.data.importexport

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Test

/**
 * T151 / SC-006 — importar 1.000 linhas com pré-visualização completa e relatório de erro
 * por linha dentro do tempo aceitável.
 *
 * Exercita o pipeline real (`CsvReader` + `CsvImportPipeline`), não um stub: lê, valida,
 * classifica duplicatas e gera o CSV de erros, medindo o tempo da pré-visualização.
 */
class ImportScaleTest {

    @Test
    fun `importa 1000 linhas com previa e relatorio de erros dentro do tempo`() {
        val csv = buildCsv(valid = 900, invalid = 100)

        val start = System.nanoTime()
        val preview = CsvImportPipeline.preview(csv)
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)

        assertThat(preview.totalRows).isEqualTo(1000)
        assertThat(preview.validRows).isEqualTo(900)
        assertThat(preview.invalidRows).isEqualTo(100)
        assertThat(preview.duplicateRows).isEqualTo(0)

        // Relatório de erros: uma linha por problema + cabeçalho.
        assertThat(preview.problems).hasSize(100)
        assertThat(preview.problems.all { it.code == ImportErrorCode.MANDATORY_EMPTY }).isTrue()

        val report = preview.errorReportCsv()
        val reportLines = report.trimEnd().lines()
        assertThat(reportLines.first()).isEqualTo(ImportErrorReport.HEADER)
        assertThat(reportLines).hasSize(101)

        // SC-006: 1.000 linhas em menos de 10 s.
        assertThat(elapsedMs).isLessThan(10_000L)
    }

    @Test
    fun `classifica duplicatas por codigo interno contra o banco e dentro do arquivo`() {
        val csv = buildString {
            appendLine("fabricante,modelo,codigo_interno")
            appendLine("Mattel,Modelo A,HW-0001") // já existe no banco
            appendLine("Mattel,Modelo B,HW-0002") // inédito
            appendLine("Mattel,Modelo C,HW-0002") // repetido no arquivo
        }

        val preview = CsvImportPipeline.preview(csv, existingInternalCodes = setOf("HW-0001"))

        assertThat(preview.totalRows).isEqualTo(3)
        assertThat(preview.duplicateRows).isEqualTo(2)
        assertThat(preview.validRows).isEqualTo(1)
        assertThat(preview.invalidRows).isEqualTo(2)
        assertThat(preview.problems.map { it.code }).containsExactly(
            ImportErrorCode.DUPLICATE,
            ImportErrorCode.DUPLICATE,
        )
    }

    @Test
    fun `detecta delimitador ponto e virgula e remove BOM`() {
        val csv = "\uFEFFfabricante;modelo;condicao\r\n" +
            "Mattel;Modelo A;solto\r\n" +
            "Mattel;Modelo B;lacrado\r\n"

        val preview = CsvImportPipeline.preview(csv)

        assertThat(preview.totalRows).isEqualTo(2)
        assertThat(preview.validRows).isEqualTo(2)
        assertThat(preview.invalidRows).isEqualTo(0)
    }

    @Test
    fun `respeita campos entre aspas com virgula`() {
        val csv = "fabricante,modelo,observacoes\n" +
            "Mattel,\"Skyline, edição especial\",\"caixa, com marca\"\n"

        val preview = CsvImportPipeline.preview(csv)

        assertThat(preview.validRows).isEqualTo(1)
        assertThat(preview.rows.single().values["modelo"]).isEqualTo("Skyline, edição especial")
        assertThat(preview.rows.single().values["observacoes"]).isEqualTo("caixa, com marca")
    }

    @Test
    fun `mapeia erros de validacao para os codigos do contrato`() {
        val csv = buildString {
            appendLine(
                "fabricante,modelo,ano_veiculo_real,preco,data_aquisicao,condicao,localizacao",
            )
            // E002 número, E008 negativo, E003 data, E004 enum, E007 hierarquia.
            appendLine("Mattel,Modelo A,abc,-5.00,31/02/2024,novo,Armário >  > Caixa")
        }

        val preview = CsvImportPipeline.preview(csv)
        val codes = preview.problems.map { it.code }.toSet()

        assertThat(codes).containsAtLeast(
            ImportErrorCode.INVALID_NUMBER,
            ImportErrorCode.NEGATIVE_MONEY,
            ImportErrorCode.INVALID_DATE,
            ImportErrorCode.ENUM_OUT_OF_DOMAIN,
            ImportErrorCode.INVALID_LOCATION_HIERARCHY,
        )
    }

    private fun buildCsv(valid: Int, invalid: Int): String {
        val builder = StringBuilder()
        builder.append("fabricante,modelo,escala,condicao,ano_lancamento,codigo_interno\n")
        var line = 0
        repeat(valid) {
            line++
            builder.append("Mattel,Modelo $line,1:64,solto,2024,HW-")
                .append(line.toString().padStart(4, '0'))
                .append('\n')
        }
        repeat(invalid) {
            line++
            // Fabricante vazio → E001 (um único problema por linha).
            builder.append(",Modelo $line,1:64,solto,2024,HW-")
                .append(line.toString().padStart(4, '0'))
                .append('\n')
        }
        return builder.toString()
    }
}
