package br.com.hotwheelscollector.feature.catalog

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.test.platform.app.InstrumentationRegistry
import br.com.hotwheelscollector.domain.model.PhotoAngle
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Captura pela câmera e recusa de permissão (T045 - US2, FR-020, FR-108).
 *
 * A câmera é outro aplicativo e a permissão é estado do aparelho: mudá-la com `pm grant` /
 * `pm revoke` faz o ActivityManager matar o processo da instrumentação
 * (`Killing ...: permissions revoked`). Por isso o teste injeta as duas bordas que o código
 * de produção usa — a resposta de `checkSelfPermission` (via `LocalContext`) e o resultado
 * dos launchers (via `LocalActivityResultRegistryOwner`) — e não toca no estado do aparelho.
 *
 * O caminho da galeria está coberto pelo `CreateCatalogEntryScreenTest`, que percorre o
 * cadastro completo com uma foto escolhida no Photo Picker.
 */
class PhotoCaptureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun recusaDePermissaoAvisaATelaSemEntregarBytes() {
        val denied = AtomicInteger()
        val received = AtomicInteger()

        setContent(
            permissionGranted = false,
            registry = FakeCameraRegistry(granted = false),
            onBytes = { _, _ -> received.incrementAndGet() },
            onDenied = { denied.incrementAndGet() },
        )

        composeRule.onNodeWithText("Câmera").performClick()

        composeRule.waitUntil(TIMEOUT_MS) { denied.get() == 1 }
        assertEquals(0, received.get())
    }

    @Test
    fun capturaPelaCameraEntregaOsBytesDoArquivoTemporario() {
        val received = AtomicInteger()
        var bytes: ByteArray = ByteArray(0)
        var angle: PhotoAngle? = null

        setContent(
            permissionGranted = true,
            registry = FakeCameraRegistry(granted = true),
            onBytes = { photo, photoAngle ->
                bytes = photo
                angle = photoAngle
                received.incrementAndGet()
            },
            onDenied = {},
        )

        composeRule.onNodeWithText("Câmera").performClick()

        composeRule.waitUntil(TIMEOUT_MS) { received.get() == 1 }
        assertTrue("A captura chegou vazia", bytes.isNotEmpty())
        assertEquals(PhotoAngle.BACK, angle)
    }

    private fun setContent(
        permissionGranted: Boolean,
        registry: FakeCameraRegistry,
        onBytes: (ByteArray, PhotoAngle) -> Unit,
        onDenied: () -> Unit,
    ) {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            HotWheelsTheme {
                CompositionLocalProvider(
                    LocalContext provides PermissionContext(base, permissionGranted),
                    LocalActivityResultRegistryOwner provides RegistryOwner(registry),
                ) {
                    PhotoCaptureActions(
                        selectedAngle = PhotoAngle.BACK,
                        onPhotoBytesReceived = onBytes,
                        onPermissionDenied = onDenied,
                    )
                }
            }
        }
    }

    /** Contexto real, com só a resposta de permissão trocada. */
    private class PermissionContext(
        base: Context,
        private val granted: Boolean,
    ) : ContextWrapper(base) {

        private val answer: Int =
            if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED

        override fun checkSelfPermission(permission: String): Int = answer

        override fun checkPermission(permission: String, pid: Int, uid: Int): Int = answer
    }

    private class RegistryOwner(
        private val registry: ActivityResultRegistry,
    ) : ActivityResultRegistryOwner {
        override val activityResultRegistry: ActivityResultRegistry get() = registry
    }

    /**
     * Registro falso: a permissão devolve sempre o que o teste mandou e a captura devolve
     * sucesso depois de preencher o arquivo temporário que o app acabou de criar — é dele
     * que `PhotoCaptureActions` lê os bytes (`contentResolver.openInputStream`).
     */
    private class FakeCameraRegistry(
        private val granted: Boolean,
    ) : ActivityResultRegistry() {

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            when (contract) {
                is ActivityResultContracts.TakePicture -> {
                    fillPendingCaptureFile()
                    dispatchResult(requestCode, contract.parseResult(Activity.RESULT_OK, null))
                }

                else -> {
                    val resultCode = if (granted) Activity.RESULT_OK else Activity.RESULT_CANCELED
                    dispatchResult(requestCode, contract.parseResult(resultCode, null))
                }
            }
        }

        private fun fillPendingCaptureFile() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.cacheDir
                .listFiles { file -> file.name.startsWith("camera_capture_") }
                ?.maxByOrNull { it.lastModified() }
                ?.let { file ->
                    FileOutputStream(file).use { it.write(PhotoCaptureTest.jpegBytes()) }
                }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L

        fun jpegBytes(): ByteArray {
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawColor(Color.BLUE)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            bitmap.recycle()
            return stream.toByteArray()
        }
    }
}
