package br.com.diecastcollector.appearance

import android.content.res.Configuration
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream

/**
 * Rotina comum dos testes que sobem a activity real (T410, T411, T417).
 *
 * A troca de aparência é feita pelo caminho do usuário — aba Ajustes e toque na opção —,
 * não por injeção de estado: o que se prova é o app inteiro, do DataStore às barras.
 */
internal object AppearanceActivitySupport {

    /** Aba Ajustes da shell (barra inferior ou trilho, conforme a janela). */
    fun openSettings(rule: ComposeTestRule) {
        rule.onNode(hasText("Ajustes") and hasClickAction()).performClick()
        rule.waitForIdle()
    }

    /** Toca na opção de aparência e espera a marca de seleção refletir a escolha. */
    fun selectMode(rule: ComposeTestRule, label: String) {
        rule.onNodeWithText(label).performScrollTo().performClick()
        rule.waitForIdle()
        rule.waitUntil(3_000) {
            runCatching {
                rule.onNodeWithText(label).assertIsSelected()
                true
            }.getOrDefault(false)
        }
        // A marca só aparece depois que a escrita no DataStore terminou; um instante a mais
        // garante que a próxima recriação leia o valor novo.
        Thread.sleep(150)
    }

    fun assertModePalette(rule: ComposeTestRule, label: String, dark: Boolean) {
        rule.waitForIdle()
        val bitmap = AppearanceTestSupport.bitmapOf(rule.onRoot())
        try {
            AppearanceTestSupport.assertPalette(label, bitmap, dark)
        } finally {
            bitmap.recycle()
        }
    }

    /** Aguarda o fundo dominante virar o do modo pedido, dentro do limite informado. */
    fun waitForPalette(rule: ComposeTestRule, dark: Boolean, timeoutMs: Long = 2_000): Long {
        val started = System.currentTimeMillis()
        while (System.currentTimeMillis() - started <= timeoutMs) {
            rule.waitForIdle()
            val bitmap = AppearanceTestSupport.bitmapOf(rule.onRoot())
            val ok = try {
                AppearanceTestSupport.assertPalette("aguardando paleta", bitmap, dark)
                true
            } catch (_: AssertionError) {
                false
            } finally {
                bitmap.recycle()
            }
            if (ok) return System.currentTimeMillis() - started
            Thread.sleep(100)
        }
        throw AssertionError(
            "O fundo não virou ${if (dark) "escuro" else "claro"} em $timeoutMs ms",
        )
    }

    fun isSystemInDark(): Boolean {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
    }

    /** `cmd uimode night yes|no`, esperando o comando terminar. */
    fun setSystemNight(dark: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val descriptor = instrumentation.uiAutomation.executeShellCommand(
            "cmd uimode night ${if (dark) "yes" else "no"}",
        )
        FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        descriptor.close()
        Thread.sleep(400)
    }

    /** Local do arquivo da preferência — deliberadamente fora do banco e de `images/`. */
    fun preferenceFile(): java.io.File = java.io.File(
        InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
        "datastore/appearance.preferences_pb",
    )

    fun assertExists(node: SemanticsNodeInteraction) {
        node.assertExists()
    }
}
