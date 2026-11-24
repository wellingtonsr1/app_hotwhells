package br.com.diecastcollector.data.storage

import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream

/**
 * Orientação EXIF em um único ponto de verdade (Art. II.5).
 *
 * Usada na captura (`FileImageStore`) e na edição (`AndroidPhotoProcessor`): a foto que
 * chega da galeria ainda traz a etiqueta EXIF, e sem aplicá-la antes de editar o corte e
 * a rotação trabalhariam sobre uma imagem deitada (R.3).
 */
internal object ImageOrientation {

    /** Valor bruto do EXIF; "de pé" é o padrão quando a etiqueta não existe. */
    fun of(bytes: ByteArray): Int = runCatching {
        ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    /** Matriz que devolve a imagem à posição em que foi tirada; `null` = sem correção. */
    fun matrix(orientation: Int): Matrix? {
        val matrix = Matrix()
        return when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.apply { setScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.apply { setRotate(180f) }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.apply {
                setRotate(180f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> matrix.apply {
                setRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.apply { setRotate(90f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> matrix.apply {
                setRotate(-90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.apply { setRotate(-90f) }
            else -> null
        }
    }
}
