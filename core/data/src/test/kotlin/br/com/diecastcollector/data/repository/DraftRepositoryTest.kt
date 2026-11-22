package br.com.diecastcollector.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.diecastcollector.data.db.AppDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Testes do DraftRepository (T047 - US2, FR-023). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class DraftRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: DraftRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = DraftRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `salva e recupera rascunho com sucesso`() = runBlocking {
        repository.saveDraft("new_entry", """{"model":"Twin Mill","manufacturer":"Mattel"}""")

        val draft = repository.getDraft("new_entry")
        assertThat(draft).isEqualTo("""{"model":"Twin Mill","manufacturer":"Mattel"}""")
    }

    @Test
    fun `observeDraft emite alteracoes no rascunho`() = runBlocking {
        val initial = repository.observeDraft("catalog").first()
        assertThat(initial).isNull()

        repository.saveDraft("catalog", "draft_v1")
        val updated = repository.observeDraft("catalog").first()
        assertThat(updated).isEqualTo("draft_v1")
    }

    @Test
    fun `clearDraft remove o rascunho persistido`() = runBlocking {
        repository.saveDraft("temp", "dados_para_apagar")
        assertThat(repository.getDraft("temp")).isNotNull()

        repository.clearDraft("temp")
        assertThat(repository.getDraft("temp")).isNull()
    }
}
