package br.com.diecastcollector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import br.com.diecastcollector.navigation.AppNavHost
import br.com.diecastcollector.ui.theme.DiecastTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiecastTheme {
                AppNavHost()
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
