package br.com.hotwheelscollector.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.hotwheelscollector.data.db.entity.BrandEntity
import br.com.hotwheelscollector.data.db.entity.CarModelEntity
import br.com.hotwheelscollector.data.db.entity.ColorEntity
import br.com.hotwheelscollector.data.db.entity.ManufacturerEntity
import br.com.hotwheelscollector.data.db.entity.RealVehicleEntity
import br.com.hotwheelscollector.data.db.entity.SeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
public interface ManufacturerDao {

    @Query("SELECT * FROM manufacturer WHERE archived_at IS NULL ORDER BY name COLLATE NOCASE")
    public fun observeActive(): Flow<List<ManufacturerEntity>>

    @Query("SELECT * FROM manufacturer WHERE id = :id")
    public suspend fun findById(id: String): ManufacturerEntity?

    @Query("SELECT * FROM manufacturer WHERE name = :name COLLATE NOCASE LIMIT 1")
    public suspend fun findByName(name: String): ManufacturerEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: ManufacturerEntity)

    @Update
    public suspend fun update(entity: ManufacturerEntity)

    @Query("UPDATE manufacturer SET archived_at = :at, updated_at = :at WHERE id = :id")
    public suspend fun archive(id: String, at: Long)
}

@Dao
public interface BrandDao {

    @Query("SELECT * FROM brand WHERE manufacturer_id = :manufacturerId AND archived_at IS NULL ORDER BY name COLLATE NOCASE")
    public fun observeByManufacturer(manufacturerId: String): Flow<List<BrandEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: BrandEntity)

    @Update
    public suspend fun update(entity: BrandEntity)
}

@Dao
public interface RealVehicleDao {

    @Query("SELECT * FROM real_vehicle ORDER BY make, model")
    public fun observeAll(): Flow<List<RealVehicleEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: RealVehicleEntity)

    @Update
    public suspend fun update(entity: RealVehicleEntity)
}

@Dao
public interface ColorDao {

    @Query("SELECT * FROM color ORDER BY name COLLATE NOCASE")
    public fun observeAll(): Flow<List<ColorEntity>>

    @Query("SELECT * FROM color WHERE name = :name COLLATE NOCASE LIMIT 1")
    public suspend fun findByName(name: String): ColorEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: ColorEntity)
}

/** Projeção de modelo sem variante: o que a tela de incompletos mostra (FR-029). */
public data class IncompleteModelRow(
    @ColumnInfo(name = "model_id") val modelId: String,
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "manufacturer_name") val manufacturerName: String,
    @ColumnInfo(name = "scale") val scale: String,
)

@Dao
public interface CarModelDao {

    @Query("SELECT * FROM car_model WHERE archived_at IS NULL ORDER BY name COLLATE NOCASE")
    public fun observeActive(): Flow<List<CarModelEntity>>

    /**
     * Modelos ativos sem nenhuma variante ativa.
     *
     * `NOT EXISTS` em vez de `LEFT JOIN ... IS NULL` porque a intenção é explícita e o
     * índice de `variant(model_id)` continua sendo usado.
     */
    @Query(
        """
        SELECT m.id AS model_id,
               m.name AS model_name,
               mf.name AS manufacturer_name,
               m.scale AS scale
        FROM car_model m
        JOIN manufacturer mf ON mf.id = m.manufacturer_id
        WHERE m.archived_at IS NULL
          AND NOT EXISTS (
              SELECT 1 FROM variant v
              WHERE v.model_id = m.id AND v.archived_at IS NULL
          )
        ORDER BY mf.name COLLATE NOCASE, m.name COLLATE NOCASE
        """,
    )
    public fun observeWithoutVariant(): Flow<List<IncompleteModelRow>>

    @Query("SELECT * FROM car_model WHERE manufacturer_id = :manufacturerId AND archived_at IS NULL ORDER BY name COLLATE NOCASE")
    public fun observeByManufacturer(manufacturerId: String): Flow<List<CarModelEntity>>

    @Query("SELECT * FROM car_model WHERE id = :id")
    public suspend fun findById(id: String): CarModelEntity?

    @Query(
        """
        SELECT * FROM car_model
        WHERE manufacturer_id = :manufacturerId
          AND name = :name COLLATE NOCASE
          AND scale = :scale
        LIMIT 1
        """,
    )
    public suspend fun findByIdentity(manufacturerId: String, name: String, scale: String): CarModelEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: CarModelEntity)

    @Update
    public suspend fun update(entity: CarModelEntity)

    @Query("UPDATE car_model SET archived_at = :at, updated_at = :at WHERE id = :id")
    public suspend fun archive(id: String, at: Long)
}

@Dao
public interface SeriesDao {

    @Query("SELECT * FROM series WHERE manufacturer_id = :manufacturerId AND archived_at IS NULL ORDER BY name COLLATE NOCASE")
    public fun observeByManufacturer(manufacturerId: String): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE parent_id = :parentId AND archived_at IS NULL")
    public suspend fun findChildren(parentId: String): List<SeriesEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    public suspend fun insert(entity: SeriesEntity)

    @Update
    public suspend fun update(entity: SeriesEntity)
}
