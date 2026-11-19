package br.com.hotwheelscollector.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.feature.catalog.AddExemplarScreen
import br.com.hotwheelscollector.feature.catalog.CatalogScreen
import br.com.hotwheelscollector.feature.catalog.CreateCatalogEntryScreen
import br.com.hotwheelscollector.feature.catalog.ExemplarDetailScreen
import br.com.hotwheelscollector.feature.catalog.ExemplarListScreen
import br.com.hotwheelscollector.feature.catalog.IncompleteEntriesScreen
import br.com.hotwheelscollector.feature.catalog.VariantFormScreen
import br.com.hotwheelscollector.feature.dashboard.DashboardScreen
import br.com.hotwheelscollector.feature.finance.FinanceScreen
import br.com.hotwheelscollector.feature.search.SearchScreen

/**
 * Rotas do aplicativo.
 *
 * Os identificadores ficam em um único lugar para que argumentos e nomes de tela não
 * divirjam entre telas. A convenção de `SavedStateHandle` associada a cada rota está em
 * [RouteArgs]: quem navega usa `navigate(destination.route(argumentos))` e quem recebe lê
 * pelo nome declarado aqui, nunca por string solta.
 */
public sealed class Destination(public val route: String) {

    /** Painel do estado da coleção (US3): a primeira tela do app. */
    public data object Dashboard : Destination("dashboard")

    /** Acervo: a lista de miniaturas e o cadastro rápido (US1). */
    public data object Catalog : Destination("catalog")

    /** Busca e filtros do acervo (US4): tela própria, aberta do cabeçalho do catálogo. */
    public data object Search : Destination("catalog/search")

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

    /** Unidades físicas de uma variante: entrada da edição e da movimentação (FR-028). */
    public data object ExemplarList :
        Destination("catalog/variant/{${RouteArgs.VARIANT_ID}}/exemplars") {
        public fun routeFor(variantId: String): String = "catalog/variant/$variantId/exemplars"
    }

    /** Detalhe do exemplar: condição, embalagem, localização, histórico e arquivamento. */
    public data object ExemplarDetail : Destination("exemplar/{${RouteArgs.EXEMPLAR_ID}}") {
        public fun routeFor(exemplarId: String): String = "exemplar/$exemplarId"
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

/** Aba da barra inferior: rota, rótulo e ícone. Só rotas de primeiro nível entram aqui. */
private data class BottomTab(
    val destination: Destination,
    @StringRes val label: Int,
    val icon: ImageVector,
)

private val bottomTabs: List<BottomTab> = listOf(
    BottomTab(Destination.Dashboard, R.string.dashboard_tab, Icons.Filled.Home),
    BottomTab(Destination.Catalog, R.string.catalog_tab, Icons.Filled.ViewList),
    BottomTab(Destination.Wishlist, R.string.wishlist_tab, Icons.Filled.FavoriteBorder),
    BottomTab(Destination.Finance, R.string.finance_tab, Icons.Filled.AttachMoney),
    BottomTab(Destination.Settings, R.string.settings_tab, Icons.Filled.Settings),
)

@Composable
public fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: Destination = Destination.Dashboard,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        // O padding do Scaffold externo é só o da barra: as telas de dentro já tratam as
        // barras do sistema, e somar as duas daria margem dobrada no topo.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (bottomTabs.any { it.destination.route == currentRoute }) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy
                            ?.any { it.route == tab.destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(tab.destination.route) {
                                        // Trocar de aba não empilha: volta ao início do
                                        // grafo e preserva o estado de cada aba.
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.Dashboard.route) {
                DashboardScreen()
            }
            composable(Destination.Catalog.route) {
                CatalogScreen(
                    onVariantClick = { variantId ->
                        navController.navigate(Destination.AddExemplar.routeFor(variantId))
                    },
                    onOpenExemplars = { variantId ->
                        navController.navigate(Destination.ExemplarList.routeFor(variantId))
                    },
                    onCreateEntry = { navController.navigate(Destination.CreateEntry.route) },
                    onOpenIncomplete = { navController.navigate(Destination.Incomplete.route) },
                    onOpenSearch = { navController.navigate(Destination.Search.route) },
                )
            }
            composable(Destination.Search.route) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onOpenExemplar = { exemplarId ->
                        navController.navigate(Destination.ExemplarDetail.routeFor(exemplarId))
                    },
                )
            }
            // Cadastro da miniatura em dois passos: o modelo é resolvido primeiro e a
            // variante (com o primeiro exemplar e as fotos) vem em seguida.
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
            composable(
                route = Destination.AddExemplar.route,
                arguments = listOf(navArgument(RouteArgs.VARIANT_ID) { type = NavType.StringType }),
            ) {
                AddExemplarScreen(
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(
                route = Destination.ExemplarList.route,
                arguments = listOf(navArgument(RouteArgs.VARIANT_ID) { type = NavType.StringType }),
            ) {
                ExemplarListScreen(
                    onAddExemplar = { variantId ->
                        navController.navigate(Destination.AddExemplar.routeFor(variantId))
                    },
                    onOpenExemplar = { exemplarId ->
                        navController.navigate(Destination.ExemplarDetail.routeFor(exemplarId))
                    },
                )
            }
            composable(
                route = Destination.ExemplarDetail.route,
                arguments = listOf(navArgument(RouteArgs.EXEMPLAR_ID) { type = NavType.StringType }),
            ) {
                ExemplarDetailScreen(
                    onDone = { navController.popBackStack() },
                )
            }
            composable(Destination.Wishlist.route) {
                PlaceholderScreen(
                    title = stringResource(R.string.placeholder_wishlist),
                    message = stringResource(R.string.placeholder_wishlist_body),
                )
            }
            composable(Destination.Finance.route) {
                FinanceScreen()
            }
            composable(Destination.Settings.route) {
                PlaceholderScreen(
                    title = stringResource(R.string.placeholder_settings),
                    message = stringResource(R.string.placeholder_settings_body),
                )
            }
        }
    }
}

/**
 * Tela ainda não implementada, mas alcançável pela barra: diz o nome da área e o que já
 * funciona no lugar dela. Placeholder mudo faria a aba parecer quebrada.
 */
@Composable
private fun PlaceholderScreen(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
