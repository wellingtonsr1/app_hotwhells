package br.com.diecastcollector.appearance

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import br.com.diecastcollector.feature.settings.SettingsScreen
import br.com.diecastcollector.ui.theme.DiecastTheme
import br.com.diecastcollector.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Seção Aparência (Spec 006, US1): três opções, seleção única, padrão Sistema, alvos
 * mínimos e aplicação da paleta em cada modo.
 */
public class AppearanceSettingsTest {

    @get:Rule
    public val composeRule: androidx.compose.ui.test.junit4.ComposeContentTestRule =
        createComposeRule()

    @Test
    public fun mostraAsTresOpcoesComSistemaSelecionadoPorPadrao() {
        composeRule.setContent {
            DiecastTheme {
                SettingsScreen(onAbout = {}, onPrivacy = {})
            }
        }

        composeRule.onNodeWithText("Aparência").assertIsDisplayed()
        composeRule.onNodeWithText("Sistema").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithText("Claro").assertIsDisplayed()
        composeRule.onNodeWithText("Escuro").assertIsDisplayed()
    }

    @Test
    public fun tocarNaLinhaSelecionaAOpcaoEInformaOCallback() {
        var escolhido: ThemeMode? = null

        composeRule.setContent {
            DiecastTheme {
                SettingsScreen(
                    onAbout = {},
                    onPrivacy = {},
                    appearance = ThemeMode.SYSTEM,
                    onAppearanceChange = { escolhido = it },
                )
            }
        }

        composeRule.onNodeWithText("Escuro").performClick()

        assertEquals(ThemeMode.DARK, escolhido)
        // A marca de seleção reflete o modo efetivamente aplicado, não o toque.
        composeRule.onNodeWithText("Sistema").assertIsSelected()
    }

    @Test
    public fun opcaoAtivaFicaMarcada() {
        composeRule.setContent {
            DiecastTheme {
                SettingsScreen(
                    onAbout = {},
                    onPrivacy = {},
                    appearance = ThemeMode.LIGHT,
                )
            }
        }

        composeRule.onNodeWithText("Claro").assertIsSelected()
    }

    @Test
    public fun cadaOpcaoTemAlvoDeToqueMinimo() {
        composeRule.setContent {
            DiecastTheme {
                SettingsScreen(onAbout = {}, onPrivacy = {})
            }
        }

        composeRule.onNodeWithText("Sistema").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Claro").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Escuro").assertHeightIsAtLeast(48.dp)
    }

    /**
     * T409, fonte 200%: nenhuma linha da seção trunca. Olha o texto realmente desenhado,
     * não a semântica — o `text` do nó continua completo mesmo quando a linha é cortada
     * com reticências.
     *
     * O sinal usado é `getLineEnd(última, visibleEnd = true)` contra o tamanho do texto:
     * quando falta trecho, esse índice para antes do fim. `hasVisualOverflow` /
     * `didOverflowWidth` **não** servem aqui: ficam verdadeiros quando o texto preenche
     * exatamente a largura medida (arredondamento), o que acontece com "Aparência" a
     * 200% — conferido na tela do emulador, sem reticências.
     */
    @Test
    public fun fonteDuzentosPorCentoNaoTruncaOsRotulos() {
        composeRule.setContent {
            val escalaDensidade = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(density = escalaDensidade, fontScale = 2f),
            ) {
                DiecastTheme {
                    SettingsScreen(onAbout = {}, onPrivacy = {})
                }
            }
        }

        listOf(
            "Aparência",
            "Sistema acompanha o Android; Claro e Escuro valem sempre.",
            "Sistema",
            "Claro",
            "Escuro",
        ).forEach(::assertNaoTrunca)

        // Cada opção continua com alvo de toque mínimo mesmo com a fonte dobrada.
        composeRule.onNodeWithText("Escuro").assertHeightIsAtLeast(48.dp)
    }

    /**
     * T409, 1.000 dp: a seção herda a coluna de leitura de 720 dp (`formMaxWidth`) em vez
     * de esticar até a borda, e fica centralizada.
     */
    @Test
    public fun janelaDeMilDpMantemAColunaDeLeituraCentralizada() {
        // Densidade 1: 1.000 dp cabem nos 1.080 px do emulador, então a largura pedida é
        // a largura medida de verdade.
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1f)) {
                Box(Modifier.width(1000.dp).fillMaxHeight()) {
                    DiecastTheme {
                        SettingsScreen(onAbout = {}, onPrivacy = {})
                    }
                }
            }
        }

        // A linha inteira é um cartão tocável: o nó mesclado tem a largura do cartão.
        val cartao = composeRule.onNodeWithText("Localizações")
        cartao.assertIsDisplayed()

        val limites = cartao.getUnclippedBoundsInRoot()
        val largura = limites.right - limites.left
        assertTrue("cartão esticou até $largura", largura <= 720.dp)
        assertTrue("cartão encolheu para $largura", largura >= 640.dp)
        val sobraEsquerda = limites.left
        val sobraDireita = 1000.dp - limites.right
        assertTrue(
            "coluna descentralizada: $sobraEsquerda à esquerda e $sobraDireita à direita",
            abs((sobraEsquerda - sobraDireita).value) <= 1f,
        )
    }

    @Test
    public fun avisoDeFalhaDeEscritaApareceSemBloquear() {
        composeRule.setContent {
            DiecastTheme {
                SettingsScreen(onAbout = {}, onPrivacy = {}, appearanceWriteFailed = true)
            }
        }

        composeRule
            .onNodeWithText("Não foi possível salvar a aparência", substring = true)
            .assertIsDisplayed()
    }

    @Test
    public fun modoForcadoPrevaleceSobreOTemaDoAndroid() {
        // Um único setContent: 3 amostras em composições aninhadas, cada uma com a
        // configuração do Android que queremos simular.
        val cores = mutableMapOf<String, ULong>()
        composeRule.setContent {
            Column {
                amostra(ThemeMode.LIGHT, androidEscuro = true) { cores["claro_android_escuro"] = it }
                amostra(ThemeMode.LIGHT, androidEscuro = false) { cores["claro_android_claro"] = it }
                amostra(ThemeMode.DARK, androidEscuro = false) { cores["escuro_android_claro"] = it }
            }
        }
        composeRule.waitForIdle()

        assertEquals(cores["claro_android_claro"], cores["claro_android_escuro"])
        assertNotEquals(cores["claro_android_claro"], cores["escuro_android_claro"])
    }

    private fun limitesDoNo(no: androidx.compose.ui.test.SemanticsNodeInteraction): String {
        val l = no.getUnclippedBoundsInRoot()
        return "no=${l.left}..${l.right} x ${l.top}..${l.bottom}"
    }

    private fun assertNaoTrunca(texto: String) {
        val no = composeRule.onNodeWithText(texto, useUnmergedTree = true)
        no.assertExists()
        no.performScrollTo()
        no.assertIsDisplayed()

        val resultados = mutableListOf<TextLayoutResult>()
        no.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(resultados) }
        val layout = resultados.single()
        val completo = layout.layoutInput.text.length
        val visivelAte = layout.getLineEnd(layout.lineCount - 1, visibleEnd = true)
        val diagnostico = "\"$texto\": linhas=${layout.lineCount} " +
            "tamanho=${layout.size} visivelAte=$visivelAte/$completo " +
            "transbordouLargura=${layout.didOverflowWidth} " +
            "cortado=${layout.hasVisualOverflow} (${limitesDoNo(no)})"
        assertTrue(diagnostico, layout.lineCount >= 1)
        assertEquals(diagnostico, completo, visivelAte)
    }

    @Composable
    private fun amostra(mode: ThemeMode, androidEscuro: Boolean, onFundo: (ULong) -> Unit) {
        val base = LocalConfiguration.current
        val config = Configuration(base).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (androidEscuro) {
                    Configuration.UI_MODE_NIGHT_YES
                } else {
                    Configuration.UI_MODE_NIGHT_NO
                }
        }
        CompositionLocalProvider(LocalConfiguration provides config) {
            DiecastTheme(mode = mode) {
                onFundo(MaterialTheme.colorScheme.background.value)
                Text("amostra")
            }
        }
    }
}
