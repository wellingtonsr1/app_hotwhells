package br.com.diecastcollector.domain.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Retângulo de corte em coordenadas normalizadas (0..1) da imagem **já orientada**
 * (rotação e espelho aplicados antes do corte).
 *
 * Normalizar permite guardar a edição sem depender do tamanho do arquivo e reaplicá-la
 * na prévia (menor) e na exportação (maior) com o mesmo resultado de enquadramento
 * (ADR-001, FR-205).
 */
public data class NormalizedRect(
    public val left: Float,
    public val top: Float,
    public val right: Float,
    public val bottom: Float,
) {
    public val width: Float get() = right - left
    public val height: Float get() = bottom - top

    /** Verdadeiro quando cobre a imagem inteira. */
    public val isFull: Boolean
        get() = left <= 0.001f && top <= 0.001f && right >= 0.999f && bottom >= 0.999f

    public companion object {
        public const val MIN_SIZE: Float = 0.08f

        /** Corte que preserva a imagem inteira. */
        public val FULL: NormalizedRect = NormalizedRect(0f, 0f, 1f, 1f)

        /**
         * Normaliza e limita o retângulo: ordem dos lados, tamanho mínimo e limites 0..1.
         * Entrada inválida (NaN/infinito) volta para [FULL] em vez de quebrar o editor.
         */
        public fun safe(left: Float, top: Float, right: Float, bottom: Float): NormalizedRect {
            if (!left.isFinite() || !top.isFinite() || !right.isFinite() || !bottom.isFinite()) {
                return FULL
            }
            val l = min(left, right).coerceIn(0f, 1f - MIN_SIZE)
            val t = min(top, bottom).coerceIn(0f, 1f - MIN_SIZE)
            val r = max(left, right).coerceIn(l + MIN_SIZE, 1f)
            val b = max(top, bottom).coerceIn(t + MIN_SIZE, 1f)
            return NormalizedRect(l, t, r, b)
        }
    }
}

/** Proporções de enquadramento oferecidas no editor (FR-205). */
public enum class PhotoAspect(public val ratio: Float?) {
    FREE(null),
    ORIGINAL(null),
    SQUARE(1f),
    PORTRAIT_4_5(4f / 5f),
    LANDSCAPE_16_9(16f / 9f),
    ;

    public val isFixed: Boolean get() = ratio != null
}

/**
 * Perfis de exportação (FR-212). A aresta limita a resolução; a qualidade JPEG controla
 * o peso. Os três ficam dentro do limite de armazenamento já aplicado pelo `ImageStore`.
 */
public enum class PhotoQuality(public val maxEdge: Int, public val jpegQuality: Int) {
    HIGH(maxEdge = 1600, jpegQuality = 92),
    BALANCED(maxEdge = 1600, jpegQuality = 85),
    COMPACT(maxEdge = 1280, jpegQuality = 72),
}

/**
 * Parâmetros completos de uma edição não destrutiva. Nunca contém pixels: o arquivo
 * original é a fonte da verdade e este objeto descreve o que fazer com ele.
 *
 * Toda atualização passa por [normalized]; faixas inválidas vindas da interface são
 * limitadas em vez de propagadas (Art. V.6).
 */
public data class PhotoEdit(
    public val crop: NormalizedRect = NormalizedRect.FULL,
    public val aspect: PhotoAspect = PhotoAspect.ORIGINAL,
    public val rotationQuarterTurns: Int = 0,
    public val flipHorizontal: Boolean = false,
    public val straightenDegrees: Float = 0f,
    public val brightness: Float = 0f,
    public val contrast: Float = 0f,
    public val saturation: Float = 0f,
    public val sharpness: Float = 0f,
    public val quality: PhotoQuality = PhotoQuality.BALANCED,
) {
    public companion object {
        public const val MAX_STRAIGHTEN: Float = 15f

        /** Edição sem nenhuma alteração: "Salvar sem editar" e nenhuma derivada gerada. */
        public val NONE: PhotoEdit = PhotoEdit()

        /**
         * Lê os parâmetros gravados. Texto ausente ou inválido volta para [NONE] — uma
         * string corrompida não pode impedir a foto de abrir (Art. V.6).
         */
        public fun fromParameters(text: String?): PhotoEdit {
            if (text.isNullOrBlank()) return NONE
            return runCatching {
                val fields = text.split(';')
                    .mapNotNull { part ->
                        val separator = part.indexOf('=')
                        if (separator <= 0) null else part.take(separator) to part.substring(separator + 1)
                    }
                    .toMap()

                val cropValues = fields["crop"]?.split(',')?.mapNotNull { it.toFloatOrNull() }.orEmpty()
                val crop = if (cropValues.size == 4) {
                    NormalizedRect.safe(cropValues[0], cropValues[1], cropValues[2], cropValues[3])
                } else {
                    NormalizedRect.FULL
                }
                PhotoEdit(
                    crop = crop,
                    aspect = fields["aspect"]?.let { name ->
                        PhotoAspect.entries.firstOrNull { it.name == name }
                    } ?: PhotoAspect.ORIGINAL,
                    rotationQuarterTurns = fields["rot"]?.toIntOrNull() ?: 0,
                    flipHorizontal = fields["flip"] == "1",
                    straightenDegrees = fields["straighten"]?.toFloatOrNull() ?: 0f,
                    brightness = fields["brightness"]?.toFloatOrNull() ?: 0f,
                    contrast = fields["contrast"]?.toFloatOrNull() ?: 0f,
                    saturation = fields["saturation"]?.toFloatOrNull() ?: 0f,
                    sharpness = fields["sharpness"]?.toFloatOrNull() ?: 0f,
                    quality = fields["quality"]?.let { name ->
                        PhotoQuality.entries.firstOrNull { it.name == name }
                    } ?: PhotoQuality.BALANCED,
                ).normalized()
            }.getOrDefault(NONE)
        }
    }

    /** Verdadeiro quando não há alteração de pixel ou de enquadramento. */
    public val isNeutral: Boolean
        get() = crop.isFull &&
            rotationQuarterTurns == 0 &&
            !flipHorizontal &&
            abs(straightenDegrees) < 0.01f &&
            abs(brightness) < 0.001f &&
            abs(contrast) < 0.001f &&
            abs(saturation) < 0.001f &&
            abs(sharpness) < 0.001f

    /** Limita todas as faixas; aplicado em cada mudança vinda da tela. */
    public fun normalized(): PhotoEdit = copy(
        crop = crop.let {
            NormalizedRect.safe(it.left, it.top, it.right, it.bottom)
        },
        rotationQuarterTurns = ((rotationQuarterTurns % 4) + 4) % 4,
        straightenDegrees = straightenDegrees.coerceIn(-MAX_STRAIGHTEN, MAX_STRAIGHTEN),
        brightness = brightness.coerceIn(-1f, 1f),
        contrast = contrast.coerceIn(-1f, 1f),
        saturation = saturation.coerceIn(-1f, 1f),
        sharpness = sharpness.coerceIn(0f, 1f),
    )

    /**
     * Escolhe a proporção de enquadramento e recalcula o corte, centrado e maior possível
     * dentro do corte atual. [sourceWidth]/[sourceHeight] são as dimensões da imagem já
     * orientada — sem elas não há como igualar proporção em pixels.
     */
    public fun withAspect(
        aspect: PhotoAspect,
        sourceWidth: Int,
        sourceHeight: Int,
    ): PhotoEdit {
        val normalizedSelf = normalized()
        val target = aspect.ratio
        if (target == null || sourceWidth <= 0 || sourceHeight <= 0) {
            return normalizedSelf.copy(aspect = aspect)
        }

        val pxWidth = normalizedSelf.crop.width * sourceWidth
        val pxHeight = normalizedSelf.crop.height * sourceHeight
        if (pxWidth <= 0f || pxHeight <= 0f) return normalizedSelf.copy(aspect = aspect)

        val currentRatio = pxWidth / pxHeight
        val newPxWidth: Float
        val newPxHeight: Float
        if (currentRatio > target) {
            newPxHeight = pxHeight
            newPxWidth = pxHeight * target
        } else {
            newPxWidth = pxWidth
            newPxHeight = pxWidth / target
        }

        val centerX = (normalizedSelf.crop.left + normalizedSelf.crop.right) / 2f
        val centerY = (normalizedSelf.crop.top + normalizedSelf.crop.bottom) / 2f
        val halfW = (newPxWidth / sourceWidth) / 2f
        val halfH = (newPxHeight / sourceHeight) / 2f
        val crop = NormalizedRect.safe(
            left = centerX - halfW,
            top = centerY - halfH,
            right = centerX + halfW,
            bottom = centerY + halfH,
        )
        return normalizedSelf.copy(aspect = aspect, crop = crop)
    }

    /**
     * Aplica um novo retângulo de corte. Com proporção fixa, ajusta a altura à largura
     * (mantendo o centro) e limita aos limites da imagem.
     */
    public fun withCrop(
        crop: NormalizedRect,
        sourceWidth: Int,
        sourceHeight: Int,
    ): PhotoEdit {
        val normalizedSelf = normalized()
        val target = aspect.ratio
        val base = NormalizedRect.safe(crop.left, crop.top, crop.right, crop.bottom)
        if (target == null || sourceWidth <= 0 || sourceHeight <= 0) {
            return normalizedSelf.copy(crop = base)
        }

        val normalizedRatio = target * sourceHeight / sourceWidth
        val centerX = (base.left + base.right) / 2f
        val centerY = (base.top + base.bottom) / 2f
        val halfW = base.width / 2f
        val halfH = halfW / normalizedRatio
        val adjusted = NormalizedRect.safe(
            left = centerX - halfW,
            top = centerY - halfH,
            right = centerX + halfW,
            bottom = centerY + halfH,
        )
        return normalizedSelf.copy(crop = adjusted)
    }

    /**
     * Serialização compacta para `photo.edit_params`. Formato versionado e legível, sem
     * dependência de biblioteca de JSON no módulo de domínio (puro JVM).
     */
    public fun toParameters(): String {
        val value = normalized()
        return buildString {
            append("v1")
            append(";crop=").append(value.crop.left.format())
            append(',').append(value.crop.top.format())
            append(',').append(value.crop.right.format())
            append(',').append(value.crop.bottom.format())
            append(";aspect=").append(value.aspect.name)
            append(";rot=").append(value.rotationQuarterTurns)
            append(";flip=").append(if (value.flipHorizontal) 1 else 0)
            append(";straighten=").append(value.straightenDegrees.format())
            append(";brightness=").append(value.brightness.format())
            append(";contrast=").append(value.contrast.format())
            append(";saturation=").append(value.saturation.format())
            append(";sharpness=").append(value.sharpness.format())
            append(";quality=").append(value.quality.name)
        }
    }

    private fun Float.format(): String = String.format(Locale.US, "%.4f", this)
}
