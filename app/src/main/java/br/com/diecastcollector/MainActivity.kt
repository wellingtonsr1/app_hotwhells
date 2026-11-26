package br.com.diecastcollector

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import br.com.diecastcollector.appearance.AppearancePreferences
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

            // FR-409: os ícones das barras seguem a aparência do app, não a do Android —
            // inclusive quando o usuário força um modo oposto ao do sistema.
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { dark },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { dark },
                )
            }

            DiecastTheme(mode = mode) {
                // A classe de janela é medida uma vez, no contêiner que o app recebeu:
                // em duas janelas cada lado tem a sua (FR-301, FR-304).
                ProvideWindowSizeClass {
                    AppNavHost()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppNavHostPreview() {
    DiecastTheme {
        AppNavHost()
    }
}
