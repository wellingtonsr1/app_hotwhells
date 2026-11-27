package br.com.diecastcollector.feature.catalog

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
 * Negociação aberta e concluída a partir da ficha (T520, FR-513, FR-516, SC-507).
 *
 * Massa: uma variante com **dois** exemplares, então **Negociar** não passa pelo aviso. O
 * teste abre a negociação pela ficha, conclui a venda, confere o histórico e volta: a lista
 * perde o exemplar vendido e Duplicados deixa de ter grupo, porque a variante fica com uma
 * unidade só. Nenhuma rota nova é usada — o caminho é o mesmo de Duplicados.
 */
@RunWith(AndroidJUnit4::class)
class ExemplarNegotiationFlowTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()
    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @Test
    fun abrirConcluirVendaPelaFichaAtualizaListaEDuplicados() {
        seed()

        composeRule.onNodeWithText("Catálogo").performClick()
        waitForText("Twin Mill")
        composeRule.onNodeWithText("Ver exemplares").performClick()
        waitForText("Exemplares")
        composeRule.onNodeWithText("HW-0001").performClick()
        waitForText("Negociação")

        // Sem aviso: a variante tem dois exemplares.
        composeRule.onNodeWithText("Negociar").performScrollTo().performClick()
        waitForText("Nova negociação")
        composeRule.onAllNodesWithText("Último exemplar da variante").assertCountEquals(0)

        composeRule.onNodeWithText("Abrir negociação").performScrollTo().performClick()
        waitForText("Negociação aberta.")
        waitForText("Negociação Reservada")

        // Concluir a venda: valor obrigatório, data opcional.
        composeRule.onNodeWithText("Concluir venda").performScrollTo().performClick()
        waitForText("Valor recebido (R$)")
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("45,00")
        composeRule.onNodeWithText("Concluir").performClick()
        waitForText("Venda concluída.")
        waitForText("Venda · Concluída")

        // Volta para a lista: o exemplar vendido saiu do acervo ativo.
        pressBack()
        waitForText("Negociação")
        pressBack()
        waitForText("Exemplares")
        composeRule.onNodeWithText("HW-0002").assertExists()
        composeRule.onAllNodesWithText("HW-0001").assertCountEquals(0)

        // Duplicados não tem mais grupo: sobrou uma unidade na variante.
        pressBack()
        waitForText("Twin Mill")
        composeRule.onNodeWithText("Duplicados").performClick()
        waitForText("Nenhum duplicado")
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
        } finally {
            database.close()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
