package br.com.diecastcollector.domain.usecase

import br.com.diecastcollector.domain.model.DuplicateAvailability
import br.com.diecastcollector.domain.model.ExemplarStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tabela-verdade do aviso de exemplar único (T504, FR-515, BR-503).
 *
 * Cada linha de `data-model.md` §3 aparece aqui: os dois gatilhos, os três não-gatilhos e
 * os status que nunca chegam à ação.
 */
public class UniqueExemplarPolicyTest {

    @Test
    public fun `disponibilizar a unica unidade avisa`() {
        DuplicateAvailability.entries
            .filter { it != DuplicateAvailability.UNAVAILABLE }
            .forEach { availability ->
                assertThat(
                    UniqueExemplarPolicy.warnsOnAvailability(
                        ownedInVariant = 1,
                        status = ExemplarStatus.OWNED,
                        availability = availability,
                        hasActiveNegotiation = false,
                    ),
                ).isTrue()
            }
    }

    @Test
    public fun `marcar indisponivel nao avisa`() {
        assertThat(
            UniqueExemplarPolicy.warnsOnAvailability(
                ownedInVariant = 1,
                status = ExemplarStatus.OWNED,
                availability = DuplicateAvailability.UNAVAILABLE,
                hasActiveNegotiation = false,
            ),
        ).isFalse()
    }

    @Test
    public fun `editar a disponibilidade com negociacao ativa nao repete o aviso`() {
        assertThat(
            UniqueExemplarPolicy.warnsOnAvailability(
                ownedInVariant = 1,
                status = ExemplarStatus.OWNED,
                availability = DuplicateAvailability.TRADE,
                hasActiveNegotiation = true,
            ),
        ).isFalse()
    }

    @Test
    public fun `abrir negociacao da unica unidade avisa`() {
        assertThat(
            UniqueExemplarPolicy.warnsOnNegotiation(
                ownedInVariant = 1,
                status = ExemplarStatus.OWNED,
                hasActiveNegotiation = false,
            ),
        ).isTrue()
    }

    @Test
    public fun `abrir negociacao com negociacao ativa nao avisa`() {
        assertThat(
            UniqueExemplarPolicy.warnsOnNegotiation(
                ownedInVariant = 1,
                status = ExemplarStatus.OWNED,
                hasActiveNegotiation = true,
            ),
        ).isFalse()
    }

    @Test
    public fun `variante com duas ou mais unidades nunca avisa`() {
        listOf(2, 3, 10).forEach { count ->
            assertThat(
                UniqueExemplarPolicy.warnsOnAvailability(
                    count,
                    ExemplarStatus.OWNED,
                    DuplicateAvailability.SALE,
                    hasActiveNegotiation = false,
                ),
            ).isFalse()
            assertThat(
                UniqueExemplarPolicy.warnsOnNegotiation(
                    count,
                    ExemplarStatus.OWNED,
                    hasActiveNegotiation = false,
                ),
            ).isFalse()
        }
    }

    @Test
    public fun `exemplar fora do acervo nunca avisa`() {
        listOf(ExemplarStatus.SOLD, ExemplarStatus.TRADED, ExemplarStatus.ARCHIVED).forEach { status ->
            assertThat(
                UniqueExemplarPolicy.warnsOnAvailability(
                    1,
                    status,
                    DuplicateAvailability.SALE,
                    hasActiveNegotiation = false,
                ),
            ).isFalse()
            assertThat(
                UniqueExemplarPolicy.warnsOnNegotiation(1, status, hasActiveNegotiation = false),
            ).isFalse()
        }
    }

    @Test
    public fun `variante sem unidade no acervo nao avisa`() {
        assertThat(
            UniqueExemplarPolicy.warnsOnNegotiation(
                ownedInVariant = 0,
                status = ExemplarStatus.OWNED,
                hasActiveNegotiation = false,
            ),
        ).isFalse()
    }
}
