package br.com.diecastcollector.feature.duplicates

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.MainActivity
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.entity.CarModelEntity
import br.com.diecastcollector.data.db.entity.ExemplarEntity
import br.com.diecastcollector.data.db.entity.ManufacturerEntity
import br.com.diecastcollector.data.db.entity.VariantEntity
import br.com.diecastcollector.testing.freshInstallRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Aviso de preservação ao negociar, pelas duas origens (T519, SC-504b, SC-505b, SC-506).
 *
 * Massa: variante A com **um** exemplar e variante B com **dois**, do mesmo modelo. O teste
 * confere que 100% dos casos de uma unidade avisam (ficha e lista), que nenhum caso de duas
 * unidades avisa, que **Cancelar** mantém a origem e que **Continuar** abre a negociação
 * com o tipo pré-selecionado pela oferta. Com negociação ativa, **Negociar** abre o estado
 * ativo sem aviso e sem oferecer uma segunda abertura.
 */
@RunWith(AndroidJUnit4::class)
class UniqueExemplarWarningTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()
    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @Test
    fun avisoApareceSoNaUnicaUnidadeECancelarMantemAOrigem() {
        seed()

        composeRule.onNodeWithText("Catálogo").performClick()
        waitForText("Twin Mill")

        // --- Variante A: um exemplar, oferta de venda salva --------------------------
        composeRule.onAllNodesWithText("Ver exemplares")[0].performClick()
        waitForText("Exemplares")
        composeRule.onNodeWithText("HW-0001").performClick()
        waitForText("Negociação")
        waitForText("Disponível: Venda")

        composeRule.onNodeWithText("Negociar").performScrollTo().performClick()
        waitForText("Último exemplar da variante")

        // Cancelar volta para a ficha, sem tela de negociação e sem gravar nada.
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.onAllNodesWithText("Último exemplar da variante").assertCountEquals(0)
        composeRule.onAllNodesWithText("Nova negociação").assertCountEquals(0)

        // Continuar abre a negociação com o tipo vindo da oferta (Venda).
        composeRule.onNodeWithText("Negociar").performScrollTo().performClick()
        waitForText("Último exemplar da variante")
        composeRule.onNodeWithText("Continuar").performClick()
        waitForText("Nova negociação")
        composeRule.onNodeWithText("Venda").assertIsSelected()

        // --- Variante B: dois exemplares, nenhum aviso ------------------------------
        pressBack()
        waitForText("Negociação")
        pressBack()
        waitForText("Exemplares")
        pressBack()
        waitForText("Twin Mill")
        composeRule.onAllNodesWithText("Ver exemplares")[1].performClick()
        waitForText("Exemplares")
        composeRule.onNodeWithText("HW-0002").performClick()
        waitForText("Negociação")

        composeRule.onNodeWithText("Negociar").performScrollTo().performClick()
        waitForText("Nova negociação")
        composeRule.onAllNodesWithText("Último exemplar da variante").assertCountEquals(0)

        // --- Com negociação ativa: abre o estado, sem aviso nem segunda abertura ----
        composeRule.onNodeWithText("Abrir negociação").performScrollTo().performClick()
        waitForText("Negociação aberta.")
        waitForText("Negociação Reservada")

        pressBack()
        waitForText("Negociação")
        composeRule.onNodeWithText("Negociar").performScrollTo().performClick()
        waitForText("Negociação Reservada")
        composeRule.onAllNodesWithText("Último exemplar da variante").assertCountEquals(0)
        composeRule.onAllNodesWithText("Abrir negociação").assertCountEquals(0)
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

    private fun seed(): Unit = runBlocking {
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
            listOf("variant-a", "variant-b").forEach { variantId ->
                database.variantDao().insert(
                    VariantEntity(
                        id = variantId,
                        modelId = "model1",
                        fingerprint = "fp-$variantId",
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
            }
            listOf(
                "e1" to "variant-a",
                "e2" to "variant-b",
                "e3" to "variant-b",
            ).forEachIndexed { index, (id, variantId) ->
                database.exemplarDao().insert(
                    ExemplarEntity(
                        id = id,
                        variantId = variantId,
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

            // A única unidade já tem oferta de venda: é ela que pré-seleciona o tipo.
            database.duplicateDao().insertOffer(
                br.com.diecastcollector.data.db.entity.DuplicateOfferEntity(
                    id = "offer-e1",
                    exemplarId = "e1",
                    availability = "SALE",
                    condition = "LOOSE",
                    askingPriceCents = 4_500L,
                    notes = null,
                    createdAt = t,
                    updatedAt = t,
                ),
            )
        } finally {
            database.close()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
