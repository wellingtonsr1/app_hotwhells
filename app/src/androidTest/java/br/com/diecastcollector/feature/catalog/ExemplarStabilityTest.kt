package br.com.diecastcollector.feature.catalog

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
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
 * Estabilidade a recriação da janela (T525, FR-505, FR-515, SC-505).
 *
 * Massa A: uma variante com **um** exemplar, o caso que dispara o aviso. Com a folha
 * preenchida e o aviso aberto, a activity é recriada como numa rotação: nada pode ter sido
 * gravado, o aviso não volta e a folha reabre no padrão (**Venda**, sem valor). O mesmo com
 * o aviso de **Negociar** aberto: nenhuma negociação criada e a ficha continua no estado
 * do repositório.
 *
 * A morte real de processo (`am force-stop`) fica como evidência manual no `quickstart.md`
 * §13; este teste cobre a recriação de configuração.
 */
@RunWith(AndroidJUnit4::class)
class ExemplarStabilityTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()
    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @Test
    fun recriarComOFolhaEAvisoAbertosNaoGravaNemReabreOAviso() {
        seed()

        composeRule.onNodeWithText("Catálogo").performClick()
        waitForText("Twin Mill")
        composeRule.onNodeWithText("Ver exemplares").performClick()
        waitForText("Exemplares")

        // Folha preenchida e aviso de disponibilidade aberto.
        composeRule.onNodeWithText("Disponibilidade").performClick()
        waitForText("Disponibilidade do exemplar")
        composeRule.onNodeWithText("Troca").performClick()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("40,00")
        composeRule.onNodeWithText("Salvar").performScrollTo().performClick()
        waitForText("Último exemplar da variante")

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        // Aviso não volta e nada foi gravado; a folha reabre no padrão: Venda
        // pré-selecionada e valor vazio (a intenção pendente não foi restaurada).
        composeRule.onAllNodesWithText("Último exemplar da variante").assertCountEquals(0)
        composeRule.onAllNodesWithText("Disponibilidade atualizada.").assertCountEquals(0)
        waitForText("Disponibilidade do exemplar")
        composeRule.onNodeWithText("Venda").assertIsSelected()
        composeRule.onAllNodesWithText("40,00").assertCountEquals(0)

        // Fecha a folha: a lista continua sem rótulo de estado (oferta não gravada).
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Troca").assertCountEquals(0)
        composeRule.onNodeWithText("HW-0001").performClick()
        waitForText("Negociação")
        composeRule.onNodeWithText("Negociar").performScrollTo().performClick()
        waitForText("Último exemplar da variante")

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        // Nenhuma negociação criada, a ficha continua no repositório (sem «Em negociação")
        // e o aviso não reaparece.
        composeRule.onAllNodesWithText("Último exemplar da variante").assertCountEquals(0)
        composeRule.onAllNodesWithText("Em negociação").assertCountEquals(0)
        waitForText("Negociação")
        composeRule.onNodeWithText("Negociar").performScrollTo().assertExists()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Massa A: um modelo, uma variante e um único exemplar. */
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
