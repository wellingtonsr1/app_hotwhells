package br.com.diecastcollector.data.recognition

import android.graphics.BitmapFactory
import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.model.RecognitionSource
import br.com.diecastcollector.domain.recognition.RecognitionField
import br.com.diecastcollector.domain.recognition.RecognitionInput
import br.com.diecastcollector.domain.recognition.RecognitionProposal
import br.com.diecastcollector.domain.recognition.RecognitionProvider
import br.com.diecastcollector.domain.recognition.RecognitionSuggestion
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Código de barras on-device com ML Kit (T124, FR-109/FR-110).
 *
 * O modelo vem embutido no app (`com.google.mlkit:barcode-scanning`), então a leitura
 * funciona sem rede (FR-106). Não grava nada: devolve o código lido como sugestão do campo
 * `CODE`, com confiança máxima — um código decodificado é literal, não uma inferência.
 */
@Singleton
public class MlKitBarcodeProvider @Inject constructor() : RecognitionProvider {

    override val source: RecognitionSource = RecognitionSource.BARCODE

    private val scanner by lazy { BarcodeScanning.getClient() }

    override suspend fun recognize(input: RecognitionInput): RecognitionProposal =
        withContext(Dispatchers.IO) {
            val raw = when (input) {
                is RecognitionInput.Barcode -> input.value
                is RecognitionInput.Image -> scanFirstValue(input.bytes)
            }?.trim()

            if (raw.isNullOrEmpty()) {
                RecognitionProposal.unidentified(source, PROVIDER)
            } else {
                RecognitionProposal.of(
                    source = source,
                    provider = PROVIDER,
                    rawValue = raw,
                    suggestions = listOf(
                        RecognitionSuggestion(RecognitionField.CODE, raw, confidence = 1.0),
                    ),
                )
            }
        }

    /** Um pacote pode ter mais de um código; o primeiro decodificado é o que vale. */
    private fun scanFirstValue(bytes: ByteArray): String? {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw DomainError.RecognitionUnavailable
        val image = InputImage.fromBitmap(bitmap, 0)
        val barcodes: List<Barcode> = try {
            Tasks.await(scanner.process(image))
        } catch (failure: Exception) {
            throw DomainError.RecognitionUnavailable
        }
        return barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
    }

    private companion object {
        const val PROVIDER = "mlkit-barcode"
    }
}
