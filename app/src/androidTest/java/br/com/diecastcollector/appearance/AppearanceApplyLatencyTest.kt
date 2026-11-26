package br.com.diecastcollector.appearance

import android.os.SystemClock
import android.view.ViewTreeObserver
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.diecastcollector.MainActivity
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tempo de aplicação do tema (Spec 006, T420, NFR-401).
 *
 * Mede, no app real (DataStore → `MainActivity` → `DiecastTheme`), o intervalo entre o
 * toque na opção de aparência e o primeiro quadro desenhado com a nova paleta. O quadro é
 * cronometrado por um `ViewTreeObserver.OnDrawListener` no `decorView` da activity: o
 * instante gravado é o do *draw* efetivo, não o do fim da sincronização do teste.
 *
 * Cada iteração confere a paleta no bitmap renderizado, para que a medida não seja aceita
 * se a tela não tiver mudado de fato. O limite é aplicado à **mediana** das iterações: um
 * pico isolado do emulador não invalida a medição, mas a mediana acima de 100 ms invalida.
 *
 * Resultado medido fica no relatório `app/build/reports/androidTests/connected/…` e em
 * `docs/validacao.md` §11.5.2 (T420).
 */
@RunWith(AndroidJUnit4::class)
public class AppearanceApplyLatencyTest {

    @get:Rule
    public val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    public fun temaEAplicadoEmMenosDeCemMilissegundos() {
        AppearanceActivitySupport.openSettings(composeRule)

        // Aquece o caminho (primeira escrita e primeira recomposição de tema não entram
        // na medição; o critério é sobre a troca percebida pelo usuário).
        AppearanceActivitySupport.selectMode(composeRule, "Escuro")
        AppearanceActivitySupport.selectMode(composeRule, "Claro")
        composeRule.waitForIdle()

        val armed = AtomicBoolean(false)
        val drawnAt = AtomicLong(0)
        val listener = ViewTreeObserver.OnDrawListener {
            if (armed.compareAndSet(true, false)) {
                drawnAt.set(SystemClock.uptimeMillis())
            }
        }
        composeRule.runOnUiThread {
            composeRule.activity.window.decorView.viewTreeObserver.addOnDrawListener(listener)
        }

        val medidas = mutableListOf<Long>()
        var tentativas = 0
        try {
            // A troca de modo noturno do sistema gera uma configuração; um toque injetado
            // durante esse intervalo pode ser descartado. Só entram na medida os toques que
            // de fato trocaram a opção — tentativas descartadas são repetidas, não contadas.
            while (medidas.size < ITERACOES && tentativas < ITERACOES * 3) {
                tentativas++
                val destino = if (medidas.size % 2 == 0) "Escuro" else "Claro"
                val escuro = destino == "Escuro"

                // Deixa a opção na viewport fora do cronômetro: rolar também desenha.
                composeRule.onNodeWithText(destino).performScrollTo()
                composeRule.waitForIdle()

                composeRule.runOnUiThread {
                    drawnAt.set(0)
                    armed.set(true)
                }
                val toque = SystemClock.uptimeMillis()
                composeRule.onNodeWithText(destino).performClick()

                val selecionou = runCatching {
                    composeRule.waitUntil(TIMEOUT_SELECAO_MS) {
                        runCatching {
                            composeRule.onNodeWithText(destino).assertIsSelected()
                            true
                        }.getOrDefault(false)
                    }
                }.isSuccess
                composeRule.waitForIdle()
                if (!selecionou) continue

                val quadro = drawnAt.get()
                assertTrue(
                    "Nenhum quadro foi desenhado depois do toque em «$destino»",
                    quadro > 0,
                )
                medidas += quadro - toque

                // A medida só vale se o quadro desenhado for o da nova paleta.
                AppearanceActivitySupport.assertModePalette(composeRule, destino, escuro)
            }
        } finally {
            composeRule.runOnUiThread {
                composeRule.activity.window.decorView.viewTreeObserver
                    .removeOnDrawListener(listener)
            }
        }

        assertEquals(
            "Toques descartados demais: só ${medidas.size} de $ITERACOES foram medidos",
            ITERACOES,
            medidas.size,
        )
        val ordenadas = medidas.sorted()
        val mediana = ordenadas[ordenadas.size / 2]
        val relatorio = "NFR-401: $medidas ms por toque; mediana ${mediana} ms"
        println(relatorio)
        assertTrue("$relatorio — limite de 100 ms", mediana < LIMITE_MS)
    }

    private companion object {
        const val ITERACOES = 6
        const val LIMITE_MS = 100L
        const val TIMEOUT_SELECAO_MS = 3_000L
    }
}
