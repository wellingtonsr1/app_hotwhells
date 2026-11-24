package br.com.diecastcollector.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

/**
 * Regras puras da edição não destrutiva (FR-205–FR-212, ADR-001).
 *
 * O corte é normalizado; a proporção só pode ser garantida em pixels, por isso os testes
 * passam as dimensões da imagem orientada e conferem o resultado no mesmo espaço.
 */
public class PhotoEditTest {

    @Test
    public fun `edicao padrao e neutra`() {
        assertThat(PhotoEdit.NONE.isNeutral).isTrue()
        assertThat(PhotoEdit.NONE.copy(quality = PhotoQuality.HIGH).isNeutral).isTrue()
        assertThat(PhotoEdit.NONE.copy(brightness = 0.2f).isNeutral).isFalse()
        assertThat(PhotoEdit.NONE.copy(crop = NormalizedRect.safe(0.1f, 0.1f, 0.9f, 0.9f)).isNeutral)
            .isFalse()
    }

    @Test
    public fun `normalized limita todas as faixas`() {
        val edit = PhotoEdit(
            rotationQuarterTurns = 7,
            straightenDegrees = 42f,
            brightness = -4f,
            contrast = 3f,
            saturation = 9f,
            sharpness = -2f,
        ).normalized()

        assertThat(edit.rotationQuarterTurns).isEqualTo(3)
        assertThat(edit.straightenDegrees).isEqualTo(PhotoEdit.MAX_STRAIGHTEN)
        assertThat(edit.brightness).isEqualTo(-1f)
        assertThat(edit.contrast).isEqualTo(1f)
        assertThat(edit.saturation).isEqualTo(1f)
        assertThat(edit.sharpness).isEqualTo(0f)
    }

    @Test
    public fun `rotacao negativa normaliza para o equivalente positivo`() {
        val edit = PhotoEdit(rotationQuarterTurns = -1).normalized()
        assertThat(edit.rotationQuarterTurns).isEqualTo(3)
    }

    @Test
    public fun `corte invalido volta para a imagem inteira`() {
        assertThat(NormalizedRect.safe(Float.NaN, 0f, 1f, 1f)).isEqualTo(NormalizedRect.FULL)
        assertThat(NormalizedRect.safe(0.9f, 0.9f, 0.1f, 0.1f).width)
            .isAtLeast(NormalizedRect.MIN_SIZE)
        val clamped = NormalizedRect.safe(-2f, -2f, 5f, 5f)
        assertThat(clamped.left).isEqualTo(0f)
        assertThat(clamped.top).isEqualTo(0f)
        assertThat(clamped.right).isEqualTo(1f)
        assertThat(clamped.bottom).isEqualTo(1f)
    }

    @Test
    public fun `proporcao quadrada gera corte quadrado em pixels`() {
        val edit = PhotoEdit.NONE.withAspect(PhotoAspect.SQUARE, sourceWidth = 400, sourceHeight = 300)
        val pixelWidth = edit.crop.width * 400
        val pixelHeight = edit.crop.height * 300

        assertThat(abs(pixelWidth - pixelHeight)).isLessThan(0.5f)
        assertThat(edit.aspect).isEqualTo(PhotoAspect.SQUARE)
    }

    @Test
    public fun `proporcao paisagem gera corte 16 por 9`() {
        val edit = PhotoEdit.NONE.withAspect(
            PhotoAspect.LANDSCAPE_16_9,
            sourceWidth = 300,
            sourceHeight = 400,
        )
        val ratio = (edit.crop.width * 300) / (edit.crop.height * 400)

        assertThat(ratio).isWithin(0.01f).of(16f / 9f)
        assertThat(edit.crop.left).isAtLeast(0f)
        assertThat(edit.crop.bottom).isAtMost(1f)
    }

    @Test
    public fun `proporcao original preserva a imagem inteira`() {
        val edit = PhotoEdit.NONE.withAspect(PhotoAspect.ORIGINAL, 500, 700)
        assertThat(edit.crop).isEqualTo(NormalizedRect.FULL)
    }

    @Test
    public fun `corte com proporcao fixa respeita a razao`() {
        val base = PhotoEdit.NONE
            .withAspect(PhotoAspect.PORTRAIT_4_5, sourceWidth = 400, sourceHeight = 400)
        val moved = base.withCrop(
            NormalizedRect.safe(0.2f, 0.2f, 0.7f, 0.9f),
            sourceWidth = 400,
            sourceHeight = 400,
        )
        val ratio = (moved.crop.width * 400) / (moved.crop.height * 400)

        assertThat(ratio).isWithin(0.01f).of(4f / 5f)
        assertThat(moved.crop.left).isAtLeast(0f)
        assertThat(moved.crop.right).isAtMost(1f)
    }

    @Test
    public fun `parametros sobrevivem a serializacao`() {
        val edit = PhotoEdit(
            crop = NormalizedRect.safe(0.1f, 0.2f, 0.8f, 0.9f),
            aspect = PhotoAspect.SQUARE,
            rotationQuarterTurns = 1,
            flipHorizontal = true,
            straightenDegrees = -4.5f,
            brightness = 0.25f,
            contrast = -0.5f,
            saturation = 0.75f,
            sharpness = 0.4f,
            quality = PhotoQuality.HIGH,
        ).normalized()

        val restored = PhotoEdit.fromParameters(edit.toParameters())

        assertThat(restored).isEqualTo(edit)
    }

    @Test
    public fun `parametros corrompidos nao impedem abrir a foto`() {
        assertThat(PhotoEdit.fromParameters(null)).isEqualTo(PhotoEdit.NONE)
        assertThat(PhotoEdit.fromParameters("lixo")).isEqualTo(PhotoEdit.NONE)
        assertThat(PhotoEdit.fromParameters("v1;crop=1,2;aspect=NAO_EXISTE;rot=abc"))
            .isEqualTo(PhotoEdit.NONE)
    }
}
