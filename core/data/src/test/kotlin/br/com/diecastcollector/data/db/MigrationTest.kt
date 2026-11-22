package br.com.diecastcollector.data.db

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Harness de migração (T010, R.8).
 *
 * Nesta versão só existe o schema 1, mas o teste já faz o que importa:
 * - `MigrationTestHelper` cria o banco a partir de `schemas/1.json`, então **qualquer
 *   divergência entre entidades e schema exportado falha aqui**;
 * - o banco real do Room é aberto para conferir o que o `MigrationTestHelper` não executa:
 *   o callback (índice único parcial de `fingerprint`) e o `PRAGMA foreign_keys`.
 *
 * Nota que vale registrar: `MigrationTestHelper` cria tabelas direto do JSON e **não roda
 * callbacks** de `RoomDatabase` — por isso o índice parcial é verificado no banco real.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class MigrationTest {

    @get:Rule
    public val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    public fun schemaV1CriaTabelasDoModelo() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            val tables = db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name",
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.getString(0))
                }
            }

            val expected = listOf(
                "acquisition", "app_setting", "brand", "car_model", "collection",
                "collection_item", "color", "exemplar", "history_entry", "location",
                "manufacturer", "movement", "negotiation", "negotiation_event", "photo",
                "real_vehicle", "recognition_result", "series", "variant",
                "variant_secondary_color", "valuation", "wishlist_item",
            )
            assertThat(tables).containsAtLeastElementsIn(expected)
        }
    }

    /**
     * v1 → v2 (US7): tabela `duplicate_offer` e coluna `negotiation.received_exemplar_id`.
     * `runMigrationsAndValidate` compara o banco migrado com `2.json`: se a migração
     * esquecer uma coluna ou um índice, o teste falha aqui, não no aparelho do usuário.
     */
    @Test
    public fun migracaoV1ParaV2AcrescentaDisponibilidadeEExemplarRecebido() {
        helper.createDatabase(TEST_DB_V2, 1).close()

        helper.runMigrationsAndValidate(
            TEST_DB_V2,
            2,
            true,
            AppDatabase.MIGRATION_1_2,
        ).use { db ->
            val columns = db.query("PRAGMA table_info(negotiation)").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.getString(1))
                }
            }
            val tables = db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'duplicate_offer'",
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.getString(0))
                }
            }

            assertThat(columns).contains("received_exemplar_id")
            assertThat(tables).containsExactly("duplicate_offer")
        }
    }

    /**
     * Regressão do aparelho: no banco real, `variant` tem o índice parcial criado pelo
     * callback e o Room o inclui na validação da migração. Sem removê-lo antes de validar,
     * a abertura falha com «Migration didn't properly handle: variant».
     * `createDatabase` do helper não roda callbacks, por isso o índice é criado à mão.
     */
    @Test
    public fun migracaoAceitaBancoRealComIndiceParcialDeFingerprint() {
        helper.createDatabase(TEST_DB_V2, 1).use { db ->
            db.execSQL(PARTIAL_INDEX_SQL)
        }

        helper.runMigrationsAndValidate(
            TEST_DB_V2,
            2,
            true,
            AppDatabase.MIGRATION_1_2,
        ).use { db ->
            val indexCount = db.query(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' " +
                    "AND name = 'index_variant_fingerprint_active'",
            ).use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }

            // Some daqui para a validação; o callback recria em `onOpen`.
            assertThat(indexCount).isEqualTo(0)
        }
    }

    /**
     * Garantia para migrações futuras: qualquer banco real na versão N tem o índice parcial
     * (o callback o criou na primeira abertura), então toda migração N → N+1 é executada aqui
     * com o índice presente no banco de partida. Uma migração que altere `variant` sem removê-lo
     * antes da validação falha neste teste, e não no aparelho do usuário.
     */
    @Test
    public fun cadaMigracaoAceitaBancoComIndiceParcialDeFingerprint() {
        for (migration in migrations.sortedBy { it.startVersion }) {
            val name = "migration-step-${migration.startVersion}-${migration.endVersion}.db"

            helper.createDatabase(name, migration.startVersion).use { db ->
                db.execSQL(PARTIAL_INDEX_SQL)
            }

            helper.runMigrationsAndValidate(
                name,
                migration.endVersion,
                true,
                migration,
            ).close()
        }
    }

    /**
     * Caminho real do aparelho: arquivo em v1 com o índice do callback, aberto pelo `Room`
     * com as migrações. Depois de migrar e validar, o callback precisa devolver o índice —
     * senão FR-040 (fingerprint único entre variantes ativas) fica sem efeito.
     */
    @Test
    public fun aberturaDoBancoMigradoRecriaIndiceParcial() {
        helper.createDatabase(MIGRATED_DB, 1).use { db ->
            db.execSQL(PARTIAL_INDEX_SQL)
        }

        val database = Room
            .databaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                AppDatabase::class.java,
                MIGRATED_DB,
            )
            .addMigrations(*migrations.toTypedArray())
            .addCallback(AppDatabase.CALLBACK)
            .build()
        try {
            val readable = database.openHelper.readableDatabase
            val indexSql = readable.query(
                "SELECT sql FROM sqlite_master WHERE type = 'index' " +
                    "AND name = 'index_variant_fingerprint_active'",
            ).use { cursor ->
                if (cursor.moveToNext()) cursor.getString(0) else null
            }
            assertThat(indexSql).contains("UNIQUE")
            assertThat(indexSql).contains("archived_at IS NULL")
        } finally {
            database.close()
        }
    }

    /**
     * A lista acima é escrita à mão; se uma versão nova entrar no `@Database` sem a migração
     * correspondente, este teste aponta a lacuna em vez de deixar o aparelho descobrir.
     */
    @Test
    public fun migracoesCobremTodasAsVersoesDoSchema() {
        // A anotação `@Database` não é retida em runtime; a versão vem de um banco novo.
        val currentVersion = openRuntimeDatabase().let { database ->
            try {
                database.openHelper.readableDatabase.version
            } finally {
                database.close()
            }
        }
        val steps = migrations.sortedBy { it.startVersion }

        assertThat(steps.first().startVersion).isEqualTo(1)
        assertThat(steps.last().endVersion).isEqualTo(currentVersion)
        steps.zipWithNext().forEach { (previous, next) ->
            assertThat(previous.endVersion).isEqualTo(next.startVersion)
        }
    }

    @Test
    public fun callbackCriaIndiceUnicoParcialDeFingerprint() {
        val database = openRuntimeDatabase()
        try {
            val indexSql = database.openHelper.readableDatabase.query(
                "SELECT sql FROM sqlite_master WHERE type = 'index' " +
                    "AND name = 'index_variant_fingerprint_active'",
            ).use { cursor ->
                if (cursor.moveToNext()) cursor.getString(0) else null
            }

            assertThat(indexSql).isNotNull()
            assertThat(indexSql).contains("archived_at IS NULL")
        } finally {
            database.close()
        }
    }

    @Test
    public fun foreignKeysSaoAplicadas() {
        val database = openRuntimeDatabase()
        try {
            val writable = database.openHelper.writableDatabase

            // Exemplar com variante inexistente precisa ser recusado pela FK.
            val failed = runCatching {
                writable.execSQL(
                    """
                    INSERT INTO exemplar
                        (id, variant_id, internal_code, condition, status, row_version, created_at, updated_at)
                    VALUES ('e1', 'variante-que-nao-existe', 'HW-0001', 'SEALED', 'OWNED', 0, 0, 0)
                    """.trimIndent(),
                )
            }.isFailure

            assertThat(failed).isTrue()
        } finally {
            database.close()
        }
    }

    private fun openRuntimeDatabase(): AppDatabase = Room
        .databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            RUNTIME_DB,
        )
        .addCallback(AppDatabase.CALLBACK)
        .build()

    /** Toda migração registrada no banco; a ordem vem de `startVersion`. */
    private val migrations: List<Migration> = listOf(AppDatabase.MIGRATION_1_2)

    private companion object {
        const val TEST_DB = "migration-test.db"
        const val TEST_DB_V2 = "migration-v2-test.db"
        const val RUNTIME_DB = "runtime-test.db"
        const val MIGRATED_DB = "migrated-runtime-test.db"
        const val PARTIAL_INDEX_SQL =
            "CREATE UNIQUE INDEX IF NOT EXISTS index_variant_fingerprint_active " +
                "ON variant(fingerprint) WHERE archived_at IS NULL"
    }
}
