package br.com.hotwheelscollector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import br.com.hotwheelscollector.navigation.AppNavHost
import br.com.hotwheelscollector.ui.theme.HotWheelsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HotWheelsTheme {
                AppNavHost()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppNavHostPreview() {
    HotWheelsTheme {
        AppNavHost()
    }
}
