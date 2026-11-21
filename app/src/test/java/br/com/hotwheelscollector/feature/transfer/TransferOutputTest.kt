package br.com.hotwheelscollector.feature.transfer

import br.com.hotwheelscollector.data.importexport.ImportApplyResult
import br.com.hotwheelscollector.domain.backup.BackupException
import br.com.hotwheelscollector.domain.backup.BackupFailure
import br.com.hotwheelscollector.domain.backup.BackupSummary
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

/** O resultado do worker atravessa processo; o resumo precisa voltar idêntico (T134). */
class TransferOutputTest {

    @Test
    fun `resumo sobrevive a serializacao com contagens e imagens ausentes`() {
        val summary = BackupSummary(
            createdAt = Instant.ofEpochMilli(1_700_000_000_000),
            schemaVersion = 3,
            appVersion = "1.2.0",
            device = "Google Pixel 8",
            counts = mapOf("exemplar" to 42, "variant" to 7),
            imageCount = 39,
            totalBytes = 123_456_789,
            missingImages = 2,
        )

        val restored = TransferOutput.readSummary(TransferOutput.summary(summary))

        assertThat(restored).isEqualTo(summary)
    }

    @Test
    fun `resumo sem contagens continua valido`() {
        val summary = BackupSummary(
            createdAt = Instant.ofEpochMilli(10),
            schemaVersion = 1,
            appVersion = "dev",
            device = null,
            counts = emptyMap(),
            imageCount = 0,
            totalBytes = 0,
        )

        assertThat(TransferOutput.readSummary(TransferOutput.summary(summary))).isEqualTo(summary)
    }

    @Test
    fun `resultado de importacao preserva todas as contagens`() {
        val result = ImportApplyResult(
            importedExemplars = 10,
            skippedDuplicates = 2,
            invalidRows = 1,
            createdManufacturers = 3,
            createdModels = 4,
            createdVariants = 5,
            createdColors = 6,
        )

        assertThat(TransferOutput.readImported(TransferOutput.imported(result))).isEqualTo(result)
    }

    @Test
    fun `falha preserva motivo e detalhe`() {
        val data = TransferOutput.failure(
            BackupException(BackupFailure.CHECKSUM_MISMATCH, "pacote corrompido"),
        )

        assertThat(TransferOutput.readFailure(data)).isEqualTo(BackupFailure.CHECKSUM_MISMATCH)
        assertThat(TransferOutput.readDetail(data)).isEqualTo("pacote corrompido")
    }

    @Test
    fun `erro sem motivo conhecido nao inventa codigo de falha`() {
        val data = TransferOutput.failure(IllegalStateException("sem espaço"))

        assertThat(TransferOutput.readFailure(data)).isNull()
        assertThat(TransferOutput.readDetail(data)).isEqualTo("sem espaço")
    }

    @Test
    fun `dados vazios nao viram resumo`() {
        assertThat(TransferOutput.readSummary(androidx.work.Data.EMPTY)).isNull()
        assertThat(TransferOutput.readImported(androidx.work.Data.EMPTY)).isNull()
    }
}
