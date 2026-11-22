package br.com.diecastcollector.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import br.com.diecastcollector.data.db.entity.AcquisitionEntity
import br.com.diecastcollector.data.db.entity.CarModelEntity
import br.com.diecastcollector.data.db.entity.CollectionEntity
import br.com.diecastcollector.data.db.entity.CollectionItemEntity
import br.com.diecastcollector.data.db.entity.ColorEntity
import br.com.diecastcollector.data.db.entity.ExemplarEntity
import br.com.diecastcollector.data.db.entity.LocationEntity
import br.com.diecastcollector.data.db.entity.ManufacturerEntity
import br.com.diecastcollector.data.db.entity.RealVehicleEntity
import br.com.diecastcollector.data.db.entity.SeriesEntity
import br.com.diecastcollector.data.db.entity.VariantEntity
import br.com.diecastcollector.data.db.entity.VariantSecondaryColorEntity
import kotlinx.coroutines.flow.Flow

/** Linha projetada da busca; campos crus, sem rótulo de apresentação. */
public data class SearchResultRow(
    @ColumnInfo(name = "exemplar_id") val exemplarId: String,
    @ColumnInfo(name = "variant_id") val variantId: String,
    @ColumnInfo(name = "internal_code") val internalCode: String,
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "manufacturer_name") val manufacturerName: String,
    @ColumnInfo(name = "series_name") val seriesName: String?,
    @ColumnInfo(name = "collection_name") val collectionName: String?,
    @ColumnInfo(name = "primary_color_name") val primaryColorName: String?,
    @ColumnInfo(name = "decoration") val decoration: String?,
    @ColumnInfo(name = "real_vehicle_name") val realVehicleName: String?,
    @ColumnInfo(name = "manufacturer_code") val manufacturerCode: String?,
    @ColumnInfo(name = "scale") val scale: String,
    @ColumnInfo(name = "release_year") val releaseYear: Int?,
    @ColumnInfo(name = "condition") val condition: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "location_name") val locationName: String?,
    @ColumnInfo(name = "price_cents") val priceCents: Long?,
    @ColumnInfo(name = "acquisition_date") val acquisitionDate: String?,
    @ColumnInfo(name = "primary_photo_path") val primaryPhotoPath: String?,
)

public data class FilterOptionRow(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "label") val label: String,
)

public data class YearOptionRow(
    @ColumnInfo(name = "year") val year: Int,
)

public data class ScaleOptionRow(
    @ColumnInfo(name = "scale") val scale: String,
)

/**
 * Consulta do acervo e opções de filtro (FR-114–FR-122).
 *
 * A busca é montada dinamicamente porque são 13 dimensões opcionais; `@RawQuery` evita
 * 13 métodos e mantém o `Flow` reativo às tabelas de origem.
 */
@Dao
public interface SearchDao {

    @RawQuery(
        observedEntities = [
            ExemplarEntity::class,
            VariantEntity::class,
            CarModelEntity::class,
            ManufacturerEntity::class,
            SeriesEntity::class,
            ColorEntity::class,
            VariantSecondaryColorEntity::class,
            LocationEntity::class,
            AcquisitionEntity::class,
            CollectionEntity::class,
            CollectionItemEntity::class,
            RealVehicleEntity::class,
        ],
    )
    public fun observeResults(query: SupportSQLiteQuery): Flow<List<SearchResultRow>>

    @Query("SELECT id, name AS label FROM manufacturer WHERE archived_at IS NULL ORDER BY name COLLATE NOCASE")
    public fun observeManufacturers(): Flow<List<FilterOptionRow>>

    @Query(
        """
        SELECT m.id AS id, m.name || ' · ' || mf.name AS label
        FROM car_model m
        JOIN manufacturer mf ON mf.id = m.manufacturer_id
        WHERE m.archived_at IS NULL
        ORDER BY m.name COLLATE NOCASE
        """,
    )
    public fun observeModels(): Flow<List<FilterOptionRow>>

    @Query("SELECT id, name AS label FROM series WHERE archived_at IS NULL ORDER BY name COLLATE NOCASE")
    public fun observeSeries(): Flow<List<FilterOptionRow>>

    @Query("SELECT id, name AS label FROM collection WHERE archived_at IS NULL ORDER BY name COLLATE NOCASE")
    public fun observeCollections(): Flow<List<FilterOptionRow>>

    @Query("SELECT id, name AS label FROM color ORDER BY name COLLATE NOCASE")
    public fun observeColors(): Flow<List<FilterOptionRow>>

    @Query("SELECT id, name AS label FROM location WHERE archived_at IS NULL ORDER BY name COLLATE NOCASE")
    public fun observeLocations(): Flow<List<FilterOptionRow>>

    @Query("SELECT DISTINCT scale FROM car_model WHERE archived_at IS NULL AND scale <> '' ORDER BY scale")
    public fun observeScales(): Flow<List<ScaleOptionRow>>

    @Query(
        """
        SELECT DISTINCT release_year AS year
        FROM variant
        WHERE release_year IS NOT NULL
        ORDER BY release_year DESC
        """,
    )
    public fun observeYears(): Flow<List<YearOptionRow>>
}
