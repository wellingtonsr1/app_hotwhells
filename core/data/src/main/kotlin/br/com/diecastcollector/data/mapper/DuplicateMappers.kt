package br.com.diecastcollector.data.mapper

import br.com.diecastcollector.data.db.entity.DuplicateOfferEntity
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.DuplicateAvailability
import br.com.diecastcollector.domain.model.DuplicateOffer
import br.com.diecastcollector.domain.model.MoneyCents
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
