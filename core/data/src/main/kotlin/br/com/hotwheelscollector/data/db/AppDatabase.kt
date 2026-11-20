package br.com.hotwheelscollector.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.hotwheelscollector.data.db.dao.AcquisitionDao
import br.com.hotwheelscollector.data.db.dao.AppSettingDao
import br.com.hotwheelscollector.data.db.dao.BrandDao
import br.com.hotwheelscollector.data.db.dao.CarModelDao
import br.com.hotwheelscollector.data.db.dao.CollectionDao
import br.com.hotwheelscollector.data.db.dao.CollectionItemDao
import br.com.hotwheelscollector.data.db.dao.ColorDao
import br.com.hotwheelscollector.data.db.dao.DashboardDao
import br.com.hotwheelscollector.data.db.dao.DuplicateDao
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
import br.com.hotwheelscollector.data.db.entity.DuplicateOfferEntity
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
 * Banco local, versão 2, com todas as tabelas de `data-model.md`.
 *
 * A v2 acrescenta `duplicate_offer` (disponibilidade para troca/venda, FR-066–FR-067)
 * e `negotiation.received_exemplar_id` (exemplar recebido na troca, FR-071).
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
        DuplicateOfferEntity::class,
    ],
    version = 2,
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

    public abstract fun duplicateDao(): DuplicateDao

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

        /**
         * v1 → v2: nova tabela de disponibilidade e o vínculo do exemplar recebido.
         * Nada é destruído; as colunas novas são nulas por padrão.
         *
         * `index_variant_fingerprint_active` é criado pelo callback, não pelo `@Entity`, e
         * por isso não aparece no schema exportado. O Room lê **todos** os índices da tabela
         * ao validar a migração, então um banco real (que tem o índice) falharia mesmo com a
         * migração correta. Ele é removido antes da validação e recriado em `CALLBACK.onOpen`;
         * toda migração futura que altere `variant` precisa repetir esse par.
         */
        public val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS `index_variant_fingerprint_active`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `duplicate_offer` (
                        `id` TEXT NOT NULL,
                        `exemplar_id` TEXT NOT NULL,
                        `availability` TEXT NOT NULL,
                        `condition` TEXT,
                        `asking_price_cents` INTEGER,
                        `notes` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`exemplar_id`) REFERENCES `exemplar`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_duplicate_offer_exemplar_id` ON `duplicate_offer` (`exemplar_id`)",
                )
                db.execSQL(
                    "ALTER TABLE `negotiation` ADD COLUMN `received_exemplar_id` TEXT " +
                        "REFERENCES `exemplar`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                        "`index_negotiation_received_exemplar_id` ON `negotiation` (`received_exemplar_id`)",
                )
            }
        }

        /** Executado em toda abertura; `onCreate` roda só na criação do arquivo. */
        public val CALLBACK: Callback = object : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys = ON")
                // Recriado aqui, e não só em `onCreate`: a validação de migração exige que o
                // índice parcial não exista, então ele precisa voltar depois dela.
                createPartialFingerprintIndex(db)
                // Índice de busca auto-mantido; idempotente a cada abertura (FR-114–FR-122).
                SearchIndexSync.install(db)
            }

            override fun onCreate(db: SupportSQLiteDatabase) {
                createPartialFingerprintIndex(db)
            }
        }

        private fun createPartialFingerprintIndex(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "index_variant_fingerprint_active " +
                    "ON variant(fingerprint) WHERE archived_at IS NULL",
            )
        }
    }
}
