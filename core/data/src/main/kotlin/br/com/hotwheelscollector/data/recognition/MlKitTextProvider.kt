package br.com.hotwheelscollector.data.recognition

import android.graphics.BitmapFactory
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.model.RecognitionSource
import br.com.hotwheelscollector.domain.recognition.RecognitionInput
import br.com.hotwheelscollector.domain.recognition.RecognitionProposal
import br.com.hotwheelscollector.domain.recognition.RecognitionProvider
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OCR de embalagem on-device com ML Kit (T125, FR-107).
 *
 * O modelo latino vem embutido no app, então funciona sem rede (FR-106). O texto lido vira
 * sugestões conservadoras de nome, série, ano e código; aplicar continua sendo decisão do
 * usuário, campo a campo. Não grava nada.
 */
@Singleton
public class MlKitTextProvider @Inject constructor() : RecognitionProvider {

    override val source: RecognitionSource = RecognitionSource.OCR

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognize(input: RecognitionInput): RecognitionProposal =
        withContext(Dispatchers.IO) {
            // OCR só faz sentido sobre imagem; um código já lido não é texto de embalagem.
            if (input is RecognitionInput.Barcode) {
                return@withContext RecognitionProposal.unidentified(
                    source = source,
                    provider = PROVIDER,
                    rawValue = input.value,
                )
            }

            val bytes = (input as RecognitionInput.Image).bytes
            val text = readText(bytes)
            RecognitionProposal.of(
                source = source,
                provider = PROVIDER,
                rawValue = text,
                suggestions = PackagingTextParser.parse(text),
            )
        }

    private fun readText(bytes: ByteArray): String {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw DomainError.RecognitionUnavailable
        val image = InputImage.fromBitmap(bitmap, 0)
        val result: Text = try {
            Tasks.await(recognizer.process(image))
        } catch (failure: Exception) {
            throw DomainError.RecognitionUnavailable
        }
        return result.text
    }

    private companion object {
        const val PROVIDER = "mlkit-text"
    }
}
