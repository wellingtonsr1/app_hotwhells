package br.com.diecastcollector.feature.catalog

import br.com.diecastcollector.domain.error.DomainError
import br.com.diecastcollector.domain.error.DomainResult
import br.com.diecastcollector.domain.model.Acquisition
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.DuplicateAvailability
import br.com.diecastcollector.domain.model.DuplicateGroup
import br.com.diecastcollector.domain.model.DuplicateOffer
import br.com.diecastcollector.domain.model.Exemplar
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.domain.model.IsoDate
import br.com.diecastcollector.domain.model.Location
import br.com.diecastcollector.domain.model.LocationArchivePolicy
import br.com.diecastcollector.domain.model.LocationNode
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.model.Movement
import br.com.diecastcollector.domain.model.Negotiation
import br.com.diecastcollector.domain.model.NegotiationEvent
import br.com.diecastcollector.domain.model.NegotiationStatus
import br.com.diecastcollector.domain.model.NegotiationType
import br.com.diecastcollector.domain.model.PackagingCondition
import br.com.diecastcollector.domain.model.Photo
import br.com.diecastcollector.domain.model.PhotoAngle
import br.com.diecastcollector.domain.model.Valuation
import br.com.diecastcollector.domain.repository.AddExemplarOutcome
import br.com.diecastcollector.domain.repository.DuplicateRepository
import br.com.diecastcollector.domain.repository.ExemplarDependents
import br.com.diecastcollector.domain.repository.ExemplarRepository
import br.com.diecastcollector.domain.repository.NewExemplar
import br.com.diecastcollector.domain.repository.NewNegotiation
import br.com.diecastcollector.domain.repository.NegotiationRepository
import br.com.diecastcollector.domain.repository.AcquisitionRepository
import br.com.diecastcollector.domain.repository.LocationRepository
import br.com.diecastcollector.domain.repository.PhotoRepository
import br.com.diecastcollector.domain.repository.ValuationRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Fakes compartilhados pelos testes de disponibilidade (T511, T513).
 *
 * O que importa é a **decisão** dos ViewModels: as ofertas ficam num `MutableStateFlow`, do
 * mesmo jeito que o `Flow<Map>` do repositório real entrega, para que gravar uma oferta
 * atualize o estado como atualizaria em produção.
 */

internal val NOW: Instant = Instant.parse("2026-11-27T12:00:00Z")

internal fun ownedExemplar(id: String, variantId: String = "v1"): Exemplar = Exemplar(
    id = id,
    variantId = variantId,
    internalCode = "HW-$id",
    condition = Condition.SEALED,
    status = ExemplarStatus.OWNED,
    createdAt = NOW,
    updatedAt = NOW,
)

internal fun negotiation(id: String, exemplarId: String): Negotiation = Negotiation(
    id = id,
    exemplarId = exemplarId,
    type = NegotiationType.SALE,
    status = NegotiationStatus.OPEN,
    startedAt = NOW,
)

internal class FakeExemplarCatalog : ExemplarRepository {

    val exemplars = MutableStateFlow<List<Exemplar>>(emptyList())
    var findResult: Exemplar? = null
    var failure: DomainError? = null

    override fun observeOwned(): Flow<List<Exemplar>> = exemplars

    override fun observeByVariant(variantId: String): Flow<List<Exemplar>> =
        exemplars.map { list -> list.filter { it.variantId == variantId } }

    override suspend fun findById(id: String): Exemplar? =
        findResult ?: exemplars.value.firstOrNull { it.id == id }

    override suspend fun add(draft: NewExemplar): DomainResult<AddExemplarOutcome> =
        Result.failure(DomainError.NotFound)

    override suspend fun updateDetails(
        id: String,
        condition: Condition,
        packagingCondition: PackagingCondition?,
        notes: String?,
        expectedRowVersion: Long,
    ): DomainResult<Exemplar> = Result.failure(DomainError.NotFound)

    override suspend fun move(id: String, toLocationId: String?, note: String?): DomainResult<Unit> =
        Result.failure(DomainError.NotFound)

    override suspend fun reassignVariant(id: String, toVariantId: String): DomainResult<Unit> =
        Result.failure(DomainError.NotFound)

    override suspend fun countByVariant(variantId: String): Int =
        exemplars.value.count { it.variantId == variantId && it.status == ExemplarStatus.OWNED }

    override suspend fun archive(id: String): DomainResult<Unit> = Result.failure(DomainError.NotFound)

    override suspend fun dependents(id: String): ExemplarDependents = ExemplarDependents()

    override suspend fun delete(id: String): DomainResult<Unit> = Result.failure(DomainError.NotFound)
}

internal class FakeDuplicates : DuplicateRepository {

    val offers = MutableStateFlow<Map<String, DuplicateOffer>>(emptyMap())
    val saved = mutableListOf<Pair<String, DuplicateAvailability>>()
    var failure: DomainError? = null

    override fun observeGroups(): Flow<List<DuplicateGroup>> = flowOf(emptyList())

    override suspend fun offerFor(exemplarId: String): DuplicateOffer? = offers.value[exemplarId]

    override fun observeOffer(exemplarId: String): Flow<DuplicateOffer?> =
        offers.map { it[exemplarId] }

    override fun observeOffersByVariant(variantId: String): Flow<Map<String, DuplicateOffer>> = offers

    override suspend fun setOffer(
        exemplarId: String,
        availability: DuplicateAvailability,
        condition: Condition?,
        askingPrice: MoneyCents?,
        notes: String?,
    ): DomainResult<DuplicateOffer> {
        failure?.let { return Result.failure(it) }
        saved += exemplarId to availability
        val existing = offers.value[exemplarId]
        val offer = DuplicateOffer(
            id = existing?.id ?: "offer-$exemplarId",
            exemplarId = exemplarId,
            availability = availability,
            condition = condition,
            askingPrice = askingPrice,
            notes = notes,
            createdAt = existing?.createdAt ?: NOW,
            updatedAt = NOW,
        )
        offers.value = offers.value + (exemplarId to offer)
        return Result.success(offer)
    }
}

internal class FakeNegotiations : NegotiationRepository {

    val active = MutableStateFlow<Map<String, Negotiation>>(emptyMap())

    override fun observeByExemplar(exemplarId: String): Flow<List<Negotiation>> =
        active.map { map -> listOfNotNull(map[exemplarId]) }

    override fun observeActiveByVariant(variantId: String): Flow<Map<String, Negotiation>> = active

    override fun observeEventsForExemplar(exemplarId: String): Flow<List<NegotiationEvent>> =
        flowOf(emptyList())

    override suspend fun open(draft: NewNegotiation): DomainResult<Negotiation> =
        Result.failure(DomainError.NotFound)

    override suspend fun closeSale(
        negotiationId: String,
        amount: MoneyCents,
        date: IsoDate?,
        counterparty: String?,
    ): DomainResult<Unit> = Result.failure(DomainError.NotFound)

    override suspend fun closeTrade(
        negotiationId: String,
        receivedExemplarId: String?,
        note: String?,
    ): DomainResult<Unit> = Result.failure(DomainError.NotFound)

    override suspend fun cancel(negotiationId: String): DomainResult<Unit> =
        Result.failure(DomainError.NotFound)
}

internal class FakeLocations : LocationRepository {
    override fun observeActive(): Flow<List<Location>> = flowOf(emptyList())

    override fun observeTree(): Flow<List<LocationNode>> = flowOf(emptyList())

    override suspend fun children(parentId: String): List<Location> = emptyList()

    override suspend fun findById(id: String): Location? = null

    override suspend fun add(
        type: br.com.diecastcollector.domain.model.LocationType,
        name: String,
        parentId: String?,
    ): DomainResult<Location> = Result.failure(DomainError.NotFound)

    override suspend fun rename(id: String, name: String): DomainResult<Unit> =
        Result.failure(DomainError.NotFound)

    override suspend fun archive(id: String, policy: LocationArchivePolicy): DomainResult<Int> =
        Result.failure(DomainError.NotFound)

    override suspend fun movements(exemplarId: String): Flow<List<Movement>> = flowOf(emptyList())

    override fun observeContents(
        locationId: String,
    ): Flow<List<br.com.diecastcollector.domain.model.InventoryItem>> = flowOf(emptyList())
}

internal class FakePhotos : PhotoRepository {
    override fun observeByExemplar(exemplarId: String): Flow<List<Photo>> = flowOf(emptyList())

    override fun observePrimaryPathsByVariant(variantId: String): Flow<Map<String, String>> =
        flowOf(emptyMap())

    override suspend fun add(
        exemplarId: String,
        angle: PhotoAngle,
        source: ByteArray,
    ): DomainResult<Photo> = Result.failure(DomainError.NotFound)

    override suspend fun setPrimary(exemplarId: String, photoId: String): DomainResult<Unit> =
        Result.failure(DomainError.NotFound)

    override suspend fun remove(photoId: String): DomainResult<Unit> =
        Result.failure(DomainError.NotFound)

    override suspend fun saveEdit(
        photoId: String,
        edited: ByteArray,
        editParams: String,
    ): DomainResult<Photo> = Result.failure(DomainError.NotFound)

    override suspend fun restoreOriginal(photoId: String): DomainResult<Photo> =
        Result.failure(DomainError.NotFound)
}

internal class FakeAcquisitions : AcquisitionRepository {
    override suspend fun findByExemplar(exemplarId: String): Acquisition? = null

    override suspend fun save(acquisition: Acquisition): DomainResult<Acquisition> =
        Result.failure(DomainError.NotFound)

    override fun observeTotalInvested(): Flow<Long> = flowOf(0L)
}

internal class FakeValuations : ValuationRepository {
    override fun observeByExemplar(exemplarId: String): Flow<List<Valuation>> = flowOf(emptyList())

    override suspend fun add(valuation: Valuation): DomainResult<Valuation> =
        Result.failure(DomainError.NotFound)

    override fun observeCurrentEstimated(): Flow<Long> = flowOf(0L)
}
