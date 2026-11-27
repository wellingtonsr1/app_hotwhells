package br.com.diecastcollector.data.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.scale
import br.com.diecastcollector.domain.model.NormalizedRect
import br.com.diecastcollector.domain.model.PhotoEdit
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Resultado do processamento: bytes JPEG prontos para gravar + dimensões reais. */
public data class ProcessedImage(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
)

/**
 * Pipeline de edição de fotografia (FR-205–FR-212, ADR-002).
 *
 * A prévia e a exportação usam **a mesma** implementação, mudando apenas a aresta máxima e
 * a qualidade (ADR-003): o que o usuário vê é o que ele salva.
 *
 * Tudo roda em `Dispatchers.Default`; nenhum bitmap vaza para a thread principal.
 */
public interface PhotoProcessor {
    public suspend fun preview(source: ByteArray, edit: PhotoEdit): ProcessedImage

    public suspend fun export(source: ByteArray, edit: PhotoEdit): ProcessedImage

    /**
     * Moldura inicial em torno da cartela (FR-205), em coordenadas da imagem já orientada
     * pelo EXIF e pelo giro do usuário. `null` quando o fundo não é uniforme o bastante
     * para separar o objeto.
     */
    public suspend fun suggestCrop(
        source: ByteArray,
        rotationQuarterTurns: Int = 0,
    ): NormalizedRect?
}

@Singleton
public class AndroidPhotoProcessor @Inject constructor() : PhotoProcessor {

    override suspend fun preview(source: ByteArray, edit: PhotoEdit): ProcessedImage =
        process(
            source = source,
            edit = edit,
            maxEdge = PREVIEW_MAX_EDGE,
            jpegQuality = PREVIEW_QUALITY,
        )

    override suspend fun export(source: ByteArray, edit: PhotoEdit): ProcessedImage =
        process(
            source = source,
            edit = edit,
            maxEdge = edit.quality.maxEdge,
            jpegQuality = edit.quality.jpegQuality,
        )

    override suspend fun suggestCrop(
        source: ByteArray,
        rotationQuarterTurns: Int,
    ): NormalizedRect? =
        withContext(Dispatchers.Default) {
            if (source.isEmpty()) return@withContext null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            val sample = sampleSize(bounds.outWidth, bounds.outHeight, SUGGEST_MAX_EDGE * 2)
            val decoded = BitmapFactory.decodeByteArray(
                source,
                0,
                source.size,
                BitmapFactory.Options().apply { inSampleSize = sample },
            ) ?: return@withContext null

            var current = decoded
            try {
                // Mesma orientação aplicada à prévia, senão o retângulo sairia girado.
                current = orient(
                    current,
                    ImageOrientation.of(source),
                    PhotoEdit.NONE.copy(rotationQuarterTurns = rotationQuarterTurns),
                )
                val width = current.width
                val height = current.height
                if (width <= 0 || height <= 0) return@withContext null
                val pixels = IntArray(width * height)
                current.getPixels(pixels, 0, width, 0, 0, width, height)
                CardBoundsDetector.detect(pixels, width, height)
            } finally {
                if (!current.isRecycled) current.recycle()
                if (decoded !== current && !decoded.isRecycled) decoded.recycle()
            }
        }

    private suspend fun process(
        source: ByteArray,
        edit: PhotoEdit,
        maxEdge: Int,
        jpegQuality: Int,
    ): ProcessedImage = withContext(Dispatchers.Default) {
        require(source.isNotEmpty()) { "Imagem vazia." }
        val normalized = edit.normalized()

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Arquivo não é uma imagem válida." }

        val sample = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge * 2)
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(source, 0, source.size, options)
            ?: error("Não foi possível decodificar a imagem.")

        var current = decoded
        try {
            // 1. Orientação EXIF (foto da galeria) + giro e espelho escolhidos pelo usuário.
            current = orient(current, ImageOrientation.of(source), normalized)
            ensureActive()

            // 2. Corte, em coordenadas da imagem já orientada (FR-205).
            current = crop(current, normalized)
            ensureActive()

            // 3. Endireitamento (FR-206).
            if (abs(normalized.straightenDegrees) >= 0.05f) {
                current = straighten(current, normalized.straightenDegrees)
            }
            ensureActive()

            // 4. Aresta máxima do perfil. Reduzir antes de filtrar evita trabalho inútil.
            current = scaleDown(current, maxEdge)
            ensureActive()

            // 5. Cor (FR-207).
            if (hasColorChanges(normalized)) {
                current = colorize(current, normalized)
            }
            ensureActive()

            // 6. Nitidez (FR-207) — unsharp mask 3×3 limitada para não criar halo.
            if (normalized.sharpness > 0.001f) {
                current = sharpen(current, normalized.sharpness)
            }
            ensureActive()

            val width = current.width
            val height = current.height
            val bytes = encodeWithinLimit(current, jpegQuality)
            ProcessedImage(bytes = bytes, width = width, height = height)
        } finally {
            if (!current.isRecycled) current.recycle()
            if (decoded !== current && !decoded.isRecycled) decoded.recycle()
        }
    }

    // ---- Orientação, corte e escala ------------------------------------------------

    private fun orient(bitmap: Bitmap, exif: Int, edit: PhotoEdit): Bitmap {
        val exifMatrix = ImageOrientation.matrix(exif)
        val quarters = edit.rotationQuarterTurns % 4
        if (exifMatrix == null && quarters == 0 && !edit.flipHorizontal) return bitmap

        val matrix = Matrix()
        exifMatrix?.let { matrix.postConcat(it) }
        if (edit.flipHorizontal) matrix.postScale(-1f, 1f)
        if (quarters != 0) matrix.postRotate(quarters * 90f)

        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun crop(bitmap: Bitmap, edit: PhotoEdit): Bitmap {
        val rect = edit.crop
        if (rect.isFull) return bitmap
        val x = (rect.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = (rect.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val w = (rect.width * bitmap.width).toInt().coerceAtLeast(1)
        val h = (rect.height * bitmap.height).toInt().coerceAtLeast(1)
        val safeW = w.coerceAtMost(bitmap.width - x)
        val safeH = h.coerceAtMost(bitmap.height - y)
        if (safeW <= 0 || safeH <= 0) return bitmap
        val cropped = Bitmap.createBitmap(bitmap, x, y, safeW, safeH)
        if (cropped !== bitmap) bitmap.recycle()
        return cropped
    }

    private fun straighten(bitmap: Bitmap, degrees: Float): Bitmap {
        val radians = Math.toRadians(degrees.toDouble())
        val cosValue = abs(cos(radians)).toFloat()
        val sinValue = abs(sin(radians)).toFloat()
        val newWidth = (bitmap.width * cosValue + bitmap.height * sinValue).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.width * sinValue + bitmap.height * cosValue).toInt().coerceAtLeast(1)

        val result = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        // Fundo preto: o JPEG não tem transparência e a borda precisa ser previsível.
        canvas.drawColor(android.graphics.Color.BLACK)
        val matrix = Matrix().apply {
            postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
            postRotate(degrees)
            postTranslate(newWidth / 2f, newHeight / 2f)
        }
        canvas.drawBitmap(bitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        bitmap.recycle()
        return result
    }

    private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / longest
        val scaled = bitmap.scale(
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
        )
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    // ---- Cor e nitidez -------------------------------------------------------------

    private fun hasColorChanges(edit: PhotoEdit): Boolean =
        abs(edit.brightness) > 0.001f ||
            abs(edit.contrast) > 0.001f ||
            abs(edit.saturation) > 0.001f

    /**
     * Saturação → contraste em torno de 128 → brilho.
     * Ordem fixa para o resultado não depender da ordem em que o usuário mexeu nos sliders.
     */
    private fun colorize(bitmap: Bitmap, edit: PhotoEdit): Bitmap {
        val matrix = ColorMatrix()
        matrix.setSaturation((1f + edit.saturation).coerceIn(0f, 2f))

        val scale = (1f + edit.contrast).coerceIn(0f, 2f)
        val offset = 128f * (1f - scale)
        matrix.postConcat(
            ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, offset,
                    0f, scale, 0f, 0f, offset,
                    0f, 0f, scale, 0f, offset,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )

        val shift = edit.brightness * 127f
        matrix.postConcat(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, shift,
                    0f, 1f, 0f, 0f, shift,
                    0f, 0f, 1f, 0f, shift,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        bitmap.recycle()
        return result
    }

    /**
     * Unsharp mask 3×3: centro `1 + 4k`, vizinhos `-k`. `k` fica limitado a 0,45 para
     * melhorar detalhe sem produzir halos brancos nas bordas do carrinho.
     */
    private fun sharpen(bitmap: Bitmap, amount: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return bitmap

        val k = (amount.coerceIn(0f, 1f) * 0.45f)
        val center = 1f + 4f * k
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = IntArray(pixels.size)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                    output[index] = pixels[index]
                    continue
                }
                val current = pixels[index]
                val up = pixels[index - width]
                val down = pixels[index + width]
                val left = pixels[index - 1]
                val right = pixels[index + 1]

                val red = channel(current, 16) * center -
                    (channel(up, 16) + channel(down, 16) + channel(left, 16) + channel(right, 16)) * k
                val green = channel(current, 8) * center -
                    (channel(up, 8) + channel(down, 8) + channel(left, 8) + channel(right, 8)) * k
                val blue = channel(current, 0) * center -
                    (channel(up, 0) + channel(down, 0) + channel(left, 0) + channel(right, 0)) * k

                output[index] = (current and 0xFF000000.toInt()) or
                    (red.toInt().coerceIn(0, 255) shl 16) or
                    (green.toInt().coerceIn(0, 255) shl 8) or
                    blue.toInt().coerceIn(0, 255)
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(output, 0, width, 0, 0, width, height)
        bitmap.recycle()
        return result
    }

    private fun channel(pixel: Int, shift: Int): Float = ((pixel shr shift) and 0xFF).toFloat()

    // ---- Codificação -----------------------------------------------------------------

    /**
     * JPEG na qualidade do perfil, reduzindo em passos até caber no mesmo limite de 1,5 MB
     * usado pelo `ImageStore`. Se nem na qualidade mínima couber, a imagem é reduzida —
     * em vez de recusar o salvamento do usuário.
     */
    private fun encodeWithinLimit(bitmap: Bitmap, quality: Int): ByteArray {
        var current = bitmap
        var encoded = encode(current, quality)
        if (encoded.size <= MAX_BYTES) return encoded

        var qualityValue = quality
        while (encoded.size > MAX_BYTES && qualityValue > MIN_QUALITY) {
            qualityValue -= 8
            encoded = encode(current, qualityValue)
        }
        while (encoded.size > MAX_BYTES && max(current.width, current.height) > 640) {
            current = scaleDown(current, (max(current.width, current.height) * 9) / 10)
            encoded = encode(current, MIN_QUALITY)
        }
        return encoded
    }

    private fun encode(bitmap: Bitmap, quality: Int): ByteArray =
        ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), output)
            output.toByteArray()
        }

    private fun sampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        while (max(width / (sample * 2), height / (sample * 2)) >= target) {
            sample *= 2
        }
        return sample
    }

    private companion object {
        const val PREVIEW_MAX_EDGE = 1080
        const val SUGGEST_MAX_EDGE = 320
        const val PREVIEW_QUALITY = 80
        const val MAX_BYTES = 1_500_000
        const val MIN_QUALITY = 55
    }
}
