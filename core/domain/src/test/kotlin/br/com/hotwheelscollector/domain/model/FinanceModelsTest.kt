package br.com.hotwheelscollector.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import org.junit.Test

/** Custo, diferença e custo médio (FR-075, FR-081, FR-085), sem banco. */
public class FinanceModelsTest {

    @Test
    public fun `custo soma frete e desconta o abatimento`() {
        val acquisition = acquisition(price = 5900, shipping = 1200, discount = 200)

        assertThat(acquisition.totalCost()).isEqualTo(MoneyCents(6900))
    }

    @Test
    public fun `custo sem preco nao vira zero`() {
        val acquisition = acquisition(price = null, shipping = 1200, discount = null)

        assertThat(acquisition.totalCost()).isNull()
    }

    @Test
    public fun `media divide pelos exemplares com valor e nao pelos possuidos`() {
        val summary = FinanceSummary(
            investedCents = 100_000,
            estimatedCents = 80_000,
            ownedExemplars = 6,
            exemplarsWithValue = 4,
            exemplarsWithoutValuation = 2,
        )

        assertThat(summary.averageCostCents).isEqualTo(25_000)
        assertThat(summary.exemplarsWithoutValue).isEqualTo(2)
        assertThat(summary.differenceCents).isEqualTo(-20_000)
    }

    @Test
    public fun `sem nenhum valor informado a media e nula`() {
        val summary = FinanceSummary(ownedExemplars = 3, exemplarsWithValue = 0)

        assertThat(summary.averageCostCents).isNull()
        assertThat(summary.exemplarsWithoutValue).isEqualTo(3)
    }

    private fun acquisition(
        price: Long?,
        shipping: Long?,
        discount: Long?,
    ): Acquisition = Acquisition(
        id = "a1",
        exemplarId = "e1",
        date = IsoDate(LocalDate.of(2026, 11, 2)),
        price = price?.let(::MoneyCents),
        shipping = shipping?.let(::MoneyCents),
        discount = discount?.let(::MoneyCents),
        createdAt = Instant.parse("2026-11-02T12:00:00Z"),
        updatedAt = Instant.parse("2026-11-02T12:00:00Z"),
    )
}
