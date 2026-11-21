package br.com.hotwheelscollector.data.repository

import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.dao.InventoryReportRow
import br.com.hotwheelscollector.data.importexport.PdfReportResult
import br.com.hotwheelscollector.data.importexport.PdfReportRow
import br.com.hotwheelscollector.data.importexport.PdfReportWriter
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fonte de dados dos relatórios (T118).
 *
 * Lê o inventário já agregado pelo banco e monta o PDF do relatório. Fica em `core:data`
 * porque é o único módulo que conhece Room e o gerador de PDF; a tela só pede os bytes.
 */
@Singleton
public class ReportsRepository @Inject constructor(
    private val database: AppDatabase,
) {

    /** Linhas do inventário: exemplares possuídos com modelo, cor da variante e fotos. */
    public suspend fun inventoryRows(): List<InventoryReportRow> =
        withContext(Dispatchers.IO) { database.exemplarDao().inventoryReportRows() }

    /** Gera o PDF do inventário. O total devolvido é o mesmo que o painel usa (SC-007). */
    public suspend fun buildInventoryPdf(generatedAt: Date = Date()): PdfReportResult =
        withContext(Dispatchers.IO) {
            val rows = database.exemplarDao().inventoryReportRows().map { row ->
                PdfReportRow(
                    internalCode = row.internalCode,
                    model = row.modelName,
                    variant = row.variantLabel ?: "-",
                    condition = row.condition,
                    photos = row.photoCount,
                )
            }
            PdfReportWriter.write(rows, generatedAt = generatedAt)
        }
}
