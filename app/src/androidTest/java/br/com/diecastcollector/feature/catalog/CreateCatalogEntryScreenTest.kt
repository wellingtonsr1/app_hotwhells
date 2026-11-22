package br.com.diecastcollector.feature.catalog

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.MainActivity
import br.com.diecastcollector.testing.freshInstallRule
import java.io.File
import java.io.FileOutputStream
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Cadastro completo de miniatura com foto (T040 - US2, FR-014, FR-020, FR-021, FR-023).
 *
 * Sobe o [MainActivity] real — grafo Hilt, `Room` em arquivo, navegação e ViewModels de
 * produção — e percorre o caminho inteiro: catálogo vazio → modelo → variante → foto →
 * catálogo com um exemplar. Ao final confere o arquivo da foto em `filesDir/images`, não só
 * a tela: uma grade bonita com a foto perdida no caminho não é cadastro com foto.
 *
 * O seletor de fotos é do sistema e não tem como ser tocado por um teste de Compose; o
 * `PickVisualMedia` é stubbado devolvendo um JPEG real gerado no próprio teste. Todo o
 * resto do caminho — ler os bytes pela `ContentResolver`, guardar a captura pendente,
 * gravar variante, exemplar e foto — passa pelo código de produção.
 *
 * Precisa de emulador ou aparelho.
 */
@RunWith(AndroidJUnit4::class)
class CreateCatalogEntryScreenTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    /** Instalação limpa: banco sem registros e sem fotos de execuções anteriores. */
    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun cadastroCompletoComFotoChegaAoColecao() {
        // 1. Instalação nova: o app abre no painel (US3); o acervo fica na aba «Catálogo».
        composeRule.onNodeWithText("Catálogo").performClick()
        waitForText(EMPTY_CATALOG)

        // 2. O FAB abre o cadastro de miniatura.
        composeRule.onNodeWithText("+ Mini").performClick()
        waitForText("Nova miniatura")

        // 3. Identificação do carro: fabricante e modelo nos campos reais.
        typeIntoField(index = 0, text = "Mattel")
        typeIntoField(index = 1, text = "Twin Mill")

        // 4. O modelo criado leva ao cadastro da variante.
        composeRule.onNodeWithText("Salvar miniatura").performScrollTo().performClick()
        waitForText("Cadastrar variante")

        // 5. Atributos da variante: cor principal e ano.
        typeIntoField(index = 0, text = "Vermelho")
        typeIntoField(index = 1, text = "2024")

        // 6. Condição do exemplar, que nasce junto com a variante.
        composeRule.onNodeWithText("Solto").performScrollTo().performClick()

        // 7. A foto entra pelo seletor do sistema, stubbado com um JPEG de verdade.
        stubPhotoPickerWith(jpegFixture())
        composeRule.onNodeWithText("Galeria").performScrollTo().performClick()

        // A captura fica pendente na grade até a variante e o exemplar existirem.
        waitForText("Ainda não salva")

        // 8. Salvar variante + exemplar devolve o usuário ao catálogo, já com a miniatura.
        composeRule.onNodeWithText("Salvar variante e exemplar").performScrollTo().performClick()
        waitForText("1 exemplar", substring = true)
        composeRule.onNodeWithText("Twin Mill").assertExists()

        // 9. A foto não ficou só na tela: existe arquivo em filesDir/images/<exemplarId>/.
        val images = waitForPhotoFile()
        assert(images.isNotEmpty()) { "Nenhuma foto gravada em filesDir/images" }
    }

    private fun typeIntoField(index: Int, text: String) {
        composeRule.onAllNodes(hasSetTextAction())[index].apply {
            performClick()
            performTextInput(text)
        }
    }

    /** Devolve o resultado do Photo Picker sem abrir a UI do sistema. */
    private fun stubPhotoPickerWith(file: File) {
        val result = Instrumentation.ActivityResult(
            Activity.RESULT_OK,
            Intent().setData(Uri.fromFile(file)),
        )
        // O contrato usa o picker quando existe e cai para o seletor de documentos quando não.
        intending(hasAction(MediaStore.ACTION_PICK_IMAGES)).respondWith(result)
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(result)
        intending(hasAction(Intent.ACTION_GET_CONTENT)).respondWith(result)
    }

    /** JPEG válido de 64x64, gerado aqui para não depender de asset versionado. */
    private fun jpegFixture(): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.RED)
        val file = File(context.cacheDir, "foto_teste_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()
        return file
    }

    /** A gravação é assíncrona em relação ao clique; espera o arquivo aparecer. */
    private fun waitForPhotoFile(): List<File> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val images = File(context.filesDir, "images")
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            images.walkTopDown().any { it.isFile }
        }
        return images.walkTopDown().filter { it.isFile }.toList()
    }

    /**
     * Espera o texto aparecer: gravar no banco é assíncrono, então a tela muda depois do
     * clique e uma asserção imediata daria falso negativo.
     */
    private fun waitForText(text: String, substring: Boolean = false) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text, substring = substring)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private companion object {
        const val EMPTY_CATALOG = "Nenhuma miniatura ainda"
        const val TIMEOUT_MS = 10_000L
    }
}
