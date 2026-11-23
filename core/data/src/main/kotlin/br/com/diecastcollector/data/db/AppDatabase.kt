package br.com.diecastcollector.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.diecastcollector.data.db.dao.AcquisitionDao
import br.com.diecastcollector.data.db.dao.AppSettingDao
import br.com.diecastcollector.data.db.dao.BrandDao
import br.com.diecastcollector.data.db.dao.CarModelDao
import br.com.diecastcollector.data.db.dao.CollectionDao
import br.com.diecastcollector.data.db.dao.CollectionItemDao
import br.com.diecastcollector.data.db.dao.ColorDao
import br.com.diecastcollector.data.db.dao.DashboardDao
import br.com.diecastcollector.data.db.dao.DataControlDao
import br.com.diecastcollector.data.db.dao.DuplicateDao
import br.com.diecastcollector.data.db.dao.ExemplarDao
import br.com.diecastcollector.data.db.dao.FinanceDao
import br.com.diecastcollector.data.db.dao.HistoryDao
import br.com.diecastcollector.data.db.dao.LocationDao
import br.com.diecastcollector.data.db.dao.ManufacturerDao
import br.com.diecastcollector.data.db.dao.MovementDao
import br.com.diecastcollector.data.db.dao.NegotiationDao
import br.com.diecastcollector.data.db.dao.NegotiationEventDao
import br.com.diecastcollector.data.db.dao.PhotoDao
import br.com.diecastcollector.data.db.dao.RealVehicleDao
import br.com.diecastcollector.data.db.dao.RecognitionResultDao
import br.com.diecastcollector.data.db.dao.SearchDao
import br.com.diecastcollector.data.db.dao.SearchIndexDao
import br.com.diecastcollector.data.db.dao.SeriesDao
import br.com.diecastcollector.data.db.dao.ValuationDao
import br.com.diecastcollector.data.db.dao.VariantDao
import br.com.diecastcollector.data.db.dao.VariantSecondaryColorDao
import br.com.diecastcollector.data.db.dao.WishlistDao
import br.com.diecastcollector.data.db.entity.AcquisitionEntity
import br.com.diecastcollector.data.db.entity.AppSettingEntity
import br.com.diecastcollector.data.db.entity.BrandEntity
import br.com.diecastcollector.data.db.entity.CarModelEntity
import br.com.diecastcollector.data.db.entity.CollectionEntity
import br.com.diecastcollector.data.db.entity.CollectionItemEntity
import br.com.diecastcollector.data.db.entity.ColorEntity
import br.com.diecastcollector.data.db.entity.DuplicateOfferEntity
import br.com.diecastcollector.data.db.entity.ExemplarEntity
import br.com.diecastcollector.data.db.entity.HistoryEntryEntity
import br.com.diecastcollector.data.db.entity.LocationEntity
import br.com.diecastcollector.data.db.entity.ManufacturerEntity
import br.com.diecastcollector.data.db.entity.MovementEntity
import br.com.diecastcollector.data.db.entity.NegotiationEntity
import br.com.diecastcollector.data.db.entity.NegotiationEventEntity
import br.com.diecastcollector.data.db.entity.PhotoEntity
import br.com.diecastcollector.data.db.entity.RealVehicleEntity
import br.com.diecastcollector.data.db.entity.RecognitionResultEntity
import br.com.diecastcollector.data.db.entity.SearchIndexEntity
import br.com.diecastcollector.data.db.entity.SeriesEntity
import br.com.diecastcollector.data.db.entity.ValuationEntity
import br.com.diecastcollector.data.db.entity.VariantEntity
import br.com.diecastcollector.data.db.entity.VariantSecondaryColorEntity
import br.com.diecastcollector.data.db.entity.WishlistItemEntity

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
    version = 3,
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

    /** Contagens do que a exclusão total vai apagar (T146). */
    public abstract fun dataControlDao(): DataControlDao

    public companion object {
        public const val DATABASE_NAME: String = "diecast.db"

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

        /**
         * v2 → v3: foto própria do desejo, para quem procura um carro que ainda não tem.
         * Nula por padrão; a lista continua caindo na foto de um exemplar do mesmo alvo.
         *
         * O índice parcial de `variant` não está no schema exportado e o Room valida a
         * tabela inteira depois da migração; ele sai antes da validação e volta no callback.
         */
        public val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS `index_variant_fingerprint_active`")
                db.execSQL("ALTER TABLE `wishlist_item` ADD COLUMN `photo_path` TEXT")
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
