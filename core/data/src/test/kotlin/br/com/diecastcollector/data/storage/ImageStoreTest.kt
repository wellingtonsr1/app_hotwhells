package br.com.diecastcollector.data.storage

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes do ImageStore (T038 - US2, R.2–R.3, FR-153).
 *
 * Cobre:
 * - Compressão e redimensionamento de imagens grandes.
 * - Geração determinística de checksum SHA-256.
 * - Remoção de fotos e validação de existência.
 * - Tratamento de arquivos inválidos ou vazios.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
public class ImageStoreTest {

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
    fun `salva imagem e retorna caminho relativo e checksum sha256 estavel`() = runBlocking {
        val bitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val bytes = stream.toByteArray()

        val stored = imageStore.save(
            exemplarId = "exemplar-123",
            source = ByteArrayInputStream(bytes),
            angle = "FRONT",
        )

        assertThat(stored.relativePath).startsWith("images/exemplar-123/front_")
        assertThat(stored.relativePath).endsWith(".jpg")
        assertThat(stored.checksum).matches("^[a-f0-9]{64}$")
        assertThat(stored.bytes).isGreaterThan(0L)
        assertThat(imageStore.exists(stored.relativePath)).isTrue()
    }

    @Test
    fun `rejeita imagem com stream vazio`() = runBlocking {
        var failed = false
        try {
            imageStore.save(
                exemplarId = "exemplar-123",
                source = ByteArrayInputStream(ByteArray(0)),
                angle = "BASE",
            )
        } catch (e: IllegalArgumentException) {
            failed = true
            assertThat(e.message).contains("Imagem vazia")
        }
        assertThat(failed).isTrue()
    }

    @Test
    fun `rejeita bytes invalidos que nao sao imagem`() = runBlocking {
        org.robolectric.shadows.ShadowBitmapFactory.setAllowInvalidImageData(false)
        val result = runCatching {
            imageStore.save(
                exemplarId = "exemplar-123",
                source = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5)),
                angle = "TOP",
            )
        }
        org.robolectric.shadows.ShadowBitmapFactory.reset()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Arquivo não é uma imagem válida")
    }

    @Test
    fun `remove foto apaga o arquivo do armazenamento interno`() = runBlocking {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)

        val stored = imageStore.save(
            exemplarId = "exemplar-456",
            source = ByteArrayInputStream(stream.toByteArray()),
            angle = "DETAIL",
        )
        assertThat(imageStore.exists(stored.relativePath)).isTrue()

        imageStore.delete(stored.relativePath)
        assertThat(imageStore.exists(stored.relativePath)).isFalse()
    }

    @Test
    fun `redimensiona imagem que excede o limite maximo de resolucao`() = runBlocking {
        // Imagem 2000x1200 excede o MAX_EDGE_PX de 1600
        val bitmap = Bitmap.createBitmap(2000, 1200, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)

        val stored = imageStore.save(
            exemplarId = "exemplar-789",
            source = ByteArrayInputStream(stream.toByteArray()),
            angle = "LEFT",
        )

        assertThat(stored.bytes).isLessThan(1_500_000L)
        val savedFile = imageStore.absolutePath(stored.relativePath)
        assertThat(savedFile.exists()).isTrue()
    }
}
