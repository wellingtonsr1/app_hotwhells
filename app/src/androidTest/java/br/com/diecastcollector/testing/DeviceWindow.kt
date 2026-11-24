package br.com.diecastcollector.testing

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream

/**
 * Redimensiona a janela do aparelho de teste, em dp (T322, T324, FR-301).
 *
 * A activity real é medida de novo quando `wm size`/`wm density` mudam — é o mesmo caminho
 * de uma janela redimensionada de verdade, sem injetar classe à mão. A densidade é fixada
 * em 320 dpi para que `px = dp × 2`, e o tamanho físico é restaurado por [reset].
 */
public object DeviceWindow {

    private const val DENSITY_DPI = 320

    /** Aplica [widthDp] × [heightDp]; a janela do app passa a medir exatamente isso. */
    public fun use(widthDp: Int, heightDp: Int = 900) {
        shell("wm density $DENSITY_DPI")
        shell("wm size ${widthDp * DENSITY_DPI / 160}x${heightDp * DENSITY_DPI / 160}")
    }

    /** Volta ao tamanho e à densidade físicos do emulador. */
    public fun reset() {
        shell("wm density reset")
        shell("wm size reset")
    }

    private fun shell(command: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val descriptor: ParcelFileDescriptor = instrumentation.uiAutomation
            .executeShellCommand(command)
        // O comando termina quando o descritor fecha; ler até o fim evita deixar o shell
        // pendurado e o próximo `wm` chegar antes da mudança valer.
        FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        descriptor.close()
    }
}
