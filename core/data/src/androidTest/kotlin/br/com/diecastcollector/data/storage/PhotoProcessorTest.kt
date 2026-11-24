package br.com.diecastcollector.data.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.domain.model.PhotoAspect
import br.com.diecastcollector.domain.model.PhotoEdit
import br.com.diecastcollector.domain.model.PhotoQuality
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Processamento real de imagem (FR-205–FR-212, contrato P-02–P-09).
 *
 * Roda no aparelho porque `android.graphics` não existe na JVM. É o que prova que as
 * ferramentas funcionam de fato, não apenas que a tela existe (Art. V.1).
 */
@RunWith(AndroidJUnit4::class)
class PhotoProcessorTest {

    private val processor = AndroidPhotoProcessor()

    private fun jpeg(width: Int, height: Int, color: Int = Color.GRAY): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(color)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    @Test
    fun exportacaoRespeitaArestaDoPerfil() = runBlocking {
        val exported = processor.export(jpeg(2400, 1600), PhotoEdit(quality = PhotoQuality.COMPACT))

        assertTrue(
            "Aresta ${max(exported.width, exported.height)} acima do perfil compacto",
            max(exported.width, exported.height) <= 1280,
        )
        assertTrue("Arquivo acima de 1,5 MB", exported.bytes.size <= 1_500_000)
    }

    @Test
    fun rotacaoDe90GrausTrocaLarguraEAltura() = runBlocking {
        val exported = processor.export(
            jpeg(400, 200),
            PhotoEdit(rotationQuarterTurns = 1),
        )

        assertEquals(200, exported.width)
        assertEquals(400, exported.height)
    }

    @Test
    fun corteQuadradoProduzImagemQuadrada() = runBlocking {
        val edit = PhotoEdit.NONE.withAspect(PhotoAspect.SQUARE, 400, 200)
        val exported = processor.export(jpeg(400, 200), edit)

        assertEquals(exported.width, exported.height)
    }

    @Test
    fun espelhamentoProduzArquivoValidoDoMesmoTamanho() = runBlocking {
        val exported = processor.export(jpeg(300, 180), PhotoEdit(flipHorizontal = true))

        assertEquals(300, exported.width)
        assertEquals(180, exported.height)
        assertNotNull(BitmapFactory.decodeByteArray(exported.bytes, 0, exported.bytes.size))
    }

    @Test
    fun brilhoNegativoEscureceEPositivoClareia() = runBlocking {
        val source = jpeg(120, 120, Color.rgb(128, 128, 128))

        val darker = averageLuminance(processor.export(source, PhotoEdit(brightness = -0.6f)).bytes)
        val neutral = averageLuminance(processor.export(source, PhotoEdit()).bytes)
        val brighter = averageLuminance(processor.export(source, PhotoEdit(brightness = 0.6f)).bytes)

        assertTrue("Brilho negativo não escureceu: $darker vs $neutral", darker < neutral)
        assertTrue("Brilho positivo não clareou: $brighter vs $neutral", brighter > neutral)
        assertTrue("Diferença pequena demais: ${abs(darker - neutral)}", abs(darker - neutral) > 10f)
    }

    @Test
    fun nitidezAplicadaNaoQuebraAImagem() = runBlocking {
        val exported = processor.export(jpeg(240, 160), PhotoEdit(sharpness = 1f))
        val decoded = BitmapFactory.decodeByteArray(exported.bytes, 0, exported.bytes.size)

        assertNotNull(decoded)
        assertEquals(240, decoded.width)
        decoded.recycle()
    }

    @Test
    fun imagemInvalidaFalhaComMensagem() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { processor.export(byteArrayOf(1, 2, 3), PhotoEdit()) }
        }
    }

    @Test
    fun previaEMenorQueAExportacao() = runBlocking {
        val source = jpeg(2000, 1500)
        val preview = processor.preview(source, PhotoEdit())
        val exported = processor.export(source, PhotoEdit())

        assertTrue(max(preview.width, preview.height) <= 1080)
        assertTrue(max(exported.width, exported.height) <= 1600)
        assertTrue(exported.width >= preview.width)
    }

    private fun averageLuminance(bytes: ByteArray): Float {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        bitmap.recycle()
        var sum = 0.0
        pixels.forEach { pixel ->
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            sum += 0.299 * red + 0.587 * green + 0.114 * blue
        }
        return (sum / pixels.size).toFloat()
    }
}
