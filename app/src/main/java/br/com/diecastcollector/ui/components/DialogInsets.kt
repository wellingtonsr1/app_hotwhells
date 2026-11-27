package br.com.diecastcollector.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Insets do sistema para um diálogo de tela cheia.
 *
 * O `Dialog` do Compose cria uma janela própria, e nem sempre essa janela reporta os
 * insets: em alguns aparelhos o inset inferior chega como 0 (o rodapé ficava sob a barra de
 * navegação, sem receber toque) enquanto o topo vem certo. Por isso são três leituras
 * independentes, e vale a maior por lado:
 *
 * 1. `WindowInsets.safeDrawing` da janela do diálogo (`decorFitsSystemWindows = false`);
 * 2. o `decorView` da Activity que abriu o diálogo;
 * 3. `WindowManager.currentWindowMetrics`, que não depende de dispatch de insets.
 *
 * Se as três reportarem zero na base, usa-se a altura canônica de `navigation_bar_height`,
 * para que o rodapé continue alcançável.
 */
@Composable
internal fun dialogInsetsPadding(): Modifier {
    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val windowInsets = WindowInsets.safeDrawing.asPaddingValues()
    var activityInsets by remember { mutableStateOf(IntArray(4)) }
    val metricsInsets = remember(view) { systemBarInsetsFromWindowMetrics(view.context) }
    val navigationBarFallback = remember(view) { navigationBarHeightPx(view.context) }

    DisposableEffect(view) {
        // Reserva: a Activity que abriu o diálogo. É dela que vem o inset quando a janela
        // do diálogo ainda não recebeu os seus.
        val activityDecor = view.context.findActivity()?.window?.decorView
        val target = activityDecor ?: view
        fun read(systemInsets: WindowInsetsCompat?) {
            val bars = systemInsets?.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            ) ?: return
            val next = intArrayOf(bars.left, bars.top, bars.right, bars.bottom)
            if (!next.contentEquals(activityInsets)) activityInsets = next
        }
        read(ViewCompat.getRootWindowInsets(target))
        ViewCompat.setOnApplyWindowInsetsListener(target) { _, systemInsets ->
            read(systemInsets)
            systemInsets
        }
        ViewCompat.requestApplyInsets(target)
        onDispose { ViewCompat.setOnApplyWindowInsetsListener(target, null) }
    }

    fun sidePx(index: Int): Int {
        val fromWindow = when (index) {
            0 -> with(density) { windowInsets.calculateLeftPadding(layoutDirection).roundToPx() }
            1 -> with(density) { windowInsets.calculateTopPadding().roundToPx() }
            2 -> with(density) { windowInsets.calculateRightPadding(layoutDirection).roundToPx() }
            else -> with(density) { windowInsets.calculateBottomPadding().roundToPx() }
        }
        return maxOf(fromWindow, activityInsets[index], metricsInsets[index])
    }

    val bottomPx = sidePx(3).let { if (it > 0) it else navigationBarFallback }
    return Modifier.padding(
        start = with(density) { sidePx(0).toDp() },
        top = with(density) { sidePx(1).toDp() },
        end = with(density) { sidePx(2).toDp() },
        bottom = with(density) { bottomPx.toDp() },
    )
}

/**
 * Insets dos system bars pela janela, e não pelo dispatch: `getInsetsIgnoringVisibility`
 * devolve a área reservada mesmo que a barra esteja oculta naquele instante.
 */
private fun systemBarInsetsFromWindowMetrics(context: Context): IntArray {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return IntArray(4)
    val manager = context.getSystemService(WindowManager::class.java) ?: return IntArray(4)
    val insets = manager.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(
        android.view.WindowInsets.Type.systemBars() or
            android.view.WindowInsets.Type.displayCutout(),
    )
    return intArrayOf(insets.left, insets.top, insets.right, insets.bottom)
}

/** Altura canônica da barra de navegação, usada só quando nenhuma fonte reporta inset. */
private fun navigationBarHeightPx(context: Context): Int {
    val resources = context.resources
    val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (id > 0) resources.getDimensionPixelSize(id) else 0
}

/** Sobe a cadeia de contextos até a Activity que hospeda o diálogo. */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
