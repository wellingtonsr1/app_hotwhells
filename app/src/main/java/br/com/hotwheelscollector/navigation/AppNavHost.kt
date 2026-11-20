package br.com.hotwheelscollector.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
import br.com.hotwheelscollector.feature.catalog.ModelFormScreen
import br.com.hotwheelscollector.feature.catalog.VariantEditScreen
import br.com.hotwheelscollector.feature.catalog.VariantFormScreen
import br.com.hotwheelscollector.feature.dashboard.DashboardScreen
import br.com.hotwheelscollector.feature.duplicates.DuplicatesScreen
import br.com.hotwheelscollector.feature.duplicates.NegotiationScreen
import br.com.hotwheelscollector.feature.finance.FinanceScreen
import br.com.hotwheelscollector.feature.locations.LocationContentsScreen
import br.com.hotwheelscollector.feature.locations.LocationsScreen
import br.com.hotwheelscollector.feature.organization.CollectionDetailScreen
import br.com.hotwheelscollector.feature.organization.CollectionFormScreen
import br.com.hotwheelscollector.feature.organization.CollectionsScreen
import br.com.hotwheelscollector.feature.organization.SeriesScreen
import br.com.hotwheelscollector.feature.search.SearchScreen
import br.com.hotwheelscollector.feature.settings.AboutScreen
import br.com.hotwheelscollector.feature.settings.PrivacyPolicyScreen
import br.com.hotwheelscollector.feature.settings.SettingsRoute
import br.com.hotwheelscollector.feature.transfer.BackupScreen
import br.com.hotwheelscollector.feature.wishlist.WishlistFormScreen
import br.com.hotwheelscollector.feature.wishlist.WishlistScreen

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

    /** Edição do modelo e do veículo real que ele representa (T043). */
    public data object ModelForm : Destination("catalog/model/{${RouteArgs.MODEL_ID}}") {
        public fun routeFor(modelId: String): String = "catalog/model/$modelId"
    }

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
    /** Coleções com progresso (US5): abertas do cabeçalho do catálogo. */
    public data object Collections : Destination("catalog/collections")

    /** Detalhe da coleção: percentual, faltantes e itens. */
    public data object CollectionDetail :
        Destination("collections/{${RouteArgs.COLLECTION_ID}}") {
        public fun routeFor(collectionId: String): String = "collections/$collectionId"
    }

    /** Formulário de coleção; sem argumento cria, com argumento edita. */
    public data object CollectionForm :
        Destination("collections/form?${RouteArgs.COLLECTION_ID}={${RouteArgs.COLLECTION_ID}}") {
        public fun routeFor(collectionId: String? = null): String =
            if (collectionId == null) {
                "collections/form"
            } else {
                "collections/form?${RouteArgs.COLLECTION_ID}=$collectionId"
            }
    }

    /** Séries do fabricante, com criação, renomeação e arquivamento (FR-048–FR-049). */
    public data object Series : Destination("catalog/series")

    /** Duplicados por variante, com disponibilidade e negociação (US7). */
    public data object Duplicates : Destination("duplicates")

    /** Negociação e histórico de um exemplar duplicado (US7). */
    public data object Negotiation :
        Destination("duplicates/{${RouteArgs.EXEMPLAR_ID}}/negotiation") {
        public fun routeFor(exemplarId: String): String =
            "duplicates/$exemplarId/negotiation"
    }

    /** Lista de desejos (US6). */
    public data object Wishlist : Destination("wishlist")

    /** Formulário de desejo; sem argumento cria um novo, com argumento edita o existente. */
    public data object WishlistForm : Destination("wishlist/form?${RouteArgs.WISHLIST_ITEM_ID}={${RouteArgs.WISHLIST_ITEM_ID}}") {
        public fun routeFor(itemId: String? = null): String =
            if (itemId == null) "wishlist/form" else "wishlist/form?${RouteArgs.WISHLIST_ITEM_ID}=$itemId"
    }

    public data object Finance : Destination("finance")

    /**
     * Edição de variante incompleta: cor principal e ano (FR-029).
     *
     * O sufixo `/edit` não é decorativo: `VariantForm` já ocupa
     * `catalog/variant/{modelId}` e as duas rotas seriam idênticas para o NavHost, que
     * resolveria a edição para a tela de criação.
     */
    public data object VariantEdit : Destination("catalog/variant/{${RouteArgs.VARIANT_ID}}/edit") {
        public fun routeFor(variantId: String): String = "catalog/variant/$variantId/edit"
    }

    /** Árvore de localizações físicas (US9). */
    public data object Locations : Destination("locations")

    /** Inventário da localização e das sublocalizações (FR-094–FR-095). */
    public data object LocationContents : Destination("locations/{${RouteArgs.LOCATION_ID}}") {
        public fun routeFor(locationId: String): String = "locations/$locationId"
    }

    /** Ajustes: hub das opções do app (T132, T133). */
    public data object Settings : Destination("settings")

    /** Backup manual: pacote com banco, fotos e manifesto (T119/T120). */
    public data object Backup : Destination("settings/backup")
    public data object SettingsAbout : Destination("settings/about")
    public data object SettingsPrivacy : Destination("settings/privacy")
}

/** Nomes dos argumentos de navegação, iguais às chaves lidas do `SavedStateHandle`. */
public object RouteArgs {
    public const val EXEMPLAR_ID: String = "exemplarId"
    public const val VARIANT_ID: String = "variantId"
    public const val MODEL_ID: String = "modelId"
    public const val PHOTO_ID: String = "photoId"
    public const val WISHLIST_ITEM_ID: String = "itemId"
    public const val COLLECTION_ID: String = "collectionId"
    public const val LOCATION_ID: String = "locationId"

    /** Confirmação que a tela de edição devolve para a lista de incompletos. */
    public const val INCOMPLETE_MESSAGE: String = "incompleteMessage"
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
                    onOpenCollections = { navController.navigate(Destination.Collections.route) },
                    onOpenDuplicates = { navController.navigate(Destination.Duplicates.route) },
                )
            }
            composable(Destination.Duplicates.route) {
                DuplicatesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenNegotiation = { exemplarId ->
                        navController.navigate(Destination.Negotiation.routeFor(exemplarId))
                    },
                )
            }
            composable(
                route = Destination.Negotiation.route,
                arguments = listOf(
                    navArgument(RouteArgs.EXEMPLAR_ID) { type = NavType.StringType },
                ),
            ) {
                NegotiationScreen(onBack = { navController.popBackStack() })
            }
            composable(Destination.Collections.route) {
                CollectionsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCollection = { collectionId ->
                        navController.navigate(Destination.CollectionDetail.routeFor(collectionId))
                    },
                    onCreate = { navController.navigate(Destination.CollectionForm.routeFor()) },
                    onOpenSeries = { navController.navigate(Destination.Series.route) },
                )
            }
            composable(
                route = Destination.CollectionDetail.route,
                arguments = listOf(
                    navArgument(RouteArgs.COLLECTION_ID) { type = NavType.StringType },
                ),
            ) { entry ->
                CollectionDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { collectionId ->
                        navController.navigate(Destination.CollectionForm.routeFor(collectionId))
                    },
                )
            }
            composable(
                route = Destination.CollectionForm.route,
                arguments = listOf(
                    navArgument(RouteArgs.COLLECTION_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                CollectionFormScreen(onDone = { navController.popBackStack() })
            }
            composable(Destination.Series.route) {
                SeriesScreen(onBack = { navController.popBackStack() })
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
            composable(Destination.Incomplete.route) { entry ->
                // A confirmação vem da tela que editou: ela sai antes de a mensagem ser
                // lida, então o resultado viaja pelo SavedStateHandle da própria lista.
                val messages = remember(entry) {
                    entry.savedStateHandle.getStateFlow<String?>(RouteArgs.INCOMPLETE_MESSAGE, null)
                }
                val message by messages.collectAsState()
                IncompleteEntriesScreen(
                    onSelectModel = { modelId ->
                        navController.navigate(Destination.VariantForm.routeFor(modelId))
                    },
                    onEditModel = { modelId ->
                        navController.navigate(Destination.ModelForm.routeFor(modelId))
                    },
                    onEditVariant = { variantId ->
                        navController.navigate(Destination.VariantEdit.routeFor(variantId))
                    },
                    message = message,
                    onMessageShown = {
                        entry.savedStateHandle[RouteArgs.INCOMPLETE_MESSAGE] = null
                    },
                )
            }
            composable(
                route = Destination.VariantEdit.route,
                arguments = listOf(navArgument(RouteArgs.VARIANT_ID) { type = NavType.StringType }),
            ) {
                val variantUpdated = stringResource(R.string.incomplete_variant_updated)
                VariantEditScreen(
                    variantId = it.arguments?.getString(RouteArgs.VARIANT_ID).orEmpty(),
                    onSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            RouteArgs.INCOMPLETE_MESSAGE,
                            variantUpdated,
                        )
                        navController.popBackStack()
                    },
                )
            }
            composable(
                route = Destination.ModelForm.route,
                arguments = listOf(
                    navArgument(RouteArgs.MODEL_ID) { type = NavType.StringType },
                ),
            ) {
                val modelUpdated = stringResource(R.string.incomplete_model_updated)
                ModelFormScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            RouteArgs.INCOMPLETE_MESSAGE,
                            modelUpdated,
                        )
                        navController.popBackStack()
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
                WishlistScreen(
                    onAdd = { navController.navigate(Destination.WishlistForm.routeFor()) },
                    onEdit = { itemId ->
                        navController.navigate(Destination.WishlistForm.routeFor(itemId))
                    },
                )
            }
            composable(
                route = Destination.WishlistForm.route,
                arguments = listOf(
                    navArgument(RouteArgs.WISHLIST_ITEM_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                WishlistFormScreen(onSaved = { navController.popBackStack() })
            }
            composable(Destination.Finance.route) {
                FinanceScreen()
            }
            composable(Destination.Locations.route) {
                LocationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenContents = { locationId ->
                        navController.navigate(Destination.LocationContents.routeFor(locationId))
                    },
                )
            }
            composable(
                route = Destination.LocationContents.route,
                arguments = listOf(
                    navArgument(RouteArgs.LOCATION_ID) { type = NavType.StringType },
                ),
            ) {
                LocationContentsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenExemplar = { exemplarId ->
                        navController.navigate(Destination.ExemplarDetail.routeFor(exemplarId))
                    },
                )
            }
            composable(Destination.Settings.route) {
                SettingsRoute(
                    onAbout = { navController.navigate(Destination.SettingsAbout.route) },
                    onPrivacy = { navController.navigate(Destination.SettingsPrivacy.route) },
                    onLocations = { navController.navigate(Destination.Locations.route) },
                    onBackup = { navController.navigate(Destination.Backup.route) },
                )
            }
            composable(Destination.Backup.route) {
                BackupScreen(onBack = { navController.popBackStack() })
            }
            composable(Destination.SettingsAbout.route) {
                AboutScreen()
            }
            composable(Destination.SettingsPrivacy.route) {
                PrivacyPolicyScreen()
            }
        }
    }
}
