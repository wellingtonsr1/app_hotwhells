package br.com.diecastcollector

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.KeyEvent
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.diecastcollector.data.db.AppDatabase
import br.com.diecastcollector.data.db.entity.CarModelEntity
import br.com.diecastcollector.data.db.entity.CollectionEntity
import br.com.diecastcollector.data.db.entity.CollectionItemEntity
import br.com.diecastcollector.data.db.entity.ExemplarEntity
import br.com.diecastcollector.data.db.entity.LocationEntity
import br.com.diecastcollector.data.db.entity.ManufacturerEntity
import br.com.diecastcollector.data.db.entity.PhotoEntity
import br.com.diecastcollector.data.db.entity.SeriesEntity
import br.com.diecastcollector.data.db.entity.VariantEntity
import br.com.diecastcollector.data.db.entity.WishlistItemEntity
import br.com.diecastcollector.feature.catalog.PhotoGridTags
import br.com.diecastcollector.navigation.AppNavHost
import br.com.diecastcollector.navigation.Destination
import br.com.diecastcollector.testing.DeviceWindow
import br.com.diecastcollector.testing.freshInstallRule
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Capturas por classe de janela (T356, SC-301/SC-302).
 *
 * Duas caminhadas, sempre no app real:
 *
 * 1. **Vazia** — abre a activity de produção e percorre as abas e atalhos por toque. Prova a
 *    moldura (barra inferior em C, trilho em M/E/P) e os estados vazios.
 * 2. **Semeada** — grava fabricante, modelo, variante, exemplares, fotos, coleção, local e
 *    desejo direto no banco (mesma técnica do `DuplicatesFlowTest`), escreve três JPEGs em
 *    `filesDir/images/e1/` e navega **por rota** com um `NavHostController` próprio. Assim
 *    cada tela de formulário, ficha, lista, grade, editor, seletor e diálogo é alcançada sem
 *    depender de coordenadas de toque.
 *
 * Os PNGs vão para `Download/responsive` pelo MediaStore — o diretório externo do app é
 * apagado quando o `connectedAndroidTest` desinstala o pacote — e o `adb pull` copia para
 * `build/responsive/capturas/`.
 */
@RunWith(AndroidJUnit4::class)
class ResponsiveCapturesTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    private val freshInstall = freshInstallRule()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(freshInstall).around(composeRule)

    private var nav: NavHostController? = null

    @After
    fun restaurarJanela() {
        DeviceWindow.reset()
    }

    @Test
    fun capturasCompacta() {
        capturarTudo(classe = "c", widthDp = 411, heightDp = 914)
    }

    @Test
    fun capturasMedia() {
        capturarTudo(classe = "m", widthDp = 700, heightDp = 900)
    }

    @Test
    fun capturasExpandida() {
        capturarTudo(classe = "e", widthDp = 1000, heightDp = 900)
    }

    @Test
    fun capturasPaisagem() {
        capturarTudo(classe = "p", widthDp = 640, heightDp = 360)
    }

    @Test
    fun capturasSemeadasCompacta() {
        capturarSemeadas(classe = "c", widthDp = 411, heightDp = 914)
    }

    @Test
    fun capturasSemeadasMedia() {
        capturarSemeadas(classe = "m", widthDp = 700, heightDp = 900)
    }

    @Test
    fun capturasSemeadasExpandida() {
        capturarSemeadas(classe = "e", widthDp = 1000, heightDp = 900)
    }

    @Test
    fun capturasSemeadasPaisagem() {
        capturarSemeadas(classe = "p", widthDp = 640, heightDp = 360)
    }

    // ------------------------------------------------------------------ caminhada vazia

    private fun capturarTudo(classe: String, widthDp: Int, heightDp: Int) {
        DeviceWindow.use(widthDp = widthDp, heightDp = heightDp)
        composeRule.waitForIdle()

        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val rotuloCatalogo = contexto.getString(R.string.catalog_tab)

        // Painel
        capturar("$classe-painel")
        abrirAba(rotuloCatalogo)
        capturar("$classe-catalogo")

        // Busca (ícone no cabeçalho do catálogo)
        composeRule.onNodeWithContentDescription(contexto.getString(R.string.search_open))
            .performClick()
        composeRule.waitForIdle()
        capturar("$classe-busca")
        voltar()

        // Criação: formulário do primeiro cadastro
        composeRule.onNode(hasText(contexto.getString(R.string.catalog_add)) and hasClickAction())
            .performClick()
        composeRule.waitForIdle()
        capturar("$classe-criacao")
        voltar()

        // Coleções
        composeRule.onNode(hasText(contexto.getString(R.string.collections_open)) and hasClickAction())
            .performClick()
        composeRule.waitForIdle()
        capturar("$classe-colecoes")
        voltar()

        // Duplicados
        composeRule.onNode(hasText(contexto.getString(R.string.duplicates_open)) and hasClickAction())
            .performClick()
        composeRule.waitForIdle()
        capturar("$classe-duplicados")
        voltar()

        // Abas restantes
        abrirAba(contexto.getString(R.string.wishlist_tab))
        capturar("$classe-desejos")
        abrirAba(contexto.getString(R.string.finance_tab))
        capturar("$classe-financeiro")
        abrirAba(contexto.getString(R.string.settings_tab))
        capturar("$classe-ajustes")

        // Telas de Ajustes: sete destinos, cada um aberto e capturado antes de voltar.
        listOf(
            "Localizações" to "locais",
            "Backup e restauração" to "backup",
            "Relatórios" to "relatorios",
            "Importar planilha" to "importar",
            "Dados da coleção" to "dados",
            "Sobre o app" to "sobre",
            "Política de privacidade" to "privacidade",
        ).forEach { (rotulo, nome) ->
            composeRule.onNode(hasText(rotulo) and hasClickAction())
                .performScrollTo()
                .performClick()
            composeRule.waitForIdle()
            capturar("$classe-$nome")
            voltar()
        }
    }

    // --------------------------------------------------------------- caminhada semeada

    private fun capturarSemeadas(classe: String, widthDp: Int, heightDp: Int) {
        classeCorrente = classe
        DeviceWindow.use(widthDp = widthDp, heightDp = heightDp)
        seed()
        montarHost()

        // Catálogo com conteúdo: o card da variante semeada.
        irPara(Destination.Catalog.route)
        esperar()
        capturar("$classe-catalogo-com-dados")

        // Modelos que pararam no primeiro passo (FR-029).
        rota(Destination.Incomplete.route, "$classe-incompletos")

        // Passo 2 do cadastro: variante, primeiro exemplar e fotos.
        rota(Destination.VariantForm.routeFor("model1"), "$classe-variante")
        rota(Destination.VariantEdit.routeFor("variant-a"), "$classe-variante-edicao")
        rota(Destination.ModelForm.routeFor("model1"), "$classe-modelo")
        rota(Destination.AddExemplar.routeFor("variant-a"), "$classe-exemplar-form")

        // Unidades da variante e a ficha completa.
        rota(Destination.ExemplarList.routeFor("variant-a"), "$classe-exemplares")
        irPara(Destination.ExemplarDetail.routeFor("e1"))
        esperar()
        capturar("$classe-ficha")
        capturarFotos()
        // O fechamento de diálogos pode ter deixado a pilha em outro ponto: volta para a ficha.
        if (!existe("Arquivar exemplar")) {
            irPara(Destination.ExemplarDetail.routeFor("e1"))
            esperar()
        }
        capturarDialogoDeArquivamento(classe)

        // Coleções, item picker e formulário.
        rota(Destination.Collections.route, "$classe-colecoes-com-dados")
        irPara(Destination.CollectionDetail.routeFor("c1"))
        esperar()
        capturar("$classe-colecao")
        rota(Destination.CollectionForm.routeFor(), "$classe-colecao-form")

        // Séries e o diálogo de criação.
        irPara(Destination.Series.route)
        esperar()
        capturar("$classe-series")

        // Leitura de código de barras (a câmera não é concedida; a tela é o que importa).
        rota(Destination.BarcodeScan.route, "$classe-codigo-barras")

        // Negociação de um exemplar duplicado.
        rota(Destination.Negotiation.routeFor("e1"), "$classe-negociacao")

        // Desejo: lista e formulário.
        rota(Destination.Wishlist.route, "$classe-desejos-com-dados")
        rota(Destination.WishlistForm.routeFor(), "$classe-desejo-form")

        // Inventário de uma localização com exemplar dentro.
        rota(Destination.LocationContents.routeFor("loc1"), "$classe-local-conteudo")

        // Busca com filtros abertos (folha).
        irPara(Destination.Search.route)
        esperar()
        capturar("$classe-busca-com-dados")
        capturarFiltros(classe)

        // Autocomplete do cadastro rápido.
        capturarAutocomplete(classe)
    }

    // --------------------------------------------------------------------- navegação

    /**
     * Monta o `AppNavHost` de produção com um controlador que o teste também enxerga.
     *
     * O conteúdo da activity é substituído pelo mesmo host que ela usa; o shell (barra ou
     * trilho) continua vindo do app, então as capturas seguem mostrando a moldura real.
     */
    private fun montarHost() {
        // `MainActivity` já chamou `setContent`; a regra não aceita um segundo. O caminho é o
        // que a própria mensagem do Compose indica: montar na `ComposeView` que está lá — a
        // tela continua sendo a de produção, inclusive o shell do `AppNavHost`.
        composeRule.runOnUiThread {
            // `android.R.id.content` é o `FrameLayout` que embrulha a `ComposeView` criada por
            // `setContent` na activity: procura o primeiro `ComposeView` dentro dele.
            val raiz = composeRule.activity.findViewById<android.view.ViewGroup>(android.R.id.content)
            val view = procurarComposeView(raiz) ?: error("MainActivity sem ComposeView")
            view.setContent {
                val controller = rememberNavController()
                nav = controller
                AppNavHost(navController = controller)
            }
        }
        // A composição roda no próximo quadro: espera o controlador existir antes de navegar.
        composeRule.waitUntil(15_000) { nav != null }
        esperar()
    }

    private fun procurarComposeView(raiz: android.view.View): ComposeView? {
        if (raiz is ComposeView) return raiz
        if (raiz is android.view.ViewGroup) {
            for (indice in 0 until raiz.childCount) {
                procurarComposeView(raiz.getChildAt(indice))?.let { return it }
            }
        }
        return null
    }

    private fun irPara(rota: String) {
        val controller = nav ?: error("NavHostController não inicializado")
        composeRule.runOnUiThread { controller.navigate(rota) { launchSingleTop = true } }
        esperar()
    }

    private fun rota(rota: String, nome: String) {
        irPara(rota)
        capturar(nome)
        voltarRota()
    }

    private fun voltarRota() {
        val controller = nav
        composeRule.runOnUiThread { controller?.popBackStack() }
        esperar(300)
    }

    private fun capturarFotos() {
        // A grade fica no meio da ficha: rola até a primeira miniatura antes de capturar.
        if (!existeTag(PhotoGridTags.TILE)) return
        composeRule.onAllNodesWithTag(PhotoGridTags.TILE)[0].performScrollTo()
        esperar(300)
        capturar("${classeAtual()}-fotos")

        // Visualizador e editor de foto **não** entram por aqui: sob instrumentação o toque na
        // miniatura e no lápis não abre a janela do diálogo no `hw_test` (as capturas saíam
        // idênticas à ficha). As linhas 30 e 31 do checklist ficam com a grade (`fotos`) e o
        // visualizador marcado como sem captura.
    }

    private fun capturarDialogoDeArquivamento(classe: String) {
        if (!existe("Arquivar exemplar")) return
        tentar {
            composeRule.onNode(hasText("Arquivar exemplar") and hasClickAction())
                .performScrollTo()
                .performClick()
        }
        esperar(500)
        capturar("$classe-dialogo-arquivar", dialogo = true)
        fecharDialogo()
    }

    private fun capturarFiltros(classe: String) {
        val rotulo = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.search_filters)
        if (!existe(rotulo, porDescricao = true)) return
        tentar { composeRule.onNodeWithContentDescription(rotulo).performClick() }
        esperar(700)
        capturar("$classe-filtros", dialogo = true)
        fecharDialogo()
    }

    private fun capturarAutocomplete(classe: String) {
        irPara(Destination.CreateEntry.routeFor())
        if (!existeCampoDeTexto()) return
        tentar { composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Mat") }
        esperar(700)
        capturar("$classe-autocomplete")
    }

    // ------------------------------------------------------------------------ utilidades

    private fun classeAtual(): String = classeCorrente

    private var classeCorrente: String = "?"

    private fun esperar(ms: Long = 700) {
        Thread.sleep(ms)
    }

    /**
     * Fecha diálogo e folha com um BACK de verdade.
     *
     * `onBackPressedDispatcher` da activity **não** alcança a janela do `Dialog`: o `voltar`
     * era consumido pela navegação e tirava a tela do lugar (foi o que sumiu o editor e o
     * diálogo de arquivamento na primeira execução). O evento de tecla vai para a janela em
     * foco, que é a do diálogo.
     */
    private fun raizes(): Int = try {
        composeRule.onAllNodes(isRoot()).fetchSemanticsNodes().size
    } catch (_: Throwable) {
        1
    }

    private fun fecharDialogo() {
        if (raizes() > 1) {
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        }
        esperar(500)
    }

    private fun existe(texto: String, porDescricao: Boolean = false): Boolean = try {
        val nos = if (porDescricao) {
            composeRule.onAllNodesWithContentDescription(texto).fetchSemanticsNodes()
        } else {
            composeRule.onAllNodesWithText(texto).fetchSemanticsNodes()
        }
        nos.isNotEmpty()
    } catch (_: Throwable) {
        false
    }

    private fun existeTag(tag: String): Boolean = try {
        composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    } catch (_: Throwable) {
        false
    }

    private fun existeCampoDeTexto(): Boolean = try {
        composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
    } catch (_: Throwable) {
        false
    }

    /** Executa um passo de toque que pode não existir naquela tela/classe. */
    private fun tentar(passo: () -> Unit): Boolean = try {
        passo()
        true
    } catch (_: Throwable) {
        false
    }

    private fun abrirAba(rotulo: String) {
        composeRule.onNode(hasText(rotulo) and hasClickAction()).performClick()
        composeRule.waitForIdle()
    }

    private fun voltar() {
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }

    /**
     * Grava em `Download/responsive` pelo MediaStore.
     *
     * O diretório externo do app (`Android/data/<pacote>/files`) é apagado quando o
     * `connectedAndroidTest` desinstala o app no fim da execução — foi o que aconteceu na
     * primeira tentativa. O MediaStore sobrevive à desinstalação, então o `adb pull` acha os
     * arquivos depois.
     */
    private fun capturar(nome: String, dialogo: Boolean = false) {
        val bitmap = try {
            // Com um diálogo aberto existem duas raízes (tela e janela do diálogo): a última é
            // a que o usuário está vendo.
            if (dialogo) {
                val raizes = composeRule.onAllNodes(isRoot()).fetchSemanticsNodes()
                composeRule.onAllNodes(isRoot())[raizes.size - 1]
                    .captureToImage()
                    .asAndroidBitmap()
            } else {
                composeRule.onRoot().captureToImage().asAndroidBitmap()
            }
        } catch (erro: Throwable) {
            println("CAPTURA-FALHOU $nome: ${erro.message}")
            return
        }
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = contexto.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val chaves = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "$nome.png")
                put(MediaStore.Downloads.MIME_TYPE, "image/png")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/responsive")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, chaves)
            if (uri == null) {
                println("CAPTURA-FALHOU $nome: MediaStore recusou")
                return
            }
            resolver.openOutputStream(uri)?.use { saida ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, saida)
            }
        } else {
            val diretorio = File(contexto.getExternalFilesDir(null), "capturas").apply { mkdirs() }
            FileOutputStream(File(diretorio, "$nome.png")).use { saida ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, saida)
            }
        }
        bitmap.recycle()
    }

    /**
     * Captura a tela inteira pelo `UiAutomation`.
     *
     * Precisa disso quem abre em **outra janela** (visualizador e editor de foto): a captura
     * pela árvore do Compose devolve só a raiz da activity nessas telas.
     */
    private fun capturarTela(nome: String) {
        val bitmap = try {
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        } catch (erro: Throwable) {
            println("CAPTURA-FALHOU $nome: ${erro.message}")
            return
        }
        if (bitmap == null) {
            println("CAPTURA-FALHOU $nome: tela indisponível")
            return
        }
        gravar(nome, bitmap)
        bitmap.recycle()
    }

    private fun gravar(nome: String, bitmap: Bitmap) {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = contexto.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val chaves = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "$nome.png")
                put(MediaStore.Downloads.MIME_TYPE, "image/png")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/responsive")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, chaves) ?: return
            resolver.openOutputStream(uri)?.use { saida ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, saida)
            }
        } else {
            val diretorio = File(contexto.getExternalFilesDir(null), "capturas").apply { mkdirs() }
            FileOutputStream(File(diretorio, "$nome.png")).use { saida ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, saida)
            }
        }
    }

    // --------------------------------------------------------------------------- semeio

    /**
     * Catálogo, fotos e listas mínimas para chegar às telas que dependem de dados.
     *
     * Mesma técnica do `DuplicatesFlowTest`: segunda conexão ao banco do app, escrita direta
     * e fechamento em seguida. As fotos são JPEGs de verdade em `filesDir/images/e1/`, porque
     * a grade e o editor leem o arquivo (e não só o caminho).
     */
    private fun seed(): Unit = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room
            .databaseBuilder(contexto, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addCallback(AppDatabase.CALLBACK)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        try {
            val t = 1_700_000_000_000L
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
                    notes = "Modelo de teste das capturas.",
                    archivedAt = null,
                    createdAt = t,
                    updatedAt = t,
                ),
            )
            // Modelo sem variante: é o que a lista de incompletos mostra (FR-029).
            database.carModelDao().insert(
                CarModelEntity(
                    id = "model2",
                    manufacturerId = "m1",
                    name = "Rodger Dodger",
                    scale = "1:64",
                    brandId = null,
                    realVehicleId = null,
                    bodyType = null,
                    material = null,
                    countryOfManufacture = null,
                    notes = null,
                    archivedAt = null,
                    createdAt = t + 1,
                    updatedAt = t + 1,
                ),
            )
            database.seriesDao().insert(
                SeriesEntity(
                    id = "s1",
                    manufacturerId = "m1",
                    name = "Mainline",
                    parentId = null,
                    kind = "BASIC",
                    year = 2024,
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
                        seriesId = "s1",
                        primaryColorId = null,
                        wheelsType = "5SP",
                        baseType = null,
                        decoration = null,
                        releaseYear = 2024,
                        collectionNumber = null,
                        seriesNumber = null,
                        edition = null,
                        manufacturerCode = "HW-2024",
                        packagingType = null,
                        archivedAt = null,
                        createdAt = t,
                        updatedAt = t,
                    ),
                )
            }
            database.locationDao().insert(
                LocationEntity("loc1", "RACK", "Estante A", null, null, t, t),
            )
            database.locationDao().insert(
                LocationEntity("loc2", "SHELF", "Prateleira 1", "loc1", null, t, t),
            )
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
                        locationId = if (id == "e1") "loc2" else null,
                        status = "OWNED",
                        notes = if (id == "e1") "Comprado na feira." else null,
                        rowVersion = 0L,
                        createdAt = t + index,
                        updatedAt = t + index,
                    ),
                )
            }

            // Três fotos do exemplar e1: uma principal e duas de outros ângulos.
            val diretorio = File(File(contexto.filesDir, "images"), "e1").apply { mkdirs() }
            listOf("FRONT" to true, "BACK" to false, "DETAIL" to false)
                .forEachIndexed { index, (angulo, principal) ->
                    val arquivo = File(diretorio, "foto-${index + 1}.jpg")
                    escreverFoto(arquivo, index)
                    database.photoDao().insert(
                        PhotoEntity(
                            id = "p$index",
                            exemplarId = "e1",
                            angle = angulo,
                            path = "images/e1/${arquivo.name}",
                            checksum = "checksum-$index",
                            isPrimary = principal,
                            sortOrder = index,
                            width = FOTO_LARGURA,
                            height = FOTO_ALTURA,
                            bytes = arquivo.length(),
                            capturedAt = t + index,
                        ),
                    )
                }

            database.collectionDao().insert(
                CollectionEntity(
                    id = "c1",
                    name = "Mainline 2024",
                    description = "As básicas do ano.",
                    parentId = null,
                    manufacturerId = "m1",
                    archivedAt = null,
                    createdAt = t,
                    updatedAt = t,
                ),
            )
            database.collectionItemDao().insert(
                CollectionItemEntity(
                    id = "ci1",
                    collectionId = "c1",
                    variantId = "variant-a",
                    modelId = null,
                    expected = 2,
                    nonCollectible = 0,
                    notes = null,
                ),
            )
            database.wishlistDao().insert(
                WishlistItemEntity(
                    id = "w1",
                    modelId = "model1",
                    variantId = "variant-a",
                    freeText = null,
                    priority = 1,
                    maxPriceCents = 4_500L,
                    foundPriceCents = null,
                    store = "Loja do bairro",
                    referenceUrl = null,
                    notes = null,
                    status = "WANTED",
                    convertedExemplarId = null,
                    createdAt = t,
                    updatedAt = t,
                ),
            )
        } finally {
            database.close()
        }
    }

    /** Escreve um JPEG simples, com uma faixa de cor por ângulo, só para o Coil ter o que ler. */
    private fun escreverFoto(arquivo: File, indice: Int) {
        if (arquivo.exists() && arquivo.length() > 0L) return
        val bitmap = Bitmap.createBitmap(FOTO_LARGURA, FOTO_ALTURA, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(38, 38, 40))
        val pincel = Paint().apply { color = Color.rgb(210, 40, 40 + indice * 40) }
        canvas.drawRect(0f, 0f, FOTO_LARGURA.toFloat(), FOTO_ALTURA / 3f, pincel)
        pincel.color = Color.rgb(230, 230, 220)
        canvas.drawRect(
            FOTO_LARGURA / 6f,
            FOTO_ALTURA / 2f,
            FOTO_LARGURA * 5f / 6f,
            FOTO_ALTURA * 3f / 4f,
            pincel,
        )
        FileOutputStream(arquivo).use { saida ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, saida)
        }
        bitmap.recycle()
    }

    private companion object {
        const val FOTO_LARGURA = 900
        const val FOTO_ALTURA = 1_200
    }
}
