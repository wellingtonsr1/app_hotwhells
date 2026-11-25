package br.com.diecastcollector

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertTrue

/**
 * Contraste WCAG 2.1 calculado sobre as cores, não sobre capturas (T371, FR-326).
 *
 * A versão anterior da tarefa previa medir contraste em cima das capturas de T356. Calcular
 * direto das cores do tema é determinístico, não depende de captura, de antialiasing nem de
 * densidade, e cobre exatamente o que a constituição pede: texto normal ≥ 4,5:1 e texto
 * grande ≥ 3:1.
 *
 * Texto grande segue a definição da WCAG: ≥ 18 sp, ou ≥ 14 sp em negrito.
 */
public object ContrastAssert {

    /** Limiar para texto normal. */
    public const val NORMAL_TEXT: Double = 4.5

    /** Limiar para texto grande. */
    public const val LARGE_TEXT: Double = 3.0

    private const val LARGE_TEXT_SP = 18f
    private const val LARGE_BOLD_SP = 14f

    /** Limiar aplicável a um texto de [fontSizeSp] sp, opcionalmente em negrito. */
    public fun requiredRatio(fontSizeSp: Float, bold: Boolean = false): Double =
        if (fontSizeSp >= LARGE_TEXT_SP || (bold && fontSizeSp >= LARGE_BOLD_SP)) {
            LARGE_TEXT
        } else {
            NORMAL_TEXT
        }

    /** Luminância relativa da WCAG (0 para preto, 1 para branco). */
    public fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val v = value.toDouble()
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    /**
     * Razão de contraste entre [foreground] e [background].
     *
     * A cor de texto entra composta sobre o fundo, para o caso de haver alfa (por exemplo
     * `onSurface.copy(alpha = 0.6f)`), em vez de ser tratada como opaca.
     */
    public fun ratio(foreground: Color, background: Color): Double {
        val text = foreground.compositeOver(background)
        val first = luminance(text)
        val second = luminance(background)
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }

    /** Falha com o par, a razão medida e o mínimo pedido quando o contraste não basta. */
    public fun assertContrast(
        label: String,
        foreground: Color,
        background: Color,
        minimum: Double = NORMAL_TEXT,
    ) {
        val measured = ratio(foreground, background)
        assertTrue(
            String.format(
                Locale.US,
                "%s: %.2f:1 medido, mínimo %.1f:1 (%s sobre %s)",
                label,
                measured,
                minimum,
                foreground.hex(),
                background.hex(),
            ),
            measured >= minimum,
        )
    }

    private fun Color.hex(): String = String.format(
        Locale.US,
        "#%02X%02X%02X",
        (red * 255f).toInt(),
        (green * 255f).toInt(),
        (blue * 255f).toInt(),
    )
}
