package br.com.hotwheelscollector.feature.settings

import br.com.hotwheelscollector.domain.backup.BackupService
import br.com.hotwheelscollector.domain.backup.BackupSummary
import br.com.hotwheelscollector.domain.backup.ValidatedBackup
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.repository.DataControlRepository
import br.com.hotwheelscollector.domain.repository.UserDataCounts
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Controle de dados (T146, FR-142, FR-157, FR-163).
 *
 * O que o teste protege: o estado mostra a contagem real; exportar grava o pacote no
 * destino e informa o tamanho; excluir só marca `deleted` depois do sucesso e, quando
 * falha, o motivo chega à tela sem esconder o erro atrás de uma exclusão "concluída".
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class DataControlViewModelTest {

    private val counts = MutableStateFlow(
        UserDataCounts(exemplars = 3, models = 2, photos = 4, collections = 1),
    )
    private val dataControl = FakeDataControl(counts)
    private val backup = FakeBackupService()

    @Before
    public fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    public fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): DataControlViewModel = DataControlViewModel(
        context = RuntimeEnvironment.getApplication(),
        dataControlRepository = dataControl,
        backupService = backup,
    )

    @Test
    public fun mostraAContagemDoQueEstaGravado() = runBlocking {
        val viewModel = viewModel()
        awaitState(viewModel) { it.counts.exemplars == 3 }

        val state = viewModel.state.value
        assertThat(state.counts.photos).isEqualTo(4)
        assertThat(state.counts.collections).isEqualTo(1)
        assertThat(state.deleted).isFalse()
    }

    @Test
    public fun excluirMarcaConcluidoEDeixaAContagemZerada() = runBlocking {
        val viewModel = viewModel()
        awaitState(viewModel) { it.counts.exemplars == 3 }

        viewModel.deleteAll()
        awaitState(viewModel) { it.deleted }

        assertThat(dataControl.deleted).isTrue()
        assertThat(viewModel.state.value.failure).isNull()
        assertThat(viewModel.state.value.counts.isEmpty).isTrue()
    }

    @Test
    public fun falhaNaExclusaoNaoMarcaConcluidoEExpoeOMotivo() = runBlocking {
        dataControl.failure = DomainError.Storage("Disco cheio.")
        val viewModel = viewModel()
        awaitState(viewModel) { it.counts.exemplars == 3 }

        viewModel.deleteAll()
        awaitState(viewModel) { it.failure == DataControlViewModel.Failure.DELETE }

        assertThat(viewModel.state.value.deleted).isFalse()
        assertThat(viewModel.state.value.failureDetail).isEqualTo("Disco cheio.")
        assertThat(viewModel.state.value.counts.exemplars).isEqualTo(3)
    }

    @Test
    public fun exportarGravaOPacoteEInformaOTamanho() = runBlocking {
        val output = ByteArrayOutputStream()
        val viewModel = viewModel()
        awaitState(viewModel) { it.counts.exemplars == 3 }

        viewModel.exportTo(output)
        awaitState(viewModel) { it.exportedBytes != null }

        assertThat(backup.received.size()).isGreaterThan(0)
        assertThat(output.toByteArray()).isEqualTo(backup.received.toByteArray())
        assertThat(viewModel.state.value.exportedBytes).isEqualTo(backup.received.size().toLong())
    }

    @Test
    public fun falhaNaExportacaoNaoApagaNada() = runBlocking {
        backup.failure = IllegalStateException("Sem permissão no destino.")
        val viewModel = viewModel()
        awaitState(viewModel) { it.counts.exemplars == 3 }

        viewModel.exportTo(ByteArrayOutputStream())
        awaitState(viewModel) { it.failure == DataControlViewModel.Failure.EXPORT }

        assertThat(viewModel.state.value.failureDetail).isEqualTo("Sem permissão no destino.")
        assertThat(dataControl.deleted).isFalse()
        assertThat(viewModel.state.value.counts.exemplars).isEqualTo(3)
    }

    private suspend fun awaitState(
        viewModel: DataControlViewModel,
        predicate: (DataControlViewModel.State) -> Boolean,
    ): DataControlViewModel.State = withTimeout(5_000) {
        viewModel.state.first(predicate)
    }
}

private class FakeDataControl(
    private val counts: MutableStateFlow<UserDataCounts>,
) : DataControlRepository {

    var deleted: Boolean = false
    var failure: DomainError? = null

    override fun observeCounts(): Flow<UserDataCounts> = counts

    override suspend fun deleteAll(): DomainResult<Unit> {
        failure?.let { return Result.failure(it) }
        deleted = true
        counts.value = UserDataCounts()
        return Result.success(Unit)
    }
}

private class FakeBackupService : BackupService {

    val received = ByteArrayOutputStream()
    var failure: Throwable? = null

    override suspend fun create(
        output: OutputStream,
        appVersion: String,
        deviceName: String?,
    ): BackupSummary {
        failure?.let { throw it }
        val bytes = "pacote".toByteArray()
        output.write(bytes)
        received.write(bytes)
        return BackupSummary(
            createdAt = Instant.parse("2026-11-20T12:00:00Z"),
            schemaVersion = 2,
            appVersion = appVersion,
            device = deviceName,
            counts = mapOf("exemplar" to 3),
            imageCount = 4,
            totalBytes = bytes.size.toLong(),
        )
    }

    override suspend fun inspect(packageFile: File): ValidatedBackup =
        throw UnsupportedOperationException("Não usado na exportação.")

    override suspend fun restore(packageFile: File): BackupSummary =
        throw UnsupportedOperationException("Não usado na exportação.")
}
