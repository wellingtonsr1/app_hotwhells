package br.com.diecastcollector.feature.catalog

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
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
import br.com.diecastcollector.data.db.entity.DuplicateOfferEntity
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
 * Ciclo de vida pela ficha do exemplar (T526, FR-516–FR-519, SC-507).
 *
 * Massa: duas variantes do mesmo modelo com dois exemplares cada, então **Negociar** não
 * passa pelo aviso. O teste conclui uma venda (o exemplar sai do acervo ativo e o histórico
 * registra), uma troca (o negociado vira `TRADED`, o recebido segue `OWNED` e vinculado) e
 * um cancelamento (o exemplar segue `OWNED`, com a disponibilidade anterior intacta e o
 * evento no histórico). Nenhum registro é apagado em nenhum dos três desfechos.
 */
@RunWith(AndroidJUnit4::class)
class ExemplarNegotiationLifecycleTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()
    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @Test
    fun vendaTrocaECancelamentoPelaFicha() {
        seed()

        composeRule.onNodeWithText("Catálogo").performClick()
        waitForText("Twin Mill")
        composeRule.onAllNodesWithText("Ver exemplares")[0].performClick()
        waitForText("Exemplares")

        // ---- Venda de HW-0001 -------------------------------------------------------
        abrirFicha("HW-0001")
        composeRule.onNodeWithText("Negociar").performScrollTo().performClick()
        waitForText("Nova negociação")
        composeRule.onAllNodesWithText("Último exemplar da variante").assertCountEquals(0)
        composeRule.onNodeWithText("Abrir negociação").performScrollTo().performClick()
        waitForText("Negociação Reservada")
        composeRule.onNodeWithText("Concluir venda").performScrollTo().performClick()
        waitForText("Valor recebido (R$)")
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("45,00")
        composeRule.onNodeWithText("Concluir").performClick()
        waitForText("Venda concluída.")
        waitForText("Venda · Concluída")

        // Fora do acervo: a ficha explica o bloqueio e as ações ficam desabilitadas.
        pressBack()
        waitForText("Negociação")
        waitForText("Só exemplar no acervo pode ser disponibilizado ou negociado.")
        composeRule.onNodeWithText("Disponibilidade").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Negociar").performScrollTo().assertIsNotEnabled()

        // O exemplar vendido sai da lista ativa.
        pressBack()
        waitForText("Exemplares")
        composeRule.onAllNodesWithText("HW-0001").assertCountEquals(0)

        // ---- Troca de HW-0003, recebendo HW-0004 (outra variante) -------------------
        pressBack()
        waitForText("Twin Mill")
        composeRule.onAllNodesWithText("Ver exemplares")[1].performClick()
        waitForText("Exemplares")
        abrirFicha("HW-0003")
        composeRule.onNodeWithText("Negociar").performScrollTo().performClick()
        waitForText("Nova negociação")
        composeRule.onNodeWithText("Troca").performClick()
        composeRule.onNodeWithText("Abrir negociação").performScrollTo().performClick()
        waitForText("Negociação Reservada")
        composeRule.onNodeWithText("Concluir troca").performScrollTo().performClick()
        waitForText("Exemplar recebido")
        composeRule.onNodeWithText("Nenhum (só saída)").performClick()
        waitForText("HW-0004")
        composeRule.onNodeWithText("HW-0004").performClick()
        composeRule.onNodeWithText("Concluir").performClick()
        waitForText("Troca concluída.")
        waitForText("Troca · Concluída")

        // O negociado sai do acervo ativo; o recebido segue no acervo, vinculado.
        pressBack()
        waitForText("Negociação")
        composeRule.onAllNodesWithText("Em negociação").assertCountEquals(0)
        pressBack()
        waitForText("Exemplares")
        composeRule.onAllNodesWithText("HW-0003").assertCountEquals(0)
        composeRule.onNodeWithText("HW-0004").assertExists()

        // ---- Cancelamento de HW-0005, com a disponibilidade anterior intacta --------
        pressBack()
        waitForText("Twin Mill")
        composeRule.onAllNodesWithText("Ver exemplares")[2].performClick()
        waitForText("Exemplares")
        abrirFicha("HW-0005")
        waitForText("Disponível: Troca")
        composeRule.onNodeWithText("Negociar").performScrollTo().performClick()
        waitForText("Nova negociação")
        composeRule.onNodeWithText("Abrir negociação").performScrollTo().performClick()
        waitForText("Negociação Reservada")
        composeRule.onNodeWithText("Cancelar negociação").performScrollTo().performClick()
        waitForText("Cancelar negociação?")
        composeRule.onNodeWithText("Confirmar cancelamento").performClick()
        waitForText("Negociação cancelada.")
        // A oferta de troca pré-seleciona o tipo (FR-514), então o histórico é de troca.
        waitForText("Troca · Cancelada")

        // Segue no acervo, sem negociação ativa e com a oferta registrada preservada.
        pressBack()
        waitForText("Negociação")
        composeRule.onAllNodesWithText("Em negociação").assertCountEquals(0)
        waitForText("Disponível: Troca")
        pressBack()
        waitForText("Exemplares")
        composeRule.onNodeWithText("HW-0005").assertExists()
    }

    private fun abrirFicha(internalCode: String) {
        composeRule.onNodeWithText(internalCode).performClick()
        waitForText("Negociação")
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
     * Três variantes do mesmo modelo com dois exemplares cada: uma para a venda, uma para
     * a troca e uma para o cancelamento. HW-0005 já tem oferta de troca, para o
     * cancelamento provar que a disponibilidade anterior sobrevive.
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
            listOf("variant-a", "variant-b", "variant-c").forEachIndexed { index, variantId ->
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
            listOf(
                "e1" to "variant-a",
                "e2" to "variant-a",
                "e3" to "variant-b",
                "e4" to "variant-b",
                "e5" to "variant-c",
                "e6" to "variant-c",
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
            database.duplicateDao().insertOffer(
                DuplicateOfferEntity(
                    id = "offer-e5",
                    exemplarId = "e5",
                    availability = "TRADE",
                    condition = "LOOSE",
                    askingPriceCents = 3_000L,
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
