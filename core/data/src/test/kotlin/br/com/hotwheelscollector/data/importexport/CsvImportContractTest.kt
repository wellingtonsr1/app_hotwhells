package br.com.hotwheelscollector.data.importexport

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Contrato do CSV de importação (T107, FR-134–FR-139).
 *
 * Verifica o que o usuário vê na pré-visualização: cabeçalhos normalizados, delimitador e
 * BOM, domínio de enums e datas, limites de texto e os códigos `E001`–`E008` do relatório
 * de erros. Nada aqui grava: o pipeline é leitura + validação.
 */
class CsvImportContractTest {

    @Test
    fun `le cabecalho com acentos caixa e espacos extras`() {
        val preview = CsvImportPipeline.preview(
            """
            FABRICANTE,Modelo ,Ano Lançamento,Condição
            Hot Wheels,Twin Mill,2024,lacrado
            """.trimIndent(),
        )

        assertThat(preview.rows.single().values).containsEntry("ano_lancamento", "2024")
        assertThat(preview.rows.single().values).containsEntry("condicao", "lacrado")
        assertThat(preview.unknownColumns).isEmpty()
        assertThat(preview.validRows).isEqualTo(1)
    }

    @Test
    fun `aceita ponto e virgula como delimitador e remove BOM`() {
        val preview = CsvImportPipeline.preview(
            "\uFEFFfabricante;modelo;preco\r\nHot Wheels;Twin Mill;19,90\r\n",
        )

        assertThat(preview.totalRows).isEqualTo(1)
        assertThat(preview.rows.single().values["preco"]).isEqualTo("19,90")
        assertThat(CsvReader.read("fabricante;modelo\nA;B\n").delimiter).isEqualTo(';')
    }

    @Test
    fun `coluna desconhecida vira aviso e nao invalida a linha`() {
        val preview = CsvImportPipeline.preview("fabricante,modelo,brinde\nHot Wheels,Twin Mill,sim\n")

        assertThat(preview.unknownColumns).containsExactly("brinde")
        assertThat(preview.validRows).isEqualTo(1)
        assertThat(preview.rows.single().values).doesNotContainKey("brinde")
    }

    @Test
    fun `linha valida aceita ISO e dd-MM-yyyy em datas`() {
        val preview = CsvImportPipeline.preview(
            """
            fabricante,modelo,data_aquisicao,preco,condicao,condicao_embalagem
            Hot Wheels,Twin Mill,2024-05-18,"19,90",lacrado,lacrada
            Hot Wheels,Bone Shaker,18/05/2024,8.50,solto,sem embalagem
            """.trimIndent(),
        )

        assertThat(preview.problems).isEmpty()
        assertThat(preview.validRows).isEqualTo(2)
    }

    @Test
    fun `E001 campos obrigatorios vazios`() {
        val preview = CsvImportPipeline.preview("fabricante,modelo\n,Twin Mill\nHot Wheels,\n")

        val problems = preview.problems.filter { it.code == ImportErrorCode.MANDATORY_EMPTY }
        assertThat(problems.map { it.field }).containsExactly("fabricante", "modelo")
        assertThat(problems.map { it.code.code }).containsExactly("E001", "E001")
        assertThat(preview.invalidRows).isEqualTo(2)
    }

    @Test
    fun `E002 numero invalido e E008 valor monetario negativo`() {
        val preview = CsvImportPipeline.preview(
            """
            fabricante,modelo,preco,frete
            Hot Wheels,Twin Mill,abc,-5.00
            """.trimIndent(),
        )

        val problems = preview.problems
        assertThat(problems.filter { it.field == "preco" }.single().code.code).isEqualTo("E002")
        assertThat(problems.filter { it.field == "frete" }.single().code.code).isEqualTo("E008")
    }

    @Test
    fun `E003 data invalida`() {
        val preview = CsvImportPipeline.preview(
            "fabricante,modelo,data_aquisicao\nHot Wheels,Twin Mill,31/02/2024\n",
        )

        assertThat(preview.problems.single().code.code).isEqualTo("E003")
        assertThat(preview.problems.single().field).isEqualTo("data_aquisicao")
    }

    @Test
    fun `E004 valor fora do dominio de enums`() {
        val preview = CsvImportPipeline.preview(
            "fabricante,modelo,condicao,condicao_embalagem\n" +
                "Hot Wheels,Twin Mill,queimado,amassada\n",
        )

        val codes = preview.problems.map { it.field to it.code.code }
        assertThat(codes).containsExactly(
            "condicao" to "E004",
            "condicao_embalagem" to "E004",
        )
    }

    @Test
    fun `E005 duplicata no proprio arquivo e contra o banco`() {
        val csv = """
            fabricante,modelo,codigo_interno
            Hot Wheels,Twin Mill,HW-1
            Hot Wheels,Twin Mill,HW-1
            Hot Wheels,Bone Shaker,HW-9
        """.trimIndent()

        val preview = CsvImportPipeline.preview(csv, existingInternalCodes = setOf("HW-9"))

        assertThat(preview.duplicateRows).isEqualTo(2)
        assertThat(preview.rows[0].duplicate).isFalse()
        assertThat(preview.rows[1].duplicate).isTrue()
        assertThat(preview.rows[2].duplicate).isTrue()
        assertThat(preview.rows[1].problems.single().code.code).isEqualTo("E005")
    }

    @Test
    fun `E006 texto acima do limite`() {
        val longName = "x".repeat(121)
        val preview = CsvImportPipeline.preview("fabricante,modelo\nHot Wheels,$longName\n")

        assertThat(preview.problems.single().code.code).isEqualTo("E006")
        assertThat(preview.problems.single().field).isEqualTo("modelo")
    }

    @Test
    fun `E007 hierarquia de localizacao com nivel vazio`() {
        val preview = CsvImportPipeline.preview(
            "fabricante,modelo,localizacao\nHot Wheels,Twin Mill,Armario A > > Caixa 1\n",
        )

        assertThat(preview.problems.single().code.code).isEqualTo("E007")
    }

    @Test
    fun `relatorio de erros usa o cabecalho e os codigos do contrato`() {
        val preview = CsvImportPipeline.preview(
            "fabricante,modelo,preco\n,Twin Mill,\nHot Wheels,,\n",
        )
        val csv = preview.errorReportCsv()

        assertThat(csv.lineSequence().first()).isEqualTo(ImportErrorReport.HEADER)
        val lines = csv.trim().lines()
        assertThat(lines).hasSize(3)
        assertThat(lines[1]).contains(",fabricante,E001,")
        assertThat(lines[2]).contains(",modelo,E001,")
    }

    @Test
    fun `arquivo vazio ou sem cabecalho e recusado`() {
        assertThat(runCatching { CsvImportPipeline.preview("  \n") }.exceptionOrNull())
            .isInstanceOf(CsvFormatException::class.java)
        assertThat(runCatching { CsvImportPipeline.preview(",,\nA,B,C\n") }.exceptionOrNull())
            .isInstanceOf(CsvFormatException::class.java)
    }
}
