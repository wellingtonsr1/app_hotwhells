package br.com.diecastcollector.feature.duplicates

import androidx.compose.ui.test.assertCountEquals
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
 * Duplicados continua sendo duplicados (T521, FR-503, FR-504, FR-524, SC-503).
 *
 * Massas do `quickstart.md`: A = variante com **1** exemplar, B = variante com **2**,
 * C = variante com **3** e D = outra variante do mesmo modelo. A tela só pode mostrar B e
 * C, com a contagem certa, e o atalho do Catálogo tem de continuar no lugar e navegar.
 *
 * O teste também prova que ampliar o acesso não mudou o significado: criar oferta em A pela
 * ficha **não** faz A aparecer, e vender uma unidade de B encolhe o grupo até ele sair
 * quando sobra uma unidade.
 */
@RunWith(AndroidJUnit4::class)
class DuplicatesUnchangedTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()
    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @Test
    fun duplicadosMantemAsMassasEAsAcoes() {
        seed()

        composeRule.onNodeWithText("Catálogo").performClick()
        waitForText("Twin Mill")

        // O atalho continua no Catálogo, no mesmo lugar, e navega para a tela.
        composeRule.onNodeWithText("Duplicados").assertIsDisplayed()
        composeRule.onNodeWithText("Duplicados").performClick()

        // Só B e C entram; A (uma unidade) e D (outra variante) nunca aparecem.
        waitForText("2 exemplares nesta variante")
        waitForText("3 exemplares nesta variante")
        composeRule.onAllNodesWithText("HW-0001").assertCountEquals(0)
        composeRule.onAllNodesWithText("HW-0007").assertCountEquals(0)
        composeRule.onNodeWithText("HW-0002").assertExists()
        composeRule.onNodeWithText("HW-0006").assertExists()

        // As ações por exemplar continuam funcionando a partir de Duplicados.
        composeRule.onAllNodesWithText("Disponibilidade")[0].performClick()
        waitForText("Disponibilidade do exemplar")
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("45,00")
        composeRule.onNodeWithText("Salvar").performScrollTo().performClick()
        waitForText("Disponibilidade atualizada.")
        waitForText("Venda · R$ 45,00")

        // Negociar por ali abre a rota existente, sem aviso (B tem duas unidades).
        composeRule.onAllNodesWithText("Negociar")[0].performClick()
        waitForText("Nova negociação")
        composeRule.onAllNodesWithText("Último exemplar da variante").assertCountEquals(0)
        pressBack()
        waitForText("3 exemplares nesta variante")

        // Oferta criada em A pela ficha não transforma A em duplicata.
        pressBack()
        waitForText("Twin Mill")
        composeRule.onAllNodesWithText("Ver exemplares")[0].performClick()
        waitForText("Exemplares")
        composeRule.onNodeWithText("HW-0001").performClick()
        waitForText("Negociação")
        composeRule.onNodeWithText("Disponibilidade").performScrollTo().performClick()
        waitForText("Disponibilidade do exemplar")
        composeRule.onNodeWithText("Salvar").performScrollTo().performClick()
        waitForText("Último exemplar da variante")
        composeRule.onNodeWithText("Continuar").performClick()
        waitForText("Disponibilidade atualizada.")
        pressBack()
        waitForText("Exemplares")
        pressBack()
        waitForText("Twin Mill")
        composeRule.onNodeWithText("Duplicados").performClick()
        waitForText("2 exemplares nesta variante")
        composeRule.onAllNodesWithText("HW-0001").assertCountEquals(0)

        // Vender uma unidade de B: com uma só restante, o grupo desaparece.
        composeRule.onAllNodesWithText("Negociar")[0].performClick()
        waitForText("Nova negociação")
        composeRule.onNodeWithText("Abrir negociação").performScrollTo().performClick()
        waitForText("Negociação aberta.")
        composeRule.onNodeWithText("Concluir venda").performScrollTo().performClick()
        waitForText("Valor recebido (R$)")
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("45,00")
        composeRule.onNodeWithText("Concluir").performClick()
        waitForText("Venda concluída.")
        pressBack()
        waitForText("3 exemplares nesta variante")
        composeRule.onAllNodesWithText("2 exemplares nesta variante").assertCountEquals(0)
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

    /**
     * Massas A–D do `quickstart.md`: quatro variantes do mesmo modelo, com 1, 2, 3 e 1
     * exemplar. `created_at` crescente fixa a ordem da tela, então o índice de
     * «Ver exemplares» corresponde à massa (A, B, C, D).
     */
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
            listOf("variant-a", "variant-b", "variant-c", "variant-d")
                .forEachIndexed { index, variantId ->
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
                            createdAt = index.toLong(),
                            updatedAt = index.toLong(),
                        ),
                    )
                }

            // A: 1 exemplar; B: 2; C: 3; D: 1 (variante diferente do mesmo modelo).
            val exemplars = listOf(
                "e1" to "variant-a",
                "e2" to "variant-b",
                "e3" to "variant-b",
                "e4" to "variant-c",
                "e5" to "variant-c",
                "e6" to "variant-c",
                "e7" to "variant-d",
            )
            exemplars.forEachIndexed { index, (id, variantId) ->
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

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
