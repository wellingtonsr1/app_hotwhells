package br.com.diecastcollector.data.db

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Mantém o índice FTS4 de busca em sincronia com o catálogo (FR-114–FR-122).
 *
 * O índice já nasceu no schema v1; o que faltava eram os gatilhos. Eles são instalados no
 * `onOpen` do banco (idempotente) em vez de numa migração: não mudam o schema validado pelo
 * Room e passam a valer também para bancos criados antes desta entrega. Na abertura, se a
 * contagem do índice divergir da de variantes, ele é reconstruído do zero — assim dados
 * antigos e um índice interrompido não ficam invisíveis para a busca.
 */
internal object SearchIndexSync {

    fun install(database: SupportSQLiteDatabase) {
        TRIGGERS.forEach(database::execSQL)

        val variants = count(database, "variant")
        val indexed = count(database, "search_index")
        if (variants != indexed) {
            database.execSQL("DELETE FROM search_index")
            database.execSQL(populateAll())
        }
    }

    private fun count(database: SupportSQLiteDatabase, table: String): Int =
        database.query("SELECT COUNT(*) FROM $table").use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }

    private const val COLUMNS =
        "(variant_id, model_name, real_vehicle_name, series_name, collection_names, " +
            "manufacturer_name, manufacturer_code, notes)"

    private const val SELECT_FROM = """
        SELECT v.id,
               m.name,
               COALESCE(rv.make || ' ' || rv.model, ''),
               COALESCE(s.name, ''),
               COALESCE((SELECT group_concat(c.name, ' ')
                         FROM collection_item ci
                         JOIN collection c ON c.id = ci.collection_id
                         WHERE ci.variant_id = v.id), ''),
               mf.name,
               COALESCE(v.manufacturer_code, ''),
               COALESCE(v.decoration, '')
        FROM variant v
        JOIN car_model m ON m.id = v.model_id
        JOIN manufacturer mf ON mf.id = m.manufacturer_id
        LEFT JOIN real_vehicle rv ON rv.id = m.real_vehicle_id
        LEFT JOIN series s ON s.id = v.series_id
    """

    private fun populateAll(): String =
        "INSERT INTO search_index $COLUMNS $SELECT_FROM"

    private fun reindex(predicate: String): String =
        "INSERT INTO search_index $COLUMNS $SELECT_FROM WHERE $predicate"

    private fun reindexVariant(idExpression: String): String =
        "DELETE FROM search_index WHERE variant_id = $idExpression; " +
            reindex("v.id = $idExpression")

    private fun reindexModel(idExpression: String): String =
        "DELETE FROM search_index WHERE variant_id IN " +
            "(SELECT id FROM variant WHERE model_id = $idExpression); " +
            reindex("v.model_id = $idExpression")

    private fun reindexSeries(idExpression: String): String =
        "DELETE FROM search_index WHERE variant_id IN " +
            "(SELECT id FROM variant WHERE series_id = $idExpression); " +
            reindex("v.series_id = $idExpression")

    private fun reindexManufacturer(idExpression: String): String =
        "DELETE FROM search_index WHERE variant_id IN " +
            "(SELECT v.id FROM variant v JOIN car_model m ON m.id = v.model_id " +
            "WHERE m.manufacturer_id = $idExpression); " +
            reindex("mf.id = $idExpression")

    private fun reindexCollection(idExpression: String): String =
        "DELETE FROM search_index WHERE variant_id IN " +
            "(SELECT variant_id FROM collection_item WHERE collection_id = $idExpression); " +
            reindex(
                "v.id IN (SELECT variant_id FROM collection_item " +
                    "WHERE collection_id = $idExpression)",
            )

    private fun reindexCollectionItem(idExpression: String): String =
        "DELETE FROM search_index WHERE variant_id = $idExpression; " +
            reindex("v.id = $idExpression")

    private fun reindexRealVehicle(idExpression: String): String =
        "DELETE FROM search_index WHERE variant_id IN " +
            "(SELECT v.id FROM variant v JOIN car_model m ON m.id = v.model_id " +
            "WHERE m.real_vehicle_id = $idExpression); " +
            reindex("m.real_vehicle_id = $idExpression")

    private fun trigger(
        name: String,
        timing: String,
        table: String,
        event: String,
        body: String,
    ): String = "CREATE TRIGGER IF NOT EXISTS $name $timing $event ON $table BEGIN $body; END"

    private val TRIGGERS: List<String> = listOf(
        trigger("search_variant_ai", "AFTER", "variant", "INSERT", reindexVariant("NEW.id")),
        trigger("search_variant_au", "AFTER", "variant", "UPDATE", reindexVariant("NEW.id")),
        trigger(
            "search_variant_ad",
            "AFTER",
            "variant",
            "DELETE",
            "DELETE FROM search_index WHERE variant_id = OLD.id",
        ),
        trigger("search_model_au", "AFTER", "car_model", "UPDATE", reindexModel("NEW.id")),
        trigger("search_series_au", "AFTER", "series", "UPDATE", reindexSeries("NEW.id")),
        trigger(
            "search_manufacturer_au",
            "AFTER",
            "manufacturer",
            "UPDATE",
            reindexManufacturer("NEW.id"),
        ),
        trigger(
            "search_collection_au",
            "AFTER",
            "collection",
            "UPDATE",
            reindexCollection("NEW.id"),
        ),
        trigger(
            "search_collection_item_ai",
            "AFTER",
            "collection_item",
            "INSERT",
            reindexCollectionItem("NEW.variant_id"),
        ),
        trigger(
            "search_collection_item_au",
            "AFTER",
            "collection_item",
            "UPDATE",
            reindexCollectionItem("NEW.variant_id"),
        ),
        trigger(
            "search_collection_item_ad",
            "AFTER",
            "collection_item",
            "DELETE",
            reindexCollectionItem("OLD.variant_id"),
        ),
        trigger(
            "search_real_vehicle_au",
            "AFTER",
            "real_vehicle",
            "UPDATE",
            reindexRealVehicle("NEW.id"),
        ),
    )
}
