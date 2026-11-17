package br.com.hotwheelscollector.domain.validation

import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.PackagingCondition
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Testes unitários de validação do catálogo (T037 - US2).
 *
 * Cobre:
 * - Escala no formato 1:N (válidas e inválidas).
 * - Prioridade no intervalo 1..3.
 * - Tratamento de campos desconhecidos como NULL (rejeitando strings sentinelas).
 * - Condição da miniatura e da embalagem.
 * - Validação de ano e limites de texto.
 */
public class CatalogValidationTest {

    @Test
    fun `escala 1 por N valida formatos usuais`() {
        val validScales = listOf("1:64", "1:18", "1:43", "1:87", "1:100", "1:1", "1:5")
        for (scale in validScales) {
            val result = CatalogValidation.validateScale(scale)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isEqualTo(scale)
        }
    }

    @Test
    fun `escala com espacos extras e normalizada`() {
        val result = CatalogValidation.validateScale("  1:64  ")
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo("1:64")
    }

    @Test
    fun `escala invalida e recusada com erro em portugues`() {
        val invalidScales = listOf("64", "1:", "1:0", "1:abc", "2:64", "", "   ", "1:9999")
        for (invalid in invalidScales) {
            val result = CatalogValidation.validateScale(invalid)
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()?.message)
                .contains("Escala deve estar no formato 1:N")
        }
    }

    @Test
    fun `prioridade valida aceita 1, 2 e 3`() {
        assertThat(CatalogValidation.validatePriority(1).isSuccess).isTrue()
        assertThat(CatalogValidation.validatePriority(2).isSuccess).isTrue()
        assertThat(CatalogValidation.validatePriority(3).isSuccess).isTrue()
    }

    @Test
    fun `prioridade fora de 1 a 3 e recusada`() {
        assertThat(CatalogValidation.validatePriority(0).isFailure).isTrue()
        assertThat(CatalogValidation.validatePriority(4).isFailure).isTrue()
        assertThat(CatalogValidation.validatePriority(-1).isFailure).isTrue()
    }

    @Test
    fun `campos opcionais desconhecidos viram NULL e sentinelas sao limpas`() {
        assertThat(CatalogValidation.cleanOptionalString(null)).isNull()
        assertThat(CatalogValidation.cleanOptionalString("")).isNull()
        assertThat(CatalogValidation.cleanOptionalString("   ")).isNull()
        assertThat(CatalogValidation.cleanOptionalString("N/A")).isNull()
        assertThat(CatalogValidation.cleanOptionalString("n/a")).isNull()
        assertThat(CatalogValidation.cleanOptionalString("null")).isNull()
        assertThat(CatalogValidation.cleanOptionalString("NULL")).isNull()
        assertThat(CatalogValidation.cleanOptionalString("none")).isNull()
        assertThat(CatalogValidation.cleanOptionalString("não informado")).isNull()

        // Texto legítimo preservado
        assertThat(CatalogValidation.cleanOptionalString("  Mainline 2024  ")).isEqualTo("Mainline 2024")
    }

    @Test
    fun `valida todas as condicoes permitidas de miniatura`() {
        for (condition in Condition.entries) {
            val result = CatalogValidation.validateCondition(condition)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isEqualTo(condition)
        }
    }

    @Test
    fun `valida todas as condicoes de embalagem permitidas`() {
        assertThat(CatalogValidation.validatePackagingCondition(null).isSuccess).isTrue()
        for (packaging in PackagingCondition.entries) {
            val result = CatalogValidation.validatePackagingCondition(packaging)
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isEqualTo(packaging)
        }
    }

    @Test
    fun `valida limites de ano`() {
        assertThat(CatalogValidation.validateYear(null).isSuccess).isTrue()
        assertThat(CatalogValidation.validateYear(1968, 2026).isSuccess).isTrue()
        assertThat(CatalogValidation.validateYear(2026, 2026).isSuccess).isTrue()
        assertThat(CatalogValidation.validateYear(2028, 2026).isSuccess).isTrue()

        // Antes do primeiro carro
        assertThat(CatalogValidation.validateYear(1800, 2026).isFailure).isTrue()
        // Muito no futuro
        assertThat(CatalogValidation.validateYear(2035, 2026).isFailure).isTrue()
    }

    @Test
    fun `valida nomes obrigatorios e limites`() {
        assertThat(CatalogValidation.validateRequiredName("", "fabricante").isFailure).isTrue()
        assertThat(CatalogValidation.validateRequiredName("   ", "modelo").isFailure).isTrue()
        assertThat(CatalogValidation.validateRequiredName("Hot Wheels", "fabricante").isSuccess).isTrue()
        assertThat(CatalogValidation.validateRequiredName("a".repeat(121), "modelo").isFailure).isTrue()
    }
}
