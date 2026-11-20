package br.com.hotwheelscollector.data.mapper

import br.com.hotwheelscollector.data.db.entity.DuplicateOfferEntity
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.DuplicateAvailability
import br.com.hotwheelscollector.domain.model.DuplicateOffer
import br.com.hotwheelscollector.domain.model.MoneyCents
import java.time.Instant

internal fun DuplicateOfferEntity.toDomain(): DuplicateOffer = DuplicateOffer(
    id = id,
    exemplarId = exemplarId,
    availability = DuplicateAvailability.valueOf(availability),
    condition = condition?.let(Condition::valueOf),
    askingPrice = askingPriceCents?.let(::MoneyCents),
    notes = notes,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)
