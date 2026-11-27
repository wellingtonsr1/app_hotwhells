package br.com.diecastcollector.data.storage

import br.com.diecastcollector.domain.model.NormalizedRect
import kotlin.math.abs
import kotlin.math.max

/**
 * Sugere o retângulo da cartela dentro da foto (FR-205).
 *
 * A foto de um exemplar quase sempre tem fundo relativamente uniforme (mesa, chão, parede)
 * e a cartela como objeto de maior contraste. O detector alaga o fundo a partir das bordas:
 * um pixel vizinho só entra no fundo quando é parecido com o pixel de onde veio e não se
 * afasta muito da cor mediana da borda. Assim sombras e gradientes suaves são absorvidos,
 * mas a aresta da cartela interrompe o avanço.
 *
 * O maior agrupamento restante é a cartela; sua caixa envolvente vira a moldura inicial.
 * Quando o fundo não é uniforme ou ocupa pouco espaço, devolve `null` e o editor segue com
 * a imagem inteira — uma sugestão errada é pior do que nenhuma.
 *
 * Recebe pixels crus (`ARGB_8888`) para ser testável sem Android; quem decodifica é o
 * [AndroidPhotoProcessor], que também aplica a orientação EXIF antes de chamar.
 */
internal object CardBoundsDetector {

    /** Diferença máxima por canal entre dois pixels vizinhos para continuar o fundo. */
    private const val LOCAL_TOLERANCE = 16

    /** Diferença máxima por canal em relação à mediana da borda. */
    private const val GLOBAL_TOLERANCE = 64

    /** Um pixel da borda só inicia o alagamento se for próximo da mediana. */
    private const val SEED_TOLERANCE = 40

    /** Fração mínima de pixels de fundo para valer a pena sugerir um recorte. */
    private const val MIN_BACKGROUND = 0.05f

    /** Fração mínima da imagem que a cartela precisa ocupar. */
    private const val MIN_SUBJECT = 0.15f

    /** Folga para não cortar a borda da cartela por um pixel. */
    private const val MARGIN = 0.003f

    fun detect(pixels: IntArray, width: Int, height: Int): NormalizedRect? {
        if (width < 24 || height < 24 || pixels.size < width * height) return null
        val total = width * height

        val border = borderIndices(width, height)
        val refRed = median(border.map { red(pixels[it]) })
        val refGreen = median(border.map { green(pixels[it]) })
        val refBlue = median(border.map { blue(pixels[it]) })

        val isBackground = BooleanArray(total)
        val queue = IntArray(total)
        var head = 0
        var tail = 0

        for (index in border) {
            if (isBackground[index]) continue
            if (distance(pixels[index], refRed, refGreen, refBlue) <= SEED_TOLERANCE) {
                isBackground[index] = true
                queue[tail++] = index
            }
        }
        // Borda heterogênea: provavelmente a cartela encosta nela ou o fundo é uma cena
        // qualquer; não há como separar fundo e objeto com segurança.
        if (tail < border.size / 3) return null

        while (head < tail) {
            val index = queue[head++]
            val x = index % width
            val y = index / width
            val source = pixels[index]

            if (x > 0) tail = grow(index - 1, source, isBackground, queue, tail, pixels, refRed, refGreen, refBlue)
            if (x < width - 1) tail = grow(index + 1, source, isBackground, queue, tail, pixels, refRed, refGreen, refBlue)
            if (y > 0) tail = grow(index - width, source, isBackground, queue, tail, pixels, refRed, refGreen, refBlue)
            if (y < height - 1) tail = grow(index + width, source, isBackground, queue, tail, pixels, refRed, refGreen, refBlue)
        }

        val backgroundCount = tail
        if (backgroundCount < total * MIN_BACKGROUND) return null

        val subject = largestForeground(isBackground, width, height) ?: return null
        if (subject.area < total * MIN_SUBJECT) return null

        val left = subject.left / width.toFloat()
        val top = subject.top / height.toFloat()
        val right = (subject.right + 1) / width.toFloat()
        val bottom = (subject.bottom + 1) / height.toFloat()

        // Nada a ganhar quando a cartela já ocupa quase tudo.
        if ((right - left) * (bottom - top) > 0.97f) return null

        return NormalizedRect.safe(
            (left - MARGIN).coerceAtLeast(0f),
            (top - MARGIN).coerceAtLeast(0f),
            (right + MARGIN).coerceAtMost(1f),
            (bottom + MARGIN).coerceAtMost(1f),
        ).takeUnless { it.isFull }
    }

    private fun grow(
        candidate: Int,
        source: Int,
        isBackground: BooleanArray,
        queue: IntArray,
        tail: Int,
        pixels: IntArray,
        refRed: Int,
        refGreen: Int,
        refBlue: Int,
    ): Int {
        if (isBackground[candidate]) return tail
        val pixel = pixels[candidate]
        val local = max(
            max(abs(red(pixel) - red(source)), abs(green(pixel) - green(source))),
            abs(blue(pixel) - blue(source)),
        )
        if (local > LOCAL_TOLERANCE) return tail
        if (distance(pixel, refRed, refGreen, refBlue) > GLOBAL_TOLERANCE) return tail
        isBackground[candidate] = true
        queue[tail] = candidate
        return tail + 1
    }

    private data class Subject(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
        val area: Int,
    )

    private fun largestForeground(isBackground: BooleanArray, width: Int, height: Int): Subject? {
        val visited = BooleanArray(isBackground.size)
        val queue = IntArray(isBackground.size)
        var best: Subject? = null

        for (start in isBackground.indices) {
            if (isBackground[start] || visited[start]) continue
            var head = 0
            var tail = 0
            queue[tail++] = start
            visited[start] = true
            var area = 0
            var left = width
            var top = height
            var right = 0
            var bottom = 0

            while (head < tail) {
                val index = queue[head++]
                val x = index % width
                val y = index / width
                area++
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y

                if (x > 0) tail = visit(index - 1, isBackground, visited, queue, tail)
                if (x < width - 1) tail = visit(index + 1, isBackground, visited, queue, tail)
                if (y > 0) tail = visit(index - width, isBackground, visited, queue, tail)
                if (y < height - 1) tail = visit(index + width, isBackground, visited, queue, tail)
            }

            if (area > (best?.area ?: 0)) {
                best = Subject(left, top, right, bottom, area)
            }
        }
        return best
    }

    private fun visit(
        index: Int,
        isBackground: BooleanArray,
        visited: BooleanArray,
        queue: IntArray,
        tail: Int,
    ): Int {
        if (isBackground[index] || visited[index]) return tail
        visited[index] = true
        queue[tail] = index
        return tail + 1
    }

    private fun borderIndices(width: Int, height: Int): IntArray {
        val indices = ArrayList<Int>(2 * (width + height))
        for (x in 0 until width) {
            indices.add(x)
            indices.add((height - 1) * width + x)
        }
        for (y in 1 until height - 1) {
            indices.add(y * width)
            indices.add(y * width + width - 1)
        }
        return indices.toIntArray()
    }

    private fun distance(pixel: Int, red: Int, green: Int, blue: Int): Int = max(
        max(abs(this.red(pixel) - red), abs(this.green(pixel) - green)),
        abs(this.blue(pixel) - blue),
    )

    private fun median(values: List<Int>): Int {
        if (values.isEmpty()) return 0
        val sorted = values.sorted()
        return sorted[sorted.size / 2]
    }

    private fun red(pixel: Int): Int = (pixel shr 16) and 0xFF

    private fun green(pixel: Int): Int = (pixel shr 8) and 0xFF

    private fun blue(pixel: Int): Int = pixel and 0xFF
}
