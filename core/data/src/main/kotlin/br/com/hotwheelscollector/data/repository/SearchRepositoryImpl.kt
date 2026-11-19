package br.com.hotwheelscollector.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.db.SearchIndexSync
import br.com.hotwheelscollector.data.db.dao.SearchResultRow
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.CatalogSort
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.ExemplarStatus
import br.com.hotwheelscollector.domain.model.FilterOption
import br.com.hotwheelscollector.domain.model.IsoDate
import br.com.hotwheelscollector.domain.model.SearchFilterOptions
import br.com.hotwheelscollector.domain.model.SearchFilters
import br.com.hotwheelscollector.domain.model.SearchResultItem
import br.com.hotwheelscollector.domain.repository.SearchRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Busca do acervo (FR-114–FR-122).
 *
 * A tabela FTS responde à parte textual; o resto são filtros relacionais. O SQL é montado
 * com placeholders — nenhum valor digitado é concatenado na consulta. A contagem exibida é
 * o tamanho da lista, sem uma segunda ida ao banco.
 */
@Singleton
public class SearchRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
) : SearchRepository {

    override fun observeResults(filters: SearchFilters): Flow<List<SearchResultItem>> =
        database.searchDao().observeResults(buildQuery(filters)).map { rows ->
            rows.mapNotNull { it.toDomainOrNull() }
        }

    override suspend fun variantIds(query: String, limit: Int): List<String> {
        val match = query.toFtsMatch() ?: return emptyList()
        return database.searchIndexDao().searchVariantIds(match, limit)
    }

    override suspend fun rebuild(): DomainResult<Unit> = try {
        val databaseFile = database.openHelper.writableDatabase
        databaseFile.execSQL("DELETE FROM search_index")
        SearchIndexSync.install(databaseFile)
        Result.success(Unit)
    } catch (error: Throwable) {
        Result.failure(
            DomainError.Storage(
                error.message ?: "Falha ao reconstruir o índice de busca.",
            ),
        )
    }

    override fun observeOptions(): Flow<SearchFilterOptions> {
        val dao = database.searchDao()
        val selectable = combine(
            dao.observeManufacturers(),
            dao.observeModels(),
            dao.observeSeries(),
            dao.observeCollections(),
            dao.observeColors(),
        ) { manufacturers, models, series, collections, colors ->
            listOf(manufacturers, models, series, collections, colors)
        }
        val simple = combine(
            dao.observeLocations(),
            dao.observeYears(),
            dao.observeScales(),
        ) { locations, years, scales ->
            Triple(locations, years, scales)
        }
        return combine(selectable, simple) { groups, (locations, years, scales) ->
            SearchFilterOptions(
                manufacturers = groups[0].toOptions(),
                models = groups[1].toOptions(),
                series = groups[2].toOptions(),
                collections = groups[3].toOptions(),
                colors = groups[4].toOptions(),
                locations = locations.toOptions(),
                scales = scales.map { it.scale },
                years = years.map { it.year },
            )
        }
    }

    private fun buildQuery(filters: SearchFilters): SimpleSQLiteQuery {
        val args = mutableListOf<Any?>()
        val sql = StringBuilder(BASE_QUERY).append(" WHERE 1 = 1")

        filters.query.toFtsMatch()?.let { match ->
            sql.append(
                " AND v.id IN (SELECT variant_id FROM search_index " +
                    "WHERE search_index MATCH ?)",
            )
            args.add(match)
        }

        if (filters.manufacturerIds.isNotEmpty()) {
            sql.appendIn("mf.id", filters.manufacturerIds, args)
        }
        if (filters.modelIds.isNotEmpty()) {
            sql.appendIn("m.id", filters.modelIds, args)
        }
        if (filters.seriesIds.isNotEmpty()) {
            sql.appendIn("s.id", filters.seriesIds, args)
        }
        if (filters.collectionIds.isNotEmpty()) {
            val holes = filters.collectionIds.joinToString(", ") { "?" }
            sql.append(
                " AND (v.id IN (SELECT variant_id FROM collection_item " +
                    "WHERE collection_id IN ($holes))" +
                    " OR v.model_id IN (SELECT model_id FROM collection_item " +
                    "WHERE model_id IS NOT NULL AND collection_id IN ($holes)))",
            )
            repeat(2) { filters.collectionIds.forEach { args.add(it) } }
        }
        if (filters.years.isNotEmpty()) {
            sql.appendIn("v.release_year", filters.years, args)
        }
        if (filters.colorIds.isNotEmpty()) {
            val holes = filters.colorIds.joinToString(", ") { "?" }
            sql.append(
                " AND (v.primary_color_id IN ($holes)" +
                    " OR EXISTS (SELECT 1 FROM variant_secondary_color vc " +
                    "WHERE vc.variant_id = v.id AND vc.color_id IN ($holes)))",
            )
            repeat(2) { filters.colorIds.forEach { args.add(it) } }
        }
        if (filters.scales.isNotEmpty()) {
            sql.appendIn("m.scale", filters.scales, args)
        }
        if (filters.conditions.isNotEmpty()) {
            sql.appendIn("e.condition", filters.conditions.map { it.name }, args)
        }
        if (filters.variantIds.isNotEmpty()) {
            sql.appendIn("e.variant_id", filters.variantIds, args)
        }
        if (filters.statuses.isNotEmpty()) {
            sql.appendIn("e.status", filters.statuses.map { it.name }, args)
        }
        if (filters.locationIds.isNotEmpty()) {
            sql.appendIn("e.location_id", filters.locationIds, args)
        }
        filters.priceMinCents?.let {
            sql.append(" AND a.price_cents >= ?")
            args.add(it)
        }
        filters.priceMaxCents?.let {
            sql.append(" AND a.price_cents <= ?")
            args.add(it)
        }

        sql.append(" ORDER BY ").append(
            when (filters.sort) {
                CatalogSort.NAME ->
                    "m.name COLLATE NOCASE, mf.name COLLATE NOCASE, e.internal_code"
                CatalogSort.YEAR ->
                    "v.release_year IS NULL, v.release_year DESC, m.name COLLATE NOCASE"
                CatalogSort.ACQUISITION_DATE ->
                    "a.date IS NULL, a.date DESC, m.name COLLATE NOCASE"
                CatalogSort.VALUE ->
                    "a.price_cents IS NULL, a.price_cents DESC, m.name COLLATE NOCASE"
            },
        )

        return SimpleSQLiteQuery(sql.toString(), args.toTypedArray())
    }

    private fun StringBuilder.appendIn(
        column: String,
        values: Collection<*>,
        args: MutableList<Any?>,
    ) {
        append(" AND ").append(column).append(" IN (")
        values.forEachIndexed { index, value ->
            if (index > 0) append(", ")
            append("?")
            args.add(value)
        }
        append(")")
    }

    /**
     * `skyline r34` → `skyline* r34*`: ordem livre e prefixo tolerante (FR-115).
     *
     * O espaço é a conjunção implícita do FTS4. Usar o operador `AND` explícito faria a
     * consulta voltar vazia no SQLite do Android, então os tokens são só justapostos.
     */
    private fun String.toFtsMatch(): String? {
        val tokens = trim()
            .split(Regex("\\s+"))
            .map { token -> token.filter { it.isLetterOrDigit() } }
            .filter { it.isNotEmpty() }
        return if (tokens.isEmpty()) null else tokens.joinToString(" ") { "$it*" }
    }

    private fun SearchResultRow.toDomainOrNull(): SearchResultItem? {
        val parsedCondition = enumValueOrNull<Condition>(condition) ?: return null
        val parsedStatus = enumValueOrNull<ExemplarStatus>(status) ?: return null
        return SearchResultItem(
            exemplarId = exemplarId,
            variantId = variantId,
            internalCode = internalCode,
            modelName = modelName,
            manufacturerName = manufacturerName,
            seriesName = seriesName,
            collectionName = collectionName,
            primaryColorName = primaryColorName,
            decoration = decoration,
            realVehicleName = realVehicleName?.ifBlank { null },
            manufacturerCode = manufacturerCode?.ifBlank { null },
            scale = scale,
            releaseYear = releaseYear,
            condition = parsedCondition,
            status = parsedStatus,
            locationName = locationName,
            priceCents = priceCents,
            acquisitionDate = acquisitionDate?.let(IsoDate::parseIsoOrNull),
            primaryPhotoPath = primaryPhotoPath?.ifBlank { null },
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? =
        enumValues<T>().firstOrNull { it.name == value }

    private fun List<br.com.hotwheelscollector.data.db.dao.FilterOptionRow>.toOptions(): List<FilterOption> =
        map { FilterOption(id = it.id, label = it.label) }

    private companion object {
        val BASE_QUERY = """
            SELECT e.id AS exemplar_id,
                   e.variant_id AS variant_id,
                   e.internal_code AS internal_code,
                   m.name AS model_name,
                   mf.name AS manufacturer_name,
                   s.name AS series_name,
                   (SELECT c.name
                    FROM collection_item ci
                    JOIN collection c ON c.id = ci.collection_id
                    WHERE ci.variant_id = v.id
                    ORDER BY c.name
                    LIMIT 1) AS collection_name,
                   col.name AS primary_color_name,
                   v.decoration AS decoration,
                   COALESCE(rv.make || ' ' || rv.model, '') AS real_vehicle_name,
                   v.manufacturer_code AS manufacturer_code,
                   m.scale AS scale,
                   v.release_year AS release_year,
                   e.condition AS condition,
                   e.status AS status,
                   loc.name AS location_name,
                   a.price_cents AS price_cents,
                   a.date AS acquisition_date,
                   (SELECT p.path
                    FROM photo p
                    WHERE p.exemplar_id = e.id
                    ORDER BY p.is_primary DESC, p.sort_order
                    LIMIT 1) AS primary_photo_path
            FROM exemplar e
            JOIN variant v ON v.id = e.variant_id
            JOIN car_model m ON m.id = v.model_id
            JOIN manufacturer mf ON mf.id = m.manufacturer_id
            LEFT JOIN series s ON s.id = v.series_id
            LEFT JOIN color col ON col.id = v.primary_color_id
            LEFT JOIN real_vehicle rv ON rv.id = m.real_vehicle_id
            LEFT JOIN location loc ON loc.id = e.location_id
            LEFT JOIN acquisition a ON a.exemplar_id = e.id
        """.trimIndent()
    }
}
