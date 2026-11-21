package br.com.hotwheelscollector.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Perfil de abertura: processo frio até o painel exibir os primeiros números (SC-014).
 *
 * O painel é a primeira tela e concentra o custo de inicialização: Hilt, Compose, Room e
 * as consultas agregadas. Gerar o perfil a partir daqui aquece exatamente o que o usuário
 * espera ao tocar no ícone.
 */
@RunWith(AndroidJUnit4::class)
class StartupBaselineProfile {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun gerarPerfilDeAbertura() {
        rule.collect(
            packageName = "br.com.hotwheelscollector",
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            // Espera o painel terminar de carregar, para o perfil cobrir a primeira leitura.
            device.waitForIdle(3_000)
        }
    }
}
