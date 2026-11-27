package br.com.diecastcollector

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import br.com.diecastcollector.appearance.AppearancePreferences
import br.com.diecastcollector.appearance.ApplicationNightMode
import br.com.diecastcollector.feature.welcome.WELCOME_SKIP_PROPERTY
import br.com.diecastcollector.feature.welcome.WelcomeScreen
import br.com.diecastcollector.navigation.AppNavHost
import br.com.diecastcollector.ui.components.ProvideWindowSizeClass
import br.com.diecastcollector.ui.theme.DiecastTheme
import br.com.diecastcollector.ui.theme.ThemeMode
import br.com.diecastcollector.ui.theme.isDark
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appearance: AppearancePreferences

    @Inject
    lateinit var applicationNightMode: ApplicationNightMode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Primeiro quadro seguro: se a preferência ainda não chegou do disco, as barras
        // seguem o Android até o valor real ser lido (NFR-403).
        enableEdgeToEdge()
        setContent {
            // Sem `runBlocking`: o primeiro valor é SYSTEM e a leitura chega em
            // milissegundos, sem atrasar a abertura (FR-413, NFR-402).
            val mode by appearance.mode.collectAsState(initial = ThemeMode.SYSTEM)
            val dark = mode.isDark(isSystemInDarkTheme())

            // Abertura com a arte da marca em toda inicialização: o painel só aparece
            // depois do toque em Começar. Voltar do segundo plano não repete a tela
            // (`rememberSaveable`); um processo novo começa nela de novo. A suíte
            // instrumentada liga a propriedade antes de abrir a activity.
            var welcomeOpen by rememberSaveable {
                mutableStateOf(!skipWelcomeForTests())
            }

            // FR-410/NFR-403: espelha o modo no mecanismo de modo noturno do sistema, que
            // o Android aplica **antes** de criar o processo — assim o splash e a janela
            // já usam `values`/`values-night` da aparência escolhida. O DataStore continua
            // sendo a única fonte da preferência; repetir o mesmo modo é inócuo.
            LaunchedEffect(mode) { applicationNightMode.apply(mode) }

            // FR-409: os ícones das barras seguem a aparência do app, não a do Android —
            // inclusive quando o usuário força um modo oposto ao do sistema.
            SideEffect {
                // A arte da apresentação é escura: sobre ela os ícones precisam ser claros
                // mesmo no tema claro.
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { dark || welcomeOpen },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { dark || welcomeOpen },
                )
            }

            DiecastTheme(mode = mode) {
                if (welcomeOpen) {
                    WelcomeScreen(onStart = { welcomeOpen = false })
                } else {
                    Navigation()
                }
            }
        }
    }
}

/**
 * Navegação principal.
 *
 * A classe de janela é medida uma vez, no contêiner que o app recebeu: em duas janelas
 * cada lado tem a sua (FR-301, FR-304).
 */
@Composable
private fun Navigation() {
    ProvideWindowSizeClass {
        AppNavHost()
    }
}

/**
 * Saída só para a instrumentação: `createAndroidComposeRule<MainActivity>` abre a activity
 * com o intent padrão e não teria como responder à apresentação antes do primeiro teste.
 * Fora da suíte, a propriedade nunca existe.
 */
private fun skipWelcomeForTests(): Boolean =
    System.getProperty(WELCOME_SKIP_PROPERTY) == "true"

@Preview(showBackground = true)
@Composable
private fun AppNavHostPreview() {
    DiecastTheme {
        AppNavHost()
    }
}
