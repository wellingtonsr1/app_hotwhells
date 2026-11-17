package br.com.hotwheelscollector.data.mapper

import br.com.hotwheelscollector.data.db.entity.AcquisitionEntity
import br.com.hotwheelscollector.data.db.entity.ExemplarEntity
import br.com.hotwheelscollector.data.db.entity.LocationEntity
import br.com.hotwheelscollector.data.db.entity.MovementEntity
import br.com.hotwheelscollector.data.db.entity.PhotoEntity
import br.com.hotwheelscollector.data.db.entity.ValuationEntity
import br.com.hotwheelscollector.domain.model.Acquisition
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.Exemplar
import br.com.hotwheelscollector.domain.model.ExemplarStatus
import br.com.hotwheelscollector.domain.model.IsoDate
import br.com.hotwheelscollector.domain.model.Location
import br.com.hotwheelscollector.domain.model.LocationType
import br.com.hotwheelscollector.domain.model.MoneyCents
import br.com.hotwheelscollector.domain.model.Movement
import br.com.hotwheelscollector.domain.model.PackagingCondition
import br.com.hotwheelscollector.domain.model.Photo
import br.com.hotwheelscollector.domain.model.PhotoAngle
import br.com.hotwheelscollector.domain.model.Valuation
import java.time.Instant
import java.time.LocalDate

internal fun ExemplarEntity.toDomain(): Exemplar = Exemplar(
    id = id,
    variantId = variantId,
    internalCode = internalCode,
    condition = Condition.valueOf(condition),
    status = ExemplarStatus.valueOf(status),
    packagingCondition = packagingCondition?.let(PackagingCondition::valueOf),
    locationId = locationId,
    notes = notes,
    rowVersion = rowVersion,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

internal fun Exemplar.toEntity(): ExemplarEntity = ExemplarEntity(
    id = id,
    variantId = variantId,
    internalCode = internalCode.trim(),
    condition = condition.name,
    packagingCondition = packagingCondition?.name,
    locationId = locationId,
    status = status.name,
    notes = notes,
    rowVersion = rowVersion,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

internal fun PhotoEntity.toDomain(): Photo = Photo(
    id = id,
    exemplarId = exemplarId,
    angle = PhotoAngle.valueOf(angle),
    path = path,
    checksum = checksum,
    isPrimary = isPrimary,
    sortOrder = sortOrder,
    width = width,
    height = height,
    bytes = bytes,
    capturedAt = capturedAt?.let(Instant::ofEpochMilli),
)

internal fun Photo.toEntity(): PhotoEntity = PhotoEntity(
    id = id,
    exemplarId = exemplarId,
    angle = angle.name,
    path = path,
    checksum = checksum,
    isPrimary = isPrimary,
    sortOrder = sortOrder,
    width = width,
    height = height,
    bytes = bytes,
    capturedAt = capturedAt?.toEpochMilli(),
)

internal fun LocationEntity.toDomain(): Location = Location(
    id = id,
    type = LocationType.valueOf(type),
    name = name,
    parentId = parentId,
    archivedAt = archivedAt?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

internal fun Location.toEntity(): LocationEntity = LocationEntity(
    id = id,
    type = type.name,
    name = name.trim(),
    parentId = parentId,
    archivedAt = archivedAt?.toEpochMilli(),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

internal fun MovementEntity.toDomain(): Movement = Movement(
    id = id,
    exemplarId = exemplarId,
    at = Instant.ofEpochMilli(at),
    fromLocationId = fromLocationId,
    toLocationId = toLocationId,
    note = note,
)

internal fun Movement.toEntity(): MovementEntity = MovementEntity(
    id = id,
    exemplarId = exemplarId,
    at = at.toEpochMilli(),
    fromLocationId = fromLocationId,
    toLocationId = toLocationId,
    note = note,
)

internal fun AcquisitionEntity.toDomain(): Acquisition = Acquisition(
    id = id,
    exemplarId = exemplarId,
    date = IsoDate(LocalDate.parse(date)),
    currency = currency,
    price = priceCents?.let(::MoneyCents),
    shipping = shippingCents?.let(::MoneyCents),
    discount = discountCents?.let(::MoneyCents),
    store = store,
    seller = seller,
    method = method,
    notes = notes,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

internal fun Acquisition.toEntity(): AcquisitionEntity = AcquisitionEntity(
    id = id,
    exemplarId = exemplarId,
    date = date.toIsoString(),
    priceCents = price?.cents,
    shippingCents = shipping?.cents,
    discountCents = discount?.cents,
    currency = currency,
    store = store,
    seller = seller,
    method = method,
    notes = notes,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

internal fun ValuationEntity.toDomain(): Valuation = Valuation(
    id = id,
    exemplarId = exemplarId,
    date = IsoDate(LocalDate.parse(date)),
    value = MoneyCents(valueCents),
    currency = currency,
    source = source,
    notes = notes,
    createdAt = Instant.ofEpochMilli(createdAt),
)

internal fun Valuation.toEntity(): ValuationEntity = ValuationEntity(
    id = id,
    exemplarId = exemplarId,
    valueCents = value.cents,
    currency = currency,
    date = date.toIsoString(),
    source = source,
    notes = notes,
    createdAt = createdAt.toEpochMilli(),
)
