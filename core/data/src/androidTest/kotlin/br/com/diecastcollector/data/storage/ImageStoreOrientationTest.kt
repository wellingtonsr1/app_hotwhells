package br.com.diecastcollector.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Orientação da foto no `ImageStore` (FR-020).
 *
 * O teste roda em aparelho porque `android.media.ExifInterface` e a rotação de `Bitmap` não
 * existem no Robolectric: a foto de câmera é gravada deitada, com a orientação no EXIF, e a
 * recompressão do `ImageStore` jogaria essa etiqueta fora — o arquivo salvo precisa ficar em pé.
 */
@RunWith(AndroidJUnit4::class)
class ImageStoreOrientationTest {

    private lateinit var context: Context
    private lateinit var imageStore: FileImageStore
    private lateinit var imagesDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        imageStore = FileImageStore(context)
        imagesDir = File(context.filesDir, "images")
        imagesDir.deleteRecursively()
    }

    @After
    fun tearDown() {
        imagesDir.deleteRecursively()
    }

    @Test
    fun fotoDeCameraComExifDeRotacao90EGravadaEmPe() = runBlocking {
        // 400x300 deitada + etiqueta "girar 90°" = 300x400 em pé, que é o que o usuário vê.
        val source = jpegWithExifOrientation(
            width = 400,
            height = 300,
            orientation = ExifInterface.ORIENTATION_ROTATE_90,
        )

        val stored = imageStore.save(
            exemplarId = "exemplar-orientacao",
            source = ByteArrayInputStream(source),
            angle = "FRONT",
        )

        assertEquals(300, stored.width)
        assertEquals(400, stored.height)

        val saved = imageStore.absolutePath(stored.relativePath)
        val savedOrientation = ExifInterface(saved.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED,
        )
        // A rotação já está nos pixels: o arquivo não pode pedir para girar de novo.
        assertTrue(
            "orientação salva deveria ser normal ou ausente, veio $savedOrientation",
            savedOrientation == ExifInterface.ORIENTATION_NORMAL ||
                savedOrientation == ExifInterface.ORIENTATION_UNDEFINED,
        )
    }

    private fun jpegWithExifOrientation(width: Int, height: Int, orientation: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        bitmap.recycle()

        val file = File(context.cacheDir, "exif_${System.currentTimeMillis()}.jpg")
        file.writeBytes(output.toByteArray())
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            saveAttributes()
        }
        return file.readBytes().also { file.delete() }
    }
}
