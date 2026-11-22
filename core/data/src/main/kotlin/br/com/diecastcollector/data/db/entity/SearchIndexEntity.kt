package br.com.diecastcollector.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * Índice de busca (FR-114–FR-122): tabela virtual FTS4.
 *
 * `unicode61` com `remove_diacritics=1` faz "serie" encontrar "Série" — requisito de
 * quem digita rápido no celular. Não é tabela de conteúdo: `variant_id` é a chave lógica
 * usada no join com `variant`.
 */
@Entity(tableName = "search_index")
@Fts4(
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    tokenizerArgs = ["remove_diacritics=1"],
)
public data class SearchIndexEntity(
    @ColumnInfo(name = "variant_id")
    val variantId: String,
    @ColumnInfo(name = "model_name")
    val modelName: String,
    @ColumnInfo(name = "real_vehicle_name")
    val realVehicleName: String,
    @ColumnInfo(name = "series_name")
    val seriesName: String,
    @ColumnInfo(name = "collection_names")
    val collectionNames: String,
    @ColumnInfo(name = "manufacturer_name")
    val manufacturerName: String,
    @ColumnInfo(name = "manufacturer_code")
    val manufacturerCode: String,
    @ColumnInfo(name = "notes")
    val notes: String,
)
