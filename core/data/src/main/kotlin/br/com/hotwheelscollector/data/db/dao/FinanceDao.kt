package br.com.hotwheelscollector.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Resumo financeiro em uma linha: tudo derivado, nada armazenado (FR-077–FR-086). */
public data class FinanceSummaryRow(
    @ColumnInfo(name = "invested_cents") val investedCents: Long,
    @ColumnInfo(name = "estimated_cents") val estimatedCents: Long,
    @ColumnInfo(name = "owned_exemplars") val ownedExemplars: Int,
    @ColumnInfo(name = "exemplars_with_value") val exemplarsWithValue: Int,
    @ColumnInfo(name = "exemplars_without_valuation") val exemplarsWithoutValuation: Int,
    @ColumnInfo(name = "latest_valuation_date") val latestValuationDate: String?,
)

public data class SpendingRow(
    @ColumnInfo(name = "label") val label: String?,
    @ColumnInfo(name = "cents") val cents: Long,
)

public data class MonthSpendingRow(
    @ColumnInfo(name = "month") val month: String,
    @ColumnInfo(name = "cents") val cents: Long,
)

/**
 * Agregações financeiras (FR-077–FR-081).
 *
 * `status = 'OWNED'` em todas: exemplar vendido, trocado ou arquivado continua no banco,
 * mas não faz mais parte do investimento. O custo é `preço + frete − desconto`, sempre
 * calculado — nunca coluna.
 */
@Dao
public interface FinanceDao {

    @Query(
        """
        SELECT
          (SELECT COALESCE(SUM(
               CASE WHEN a.price_cents IS NULL THEN 0
                    ELSE COALESCE(a.price_cents, 0) + COALESCE(a.shipping_cents, 0) - COALESCE(a.discount_cents, 0)
               END
           ), 0)
           FROM acquisition a
           JOIN exemplar e ON e.id = a.exemplar_id
           WHERE e.status = 'OWNED') AS invested_cents,
          (SELECT COALESCE(SUM(v.value_cents), 0)
           FROM valuation v
           JOIN exemplar e ON e.id = v.exemplar_id
           WHERE e.status = 'OWNED'
             AND v.id = (
               SELECT v2.id FROM valuation v2
               WHERE v2.exemplar_id = v.exemplar_id
               ORDER BY v2.date DESC, v2.created_at DESC
               LIMIT 1
             )) AS estimated_cents,
          (SELECT COUNT(*) FROM exemplar e WHERE e.status = 'OWNED') AS owned_exemplars,
          (SELECT COUNT(*)
           FROM exemplar e
           JOIN acquisition a ON a.exemplar_id = e.id
           WHERE e.status = 'OWNED' AND a.price_cents IS NOT NULL) AS exemplars_with_value,
          (SELECT COUNT(*)
           FROM exemplar e
           WHERE e.status = 'OWNED'
             AND NOT EXISTS (SELECT 1 FROM valuation v WHERE v.exemplar_id = e.id))
             AS exemplars_without_valuation,
          (SELECT MAX(v.date)
           FROM valuation v
           JOIN exemplar e ON e.id = v.exemplar_id
           WHERE e.status = 'OWNED') AS latest_valuation_date
        """,
    )
    public fun observeSummary(): Flow<FinanceSummaryRow>

    @Query(
        """
        SELECT mf.name AS label,
               SUM(CASE WHEN a.price_cents IS NULL THEN 0
                        ELSE COALESCE(a.price_cents, 0) + COALESCE(a.shipping_cents, 0) - COALESCE(a.discount_cents, 0)
                   END) AS cents
        FROM acquisition a
        JOIN exemplar e ON e.id = a.exemplar_id
        JOIN variant v ON v.id = e.variant_id
        JOIN car_model m ON m.id = v.model_id
        JOIN manufacturer mf ON mf.id = m.manufacturer_id
        WHERE e.status = 'OWNED'
        GROUP BY mf.id, mf.name
        ORDER BY cents DESC, label COLLATE NOCASE
        """,
    )
    public fun observeSpendingByManufacturer(): Flow<List<SpendingRow>>

    @Query(
        """
        SELECT s.name AS label,
               SUM(CASE WHEN a.price_cents IS NULL THEN 0
                        ELSE COALESCE(a.price_cents, 0) + COALESCE(a.shipping_cents, 0) - COALESCE(a.discount_cents, 0)
                   END) AS cents
        FROM acquisition a
        JOIN exemplar e ON e.id = a.exemplar_id
        JOIN variant v ON v.id = e.variant_id
        LEFT JOIN series s ON s.id = v.series_id
        WHERE e.status = 'OWNED'
        GROUP BY s.id, s.name
        ORDER BY cents DESC, label COLLATE NOCASE
        """,
    )
    public fun observeSpendingBySeries(): Flow<List<SpendingRow>>

    /**
     * Um exemplar pode estar em mais de uma coleção; nesse caso o custo aparece em cada
     * uma. Somar as linhas não é o total do acervo — é gasto por coleção.
     */
    @Query(
        """
        SELECT c.name AS label,
               SUM(CASE WHEN a.price_cents IS NULL THEN 0
                        ELSE COALESCE(a.price_cents, 0) + COALESCE(a.shipping_cents, 0) - COALESCE(a.discount_cents, 0)
                   END) AS cents
        FROM acquisition a
        JOIN exemplar e ON e.id = a.exemplar_id
        JOIN collection_item i ON i.variant_id = e.variant_id
        JOIN collection c ON c.id = i.collection_id
        WHERE e.status = 'OWNED'
        GROUP BY c.id, c.name
        ORDER BY cents DESC, label COLLATE NOCASE
        """,
    )
    public fun observeSpendingByCollection(): Flow<List<SpendingRow>>

    /**
     * Gastos por mês (FR-078). As datas são ISO `AAAA-MM-DD`, então a comparação de texto
     * é também comparação cronológica; `from`/`to` nulos significam sem limite.
     */
    @Query(
        """
        SELECT substr(a.date, 1, 7) AS month,
               SUM(CASE WHEN a.price_cents IS NULL THEN 0
                        ELSE COALESCE(a.price_cents, 0) + COALESCE(a.shipping_cents, 0) - COALESCE(a.discount_cents, 0)
                   END) AS cents
        FROM acquisition a
        JOIN exemplar e ON e.id = a.exemplar_id
        WHERE e.status = 'OWNED'
          AND (:from IS NULL OR a.date >= :from)
          AND (:to IS NULL OR a.date <= :to)
        GROUP BY month
        ORDER BY month DESC
        """,
    )
    public fun observeSpendingByMonth(from: String?, to: String?): Flow<List<MonthSpendingRow>>
}
