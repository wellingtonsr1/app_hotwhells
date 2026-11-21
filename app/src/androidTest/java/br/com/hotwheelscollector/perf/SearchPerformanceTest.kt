package br.com.hotwheelscollector.perf

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.hotwheelscollector.data.db.AppDatabase
import br.com.hotwheelscollector.data.repository.SearchRepositoryImpl
import br.com.hotwheelscollector.domain.model.CatalogSort
import br.com.hotwheelscollector.domain.model.ExemplarStatus
import br.com.hotwheelscollector.domain.model.SearchFilters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T058 — desempenho da busca com 10.000 exemplares no banco real (R.7).
 *
 * Roda no aparelho: mede a consulta completa (FTS + filtros + ordenação + mapeamento)
 * incluindo a primeira coleta do `Flow`. Os limites são os do requisito: busca e filtros
 * abaixo de 300 ms e listagem completa abaixo de 1 s.
 *
 * O teste é de medição, não de correção: o resultado só vale no aparelho em que rodou.
 */
@RunWith(AndroidJUnit4::class)
public class SearchPerformanceTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: SearchRepositoryImpl

    @Before
    public fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.CALLBACK)
            .allowMainThreadQueries()
            .build()
        repository = SearchRepositoryImpl(database)
        seed()
    }

    @After
    public fun tearDown() {
        database.close()
    }

    @Test
    public fun buscaFtsRespondeEmMenosDe300Ms() = runBlocking {
        val elapsed = measure { repository.observeResults(SearchFilters(query = "twin")).first() }

        assertTrue("busca levou ${elapsed}ms (limite 300ms)", elapsed < 300)
        // A busca padrão mostra só exemplares possuídos (OWNED): 4 de cada 5 semeados.
        assertEquals(OWNED_PER_VARIANT, repository.observeResults(SearchFilters(query = "twin")).first().size)
    }

    @Test
    public fun listagemCompletaRespondeEmMenosDe1S() = runBlocking {
        val results = mutableListOf<Int>()
        val elapsed = measure {
            results += repository.observeResults(SearchFilters()).first().size
        }

        assertTrue("listagem levou ${elapsed}ms (limite 1000ms)", elapsed < 1000)
        assertEquals(OWNED_TOTAL, results.single())
    }

    @Test
    public fun filtrosCombinadosRespondemEmMenosDe300Ms() = runBlocking {
        val elapsed = measure {
            repository.observeResults(
                SearchFilters(
                    manufacturerIds = setOf("m1"),
                    colorIds = setOf("red"),
                    statuses = setOf(ExemplarStatus.OWNED),
                ),
            ).first()
        }

        assertTrue("filtros combinados levaram ${elapsed}ms (limite 300ms)", elapsed < 300)
    }

    @Test
    public fun ordenacaoPorAnoRespondeEmMenosDe300Ms() = runBlocking {
        val elapsed = measure {
            repository.observeResults(SearchFilters(sort = CatalogSort.YEAR)).first()
        }

        assertTrue("ordenação levou ${elapsed}ms (limite 300ms)", elapsed < 300)
    }

    @Test
    public fun buscaSemAcentoEncontraRegistroAcentuado() = runBlocking {
        val results = repository.observeResults(SearchFilters(query = "colecao")).first()

        // O tokenizer unicode61 remove diacríticos: "colecao" acha "Coleção Especial".
        assertTrue("busca sem acento não encontrou resultados", results.isNotEmpty())
    }

    private inline fun measure(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / 1_000_000
    }

    /** 100 modelos × 100 exemplares = 10.000 exemplares, com índice FTS pelos gatilhos. */
    private fun seed() {
        val db = database.openHelper.writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("INSERT INTO manufacturer (id, name, created_at, updated_at) VALUES ('m1', 'Mattel', 0, 0)")
            db.execSQL("INSERT INTO manufacturer (id, name, created_at, updated_at) VALUES ('m2', 'Spin Master', 0, 0)")
            db.execSQL("INSERT INTO color (id, name) VALUES ('red', 'Vermelho')")
            db.execSQL("INSERT INTO color (id, name) VALUES ('blue', 'Azul')")

            for (index in 0 until VARIANT_COUNT) {
                val manufacturer = if (index % 2 == 0) "m1" else "m2"
                val modelName = when (index) {
                    0 -> "Twin Mill"
                    1 -> "Coleção Especial"
                    else -> "Modelo $index"
                }
                db.execSQL(
                    "INSERT INTO car_model (id, manufacturer_id, name, scale, created_at, updated_at) " +
                        "VALUES (?, ?, ?, '1:64', 0, 0)",
                    arrayOf("model$index", manufacturer, modelName),
                )
                val color = if (index % 2 == 0) "red" else "blue"
                db.execSQL(
                    "INSERT INTO variant (id, model_id, fingerprint, primary_color_id, release_year, " +
                        "created_at, updated_at) VALUES (?, ?, ?, ?, ?, 0, 0)",
                    arrayOf<Any?>("variant$index", "model$index", "fp-$index", color, 2000 + index),
                )
            }

            val insert = db.compileStatement(
                "INSERT INTO exemplar (id, variant_id, internal_code, condition, status, " +
                    "row_version, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 0, 0, 0)",
            )
            try {
                for (variant in 0 until VARIANT_COUNT) {
                    for (copy in 0 until EXEMPLARS_PER_VARIANT) {
                        insert.clearBindings()
                        insert.bindString(1, "e-$variant-$copy")
                        insert.bindString(2, "variant$variant")
                        insert.bindString(3, "HW-$variant-$copy")
                        insert.bindString(4, if (copy % 2 == 0) "SEALED" else "LOOSE")
                        insert.bindString(5, if (copy % 5 == 0) "SOLD" else "OWNED")
                        insert.executeInsert()
                    }
                }
            } finally {
                insert.close()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private companion object {
        const val VARIANT_COUNT = 100
        const val EXEMPLARS_PER_VARIANT = 100
        const val OWNED_PER_VARIANT = EXEMPLARS_PER_VARIANT * 4 / 5
        const val OWNED_TOTAL = VARIANT_COUNT * OWNED_PER_VARIANT
    }
}
