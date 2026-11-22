package br.com.diecastcollector.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.diecastcollector.data.db.entity.DuplicateOfferEntity
import br.com.diecastcollector.data.db.entity.ExemplarEntity
import br.com.diecastcollector.data.db.entity.NegotiationEntity
import kotlinx.coroutines.flow.Flow

/** Linha da consulta de duplicados: exemplar + oferta + negociação ativa. */
public data class DuplicateExemplarRow(
    @Embedded public val exemplar: ExemplarEntity,
    @ColumnInfo(name = "manufacturer_name") public val manufacturerName: String,
    @ColumnInfo(name = "model_name") public val modelName: String,
    @ColumnInfo(name = "color_name") public val colorName: String?,
    @Embedded(prefix = "offer_") public val offer: DuplicateOfferEntity?,
    @Embedded(prefix = "active_") public val activeNegotiation: NegotiationEntity?,
)

@Dao
public interface DuplicateDao {

    /**
     * Exemplares possuídos de variantes com mais de um exemplar possuído.
     *
     * O filtro é pelo `variant_id`, não pelo modelo: duas variantes do mesmo carro não
     * são duplicatas (FR-065, SC-005). A oferta e a negociação ativa vêm na mesma
     * consulta para a lista não disparar uma consulta por linha.
     */
    @Query(
        """
        SELECT e.*,
               mf.name AS manufacturer_name,
               cm.name AS model_name,
               c.name AS color_name,
               o.id AS offer_id,
               o.exemplar_id AS offer_exemplar_id,
               o.availability AS offer_availability,
               o.condition AS offer_condition,
               o.asking_price_cents AS offer_asking_price_cents,
               o.notes AS offer_notes,
               o.created_at AS offer_created_at,
               o.updated_at AS offer_updated_at,
               n.id AS active_id,
               n.exemplar_id AS active_exemplar_id,
               n.type AS active_type,
               n.counterparty AS active_counterparty,
               n.channel AS active_channel,
               n.status AS active_status,
               n.started_at AS active_started_at,
               n.closed_at AS active_closed_at,
               n.amount_cents AS active_amount_cents,
               n.currency AS active_currency,
               n.notes AS active_notes,
               n.received_exemplar_id AS active_received_exemplar_id
        FROM exemplar e
        JOIN variant v ON v.id = e.variant_id
        JOIN car_model cm ON cm.id = v.model_id
        JOIN manufacturer mf ON mf.id = cm.manufacturer_id
        LEFT JOIN color c ON c.id = v.primary_color_id
        LEFT JOIN duplicate_offer o ON o.exemplar_id = e.id
        LEFT JOIN negotiation n
               ON n.exemplar_id = e.id AND n.status IN ('OPEN', 'RESERVED')
        WHERE e.status = 'OWNED'
          AND e.variant_id IN (
              SELECT variant_id FROM exemplar
              WHERE status = 'OWNED'
              GROUP BY variant_id
              HAVING COUNT(*) > 1
          )
        ORDER BY mf.name COLLATE NOCASE, cm.name COLLATE NOCASE, v.created_at, e.created_at
        """,
    )
    public fun observeDuplicates(): Flow<List<DuplicateExemplarRow>>

    @Query("SELECT * FROM duplicate_offer WHERE exemplar_id = :exemplarId LIMIT 1")
    public suspend fun findOffer(exemplarId: String): DuplicateOfferEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insertOffer(entity: DuplicateOfferEntity)

    @Update
    public suspend fun updateOffer(entity: DuplicateOfferEntity)

    @Query("DELETE FROM duplicate_offer WHERE exemplar_id = :exemplarId")
    public suspend fun deleteOffer(exemplarId: String)
}
