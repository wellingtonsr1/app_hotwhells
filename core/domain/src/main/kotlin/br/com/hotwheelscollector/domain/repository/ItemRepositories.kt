package br.com.hotwheelscollector.domain.repository

import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.Acquisition
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.Exemplar
import br.com.hotwheelscollector.domain.model.InventoryItem
import br.com.hotwheelscollector.domain.model.IsoDate
import br.com.hotwheelscollector.domain.model.Location
import br.com.hotwheelscollector.domain.model.LocationArchivePolicy
import br.com.hotwheelscollector.domain.model.LocationNode
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

    /**
     * Edição dos campos do exemplar (FR-036). A versão esperada impede que uma tela
     * sobrescreva a escrita de outra (Art. I.12).
     */
    public suspend fun updateDetails(
        id: String,
        condition: Condition,
        packagingCondition: PackagingCondition?,
        notes: String?,
        expectedRowVersion: Long,
    ): DomainResult<Exemplar>
    public suspend fun move(id: String, toLocationId: String?, note: String? = null): DomainResult<Unit>

    /**
     * Corrige a variante de um exemplar sem tocar em aquisição, avaliações, fotos ou
     * movimentações (FR-042). A versão lida é checada na escrita (Art. I.12).
     */
    public suspend fun reassignVariant(id: String, toVariantId: String): DomainResult<Unit>
    public suspend fun countByVariant(variantId: String): Int

    /** Arquiva o exemplar: sai do acervo e das contagens, mas nada é apagado (Art. I.7). */
    public suspend fun archive(id: String): DomainResult<Unit>

    /** Vínculos que impedem a exclusão física; a tela usa isso para explicar o bloqueio. */
    public suspend fun dependents(id: String): ExemplarDependents

    /**
     * Exclusão física, permitida **somente** quando não há vínculos (FR-028, Art. I.6).
     * Com qualquer dependente, devolve [br.com.hotwheelscollector.domain.error.DomainError.Blocked]
     * e a alternativa é [archive].
     */
    public suspend fun delete(id: String): DomainResult<Unit>
}

/**
 * Dependentes do exemplar, contados nas tabelas com FK `RESTRICT` (Art. I.6).
 *
 * A trilha de auditoria não entra na conta: ela é appende-only e continua válida mesmo
 * depois de uma exclusão permitida, porque registra o que aconteceu, não o que existe.
 */
public data class ExemplarDependents(
    val photos: Int = 0,
    val acquisitions: Int = 0,
    val valuations: Int = 0,
    val movements: Int = 0,
    val negotiations: Int = 0,
) {
    public val total: Int get() = photos + acquisitions + valuations + movements + negotiations
    public val hasAny: Boolean get() = total > 0
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

/**
 * Localizações hierárquicas (FR-088–FR-095).
 *
 * Arquivar nunca cascateia: com exemplares dentro, a exclusão exige uma
 * [LocationArchivePolicy] explícita, e com sublocalização ativa é recusada.
 */
public interface LocationRepository {
    public fun observeActive(): Flow<List<Location>>

    /** Árvore montada na leitura, com a contagem de exemplares por localização. */
    public fun observeTree(): Flow<List<LocationNode>>

    public suspend fun children(parentId: String): List<Location>
    public suspend fun findById(id: String): Location?
    public suspend fun add(type: LocationType, name: String, parentId: String? = null): DomainResult<Location>
    public suspend fun rename(id: String, name: String): DomainResult<Unit>

    /**
     * Arquiva a localização devolvendo quantos exemplares foram tratados (FR-093).
     * Falha com `Blocked` quando há sublocalização ativa ou exemplares sem política.
     */
    public suspend fun archive(
        id: String,
        policy: LocationArchivePolicy = LocationArchivePolicy.None,
    ): DomainResult<Int>

    public suspend fun movements(exemplarId: String): Flow<List<Movement>>

    /** Inventário da localização **e das sublocalizações** (FR-094–FR-095). */
    public fun observeContents(locationId: String): Flow<List<InventoryItem>>
}

/**
 * Aquisição do exemplar (FR-074–FR-076). `save` é upsert por exemplar: uma aquisição por
 * unidade física, e editar não cria uma segunda linha.
 *
 * O total investido sai em `Long` cru: é soma do acervo e pode passar do limite de um item
 * (FR-076), diferente de [MoneyCents], que representa um valor.
 */
public interface AcquisitionRepository {
    public suspend fun findByExemplar(exemplarId: String): Acquisition?
    public suspend fun save(acquisition: Acquisition): DomainResult<Acquisition>
    public fun observeTotalInvested(): Flow<Long>
}

/** Avaliações em ordem decrescente: a primeira é o valor atual (FR-083–FR-084). */
public interface ValuationRepository {
    public fun observeByExemplar(exemplarId: String): Flow<List<Valuation>>
    public suspend fun add(valuation: Valuation): DomainResult<Valuation>

    /** Soma das avaliações mais recentes dos exemplares possuídos, em centavos (FR-085). */
    public fun observeCurrentEstimated(): Flow<Long>
}
