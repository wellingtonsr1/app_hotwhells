package br.com.hotwheelscollector.domain.repository

import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.Acquisition
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.Exemplar
import br.com.hotwheelscollector.domain.model.IsoDate
import br.com.hotwheelscollector.domain.model.Location
import br.com.hotwheelscollector.domain.model.LocationType
import br.com.hotwheelscollector.domain.model.MoneyCents
import br.com.hotwheelscollector.domain.model.Movement
import br.com.hotwheelscollector.domain.model.PackagingCondition
import br.com.hotwheelscollector.domain.model.Photo
import br.com.hotwheelscollector.domain.model.PhotoAngle
import br.com.hotwheelscollector.domain.model.Valuation
import kotlinx.coroutines.flow.Flow

/**
 * Exemplar físico (Art. I.1, I.12).
 * `add` grava exemplar + aquisição + movimentação + auditoria numa transação só;
 * `move` grava localização e movimentação na mesma transação (FR-091).
 */
public interface ExemplarRepository {
    public fun observeOwned(): Flow<List<Exemplar>>
    public fun observeByVariant(variantId: String): Flow<List<Exemplar>>
    public suspend fun findById(id: String): Exemplar?
    public suspend fun add(draft: NewExemplar): DomainResult<AddExemplarOutcome>
    public suspend fun updateCondition(
        id: String,
        condition: Condition,
        expectedRowVersion: Long,
    ): DomainResult<Exemplar>
    public suspend fun move(id: String, toLocationId: String?, note: String? = null): DomainResult<Unit>
    public suspend fun countByVariant(variantId: String): Int
}

/**
 * Rascunho de exemplar. O código interno não vem da tela: é gerado no repositório,
 * que é o único lugar com acesso ao banco para garantir unicidade (FR-038).
 */
public data class NewExemplar(
    val variantId: String,
    val condition: Condition,
    val packagingCondition: PackagingCondition? = null,
    val locationId: String? = null,
    val notes: String? = null,
    val acquisition: NewAcquisition? = null,
)

public data class NewAcquisition(
    val date: IsoDate,
    val price: MoneyCents? = null,
    val shipping: MoneyCents? = null,
    val discount: MoneyCents? = null,
    val store: String? = null,
    val seller: String? = null,
    val method: String? = null,
    val notes: String? = null,
)

/**
 * Resultado do cadastro: o exemplar criado e quantos a variante já tem.
 * A contagem é o que permite avisar "2º exemplar desta variante" sem bloquear (FR-039).
 */
public data class AddExemplarOutcome(
    val exemplar: Exemplar,
    val sameVariantCount: Int,
)

public interface PhotoRepository {
    public fun observeByExemplar(exemplarId: String): Flow<List<Photo>>
    public suspend fun add(exemplarId: String, angle: PhotoAngle, source: ByteArray): DomainResult<Photo>
    public suspend fun setPrimary(exemplarId: String, photoId: String): DomainResult<Unit>
    public suspend fun remove(photoId: String): DomainResult<Unit>
}

/** Localizações hierárquicas; arquivar com exemplares é bloqueado (FR-093). */
public interface LocationRepository {
    public fun observeActive(): Flow<List<Location>>
    public suspend fun children(parentId: String): List<Location>
    public suspend fun add(type: LocationType, name: String, parentId: String? = null): DomainResult<Location>
    public suspend fun rename(id: String, name: String): DomainResult<Unit>
    public suspend fun archive(id: String): DomainResult<Unit>
    public suspend fun movements(exemplarId: String): Flow<List<Movement>>
}

public interface AcquisitionRepository {
    public suspend fun findByExemplar(exemplarId: String): Acquisition?
    public suspend fun save(acquisition: Acquisition): DomainResult<Acquisition>
    public fun observeTotalInvested(): Flow<MoneyCents>
}

public interface ValuationRepository {
    public fun observeByExemplar(exemplarId: String): Flow<List<Valuation>>
    public suspend fun add(valuation: Valuation): DomainResult<Valuation>
    public fun observeCurrentEstimated(): Flow<MoneyCents>
}
