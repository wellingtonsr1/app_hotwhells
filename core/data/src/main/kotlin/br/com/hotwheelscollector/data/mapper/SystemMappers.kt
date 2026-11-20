package br.com.hotwheelscollector.data.mapper

import br.com.hotwheelscollector.data.db.entity.AppSettingEntity
import br.com.hotwheelscollector.data.db.entity.CollectionEntity
import br.com.hotwheelscollector.data.db.entity.CollectionItemEntity
import br.com.hotwheelscollector.data.db.entity.HistoryEntryEntity
import br.com.hotwheelscollector.data.db.entity.NegotiationEntity
import br.com.hotwheelscollector.data.db.entity.NegotiationEventEntity
import br.com.hotwheelscollector.data.db.entity.RecognitionResultEntity
import br.com.hotwheelscollector.data.db.entity.WishlistItemEntity
import br.com.hotwheelscollector.domain.model.AppSetting
import br.com.hotwheelscollector.domain.model.Collection
import br.com.hotwheelscollector.domain.model.CollectionItem
import br.com.hotwheelscollector.domain.model.HistoryEntry
import br.com.hotwheelscollector.domain.model.MoneyCents
import br.com.hotwheelscollector.domain.model.Negotiation
import br.com.hotwheelscollector.domain.model.NegotiationEvent
import br.com.hotwheelscollector.domain.model.NegotiationStatus
import br.com.hotwheelscollector.domain.model.NegotiationType
import br.com.hotwheelscollector.domain.model.RecognitionResult
import br.com.hotwheelscollector.domain.model.RecognitionSource
import br.com.hotwheelscollector.domain.model.WishlistItem
import br.com.hotwheelscollector.domain.model.WishlistStatus
import java.time.Instant

internal fun CollectionEntity.toDomain(): Collection = Collection(
    id = id,
    name = name,
    description = description,
    parentId = parentId,
    manufacturerId = manufacturerId,
    archivedAt = archivedAt?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

internal fun CollectionItemEntity.toDomain(): CollectionItem = CollectionItem(
    id = id,
    collectionId = collectionId,
    variantId = variantId,
    modelId = modelId,
    expected = expected,
    nonCollectible = nonCollectible,
    notes = notes,
)

internal fun WishlistItemEntity.toDomain(): WishlistItem = WishlistItem(
    id = id,
    priority = priority,
    status = WishlistStatus.valueOf(status),
    modelId = modelId,
    variantId = variantId,
    freeText = freeText,
    maxPrice = maxPriceCents?.let(::MoneyCents),
    foundPrice = foundPriceCents?.let(::MoneyCents),
    store = store,
    referenceUrl = referenceUrl,
    notes = notes,
    convertedExemplarId = convertedExemplarId,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

internal fun WishlistItem.toEntity(): WishlistItemEntity = WishlistItemEntity(
    id = id,
    modelId = modelId,
    variantId = variantId,
    freeText = freeText,
    priority = priority,
    maxPriceCents = maxPrice?.cents,
    foundPriceCents = foundPrice?.cents,
    store = store,
    referenceUrl = referenceUrl,
    notes = notes,
    status = status.name,
    convertedExemplarId = convertedExemplarId,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

internal fun NegotiationEntity.toDomain(): Negotiation = Negotiation(
    id = id,
    exemplarId = exemplarId,
    type = NegotiationType.valueOf(type),
    status = NegotiationStatus.valueOf(status),
    startedAt = Instant.ofEpochMilli(startedAt),
    closedAt = closedAt?.let(Instant::ofEpochMilli),
    counterparty = counterparty,
    channel = channel,
    amount = amountCents?.let(::MoneyCents),
    currency = currency,
    notes = notes,
    receivedExemplarId = receivedExemplarId,
)

internal fun NegotiationEventEntity.toDomain(): NegotiationEvent = NegotiationEvent(
    id = id,
    negotiationId = negotiationId,
    at = Instant.ofEpochMilli(at),
    description = description,
    amount = amountCents?.let(::MoneyCents),
)

internal fun HistoryEntryEntity.toDomain(): HistoryEntry = HistoryEntry(
    id = id,
    entityType = entityType,
    entityId = entityId,
    field = field,
    at = Instant.ofEpochMilli(at),
    oldValue = oldValue,
    newValue = newValue,
    source = source,
)

internal fun RecognitionResultEntity.toDomain(): RecognitionResult = RecognitionResult(
    id = id,
    source = RecognitionSource.valueOf(source),
    provider = provider,
    payloadJson = payloadJson,
    createdAt = Instant.ofEpochMilli(createdAt),
    exemplarId = exemplarId,
    draftId = draftId,
    confidence = confidence,
    appliedFields = appliedFields,
)

internal fun AppSettingEntity.toDomain(): AppSetting = AppSetting(key = key, value = value)
