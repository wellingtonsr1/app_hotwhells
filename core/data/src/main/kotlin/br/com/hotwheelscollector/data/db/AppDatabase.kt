package br.com.hotwheelscollector.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.hotwheelscollector.data.db.dao.AcquisitionDao
import br.com.hotwheelscollector.data.db.dao.AppSettingDao
import br.com.hotwheelscollector.data.db.dao.BrandDao
import br.com.hotwheelscollector.data.db.dao.CarModelDao
import br.com.hotwheelscollector.data.db.dao.CollectionDao
import br.com.hotwheelscollector.data.db.dao.CollectionItemDao
import br.com.hotwheelscollector.data.db.dao.ColorDao
import br.com.hotwheelscollector.data.db.dao.DashboardDao
import br.com.hotwheelscollector.data.db.dao.ExemplarDao
import br.com.hotwheelscollector.data.db.dao.FinanceDao
import br.com.hotwheelscollector.data.db.dao.HistoryDao
import br.com.hotwheelscollector.data.db.dao.LocationDao
import br.com.hotwheelscollector.data.db.dao.ManufacturerDao
import br.com.hotwheelscollector.data.db.dao.MovementDao
import br.com.hotwheelscollector.data.db.dao.NegotiationDao
import br.com.hotwheelscollector.data.db.dao.NegotiationEventDao
import br.com.hotwheelscollector.data.db.dao.PhotoDao
import br.com.hotwheelscollector.data.db.dao.RealVehicleDao
import br.com.hotwheelscollector.data.db.dao.RecognitionResultDao
import br.com.hotwheelscollector.data.db.dao.SearchDao
import br.com.hotwheelscollector.data.db.dao.SearchIndexDao
import br.com.hotwheelscollector.data.db.dao.SeriesDao
import br.com.hotwheelscollector.data.db.dao.ValuationDao
import br.com.hotwheelscollector.data.db.dao.VariantDao
import br.com.hotwheelscollector.data.db.dao.VariantSecondaryColorDao
import br.com.hotwheelscollector.data.db.dao.WishlistDao
import br.com.hotwheelscollector.data.db.entity.AcquisitionEntity
import br.com.hotwheelscollector.data.db.entity.AppSettingEntity
import br.com.hotwheelscollector.data.db.entity.BrandEntity
import br.com.hotwheelscollector.data.db.entity.CarModelEntity
import br.com.hotwheelscollector.data.db.entity.CollectionEntity
import br.com.hotwheelscollector.data.db.entity.CollectionItemEntity
import br.com.hotwheelscollector.data.db.entity.ColorEntity
import br.com.hotwheelscollector.data.db.entity.ExemplarEntity
import br.com.hotwheelscollector.data.db.entity.HistoryEntryEntity
import br.com.hotwheelscollector.data.db.entity.LocationEntity
import br.com.hotwheelscollector.data.db.entity.ManufacturerEntity
import br.com.hotwheelscollector.data.db.entity.MovementEntity
import br.com.hotwheelscollector.data.db.entity.NegotiationEntity
import br.com.hotwheelscollector.data.db.entity.NegotiationEventEntity
import br.com.hotwheelscollector.data.db.entity.PhotoEntity
import br.com.hotwheelscollector.data.db.entity.RealVehicleEntity
import br.com.hotwheelscollector.data.db.entity.RecognitionResultEntity
import br.com.hotwheelscollector.data.db.entity.SearchIndexEntity
import br.com.hotwheelscollector.data.db.entity.SeriesEntity
import br.com.hotwheelscollector.data.db.entity.ValuationEntity
import br.com.hotwheelscollector.data.db.entity.VariantEntity
import br.com.hotwheelscollector.data.db.entity.VariantSecondaryColorEntity
import br.com.hotwheelscollector.data.db.entity.WishlistItemEntity

/**
 * Banco local, versão 1, com todas as tabelas de `data-model.md`.
 *
 * Regras que valem para toda abertura:
 * - `PRAGMA foreign_keys = ON` — sem isso o SQLite ignora as FKs declaradas (Art. I.5);
 * - índice único **parcial** de `variant.fingerprint`, que o `@Index` do Room não
 *   representa e que é o que efetiva FR-040: variantes ativas não se repetem, mas
 *   arquivar uma variante libera o fingerprint para uso legítimo.
 *
 * `exportSchema = true`: o JSON em `core/data/schemas/` é revisado em cada alteração (R.8).
 * Migração destrutiva é proibida — falha de migração aborta e preserva o banco.
 */
@Database(
    entities = [
        ManufacturerEntity::class,
        BrandEntity::class,
        RealVehicleEntity::class,
        ColorEntity::class,
        CarModelEntity::class,
        SeriesEntity::class,
        VariantEntity::class,
        VariantSecondaryColorEntity::class,
        ExemplarEntity::class,
        PhotoEntity::class,
        LocationEntity::class,
        MovementEntity::class,
        AcquisitionEntity::class,
        ValuationEntity::class,
        CollectionEntity::class,
        CollectionItemEntity::class,
        WishlistItemEntity::class,
        NegotiationEntity::class,
        NegotiationEventEntity::class,
        RecognitionResultEntity::class,
        HistoryEntryEntity::class,
        AppSettingEntity::class,
        SearchIndexEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
public abstract class AppDatabase : RoomDatabase() {

    public abstract fun manufacturerDao(): ManufacturerDao

    public abstract fun brandDao(): BrandDao

    public abstract fun realVehicleDao(): RealVehicleDao

    public abstract fun colorDao(): ColorDao

    public abstract fun carModelDao(): CarModelDao

    public abstract fun seriesDao(): SeriesDao

    public abstract fun variantDao(): VariantDao

    public abstract fun variantSecondaryColorDao(): VariantSecondaryColorDao

    public abstract fun exemplarDao(): ExemplarDao

    public abstract fun photoDao(): PhotoDao

    public abstract fun locationDao(): LocationDao

    public abstract fun movementDao(): MovementDao

    public abstract fun acquisitionDao(): AcquisitionDao

    public abstract fun valuationDao(): ValuationDao

    public abstract fun collectionDao(): CollectionDao

    public abstract fun collectionItemDao(): CollectionItemDao

    public abstract fun wishlistDao(): WishlistDao

    public abstract fun negotiationDao(): NegotiationDao

    public abstract fun negotiationEventDao(): NegotiationEventDao

    public abstract fun recognitionResultDao(): RecognitionResultDao

    public abstract fun historyDao(): HistoryDao

    public abstract fun appSettingDao(): AppSettingDao

    public abstract fun searchIndexDao(): SearchIndexDao

    /** Busca do acervo; só leitura (FR-114–FR-122). */
    public abstract fun searchDao(): SearchDao

    /** Só leitura agregada: o painel não grava nada (FR-001–FR-011). */
    public abstract fun dashboardDao(): DashboardDao

    /** Só leitura agregada: o financeiro não grava nada (FR-077–FR-081). */
    public abstract fun financeDao(): FinanceDao

    public companion object {
        public const val DATABASE_NAME: String = "hotwheels.db"

        /** Executado em toda abertura; `onCreate` roda só na criação do arquivo. */
        public val CALLBACK: Callback = object : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys = ON")
                // Índice de busca auto-mantido; idempotente a cada abertura (FR-114–FR-122).
                SearchIndexSync.install(db)
            }

            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_variant_fingerprint_active " +
                        "ON variant(fingerprint) WHERE archived_at IS NULL",
                )
            }
        }
    }
}
