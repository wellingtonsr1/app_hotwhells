package br.com.diecastcollector.benchmark

import android.util.Log
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Medição de SC-003 (spec 003): 500 variantes na Coleção abaixo de 1 s e rolagem fluida.
 *
 * A lista semeada tem 450 variantes com foto válida, 25 com arquivo ausente e 25 sem foto —
 * a mesma proporção de SC-005 (10% sem foto, 5% com arquivo inválido), para que o custo do
 * marcador de ausência também entre na medição.
 *
 * O app abre no painel e a Coleção é uma aba. A abertura é cronometrada do toque na aba até
 * o primeiro cartão existir na árvore de acessibilidade — a inicialização do processo fica
 * de fora, porque o critério fala da listagem, não do app. O `FrameTimingMetric` da mesma
 * execução mostra quanto custa em quadros a troca de aba.
 *
 * Mede no emulador x86_64 do projeto (o único alvo x86_64 do build, como no perfil de
 * abertura): o número vale como comparação entre versões, não como desempenho de aparelho
 * real. Ver `specs/003-colecao-miniaturas/analysis.md`.
 */
@RunWith(AndroidJUnit4::class)
class CatalogBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    /** Aberto uma vez por iteração; o resumo sai no fim do teste. */
    private val aberturaMs = mutableListOf<Long>()

    @Test
    fun abrirColecaoCom500Variantes() {
        rule.measureRepeated(
            packageName = APP_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = COMPILED,
            iterations = OPEN_ITERATIONS,
        ) {
            pressHome()
            startActivityAndWait()
            // A aba já existe quando o painel termina de compor: esperar por ela antes de
            // começar o cronômetro mantém a medição só na troca de aba.
            val catalogTab = device.wait(Until.findObject(By.text(CATALOG_TAB)), TIMEOUT)
                ?: error("Aba «$CATALOG_TAB» não apareceu em $TIMEOUT ms")

            val start = android.os.SystemClock.uptimeMillis()
            catalogTab.click()
            val primeiroCartao = device.waitForFirstCard()
            val elapsed = android.os.SystemClock.uptimeMillis() - start
            if (!primeiroCartao) error("A Coleção não exibiu nenhum cartão em $TIMEOUT ms")
            aberturaMs += elapsed
        }
    }

    @Test
    fun rolarColecaoCom500Variantes() {
        rule.measureRepeated(
            packageName = APP_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = COMPILED,
            iterations = SCROLL_ITERATIONS,
            setupBlock = { openCollection() },
        ) {
            repeat(SWIPES) {
                device.swipe(
                    device.displayWidth / 2,
                    (device.displayHeight * 0.8f).toInt(),
                    device.displayWidth / 2,
                    (device.displayHeight * 0.2f).toInt(),
                    SWIPE_STEPS,
                )
            }
        }
    }

    /**
     * Espera o primeiro cartão consultando a árvore a cada 10 ms.
     *
     * `UiDevice.wait` faz busca espaçada, e a granularidade dele (centenas de ms) engoliria
     * justamente a diferença entre "abriu em 300 ms" e "abriu em 900 ms".
     */
    private fun androidx.test.uiautomator.UiDevice.waitForFirstCard(): Boolean {
        val deadline = android.os.SystemClock.uptimeMillis() + TIMEOUT
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            if (hasObject(By.text(OPEN_EXEMPLARS))) return true
            android.os.SystemClock.sleep(POLL_MS)
        }
        return false
    }

    /** Abre a Coleção fora do bloco medido, para os quadros da rolagem ficarem isolados. */
    private fun androidx.benchmark.macro.MacrobenchmarkScope.openCollection() {
        pressHome()
        startActivityAndWait()
        val catalogTab = device.wait(Until.findObject(By.text(CATALOG_TAB)), TIMEOUT)
            ?: error("Aba «$CATALOG_TAB» não apareceu em $TIMEOUT ms")
        catalogTab.click()
        check(device.wait(Until.hasObject(By.text(OPEN_EXEMPLARS)), TIMEOUT)) {
            "A Coleção não exibiu nenhum cartão em $TIMEOUT ms"
        }
    }

    @After
    fun resumirAbertura() {
        if (aberturaMs.isEmpty()) return
        val ordenado = aberturaMs.sorted()
        fun percentil(q: Double): Long = ordenado[((ordenado.size - 1) * q).toInt()]
        val resumo = "Coleção com $VARIANTS variantes — ${ordenado.size} aberturas: " +
            "mediana ${percentil(0.5)} ms, p90 ${percentil(0.9)} ms, " +
            "mínima ${ordenado.first()} ms, máxima ${ordenado.last()} ms"
        Log.i(LOG_TAG, resumo)
        println("SC-003 $resumo")
    }

    private companion object {
        const val LOG_TAG = "CatalogBenchmark"

        /** Mesmo `applicationId` do `:app`; a variante `benchmark` não tem sufixo. */
        const val APP_PACKAGE = "br.com.diecastcollector"

        /** Rótulos de `strings.xml`: `catalog_tab` e `catalog_open_exemplars`. */
        const val CATALOG_TAB = "Catálogo"
        const val OPEN_EXEMPLARS = "Ver exemplares"

        const val VARIANTS = 500
        const val TIMEOUT = 10_000L
        const val POLL_MS = 10L

        /** 10 aberturas e 5 rolagens: o suficiente para mediana e percentis úteis. */
        const val OPEN_ITERATIONS = 10
        const val SCROLL_ITERATIONS = 5
        const val SWIPES = 6
        const val SWIPE_STEPS = 20

        /** Usa o perfil de abertura já instalado; sem perfil, a lib compila normalmente. */
        val COMPILED: CompilationMode =
            CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.UseIfAvailable)
    }
}
