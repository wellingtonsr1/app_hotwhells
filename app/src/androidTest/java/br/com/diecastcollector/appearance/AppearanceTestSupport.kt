package br.com.diecastcollector.appearance

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertTrue

/**
 * Utilidades dos testes de aparência (Spec 006, US3).
 *
 * Tudo é medido no bitmap renderizado de um nó — nada de reconstruir a cor esperada a
 * partir do token. As regiões são sempre as de um nó semântico conhecido, então a medida
 * é determinística: mesmo aparelho, mesma densidade, mesmo resultado.
 */
internal object AppearanceTestSupport {

    /** Fundos/superfícies da paleta clara (`core/ui/.../Color.kt`). */
    private val LIGHT_SURFACES = setOf(
        0xFFF4F1EE.toInt(), // Chalk — background
        0xFFFFFFFF.toInt(), // PureWhite — surface
        0xFFFFB27A.toInt(), // HotOrangeLight — primaryContainer
    )

    /** Fundos/superfícies da paleta escura (`core/ui/.../Color.kt`). */
    private val DARK_SURFACES = setOf(
        0xFF121212.toInt(), // InkBlack — background
        0xFF1E1E1E.toInt(), // Graphite — surface
        0xFF2C2C2C.toInt(), // SmokyGrey — surfaceVariant
    )

    fun bitmapOf(node: SemanticsNodeInteraction): Bitmap =
        node.captureToImage().asAndroidBitmap()

    /**
     * Cor mais frequente do bitmap. Numa tela do app ela é o fundo do `Scaffold`, que ocupa
     * mais área que qualquer cartão; é assim que o modo aplicado é identificado sem depender
     * de captura de coordenada fixa.
     */
    fun dominantColor(bitmap: Bitmap): Int {
        val counts = HashMap<Int, Int>()
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val color = bitmap.getPixel(x, y)
                counts[color] = (counts[color] ?: 0) + 1
                x += 2
            }
            y += 2
        }
        return counts.maxByOrNull { it.value }?.key ?: 0
    }

    /** Confere que a cor dominante pertence à paleta do modo e não à oposta. */
    fun assertPalette(label: String, bitmap: Bitmap, dark: Boolean) {
        val dominant = dominantColor(bitmap)
        val allowed = if (dark) DARK_SURFACES else LIGHT_SURFACES
        val forbidden = if (dark) LIGHT_SURFACES else DARK_SURFACES
        val diagnostic = "$label: cor dominante ${dominant.hex()} (${if (dark) "escuro" else "claro"})"
        assertTrue(diagnostic, dominant in allowed)
        assertTrue(diagnostic, dominant !in forbidden)
    }

    /**
     * Contraste WCAG entre os extremos de luminância do nó renderizado.
     *
     * Num texto, os extremos são a cor do glifo e o fundo atrás dele; numa etiqueta sobre
     * foto, o texto e a película composta com a imagem. O antialiasing só cria valores
     * intermediários, então os extremos continuam sendo as duas cores reais.
     */
    fun renderedContrast(node: SemanticsNodeInteraction): Double {
        val bitmap = bitmapOf(node)
        var darkest = 1.0
        var lightest = 0.0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val luminance = luminance(bitmap.getPixel(x, y))
                if (luminance < darkest) darkest = luminance
                if (luminance > lightest) lightest = luminance
            }
        }
        return (lightest + 0.05) / (darkest + 0.05)
    }

    /**
     * Média dos canais de uma região central do bitmap — usada para provar que a foto não
     * recebe tingimento do tema (fração central evita a película das etiquetas da grade).
     */
    fun meanCenterColor(bitmap: Bitmap): DoubleArray {
        val left = bitmap.width / 4
        val right = bitmap.width * 3 / 4
        val top = bitmap.height * 2 / 5
        val bottom = bitmap.height * 3 / 5
        var red = 0.0
        var green = 0.0
        var blue = 0.0
        var pixels = 0
        for (y in top until bottom) {
            for (x in left until right) {
                val color = bitmap.getPixel(x, y)
                red += android.graphics.Color.red(color)
                green += android.graphics.Color.green(color)
                blue += android.graphics.Color.blue(color)
                pixels++
            }
        }
        if (pixels == 0) return doubleArrayOf(0.0, 0.0, 0.0)
        return doubleArrayOf(red / pixels, green / pixels, blue / pixels)
    }

    /** Luminância relativa da WCAG 2.1 para uma cor ARGB. */
    fun luminance(color: Int): Double {
        fun channel(value: Int): Double {
            val v = value / 255.0
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(android.graphics.Color.red(color)) +
            0.7152 * channel(android.graphics.Color.green(color)) +
            0.0722 * channel(android.graphics.Color.blue(color))
    }

    fun ratio(first: Double, second: Double): Double =
        (max(first, second) + 0.05) / (min(first, second) + 0.05)

    private fun Int.hex(): String = "#%08X".format(this)
}
