package br.com.diecastcollector.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.entity.RecognitionResultEntity
import br.com.diecastcollector.domain.model.RecognitionSource
import br.com.diecastcollector.domain.recognition.RecognitionApplication
import br.com.diecastcollector.domain.recognition.RecognitionField
import br.com.diecastcollector.domain.recognition.RecognitionProposal
import br.com.diecastcollector.domain.recognition.RecognitionSuggestion
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * T129 — a leitura fica registrada com o que foi aceito, sem criar exemplar (FR-104/113).
 *
 * O que o teste protege: `applied_fields` guarda só os campos confirmados, o código lido
 * não se perde e a tabela de exemplares continua vazia depois de uma leitura.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class RecognitionRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: RecognitionRepositoryImpl

    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-11-21T10:00:00Z"),
        ZoneOffset.UTC,
    )

    @Before
    public fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.CALLBACK)
            .allowMainThreadQueries()
            .build()
        repository = RecognitionRepositoryImpl(database.recognitionResultDao(), clock)
    }

    @After
    public fun tearDown() {
        database.close()
    }

    @Test
    public fun `registra codigo lido com os campos aceitos`(): Unit = runBlocking {
        val proposal = barcodeProposal()

        repository.record(
            proposal = proposal,
            applied = RecognitionApplication.accept(proposal, setOf(RecognitionField.CODE)),
            draftId = "draft-1",
        )

        val saved = database.recognitionResultDao().findByDraft("draft-1").single()
        assertThat(saved.source).isEqualTo(RecognitionSource.BARCODE.name)
        assertThat(saved.appliedFields).isEqualTo(RecognitionField.CODE.name)
        assertThat(saved.payloadJson).contains("HKC25")
        assertThat(saved.rawValue()).isEqualTo("HKC25")
        assertThat(saved.createdAt).isEqualTo(clock.millis())
    }

    @Test
    public fun `leitura sem aceite mantem applied_fields vazio`(): Unit = runBlocking {
        repository.record(
            proposal = barcodeProposal(),
            applied = RecognitionApplication.NONE,
            draftId = "draft-2",
        )

        val saved = database.recognitionResultDao().findByDraft("draft-2").single()
        assertThat(saved.appliedFields).isNull()
        assertThat(saved.payloadJson).contains("\"applied_fields\":[]")
    }

    @Test
    public fun `reconhecer nao cria exemplar`(): Unit = runBlocking {
        repository.record(
            proposal = barcodeProposal(),
            applied = RecognitionApplication.accept(
                barcodeProposal(),
                setOf(RecognitionField.CODE),
            ),
            draftId = "draft-3",
        )

        val exemplars = database.query("SELECT COUNT(*) FROM exemplar", emptyArray<Any?>())
            .use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
        assertThat(exemplars).isEqualTo(0)
    }

    private fun barcodeProposal(): RecognitionProposal = RecognitionProposal.of(
        source = RecognitionSource.BARCODE,
        provider = "mlkit-barcode",
        rawValue = "HKC25",
        suggestions = listOf(
            RecognitionSuggestion(RecognitionField.CODE, "HKC25", 1.0),
        ),
    )

    /** `rawValue` vive dentro do payload; o helper evita depender do formato interno. */
    private fun RecognitionResultEntity.rawValue(): String? =
        JSONObject(payloadJson).optString("raw_value").takeIf { it.isNotEmpty() }
}
