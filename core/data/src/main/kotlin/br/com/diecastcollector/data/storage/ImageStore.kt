package br.com.diecastcollector.data.storage

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

    /**
     * Grava uma **derivada** de edição com o mesmo pipeline de validação, EXIF e limite de
     * tamanho da captura (R.3). Nome distinto garante que o original nunca é sobrescrito
     * (FR-214) e que uma nova edição não colide com a anterior (FR-215).
     */
    public suspend fun saveDerived(
        exemplarId: String,
        source: InputStream,
        angle: String,
    ): StoredImage

    /** Metadados de um arquivo já gravado; `null` quando ele não existe mais. */
    public suspend fun inspect(relativePath: String): StoredImage?

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
        write(exemplarId = exemplarId, source = source, angle = angle, suffix = "")

    /**
     * A derivada já vem codificada pelo `PhotoProcessor` (JPEG sem EXIF, no limite de
     * tamanho). Gravá-la como está evita a segunda recompressão que degradaria detalhes
     * de decalques e inscrições (FR-212, ADR-001).
     */
    override suspend fun saveDerived(
        exemplarId: String,
        source: InputStream,
        angle: String,
    ): StoredImage = withContext(Dispatchers.IO) {
        val bytes = source.use { it.readBytes() }
        require(bytes.isNotEmpty()) { "Imagem vazia." }
        require(bytes.size <= MAX_BYTES) {
            "Foto editada acima do limite de ${MAX_BYTES / 1024} KB."
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Arquivo não é uma imagem válida." }
        writeEncoded(
            exemplarId = exemplarId,
            bytes = bytes,
            angle = angle,
            suffix = "_edit_${System.currentTimeMillis()}_${(1000..9999).random()}",
            width = bounds.outWidth,
            height = bounds.outHeight,
        )
    }

    override suspend fun inspect(relativePath: String): StoredImage? = withContext(Dispatchers.IO) {
        val file = absolutePath(relativePath)
        if (!file.isFile) return@withContext null
        val bytes = file.readBytes()
        if (bytes.isEmpty()) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
        // A orientação já foi aplicada quando o arquivo foi gravado; aqui valem as
        // dimensões do arquivo como está.
        StoredImage(
            relativePath = relativePath,
            checksum = sha256(bytes),
            width = bounds.outWidth,
            height = bounds.outHeight,
            bytes = bytes.size.toLong(),
        )
    }

    private suspend fun write(
        exemplarId: String,
        source: InputStream,
        angle: String,
        suffix: String,
    ): StoredImage = withContext(Dispatchers.IO) {
        val bytes = source.use { it.readBytes() }
        require(bytes.isNotEmpty()) { "Imagem vazia." }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Arquivo não é uma imagem válida." }

        val orientation = orientationOf(bytes)
        var quality = INITIAL_QUALITY
        var encoded = compress(bytes, bounds, quality, orientation)
        while (encoded.bytes.size > MAX_BYTES && quality > MIN_QUALITY) {
            quality -= QUALITY_STEP
            encoded = compress(bytes, bounds, quality, orientation)
        }
        require(encoded.bytes.size <= MAX_BYTES) {
            "Foto acima do limite de ${MAX_BYTES / 1024} KB mesmo após compressão."
        }

        writeEncoded(
            exemplarId = exemplarId,
            bytes = encoded.bytes,
            angle = angle,
            suffix = suffix,
            width = encoded.width,
            height = encoded.height,
        )
    }

    /** Escrita atômica em `.tmp` + rename, comum à captura e à derivada. */
    private fun writeEncoded(
        exemplarId: String,
        bytes: ByteArray,
        angle: String,
        suffix: String,
        width: Int,
        height: Int,
    ): StoredImage {
        val directory = File(root, exemplarId).apply { mkdirs() }
        val safeAngle = angle.lowercase().filter { it.isLetterOrDigit() || it == '_' }
            .ifBlank { "photo" }
        val fileName = "${safeAngle}_${System.currentTimeMillis()}$suffix.jpg"
        val target = File(directory, fileName)
        val temp = File(directory, "$fileName.tmp")

        // Escreve em temporário e move: nada de arquivo truncado se faltar espaço.
        temp.writeBytes(bytes)
        if (!temp.renameTo(target)) {
            temp.delete()
            error("Não foi possível gravar a foto no armazenamento interno.")
        }

        return StoredImage(
            relativePath = "$IMAGES_DIR/$exemplarId/$fileName",
            checksum = sha256(bytes),
            // Dimensões do arquivo gravado, já com a rotação aplicada.
            width = width,
            height = height,
            bytes = bytes.size.toLong(),
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

    /**
     * Redimensiona para caber no limite e recomprime, o que também remove o EXIF.
     *
     * A rotação do EXIF é aplicada aos pixels *antes* de recomprimir. Sem isso a foto tirada
     * com o celular em pé é gravada deitada: o JPEG do arquivo perde a etiqueta de orientação
     * e nenhum visualizador sabe que ela deveria girar 90° (FR-020).
     */
    private fun compress(
        sourceBytes: ByteArray,
        bounds: BitmapFactory.Options,
        quality: Int,
        orientation: Int,
    ): Encoded {
        val sample = sampleSize(bounds.outWidth, bounds.outHeight)
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, options)
            ?: error("Não foi possível decodificar a imagem.")
        val upright = applyOrientation(decoded, orientation)
        if (upright !== decoded) decoded.recycle()
        val scaled = scaleDown(upright, MAX_EDGE_PX)
        return ByteArrayOutputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
            val result = Encoded(output.toByteArray(), scaled.width, scaled.height)
            if (scaled !== upright) upright.recycle()
            scaled.recycle()
            result
        }
    }

    /**
     * Lê a orientação do EXIF da origem. "De pé" é o padrão: foto de câmera no Android traz
     * a etiqueta, foto de galeria/captura já tratada pode não trazer nenhuma.
     */
    private fun orientationOf(sourceBytes: ByteArray): Int = ImageOrientation.of(sourceBytes)

    /** Devolve o bitmap na posição em que a foto foi tirada, girando/espelhando se preciso. */
    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = ImageOrientation.matrix(orientation) ?: return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private class Encoded(val bytes: ByteArray, val width: Int, val height: Int)

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
