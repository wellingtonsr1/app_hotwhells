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
import br.com.hotwheelscollector.feature.catalog.IncompleteEntriesScreen
import br.com.hotwheelscollector.feature.catalog.VariantFormScreen

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

    /** Primeiro passo da miniatura: fabricante, modelo e escala. */
    public data object CreateEntry : Destination("catalog/create")

    /**
     * Cadastro completo a partir de um modelo já resolvido: variante, primeiro exemplar
     * e fotos. As fotos pertencem ao exemplar, por isso ele nasce nesta mesma tela.
     */
    public data object VariantForm : Destination("catalog/variant/{${RouteArgs.MODEL_ID}}") {
        public fun routeFor(modelId: String): String = "catalog/variant/$modelId"
    }

    /** Modelos que pararam no primeiro passo e ainda não têm variante (FR-029). */
    public data object Incomplete : Destination("catalog/incomplete")

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
        // Cadastro da miniatura em dois passos: o modelo é resolvido primeiro e a
        // variante (com o primeiro exemplar e as fotos) vem em seguida.
        composable(Destination.Dashboard.route) {
            CatalogScreen(
                onVariantClick = { variantId ->
                    navController.navigate(Destination.AddExemplar.routeFor(variantId))
                },
                onCreateEntry = { navController.navigate(Destination.CreateEntry.route) },
                onOpenIncomplete = { navController.navigate(Destination.Incomplete.route) },
            )
        }
        composable(Destination.Incomplete.route) {
            IncompleteEntriesScreen(
                onSelectModel = { modelId ->
                    navController.navigate(Destination.VariantForm.routeFor(modelId))
                },
            )
        }
        composable(Destination.CreateEntry.route) {
            CreateCatalogEntryScreen(
                onModelCreated = { modelId ->
                    // Cadastrar a miniatura é uma intenção só: o modelo resolvido segue
                    // direto para a variante, que cria o exemplar e recebe as fotos.
                    navController.popBackStack()
                    navController.navigate(Destination.VariantForm.routeFor(modelId))
                },
            )
        }
        composable(
            route = Destination.VariantForm.route,
            arguments = listOf(navArgument(RouteArgs.MODEL_ID) { type = NavType.StringType }),
        ) { entry ->
            VariantFormScreen(
                modelId = entry.arguments?.getString(RouteArgs.MODEL_ID).orEmpty(),
                // Volta para quem abriu: catálogo no cadastro novo, lista de incompletos
                // quando a variante está sendo criada para completar um modelo órfão.
                onVariantSaved = { navController.popBackStack() },
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
