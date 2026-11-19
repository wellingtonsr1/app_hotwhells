package br.com.hotwheelscollector.domain.usecase

import br.com.hotwheelscollector.domain.model.Location
import br.com.hotwheelscollector.domain.model.LocationTree
import br.com.hotwheelscollector.domain.model.LocationType
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

/**
 * Árvore de localizações (T099, FR-088–FR-089).
 *
 * Os casos difíceis não são a hierarquia feliz: são pai arquivado deixando filho órfão e
 * ciclo gravado por fora — nenhum dos dois pode fazer a localização desaparecer da tela
 * enquanto ainda guarda exemplares.
 */
public class LocationTreeTest {

    @Test
    public fun `monta hierarquia em ordem alfabetica e soma exemplares da subarvore`() {
        val locations = listOf(
            location("estante", "Estante A"),
            location("prateleira3", "Prateleira 3", parentId = "estante"),
            location("prateleira1", "Prateleira 1", parentId = "estante"),
            location("caixa", "Caixa 12"),
        )
        val counts = mapOf("estante" to 1, "prateleira3" to 2, "prateleira1" to 4, "caixa" to 7)

        val tree = LocationTree.build(locations, counts)

        assertThat(tree.map { it.location.name }).containsExactly("Caixa 12", "Estante A").inOrder()
        val estante = tree.single { it.id == "estante" }
        assertThat(estante.exemplarCount).isEqualTo(1)
        assertThat(estante.children.map { it.location.name })
            .containsExactly("Prateleira 1", "Prateleira 3").inOrder()
        assertThat(estante.children.map { it.exemplarCount }).containsExactly(4, 2).inOrder()
        assertThat(estante.subtreeExemplarCount).isEqualTo(7)
    }

    @Test
    public fun `filho de localizacao arquivada aparece como raiz em vez de sumir`() {
        val locations = listOf(location("orca", "Caixa órfã", parentId = "apagada"))

        val tree = LocationTree.build(locations, mapOf("orca" to 3))

        assertThat(tree).hasSize(1)
        assertThat(tree.single().location.name).isEqualTo("Caixa órfã")
        assertThat(tree.single().subtreeExemplarCount).isEqualTo(3)
    }

    @Test
    public fun `ciclo nao trava a montagem e ainda mostra as localizacoes`() {
        val a = location("a", "A", parentId = "b")
        val b = location("b", "B", parentId = "a")

        val tree = LocationTree.build(listOf(a, b))

        assertThat(tree).isNotEmpty()
        val ids = mutableListOf<String>()
        fun collect(nodes: List<br.com.hotwheelscollector.domain.model.LocationNode>) {
            nodes.forEach { node ->
                ids += node.id
                collect(node.children)
            }
        }
        collect(tree)
        assertThat(ids).containsAtLeast("a", "b")
        assertThat(ids).hasSize(2)
    }

    @Test
    public fun `localizacao sem exemplares aparece com zero`() {
        val tree = LocationTree.build(listOf(location("vazia", "Gaveta")))
        assertThat(tree.single().exemplarCount).isEqualTo(0)
        assertThat(tree.single().children).isEmpty()
    }

    private fun location(
        id: String,
        name: String,
        parentId: String? = null,
    ): Location = Location(
        id = id,
        type = LocationType.SHELF,
        name = name,
        parentId = parentId,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
}
