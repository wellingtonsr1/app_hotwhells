package br.com.hotwheelscollector.domain.usecase

import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.error.DomainResult
import br.com.hotwheelscollector.domain.model.IsoDate
import br.com.hotwheelscollector.domain.model.MoneyCents
import br.com.hotwheelscollector.domain.repository.AddExemplarOutcome
import br.com.hotwheelscollector.domain.repository.ExemplarRepository
import br.com.hotwheelscollector.domain.repository.NewAcquisition
import br.com.hotwheelscollector.domain.repository.NewExemplar
import java.time.Clock

/**
 * Registra um exemplar de uma variante já cadastrada (US1, FR-036–FR-039).
 *
 * Regras que moram aqui, e não na tela:
 * - a data da compra não pode estar no futuro (FR-074);
 * - valor informado precisa ser coerente: desconto maior que preço + frete é recusado
 *   em vez de virar custo zero silencioso;
 * - observação em branco vira `null`, para não gravar sentinela (Art. I.9);
 * - a contagem devolvida é o que permite avisar "2º exemplar desta variante" sem
 *   bloquear o cadastro (FR-039).
 */
public class AddExemplarUseCase(
    private val exemplarRepository: ExemplarRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    public suspend operator fun invoke(draft: NewExemplar): DomainResult<AddExemplarOutcome> {
        validate(draft).let { if (it != null) return Result.failure(it) }

        val acquisition = draft.acquisition?.let { it.copy(date = it.date) }
        return exemplarRepository.add(
            draft.copy(
                notes = draft.notes?.trim()?.takeIf { it.isNotEmpty() },
                acquisition = acquisition?.copy(
                    store = acquisition.store?.trim()?.takeIf { it.isNotEmpty() },
                    seller = acquisition.seller?.trim()?.takeIf { it.isNotEmpty() },
                ),
            ),
        )
    }

    private fun validate(draft: NewExemplar): Throwable? {
        if (draft.variantId.isBlank()) {
            return DomainError.Validation("Exemplar precisa estar vinculado a uma variante.")
        }

        val acquisition = draft.acquisition ?: return null
        if (acquisition.date.isAfter(IsoDate.today(clock))) {
            return DomainError.Validation("A data da compra não pode estar no futuro.")
        }

        val gross = listOfNotNull(acquisition.price, acquisition.shipping)
            .fold(MoneyCents.ZERO) { total, value -> total + value }
        val discount = acquisition.discount ?: MoneyCents.ZERO
        if (discount > gross) {
            return DomainError.Validation("O desconto não pode ser maior que preço mais frete.")
        }
        return null
    }
}

