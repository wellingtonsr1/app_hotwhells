package br.com.hotwheelscollector.data.mapper

import br.com.hotwheelscollector.data.db.entity.BrandEntity
import br.com.hotwheelscollector.data.db.entity.CarModelEntity
import br.com.hotwheelscollector.data.db.entity.ColorEntity
import br.com.hotwheelscollector.data.db.entity.ManufacturerEntity
import br.com.hotwheelscollector.data.db.entity.RealVehicleEntity
import br.com.hotwheelscollector.data.db.entity.SeriesEntity
import br.com.hotwheelscollector.data.db.entity.VariantEntity
import br.com.hotwheelscollector.domain.model.Brand
import br.com.hotwheelscollector.domain.model.CarModel
import br.com.hotwheelscollector.domain.model.ColorItem
import br.com.hotwheelscollector.domain.model.Manufacturer
import br.com.hotwheelscollector.domain.model.RealVehicle
import br.com.hotwheelscollector.domain.model.Series
import br.com.hotwheelscollector.domain.model.Variant
import java.time.Instant

/**
 * Tradução entre linha do banco e modelo de domínio.
 *
 * O banco fala `Long` epoch millis e nome de enum; o domínio fala `Instant` e enum.
 * Nenhuma regra de negócio mora aqui — só conversão.
 */
internal fun ManufacturerEntity.toDomain(): Manufacturer = Manufacturer(
    id = id,
    name = name,
    country = country,
    archivedAt = archivedAt?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

internal fun Manufacturer.toEntity(): ManufacturerEntity = ManufacturerEntity(
    id = id,
    name = name.trim(),
    country = country,
    archivedAt = archivedAt?.toEpochMilli(),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

internal fun BrandEntity.toDomain(): Brand = Brand(
    id = id,
    manufacturerId = manufacturerId,
    name = name,
    archivedAt = archivedAt?.let(Instant::ofEpochMilli),
)

internal fun Brand.toEntity(): BrandEntity = BrandEntity(
    id = id,
    manufacturerId = manufacturerId,
    name = name.trim(),
    archivedAt = archivedAt?.toEpochMilli(),
)

internal fun RealVehicleEntity.toDomain(): RealVehicle = RealVehicle(
    id = id,
    make = make,
    model = model,
    year = year,
    country = country,
)

internal fun RealVehicle.toEntity(): RealVehicleEntity = RealVehicleEntity(
    id = id,
    make = make,
    model = model,
    year = year,
    country = country,
)

internal fun ColorEntity.toDomain(): ColorItem = ColorItem(id = id, name = name, hex = hex)

internal fun ColorItem.toEntity(): ColorEntity = ColorEntity(id = id, name = name.trim(), hex = hex)

internal fun CarModelEntity.toDomain(): CarModel = CarModel(
    id = id,
    manufacturerId = manufacturerId,
    name = name,
    scale = scale,
    brandId = brandId,
    realVehicleId = realVehicleId,
    bodyType = bodyType,
    material = material,
    countryOfManufacture = countryOfManufacture,
    notes = notes,
    archivedAt = archivedAt?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

internal fun CarModel.toEntity(): CarModelEntity = CarModelEntity(
    id = id,
    manufacturerId = manufacturerId,
    name = name.trim(),
    scale = scale,
    brandId = brandId,
    realVehicleId = realVehicleId,
    bodyType = bodyType,
    material = material,
    countryOfManufacture = countryOfManufacture,
    notes = notes,
    archivedAt = archivedAt?.toEpochMilli(),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

internal fun SeriesEntity.toDomain(): Series = Series(
    id = id,
    manufacturerId = manufacturerId,
    name = name,
    parentId = parentId,
    kind = kind,
    year = year,
    archivedAt = archivedAt?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

internal fun Series.toEntity(): SeriesEntity = SeriesEntity(
    id = id,
    manufacturerId = manufacturerId,
    name = name.trim(),
    parentId = parentId,
    kind = kind,
    year = year,
    archivedAt = archivedAt?.toEpochMilli(),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

internal fun VariantEntity.toDomain(secondaryColorIds: List<String> = emptyList()): Variant = Variant(
    id = id,
    modelId = modelId,
    fingerprint = fingerprint,
    seriesId = seriesId,
    primaryColorId = primaryColorId,
    secondaryColorIds = secondaryColorIds,
    wheelsType = wheelsType,
    baseType = baseType,
    decoration = decoration,
    releaseYear = releaseYear,
    collectionNumber = collectionNumber,
    seriesNumber = seriesNumber,
    edition = edition,
    manufacturerCode = manufacturerCode,
    packagingType = packagingType,
    archivedAt = archivedAt?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

internal fun Variant.toEntity(): VariantEntity = VariantEntity(
    id = id,
    modelId = modelId,
    fingerprint = fingerprint,
    seriesId = seriesId,
    primaryColorId = primaryColorId,
    wheelsType = wheelsType,
    baseType = baseType,
    decoration = decoration,
    releaseYear = releaseYear,
    collectionNumber = collectionNumber,
    seriesNumber = seriesNumber,
    edition = edition,
    manufacturerCode = manufacturerCode,
    packagingType = packagingType,
    archivedAt = archivedAt?.toEpochMilli(),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)
