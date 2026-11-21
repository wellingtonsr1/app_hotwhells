package br.com.hotwheelscollector.data.repository

import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.dao.InventoryReportRow
import br.com.hotwheelscollector.data.importexport.PdfReportRow
import br.com.hotwheelscollector.data.importexport.PdfReportWriter
import br.com.hotwheelscollector.data.importexport.ReportFormat
import br.com.hotwheelscollector.data.importexport.ReportTable
import br.com.hotwheelscollector.data.importexport.ReportWriters
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Relatório gerado, pronto para ser gravado no destino escolhido (SAF). */
public data class InventoryReport(
    val bytes: ByteArray,
    val format: ReportFormat,
    val fileName: String,
    val totalExemplares: Int,
    val pageCount: Int = 0,
)

/**
 * Fonte de dados dos relatórios (T118/T116).
 *
 * Lê o inventário já agregado pelo banco e monta o arquivo no formato pedido — PDF, CSV ou
 * XLSX (FR-124–FR-126). Fica em `core:data` porque é o único módulo que conhece Room e os
 * escritores; a tela só pede os bytes e o nome sugerido.
 */
@Singleton
public class ReportsRepository @Inject constructor(
    private val database: AppDatabase,
) {

    /** Linhas do inventário: exemplares possuídos com modelo, cor da variante e fotos. */
    public suspend fun inventoryRows(): List<InventoryReportRow> =
        withContext(Dispatchers.IO) { database.exemplarDao().inventoryReportRows() }

    /**
     * Gera o inventário no formato pedido. O total devolvido é o mesmo que o painel usa
     * (SC-007) e o nome do arquivo carrega tipo e data (FR-131). Dados ausentes aparecem
     * como "não informado", nunca como zero (FR-129).
     */
    public suspend fun buildInventoryReport(
        format: ReportFormat,
        generatedAt: Date = Date(),
    ): InventoryReport = withContext(Dispatchers.IO) {
        val rows = database.exemplarDao().inventoryReportRows()
        val fileName = ReportWriters.fileName(
            reportSlug = "colecao-inventario",
            format = format,
            generatedAt = generatedAt.toInstant(),
        )

        when (format) {
            ReportFormat.PDF -> {
                val pdf = PdfReportWriter.write(
                    rows.map { row ->
                        PdfReportRow(
                            internalCode = row.internalCode,
                            model = row.modelName,
                            variant = row.variantLabel ?: NOT_INFORMED,
                            condition = row.condition,
                            photos = row.photoCount,
                        )
                    },
                    generatedAt = generatedAt,
                )
                InventoryReport(
                    bytes = pdf.bytes,
                    format = format,
                    fileName = fileName,
                    totalExemplares = pdf.totalExemplares,
                    pageCount = pdf.pageCount,
                )
            }

            ReportFormat.CSV -> {
                val table = inventoryTable(rows)
                InventoryReport(
                    bytes = ReportWriters.writeCsv(table),
                    format = format,
                    fileName = fileName,
                    totalExemplares = rows.size,
                )
            }

            ReportFormat.XLSX -> {
                val table = inventoryTable(rows)
                InventoryReport(
                    bytes = ReportWriters.writeXlsx(listOf(table)),
                    format = format,
                    fileName = fileName,
                    totalExemplares = rows.size,
                )
            }
        }
    }

    private fun inventoryTable(rows: List<InventoryReportRow>): ReportTable = ReportTable(
        name = "Inventário",
        headers = listOf("Código", "Modelo", "Variante", "Condição", "Fotos"),
        rows = rows.map { row ->
            listOf(
                row.internalCode,
                row.modelName,
                row.variantLabel ?: NOT_INFORMED,
                row.condition,
                row.photoCount.toString(),
            )
        },
    )

    private companion object {
        const val NOT_INFORMED = "não informado"
    }
}
