package br.com.hotwheelscollector.domain.usecase

import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.Exemplar
import br.com.hotwheelscollector.domain.model.ExemplarStatus
import br.com.hotwheelscollector.domain.model.IsoDate
import br.com.hotwheelscollector.domain.model.MoneyCents
import br.com.hotwheelscollector.domain.model.PackagingCondition
import br.com.hotwheelscollector.domain.repository.AddExemplarOutcome
import br.com.hotwheelscollector.domain.repository.ExemplarDependents
import br.com.hotwheelscollector.domain.repository.ExemplarRepository
import br.com.hotwheelscollector.domain.repository.NewAcquisition
import br.com.hotwheelscollector.domain.repository.NewExemplar
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Testes do cadastro de exemplar (T029).
 *
 * O repositório falso registra o que recebeu, então o teste verifica a **decisão** do
 * caso de uso — normalização, recusas e contagem — sem banco no meio.
 */
class AddExemplarUseCaseTest {

    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC)
    private val repository = FakeExemplarRepository()
    private val useCase = AddExemplarUseCase(repository, fixedClock)

    @Test
    fun `registra exemplar como OWNED com codigo interno gerado`() = runTest {
        val result = useCase(NewExemplar(variantId = "v1", condition = Condition.SEALED))

        assertThat(result.isSuccess).isTrue()
        val outcome = result.getOrThrow()
        assertThat(outcome.exemplar.status).isEqualTo(ExemplarStatus.OWNED)
        assertThat(outcome.exemplar.internalCode).isNotEmpty()
        assertThat(outcome.sameVariantCount).isEqualTo(1)
    }

    @Test
    fun `segundo exemplar da mesma variante devolve contagem dois sem bloquear`() = runTest {
        useCase(NewExemplar(variantId = "v1", condition = Condition.SEALED))
        val second = useCase(NewExemplar(variantId = "v1", condition = Condition.LOOSE))

        assertThat(second.isSuccess).isTrue()
        assertThat(second.getOrThrow().sameVariantCount).isEqualTo(2)
        assertThat(second.getOrThrow().exemplar.condition).isEqualTo(Condition.LOOSE)
    }

    @Test
    fun `recusa data de compra no futuro`() = runTest {
        val tomorrow = IsoDate(LocalDate.of(2026, 11, 17))
        val result = useCase(
            NewExemplar(
                variantId = "v1",
                condition = Condition.SEALED,
                acquisition = NewAcquisition(date = tomorrow, price = MoneyCents(5900)),
            ),
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(DomainError.Validation::class.java)
        assertThat(repository.received).isEmpty()
    }

    @Test
    fun `recusa desconto maior que preco mais frete`() = runTest {
        val result = useCase(
            NewExemplar(
                variantId = "v1",
                condition = Condition.SEALED,
                acquisition = NewAcquisition(
                    date = IsoDate(LocalDate.of(2026, 11, 10)),
                    price = MoneyCents(5000),
                    shipping = MoneyCents(1000),
                    discount = MoneyCents(9000),
                ),
            ),
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(DomainError.Validation::class.java)
    }

    @Test
    fun `observacao em branco vira nulo em vez de string vazia`() = runTest {
        useCase(
            NewExemplar(variantId = "v1", condition = Condition.SEALED, notes = "   "),
        )

        assertThat(repository.received.single().notes).isNull()
    }

    @Test
    fun `exemplar sem variante e recusado`() = runTest {
        val result = useCase(NewExemplar(variantId = " ", condition = Condition.SEALED))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(DomainError.Validation::class.java)
    }
}

/** Repositório falso: guarda o que recebeu e numera os exemplares como o real faria. */
private class FakeExemplarRepository : ExemplarRepository {

    val received = mutableListOf<NewExemplar>()

    override fun observeOwned(): Flow<List<Exemplar>> = flowOf(emptyList())

    override fun observeByVariant(variantId: String): Flow<List<Exemplar>> = flowOf(emptyList())

    override suspend fun findById(id: String): Exemplar? = null

    override suspend fun add(draft: NewExemplar): DomainResult<AddExemplarOutcome> {
        received += draft
        val count = received.count { it.variantId == draft.variantId }
        val now = Instant.parse("2026-11-16T12:00:00Z")
        return Result.success(
            AddExemplarOutcome(
                exemplar = Exemplar(
                    id = "exemplar-$count",
                    variantId = draft.variantId,
                    internalCode = "HW-%06d".format(count),
                    condition = draft.condition,
                    status = ExemplarStatus.OWNED,
                    packagingCondition = draft.packagingCondition,
                    locationId = draft.locationId,
                    notes = draft.notes,
                    createdAt = now,
                    updatedAt = now,
                ),
                sameVariantCount = count,
            ),
        )
    }

    override suspend fun updateDetails(
        id: String,
        condition: Condition,
        packagingCondition: PackagingCondition?,
        notes: String?,
        expectedRowVersion: Long,
    ): DomainResult<Exemplar> = Result.failure(DomainError.NotFound)

    override suspend fun move(id: String, toLocationId: String?, note: String?): DomainResult<Unit> =
        Result.success(Unit)

    override suspend fun countByVariant(variantId: String): Int = received.count { it.variantId == variantId }

    override suspend fun archive(id: String): DomainResult<Unit> = Result.success(Unit)

    override suspend fun dependents(id: String): ExemplarDependents = ExemplarDependents()

    override suspend fun delete(id: String): DomainResult<Unit> = Result.success(Unit)
}
