package br.com.diecastcollector.feature.duplicates

import androidx.compose.ui.test.assertCountEquals
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
 * Duplicados e negociação pelo app real (US7, FR-065–FR-073).
 *
 * Semeia três exemplares da variante A e um da variante B do mesmo modelo: só os três da
 * A são duplicatas. Depois percorre o caminho do toque — disponibilidade, reserva e
 * cancelamento — e confere que o histórico continua na tela.
 */
@RunWith(AndroidJUnit4::class)
class DuplicatesFlowTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()
    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @Test
    fun duplicadosPorVarianteAceitamDisponibilidadeENegociacao() {
        seed()

        composeRule.onNodeWithText("Catálogo").performClick()
        waitForText("Coleção")
        composeRule.onNodeWithText("Duplicados").performClick()
        waitForText("Mattel · Twin Mill")

        // Só a variante com três exemplares entra; a outra variante do mesmo modelo não.
        composeRule.onNodeWithText("3 exemplares nesta variante").assertExists()
        composeRule.onAllNodesWithText("HW-0004").assertCountEquals(0)

        // Disponibilidade: venda por R$ 45,00 no primeiro exemplar do grupo.
        composeRule.onAllNodesWithText("Disponibilidade")[0].performClick()
        waitForText("Disponibilidade do exemplar")
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("45,00")
        composeRule.onNodeWithText("Salvar").performScrollTo().performClick()
        waitForText("Disponibilidade atualizada.")
        waitForText("Venda · R$ 45,00")

        // Reserva: abre a negociação com o exemplar reservado.
        composeRule.onAllNodesWithText("Negociar")[0].performClick()
        waitForText("Nova negociação")
        composeRule.onNodeWithText("Abrir negociação").performScrollTo().performClick()
        waitForText("Negociação aberta.")
        waitForText("Negociação Reservada")

        // Cancelamento: o exemplar volta a ficar livre e o evento continua no histórico.
        composeRule.onNodeWithText("Cancelar negociação").performScrollTo().performClick()
        waitForText("Cancelar negociação?")
        composeRule.onNodeWithText("Confirmar cancelamento").performClick()
        waitForText("Negociação cancelada.")
        waitForText("Venda · Cancelada")
    }

    /** Semeia catálogo e exemplares numa segunda conexão ao banco do app. */
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
                "e2" to "variant-a",
                "e3" to "variant-a",
                "e4" to "variant-b",
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
        } finally {
            database.close()
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
