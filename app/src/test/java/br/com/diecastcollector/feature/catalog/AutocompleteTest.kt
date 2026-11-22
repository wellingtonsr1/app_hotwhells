package br.com.diecastcollector.feature.catalog

import br.com.diecastcollector.domain.model.Manufacturer
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

/**
 * A sugestão de fabricante existe para impedir que a mesma marca entre duas vezes no
 * banco por diferença de digitação. O que se testa aqui é só a regra de filtro: quando
 * sugerir, quando calar a boca e quanto mostrar.
 */
class AutocompleteTest {

    private val manufacturers = listOf(
        manufacturer("Hot Wheels"),
        manufacturer("Matchbox"),
        manufacturer("Mini GT"),
        manufacturer("Greenlight"),
        manufacturer("Johnny Lightning"),
        manufacturer("Auto World"),
    )

    @Test
    fun `menos de dois caracteres nao sugere nada`() {
        assertThat(manufacturerSuggestions("", manufacturers)).isEmpty()
        assertThat(manufacturerSuggestions("h", manufacturers)).isEmpty()
        assertThat(manufacturerSuggestions("  h  ", manufacturers)).isEmpty()
    }

    @Test
    fun `trecho do nome sugere o fabricante ignorando caixa`() {
        assertThat(manufacturerSuggestions("match", manufacturers).map { it.name })
            .containsExactly("Matchbox")
        assertThat(manufacturerSuggestions("MINI", manufacturers).map { it.name })
            .containsExactly("Mini GT")
        assertThat(manufacturerSuggestions("Hot W", manufacturers).map { it.name })
            .containsExactly("Hot Wheels")
    }

    @Test
    fun `nome digitado por inteiro nao aparece como sugestao`() {
        assertThat(manufacturerSuggestions("Matchbox", manufacturers)).isEmpty()
        assertThat(manufacturerSuggestions("  matchbox ", manufacturers)).isEmpty()
    }

    @Test
    fun `variacao de espaco e hifen aponta para o fabricante existente`() {
        // É exatamente o caso que duplica fabricante no banco: "HotWheels" e
        // "HOT-WHEELS" devem cair em "Hot Wheels", não virar registro novo.
        assertThat(manufacturerSuggestions("HotWheels", manufacturers)).isEmpty()
        assertThat(manufacturerSuggestions("HOT-WHEELS", manufacturers)).isEmpty()
        assertThat(manufacturerSuggestions("Hot Wheel", manufacturers).map { it.name })
            .containsExactly("Hot Wheels")
        assertThat(manufacturerSuggestions("Match box", manufacturers)).isEmpty()
    }

    @Test
    fun `lista vazia nao quebra e o limite de sugestoes e cinco`() {
        assertThat(manufacturerSuggestions("hot", emptyList())).isEmpty()

        val many = (1..8).map { manufacturer("Hot Rods $it") }
        assertThat(manufacturerSuggestions("hot", many)).hasSize(5)
    }

    private fun manufacturer(name: String): Manufacturer = Manufacturer(
        id = "m-${name.lowercase().replace(' ', '-')}",
        name = name,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
}
