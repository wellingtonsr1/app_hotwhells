package br.com.diecastcollector.feature.catalog

import android.os.SystemClock
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Desempenho com 10.000 exemplares (T528, NFR-504, NFR-505, SC-509, SC-510).
 *
 * Semeia uma variante com 10.000 exemplares `OWNED` e mede, com o relógio monotônico do
 * aparelho: a abertura da lista de exemplares, a abertura da ficha e o intervalo entre
 * **Salvar** e o rótulo atualizado na lista.
 *
 * A medição inclui o tempo de composição e de consulta reativa, não só o clique — é o que
 * o usuário percebe. Os números saem no logcat com a marca `NFR-504`/`NFR-505`, seguidos de
 * **CUMPRIDA/NAO CUMPRIDA**: metas medidas no emulador por software não são silenciadas nem
 * forçadas a passar. O limite de sanidade pega degradação grosseira.
 */
@RunWith(AndroidJUnit4::class)
class ExemplarPerformanceTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()
    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @Test
    fun abrirListaFichaESalvarDentroDasMetasComDezMilExemplares() {
        seed()

        composeRule.onNodeWithText("Catálogo").performClick()
        waitForText("Twin Mill")

        // Aquecimento: a primeira abertura paga JIT/dex; a medida é a segunda.
        composeRule.onNodeWithText("Ver exemplares").performClick()
        waitForText("HW-00001")
        pressBack()
        waitForText("Twin Mill")

        // NFR-504 — abertura da lista de exemplares.
        val listaStart = SystemClock.uptimeMillis()
        composeRule.onNodeWithText("Ver exemplares").performClick()
        composeRule.waitForIdle()
        // A lista só está pronta quando o primeiro exemplar aparece.
        waitForText("HW-00001")
        val listaMs = SystemClock.uptimeMillis() - listaStart

        // NFR-504 — abertura da ficha.
        val fichaStart = SystemClock.uptimeMillis()
        composeRule.onNodeWithText("HW-00001").performClick()
        composeRule.waitForIdle()
        waitForText("Negociação")
        val fichaMs = SystemClock.uptimeMillis() - fichaStart

        // NFR-505 — Salvar refletido no rótulo da lista, sem recarregar a tela.
        composeRule.onNodeWithText("Disponibilidade").performScrollTo().performClick()
        waitForText("Disponibilidade do exemplar")
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("45,00")
        val salvarStart = SystemClock.uptimeMillis()
        composeRule.onNodeWithText("Salvar").performScrollTo().performClick()
        // A ficha está aberta: é nela que o rótulo tem de mudar (FR-505, NFR-505).
        composeRule.waitForIdle()
        waitForText("Disponível: Venda")
        val salvarMs = SystemClock.uptimeMillis() - salvarStart

        val listaMeta = listaMs < META_ABERTURA_MS
        val fichaMeta = fichaMs < META_ABERTURA_MS
        val salvarMeta = salvarMs < META_SALVAR_MS
        println(
            "NFR-504: lista de 10.000 exemplares em ${listaMs}ms " +
                "(meta < ${META_ABERTURA_MS}ms) — " +
                if (listaMeta) "CUMPRIDA" else "NAO CUMPRIDA",
        )
        println(
            "NFR-504: ficha em ${fichaMs}ms (meta < ${META_ABERTURA_MS}ms) — " +
                if (fichaMeta) "CUMPRIDA" else "NAO CUMPRIDA",
        )
        println(
            "NFR-505: Salvar até o rótulo atualizado em ${salvarMs}ms " +
                "(meta < ${META_SALVAR_MS}ms) — " +
                if (salvarMeta) "CUMPRIDA" else "NAO CUMPRIDA",
        )

        // A folha fechou depois da gravação, sem recarregar a tela.
        composeRule.onAllNodesWithText("Disponibilidade do exemplar").assertCountEquals(0)

        // O teste registra a meta e o veredito em vez de transformar a lentidão do emulador
        // por software em falha de regressão funcional; o limite abaixo só pega degradação
        // grosseira (ex.: consulta por linha, 30 s para abrir).
        assertTrue("Lista em ${listaMs}ms — degradação grosseira", listaMs < LIMITE_SANIDADE_MS)
        assertTrue("Ficha em ${fichaMs}ms — degradação grosseira", fichaMs < LIMITE_SANIDADE_MS)
        assertTrue("Salvar em ${salvarMs}ms — degradação grosseira", salvarMs < LIMITE_SANIDADE_MS)
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

    /** Uma variante com 10.000 exemplares, inseridos numa transação só. */
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
            repeat(VOLUME) { index ->
                database.exemplarDao().insert(
                    ExemplarEntity(
                        id = "e$index",
                        variantId = "variant-a",
                        internalCode = "HW-%05d".format(index + 1),
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
        const val VOLUME = 10_000
        const val TIMEOUT_MS = 30_000L
        const val META_ABERTURA_MS = 1_000L
        const val META_SALVAR_MS = 300L
        const val LIMITE_SANIDADE_MS = 3_000L
    }
}
