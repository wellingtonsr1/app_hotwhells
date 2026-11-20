package br.com.hotwheelscollector.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Contagens do acervo para a confirmação da exclusão (FR-157). */
public data class UserDataCountsRow(
    @ColumnInfo(name = "exemplars") val exemplars: Int,
    @ColumnInfo(name = "models") val models: Int,
    @ColumnInfo(name = "photos") val photos: Int,
    @ColumnInfo(name = "collections") val collections: Int,
    @ColumnInfo(name = "wishlist_items") val wishlistItems: Int,
    @ColumnInfo(name = "locations") val locations: Int,
)

/**
 * Leitura do que será apagado (T146). As contagens saem das próprias tabelas, sem
 * contador paralelo — se a exclusão deixar algo, o número denuncia.
 */
@Dao
public interface DataControlDao {

    @Query(
        """
        SELECT (SELECT COUNT(*) FROM exemplar) AS exemplars,
               (SELECT COUNT(*) FROM car_model) AS models,
               (SELECT COUNT(*) FROM photo) AS photos,
               (SELECT COUNT(*) FROM collection) AS collections,
               (SELECT COUNT(*) FROM wishlist_item) AS wishlist_items,
               (SELECT COUNT(*) FROM location) AS locations
        """,
    )
    public fun observeCounts(): Flow<UserDataCountsRow>
}
