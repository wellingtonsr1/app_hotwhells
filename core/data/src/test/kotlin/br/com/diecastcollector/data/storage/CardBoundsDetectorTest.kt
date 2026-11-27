package br.com.diecastcollector.data.storage

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CardBoundsDetectorTest {

    private fun image(
        width: Int,
        height: Int,
        background: (x: Int, y: Int) -> Int,
        subject: (x: Int, y: Int) -> Int?,
    ): IntArray = IntArray(width * height) { index ->
        val x = index % width
        val y = index / width
        subject(x, y) ?: background(x, y)
    }

    @Test
    fun `encontra cartela clara sobre fundo escuro`() {
        val pixels = image(
            width = 200,
            height = 300,
            background = { _, _ -> 0xFF303030.toInt() },
            subject = { x, y ->
                when {
                    x in 20..179 && y in 40..269 -> {
                        // Metade de cima clara, metade de baixo colorida: a cartela inteira
                        // precisa entrar, não só o fundo branco.
                        if (y < 160) 0xFFF2F2F2.toInt() else 0xFF1E5AA8.toInt()
                    }
                    else -> null
                }
            },
        )

        val crop = CardBoundsDetector.detect(pixels, 200, 300)

        assertThat(crop).isNotNull()
        assertThat(crop!!.left).isWithin(0.02f).of(0.10f)
        assertThat(crop.top).isWithin(0.02f).of(0.133f)
        assertThat(crop.right).isWithin(0.02f).of(0.90f)
        assertThat(crop.bottom).isWithin(0.02f).of(0.90f)
    }

    @Test
    fun `fundo sem contraste nao sugere recorte`() {
        val pixels = IntArray(200 * 300) { 0xFF303030.toInt() }

        assertThat(CardBoundsDetector.detect(pixels, 200, 300)).isNull()
    }

    @Test
    fun `cartela encostada na borda ainda e delimitada`() {
        val pixels = image(
            width = 200,
            height = 300,
            background = { _, _ -> 0xFF2A2A2A.toInt() },
            subject = { x, y -> if (x in 0..170 && y in 30..260) 0xFFEDEDED.toInt() else null },
        )

        val crop = CardBoundsDetector.detect(pixels, 200, 300)

        assertThat(crop).isNotNull()
        assertThat(crop!!.left).isAtMost(0.01f)
        assertThat(crop.top).isWithin(0.02f).of(0.10f)
        assertThat(crop.right).isWithin(0.02f).of(0.855f)
    }

    @Test
    fun `aceita gradiente suave de fundo`() {
        val pixels = image(
            width = 200,
            height = 300,
            background = { x, _ -> 0xFF000000.toInt() or (40 + x / 5) * 0x010101 },
            subject = { x, y -> if (x in 30..169 && y in 50..250) 0xFFE8E8E8.toInt() else null },
        )

        val crop = CardBoundsDetector.detect(pixels, 200, 300)

        assertThat(crop).isNotNull()
        assertThat(crop!!.left).isWithin(0.02f).of(0.15f)
        assertThat(crop.bottom).isWithin(0.02f).of(0.836f)
    }

    @Test
    fun `objeto pequeno demais nao vira sugestao`() {
        val pixels = image(
            width = 200,
            height = 300,
            background = { _, _ -> 0xFF303030.toInt() },
            subject = { x, y -> if (x in 90..110 && y in 140..160) 0xFFF0F0F0.toInt() else null },
        )

        assertThat(CardBoundsDetector.detect(pixels, 200, 300)).isNull()
    }
}
