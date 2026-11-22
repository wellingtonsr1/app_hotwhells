package br.com.diecastcollector.data.importexport

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Test

/** T112 — lê a primeira aba do XLSX e ignora as demais, convertendo em CSV. */
class XlsxReaderTest {

    @Test
    fun `le a primeira aba com shared strings e inline string`() {
        val xlsx = buildXlsx()

        val result = XlsxReader.toCsv(xlsx)

        assertThat(result.ignoredSheetCount).isEqualTo(1)
        assertThat(result.warnings).hasSize(1)

        val preview = CsvImportPipeline.preview(result.csv)
        assertThat(preview.totalRows).isEqualTo(1)
        assertThat(preview.validRows).isEqualTo(1)
        val row = preview.rows.single()
        assertThat(row.values["fabricante"]).isEqualTo("Mattel")
        assertThat(row.values["modelo"]).isEqualTo("Skyline, R34")
        assertThat(row.values["ano_lancamento"]).isEqualTo("2024")
    }

    private fun buildXlsx(): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write(SHARED_STRINGS.toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(SHEET_ONE.toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("xl/worksheets/sheet2.xml"))
            zip.write(SHEET_TWO.toByteArray())
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    private companion object {
        const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"

        val SHARED_STRINGS = """
            <?xml version="1.0"?>
            <sst xmlns="$NS">
              <si><t>fabricante</t></si>
              <si><t>modelo</t></si>
              <si><t>condicao</t></si>
              <si><t>codigo_interno</t></si>
              <si><t>ano_lancamento</t></si>
            </sst>
        """.trimIndent()

        val SHEET_ONE = """
            <?xml version="1.0"?>
            <worksheet xmlns="$NS"><sheetData>
              <row r="1">
                <c r="A1" t="s"><v>0</v></c>
                <c r="B1" t="s"><v>1</v></c>
                <c r="C1" t="s"><v>2</v></c>
                <c r="D1" t="s"><v>3</v></c>
                <c r="E1" t="s"><v>4</v></c>
              </row>
              <row r="2">
                <c r="A2" t="inlineStr"><is><t>Mattel</t></is></c>
                <c r="B2" t="inlineStr"><is><t>Skyline, R34</t></is></c>
                <c r="C2" t="inlineStr"><is><t>lacrado</t></is></c>
                <c r="D2" t="inlineStr"><is><t>HW-1</t></is></c>
                <c r="E2"><v>2024</v></c>
              </row>
            </sheetData></worksheet>
        """.trimIndent()

        val SHEET_TWO = """
            <?xml version="1.0"?>
            <worksheet xmlns="$NS"><sheetData>
              <row r="1"><c r="A1" t="inlineStr"><is><t>ignorada</t></is></c></row>
            </sheetData></worksheet>
        """.trimIndent()
    }
}
