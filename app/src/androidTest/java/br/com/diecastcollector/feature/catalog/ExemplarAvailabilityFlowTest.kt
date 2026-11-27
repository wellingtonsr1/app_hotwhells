package br.com.diecastcollector.feature.catalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
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
 * Disponibilizar a única unidade de uma variante, pelas duas superfícies (T515, SC-501/502).
 *
 * Massa: um modelo, uma variante e **um** exemplar — o caso que antes não tinha por onde
 * ser vendido nem trocado. O teste marca **Venda** pela lista e **Troca** pela ficha, com o
 * aviso de preservação nos dois caminhos, confere o estado na outra superfície, reabre a
 * folha para ver os campos salvos e termina provando que Duplicados continua vazio.
 */
@RunWith(AndroidJUnit4::class)
class ExemplarAvailabilityFlowTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()
    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @Test
    fun disponibilizarUnicaUnidadePelaListaEPelaFicha() {
        seed()

        composeRule.onNodeWithText("Catálogo").performClick()
        waitForText("Twin Mill")
        composeRule.onNodeWithText("Ver exemplares").performClick()
        waitForText("Exemplares")

        // 1. Pela lista: venda de R$ 45,00 com o aviso de última unidade.
        composeRule.onNodeWithText("Disponibilidade").performClick()
        waitForText("Disponibilidade do exemplar")
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("45,00")
        composeRule.onNodeWithText("Salvar").performScrollTo().performClick()
        waitForText("Último exemplar da variante")
        composeRule.onNodeWithText("Continuar").performClick()
        waitForText("Disponibilidade atualizada.")
        waitForText("Venda")

        // 2. A ficha reflete o que a lista gravou.
        composeRule.onNodeWithText("HW-0001").performClick()
        waitForText("Negociação")
        waitForText("Disponível: Venda")

        // 3. Pela ficha: troca, também com aviso; o valor já salvo continua no campo.
        composeRule.onNodeWithText("Disponibilidade").performScrollTo().performClick()
        waitForText("Disponibilidade do exemplar")
        waitForText("45,00")
        composeRule.onNodeWithText("Troca").performClick()
        composeRule.onNodeWithText("Salvar").performScrollTo().performClick()
        waitForText("Último exemplar da variante")
        composeRule.onNodeWithText("Continuar").performClick()
        waitForText("Disponibilidade atualizada.")
        waitForText("Disponível: Troca")

        // 4. De volta à lista: mesmo estado, e Duplicados não ganhou um grupo.
        pressBack()
        waitForText("Exemplares")
        waitForText("Troca")
        pressBack()
        waitForText("Twin Mill")
        composeRule.onNodeWithText("Duplicados").performClick()
        waitForText("Nenhum duplicado")
        composeRule.onNodeWithText("Nenhum duplicado").assertIsDisplayed()
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

    /** Semeia um modelo com uma variante de um único exemplar. */
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
            database.exemplarDao().insert(
                ExemplarEntity(
                    id = "e1",
                    variantId = "variant-a",
                    internalCode = "HW-0001",
                    condition = "LOOSE",
                    packagingCondition = null,
                    locationId = null,
                    status = "OWNED",
                    notes = null,
                    rowVersion = 0L,
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
