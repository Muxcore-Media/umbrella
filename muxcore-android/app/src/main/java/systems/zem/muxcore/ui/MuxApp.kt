package systems.zem.muxcore.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import systems.zem.muxcore.AppContainer
import systems.zem.muxcore.data.Capabilities
import systems.zem.muxcore.ui.screens.CollectionsScreen
import systems.zem.muxcore.ui.screens.DiscoverDetailScreen
import systems.zem.muxcore.ui.screens.DiscoverScreen
import systems.zem.muxcore.ui.screens.FavoritesScreen
import systems.zem.muxcore.ui.screens.ForgotPasswordScreen
import systems.zem.muxcore.ui.screens.HomeScreen
import systems.zem.muxcore.ui.screens.InProgressScreen
import systems.zem.muxcore.ui.screens.InviteJoinScreen
import systems.zem.muxcore.ui.screens.LibraryScreen
import systems.zem.muxcore.ui.screens.LoginFlow
import systems.zem.muxcore.ui.screens.MovieDetailScreen
import systems.zem.muxcore.ui.screens.MoviesScreen
import systems.zem.muxcore.ui.screens.PlayerScreen
import systems.zem.muxcore.ui.screens.PlaylistsScreen
import systems.zem.muxcore.ui.screens.QueueScreen
import systems.zem.muxcore.ui.screens.QuickConnectScreen
import systems.zem.muxcore.ui.screens.SearchScreen
import systems.zem.muxcore.ui.screens.SettingsScreen
import systems.zem.muxcore.ui.screens.StudiosScreen
import systems.zem.muxcore.ui.screens.TVShowDetailScreen
import systems.zem.muxcore.ui.screens.TVShowsScreen
import systems.zem.muxcore.ui.screens.UpcomingScreen
import systems.zem.muxcore.ui.screens.WatchlistScreen
import systems.zem.muxcore.ui.theme.MuxTheme

val LocalCapabilities = androidx.compose.runtime.staticCompositionLocalOf { Capabilities.DEFAULT }

@Composable
fun MuxApp(
    container: AppContainer,
    inviteToken: String?,
    onInviteConsumed: () -> Unit,
) {
    CompositionLocalProvider(LocalAppContainer provides container) {
        val sessionState by container.session.state.collectAsStateWithLifecycle(
            initialValue = container.session.cache,
        )
        val blob by container.userData.blob.collectAsStateWithLifecycle()

        MuxTheme(themePref = blob.prefs.display.theme) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (!sessionState.loggedIn) {
                    LoginFlow(inviteToken = inviteToken, onInviteConsumed = onInviteConsumed)
                } else {
                    LoggedInApp(container, inviteToken, onInviteConsumed)
                }
            }
        }
    }
}

private data class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector,
)

private val topDestinations = listOf(
    TopDestination(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    TopDestination(Routes.MOVIES, "Movies", Icons.Outlined.Movie, Icons.Filled.Movie),
    TopDestination(Routes.TV, "Shows", Icons.Outlined.LiveTv, Icons.Filled.LiveTv),
    TopDestination("search?q=&scope=all", "Search", Icons.Outlined.Search, Icons.Filled.Search),
    TopDestination(Routes.LIBRARY, "Library", Icons.Outlined.VideoLibrary, Icons.Filled.VideoLibrary),
)

@Composable
private fun LoggedInApp(
    container: AppContainer,
    inviteToken: String?,
    onInviteConsumed: () -> Unit,
) {
    val nav = rememberNavController()
    val capsState = remember { mutableStateOf(Capabilities.DEFAULT) }

    LaunchedEffect(Unit) {
        runCatching { capsState.value = container.api.getCapabilities() }
        container.userData.pullFromServer()
    }
    LaunchedEffect(inviteToken) {
        if (inviteToken != null) {
            nav.navigate("invite/$inviteToken")
            onInviteConsumed()
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute != null && !currentRoute.startsWith("player")

    CompositionLocalProvider(LocalCapabilities provides capsState.value) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        topDestinations.forEach { dest ->
                            val baseRoute = dest.route.substringBefore('?')
                            val selected = currentRoute?.substringBefore('?') == baseRoute
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    nav.navigate(dest.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        if (selected) dest.activeIcon else dest.icon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            MuxNavHost(
                nav = nav,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding()),
            )
        }
    }
}

@Composable
private fun MuxNavHost(nav: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) },
    ) {
        composable(Routes.HOME) { HomeScreen(nav) }
        composable(Routes.MOVIES) { MoviesScreen(nav) }
        composable(Routes.TV) { TVShowsScreen(nav) }
        composable(
            Routes.SEARCH,
            arguments = listOf(
                navArgument("q") { type = NavType.StringType; defaultValue = "" },
                navArgument("scope") { type = NavType.StringType; defaultValue = "all" },
            ),
        ) { entry ->
            SearchScreen(
                nav,
                initialQuery = entry.arguments?.getString("q").orEmpty(),
                initialScope = entry.arguments?.getString("scope") ?: "all",
            )
        }
        composable(Routes.LIBRARY) { LibraryScreen(nav) }
        composable(Routes.MOVIE_DETAIL) { entry ->
            MovieDetailScreen(nav, id = entry.arguments?.getString("id").orEmpty())
        }
        composable(Routes.TV_DETAIL) { entry ->
            TVShowDetailScreen(nav, id = entry.arguments?.getString("id").orEmpty())
        }
        composable(Routes.DISCOVER) { DiscoverScreen(nav) }
        composable(
            Routes.DISCOVER_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) { entry ->
            DiscoverDetailScreen(
                nav,
                type = entry.arguments?.getString("type").orEmpty(),
                id = entry.arguments?.getInt("id") ?: 0,
            )
        }
        composable(Routes.WATCHLIST) { WatchlistScreen(nav) }
        composable(Routes.FAVORITES) { FavoritesScreen(nav) }
        composable(Routes.QUEUE) { QueueScreen(nav) }
        composable(Routes.REQUESTS) { InProgressScreen(nav) }
        composable(Routes.COLLECTIONS) { CollectionsScreen(nav) }
        composable(Routes.UPCOMING) { UpcomingScreen(nav) }
        composable(Routes.STUDIOS) { StudiosScreen(nav) }
        composable(Routes.PLAYLISTS) { PlaylistsScreen(nav) }
        composable(Routes.SETTINGS) { SettingsScreen(nav) }
        composable(Routes.QUICK_CONNECT) { QuickConnectScreen(nav) }
        composable(Routes.FORGOT_PASSWORD) { ForgotPasswordScreen(nav) }
        composable(Routes.INVITE) { entry ->
            InviteJoinScreen(nav, token = entry.arguments?.getString("token").orEmpty())
        }
        composable(
            Routes.PLAYER,
            arguments = listOf("src", "title", "id", "kind", "poster", "showId", "season", "episode").map {
                navArgument(it) { type = NavType.StringType; defaultValue = "" }
            },
        ) { entry ->
            val args = entry.arguments
            PlayerScreen(
                nav = nav,
                src = args?.getString("src").orEmpty(),
                title = args?.getString("title").orEmpty(),
                mediaId = args?.getString("id").orEmpty(),
                kind = args?.getString("kind") ?: "movie",
                poster = args?.getString("poster").orEmpty(),
                showId = args?.getString("showId").orEmpty(),
                seasonArg = args?.getString("season").orEmpty(),
                episodeArg = args?.getString("episode").orEmpty(),
            )
        }
    }
}
