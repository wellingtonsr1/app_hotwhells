package br.com.hotwheelscollector.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Resultado da gravação de uma foto: caminho relativo + metadados verificáveis. */
public data class StoredImage(
    val relativePath: String,
    val checksum: String,
    val width: Int,
    val height: Int,
    val bytes: Long,
)

/**
 * Guarda as fotos em arquivo privado do app, nunca em BLOB (R.2).
 *
 * Decisões que importam para o produto:
 * - recompressão em JPEG descarta o EXIF, inclusive GPS — a foto do colecionador não
 *   carrega a localização da casa dele para um backup compartilhado (R.3);
 * - o arquivo é escrito em `.tmp` e renomeado, então nunca fica foto pela metade
 *   quando o aparelho fica sem espaço (FR-153);
 * - o checksum SHA-256 permite detectar arquivo corrompido na restauração (FR-141).
 */
public interface ImageStore {

    public suspend fun save(exemplarId: String, source: InputStream, angle: String): StoredImage

    public suspend fun delete(relativePath: String)

    /** Remove todas as fotos do app — usado só pela exclusão total (T146). */
    public suspend fun deleteAll()

    public suspend fun exists(relativePath: String): Boolean

    public fun absolutePath(relativePath: String): File
}

@Singleton
public class FileImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImageStore {

    private val root: File by lazy { File(context.filesDir, IMAGES_DIR) }

    override suspend fun save(exemplarId: String, source: InputStream, angle: String): StoredImage =
        withContext(Dispatchers.IO) {
            val bytes = source.use { it.readBytes() }
            require(bytes.isNotEmpty()) { "Imagem vazia." }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Arquivo não é uma imagem válida." }

            var quality = INITIAL_QUALITY
            var encoded = compress(bytes, bounds, quality)
            while (encoded.size > MAX_BYTES && quality > MIN_QUALITY) {
                quality -= QUALITY_STEP
                encoded = compress(bytes, bounds, quality)
            }
            require(encoded.size <= MAX_BYTES) {
                "Foto acima do limite de ${MAX_BYTES / 1024} KB mesmo após compressão."
            }

            val directory = File(root, exemplarId).apply { mkdirs() }
            val fileName = "${angle.lowercase()}_${System.currentTimeMillis()}.jpg"
            val target = File(directory, fileName)
            val temp = File(directory, "$fileName.tmp")

            // Escreve em temporário e move: nada de arquivo truncado se faltar espaço.
            temp.writeBytes(encoded)
            if (!temp.renameTo(target)) {
                temp.delete()
                error("Não foi possível gravar a foto no armazenamento interno.")
            }

            StoredImage(
                relativePath = "$IMAGES_DIR/$exemplarId/$fileName",
                checksum = sha256(encoded),
                width = bounds.outWidth,
                height = bounds.outHeight,
                bytes = encoded.size.toLong(),
            )
        }

    override suspend fun delete(relativePath: String) = withContext(Dispatchers.IO) {
        val file = absolutePath(relativePath)
        if (file.exists() && !file.delete()) {
            error("Não foi possível remover a foto ${file.name}.")
        }
    }

    override suspend fun deleteAll() = withContext(Dispatchers.IO) {
        if (root.exists() && !root.deleteRecursively()) {
            error("Não foi possível remover as fotos do armazenamento interno.")
        }
    }

    override suspend fun exists(relativePath: String): Boolean = withContext(Dispatchers.IO) {
        absolutePath(relativePath).exists()
    }

    override fun absolutePath(relativePath: String): File = File(context.filesDir, relativePath)

    /** Redimensiona para caber no limite e recomprime, o que também remove o EXIF. */
    private fun compress(sourceBytes: ByteArray, bounds: BitmapFactory.Options, quality: Int): ByteArray {
        val sample = sampleSize(bounds.outWidth, bounds.outHeight)
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, options)
            ?: error("Não foi possível decodificar a imagem.")
        val scaled = scaleDown(bitmap, MAX_EDGE_PX)
        return ByteArrayOutputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= MAX_EDGE_PX && height / (sample * 2) >= MAX_EDGE_PX) {
            sample *= 2
        }
        return sample
    }

    private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / longest
        return bitmap.scale(
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
        )
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val IMAGES_DIR = "images"
        const val MAX_EDGE_PX = 1600
        const val MAX_BYTES = 1_500_000
        const val INITIAL_QUALITY = 88
        const val MIN_QUALITY = 55
        const val QUALITY_STEP = 8
    }
}
