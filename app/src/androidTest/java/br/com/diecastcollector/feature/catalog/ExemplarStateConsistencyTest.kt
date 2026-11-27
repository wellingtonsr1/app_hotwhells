package br.com.diecastcollector.feature.catalog

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.MainActivity
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.entity.CarModelEntity
import br.com.diecastcollector.data.db.entity.DuplicateOfferEntity
import br.com.diecastcollector.data.db.entity.ExemplarEntity
import br.com.diecastcollector.data.db.entity.ManufacturerEntity
import br.com.diecastcollector.data.db.entity.NegotiationEntity
import br.com.diecastcollector.data.db.entity.VariantEntity
import br.com.diecastcollector.testing.freshInstallRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Mesmo estado nas três superfícies (T523, FR-505, FR-511, FR-520, SC-508).
 *
 * Cada combinação roda com instalação limpa e **uma** variante de dois exemplares, para que
 * a lista, a ficha e Duplicados possam ser comparados sem rolagem ambígua. A variante tem
 * sempre dois exemplares porque Duplicados só lista variantes com mais de um `OWNED`.
 *
 * Cobre: sem oferta, `TRADE`, `SALE`, `BOTH`, `UNAVAILABLE` e com negociação ativa — neste
 * último, **Em negociação** domina sem esconder a disponibilidade registrada (FR-511).
 */
@RunWith(AndroidJUnit4::class)
class ExemplarStateConsistencyTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()
    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @Test
    fun semOfertaApareceComoIndisponivelSoEmDuplicados() {
        seed(availability = null, priceCents = null)
        percorrer(
            listLabel = null,
            detailLines = emptyList(),
            duplicateLine = "Indisponível",
        )
    }

    @Test
    fun trocaApareceIgualNasTresSuperficies() {
        seed(availability = "TRADE", priceCents = 3_000L)
        percorrer(
            listLabel = "Troca",
            detailLines = listOf("Disponível: Troca"),
            duplicateLine = "Troca · R$ 30,00",
        )
    }

    @Test
    fun vendaApareceIgualNasTresSuperficies() {
        seed(availability = "SALE", priceCents = 4_500L)
        percorrer(
            listLabel = "Venda",
            detailLines = listOf("Disponível: Venda"),
            duplicateLine = "Venda · R$ 45,00",
        )
    }

    @Test
    fun trocaEVendaAparecemIguaisNasTresSuperficies() {
        seed(availability = "BOTH", priceCents = 5_000L)
        percorrer(
            listLabel = "Troca e venda",
            detailLines = listOf("Disponível: Troca e venda"),
            duplicateLine = "Troca e venda · R$ 50,00",
        )
    }

    @Test
    fun indisponivelApareceIgualNasTresSuperficies() {
        seed(availability = "UNAVAILABLE", priceCents = null)
        percorrer(
            listLabel = "Indisponível",
            detailLines = listOf("Disponível: Indisponível"),
            duplicateLine = "Indisponível",
        )
    }

    @Test
    fun negociacaoAtivaDominaSemEsconderADisponibilidade() {
        seed(availability = "SALE", priceCents = 6_000L, negotiating = true)
        percorrer(
            listLabel = "Venda · Em negociação",
            detailLines = listOf("Disponível: Venda", "Em negociação"),
            duplicateLine = "Venda · R$ 60,00 · Em negociação",
            negotiating = true,
        )
    }

    /** Lista → ficha → Duplicados, conferindo o rótulo em cada superfície. */
    private fun percorrer(
        listLabel: String?,
        detailLines: List<String>,
        duplicateLine: String,
        negotiating: Boolean = false,
    ) {
        composeRule.onNodeWithText("Catálogo").performClick()
        waitForText("Twin Mill")
        composeRule.onNodeWithText("Ver exemplares").performClick()
        waitForText("Exemplares")

        if (listLabel == null) {
            composeRule.onAllNodes(hasText("Disponível", substring = true)).assertCountEquals(0)
            composeRule.onAllNodesWithText("Em negociação").assertCountEquals(0)
        } else {
            waitForText(listLabel)
        }
        if (negotiating) {
            // Na lista o estado dominante aparece junto da disponibilidade no mesmo rótulo.
            composeRule.onNode(hasText("Em negociação", substring = true)).assertExists()
        }

        composeRule.onNodeWithText("HW-0001").performClick()
        waitForText("Negociação")
        detailLines.forEach { waitForText(it) }
        if (detailLines.isEmpty()) {
            composeRule.onAllNodes(hasText("Disponível", substring = true)).assertCountEquals(0)
        }

        pressBack()
        waitForText("Exemplares")
        pressBack()
        waitForText("Twin Mill")
        composeRule.onNodeWithText("Duplicados").performClick()
        waitForText("2 exemplares nesta variante")
        waitForText(duplicateLine)
    }

    private fun pressBack() {
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Uma variante do Twin Mill com dois exemplares e a oferta/negociação pedidas. */
    private fun seed(
        availability: String?,
        priceCents: Long?,
        negotiating: Boolean = false,
    ): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room
            .databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addCallback(AppDatabase.CALLBACK)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        try {
            val t = 0L
            database.manufacturerDao().insert(
                ManufacturerEntity("m1", "Mattel", null, null, t, t),
            )
            database.carModelDao().insert(
                CarModelEntity(
                    id = "model1",
                    manufacturerId = "m1",
                    name = "Twin Mill",
                    scale = "1:64",
                    brandId = null,
                    realVehicleId = null,
                    bodyType = null,
                    material = null,
                    countryOfManufacture = null,
                    notes = null,
                    archivedAt = null,
                    createdAt = t,
                    updatedAt = t,
                ),
            )
            database.variantDao().insert(
                VariantEntity(
                    id = "variant-a",
                    modelId = "model1",
                    fingerprint = "fp-twin-mill",
                    seriesId = null,
                    primaryColorId = null,
                    wheelsType = null,
                    baseType = null,
                    decoration = null,
                    releaseYear = 2024,
                    collectionNumber = null,
                    seriesNumber = null,
                    edition = null,
                    manufacturerCode = null,
                    packagingType = null,
                    archivedAt = null,
                    createdAt = t,
                    updatedAt = t,
                ),
            )
            listOf("e1", "e2").forEachIndexed { index, id ->
                database.exemplarDao().insert(
                    ExemplarEntity(
                        id = id,
                        variantId = "variant-a",
                        internalCode = "HW-%04d".format(index + 1),
                        condition = "LOOSE",
                        packagingCondition = null,
                        locationId = null,
                        status = "OWNED",
                        notes = null,
                        rowVersion = 0L,
                        createdAt = index.toLong(),
                        updatedAt = index.toLong(),
                    ),
                )
            }
            if (availability != null) {
                database.duplicateDao().insertOffer(
                    DuplicateOfferEntity(
                        id = "offer-e1",
                        exemplarId = "e1",
                        availability = availability,
                        condition = "LOOSE",
                        askingPriceCents = priceCents,
                        notes = null,
                        createdAt = t,
                        updatedAt = t,
                    ),
                )
            }
            if (negotiating) {
                database.negotiationDao().insert(
                    NegotiationEntity(
                        id = "negotiation-e1",
                        exemplarId = "e1",
                        type = "SALE",
                        counterparty = "Interessado",
                        channel = "Feira",
                        status = "OPEN",
                        startedAt = t,
                        closedAt = null,
                        amountCents = null,
                        currency = null,
                        notes = null,
                    ),
                )
            }
        } finally {
            database.close()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
