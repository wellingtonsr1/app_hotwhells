package br.com.hotwheelscollector.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.feature.catalog.AddExemplarScreen
import br.com.hotwheelscollector.feature.catalog.CatalogScreen
import br.com.hotwheelscollector.feature.catalog.CreateCatalogEntryScreen

/**
 * Rotas do aplicativo.
 *
 * Os identificadores ficam em um único lugar para que argumentos e nomes de tela não
 * divirjam entre telas. A convenção de `SavedStateHandle` associada a cada rota está em
 * [RouteArgs]: quem navega usa `navigate(destination.route(argumentos))` e quem recebe lê
 * pelo nome declarado aqui, nunca por string solta.
 */
public sealed class Destination(public val route: String) {
    public data object Dashboard : Destination("dashboard")
    public data object Capture : Destination("capture")

    /** Cadastro mínimo de miniatura: fabricante, modelo e variante. */
    public data object CreateEntry : Destination("catalog/create")

    /**
     * Cadastro rápido de exemplar (US1). O `variantId` chega por argumento e é lido do
     * `SavedStateHandle` no ViewModel — a tela nunca pede a variante de novo.
     */
    public data object AddExemplar : Destination("exemplar/add/{${RouteArgs.VARIANT_ID}}") {
        public fun routeFor(variantId: String): String = "exemplar/add/$variantId"
    }
    public data object Wishlist : Destination("wishlist")
    public data object Finance : Destination("finance")
    public data object Settings : Destination("settings")
}

/** Nomes dos argumentos de navegação, iguais às chaves lidas do `SavedStateHandle`. */
public object RouteArgs {
    public const val EXEMPLAR_ID: String = "exemplarId"
    public const val VARIANT_ID: String = "variantId"
    public const val MODEL_ID: String = "modelId"
    public const val PHOTO_ID: String = "photoId"
}

@Composable
public fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: Destination = Destination.Dashboard,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination.route,
    ) {
        // As telas de verdade chegam com as histórias; por enquanto o esqueleto
        // mostra o estado da fundação, sem simular funcionalidade que não existe.
        composable(Destination.Dashboard.route) {
            CatalogScreen(
                onVariantClick = { variantId ->
                    navController.navigate(Destination.AddExemplar.routeFor(variantId))
                },
                onCreateEntry = { navController.navigate(Destination.CreateEntry.route) },
            )
        }
        composable(Destination.CreateEntry.route) {
            CreateCatalogEntryScreen(
                onCreated = { variantId ->
                    // Volta para a lista e já abre o cadastro de exemplar da nova variante:
                    // cadastrar a miniatura e registrar o exemplar é uma intenção só.
                    navController.popBackStack()
                    navController.navigate(Destination.AddExemplar.routeFor(variantId))
                },
            )
        }
        composable(Destination.Capture.route) { PlaceholderScreen() }
        composable(
            route = Destination.AddExemplar.route,
            arguments = listOf(navArgument(RouteArgs.VARIANT_ID) { type = NavType.StringType }),
        ) {
            AddExemplarScreen(
                onSaved = { navController.popBackStack() },
            )
        }
        composable(Destination.Wishlist.route) { PlaceholderScreen() }
        composable(Destination.Finance.route) { PlaceholderScreen() }
        composable(Destination.Settings.route) { PlaceholderScreen() }
    }
}

@Composable
private fun PlaceholderScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.setup_status_message),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
