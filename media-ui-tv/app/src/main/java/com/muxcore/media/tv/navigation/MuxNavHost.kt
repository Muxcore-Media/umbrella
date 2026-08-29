package com.muxcore.media.tv.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.muxcore.media.tv.data.Capabilities
import com.muxcore.media.tv.data.MuxRepository
import com.muxcore.media.tv.data.PlayRequest
import com.muxcore.media.tv.data.PreferencesStore
import com.muxcore.media.tv.data.UserdataStore
import com.muxcore.media.tv.ui.screens.BookAuthorScreen
import com.muxcore.media.tv.ui.screens.BrowseHubScreen
import com.muxcore.media.tv.ui.screens.DiscoverDetailScreen
import com.muxcore.media.tv.ui.screens.StudiosScreen
import com.muxcore.media.tv.ui.screens.CollectionDetailScreen
import com.muxcore.media.tv.ui.screens.CollectionsScreen
import com.muxcore.media.tv.ui.screens.FavoritesScreen
import com.muxcore.media.tv.ui.screens.HomeScreen
import com.muxcore.media.tv.ui.screens.InProgressScreen
import com.muxcore.media.tv.ui.screens.LibraryRowsScreen
import com.muxcore.media.tv.ui.screens.LiveTvScreen
import com.muxcore.media.tv.ui.screens.MovieDetailScreen
import com.muxcore.media.tv.ui.screens.MixedScreen
import com.muxcore.media.tv.ui.screens.MoviesScreen
import com.muxcore.media.tv.ui.screens.MusicArtistScreen
import com.muxcore.media.tv.ui.screens.PairingScreen
import com.muxcore.media.tv.ui.screens.PlaylistsScreen
import com.muxcore.media.tv.ui.screens.PlayerScreen
import com.muxcore.media.tv.ui.screens.QueueScreen
import com.muxcore.media.tv.ui.screens.SearchScreen
import com.muxcore.media.tv.ui.screens.SettingsScreen
import com.muxcore.media.tv.ui.screens.SetupScreen
import com.muxcore.media.tv.ui.screens.TvDetailScreen
import com.muxcore.media.tv.ui.screens.TvShowsScreen
import com.muxcore.media.tv.ui.screens.UpcomingScreen

@Composable
fun MuxNavHost(
    navController: NavHostController,
    serverUrl: String,
    sessionToken: String,
    username: String,
    caps: Capabilities,
    repository: MuxRepository,
    preferences: PreferencesStore,
    userdata: UserdataStore,
    onLogout: () -> Unit,
) {
    val start = when {
        serverUrl.isBlank() -> "setup"
        sessionToken.isBlank() -> "pair"
        else -> "home"
    }

    fun navigatePlay(req: PlayRequest) {
        navController.navigate("player/${encodePlay(req)}")
    }

    NavHost(navController = navController, startDestination = start) {
        composable("setup") {
            SetupScreen(
                initialUrl = serverUrl,
                repository = repository,
                preferences = preferences,
                onConfigured = {
                    navController.navigate("pair") { popUpTo("setup") { inclusive = true } }
                },
            )
        }
        composable("pair") {
            PairingScreen(
                repository = repository,
                preferences = preferences,
                onPaired = {
                    navController.navigate("home") { popUpTo("pair") { inclusive = true } }
                },
                onChangeServer = { navController.navigate("setup") },
            )
        }
        composable("home") {
            HomeScreen(
                username = username,
                caps = caps,
                repository = repository,
                preferences = preferences,
                userdata = userdata,
                onNavigate = { route -> navController.navigate(route) },
                onPlay = ::navigatePlay,
                onLogout = onLogout,
            )
        }
        composable("browse") {
            BrowseHubScreen(
                caps = caps,
                onNavigate = { navController.navigate(it) },
                onBack = { navController.popBackStack() },
            )
        }
        composable("movies") {
            MoviesScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onMovie = { id -> navController.navigate("movie/$id") },
            )
        }
        composable("musicvideos") {
            MoviesScreen(
                repository = repository,
                library = "musicvideos",
                title = "Music Videos",
                onBack = { navController.popBackStack() },
                onMovie = { id -> navController.navigate("movie/$id") },
            )
        }
        composable("homevideos") {
            MoviesScreen(
                repository = repository,
                library = "homevideos",
                title = "Home Videos",
                onBack = { navController.popBackStack() },
                onMovie = { id -> navController.navigate("movie/$id") },
            )
        }
        composable("mixed") {
            MixedScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onMovie = { id -> navController.navigate("movie/$id") },
                onShow = { id -> navController.navigate("tvshow/$id") },
            )
        }
        composable(
            route = "movie/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) {
            val id = it.arguments?.getString("id") ?: return@composable
            MovieDetailScreen(
                movieId = id,
                repository = repository,
                userdata = userdata,
                onBack = { navController.popBackStack() },
                onPlay = ::navigatePlay,
            )
        }
        composable("tv") {
            TvShowsScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onShow = { id -> navController.navigate("tvshow/$id") },
            )
        }
        composable(
            route = "tvshow/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) {
            val id = it.arguments?.getString("id") ?: return@composable
            TvDetailScreen(
                showId = id,
                repository = repository,
                userdata = userdata,
                onBack = { navController.popBackStack() },
                onPlay = ::navigatePlay,
            )
        }
        composable("music") {
            LibraryRowsScreen(
                title = "Music",
                apiPath = "/api/music",
                repository = repository,
                onBack = { navController.popBackStack() },
                onRow = { id -> navController.navigate("music/$id") },
            )
        }
        composable(
            route = "music/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) {
            val id = it.arguments?.getString("id") ?: return@composable
            MusicArtistScreen(
                artistId = id,
                repository = repository,
                onBack = { navController.popBackStack() },
                onPlay = ::navigatePlay,
            )
        }
        composable("books") {
            LibraryRowsScreen(
                title = "Books",
                apiPath = "/api/books",
                repository = repository,
                onBack = { navController.popBackStack() },
                onRow = { id -> navController.navigate("books/$id") },
            )
        }
        composable(
            route = "books/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) {
            val id = it.arguments?.getString("id") ?: return@composable
            BookAuthorScreen(
                authorId = id,
                repository = repository,
                onBack = { navController.popBackStack() },
                onPlay = ::navigatePlay,
            )
        }
        composable("comics") {
            LibraryRowsScreen(
                title = "Comics",
                apiPath = "/api/comics",
                repository = repository,
                onBack = { navController.popBackStack() },
                onRow = { /* comics detail MVP: list only */ },
            )
        }
        composable("audiobooks") {
            LibraryRowsScreen(
                title = "Audiobooks",
                apiPath = "/api/audiobooks",
                repository = repository,
                onBack = { navController.popBackStack() },
                onRow = { },
            )
        }
        composable("search") {
            SearchScreen(
                caps = caps,
                repository = repository,
                onBack = { navController.popBackStack() },
                onMovie = { id -> navController.navigate("movie/$id") },
                onShow = { id -> navController.navigate("tvshow/$id") },
                onDiscover = { type, id -> navController.navigate("discover/$type/$id") },
                onNavigate = { route -> navController.navigate(route) },
            )
        }
        composable(
            route = "discover/{type}/{id}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("id") { type = NavType.IntType },
            ),
        ) {
            val type = it.arguments?.getString("type") ?: return@composable
            val id = it.arguments?.getInt("id") ?: return@composable
            DiscoverDetailScreen(
                mediaType = type,
                tmdbId = id,
                repository = repository,
                onBack = { navController.popBackStack() },
            )
        }
        composable("studios") {
            StudiosScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onMovie = { id -> navController.navigate("movie/$id") },
            )
        }
        composable("favorites") {
            FavoritesScreen(
                userdata = userdata,
                repository = repository,
                onBack = { navController.popBackStack() },
                onMovie = { id -> navController.navigate("movie/$id") },
                onShow = { id -> navController.navigate("tvshow/$id") },
            )
        }
        composable("requests") {
            InProgressScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onMovie = { id -> navController.navigate("movie/$id") },
                onShow = { id -> navController.navigate("tvshow/$id") },
            )
        }
        composable("collections") {
            CollectionsScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onCollection = { id -> navController.navigate("collection/$id") },
            )
        }
        composable(
            route = "collection/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) {
            val id = it.arguments?.getString("id") ?: return@composable
            CollectionDetailScreen(
                collectionId = id,
                repository = repository,
                onBack = { navController.popBackStack() },
                onPlay = ::navigatePlay,
            )
        }
        composable("upcoming") {
            UpcomingScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onMovie = { id -> navController.navigate("movie/$id") },
            )
        }
        composable("livetv") {
            LiveTvScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onPlay = ::navigatePlay,
            )
        }
        composable("queue") {
            QueueScreen(
                userdata = userdata,
                onBack = { navController.popBackStack() },
                onPlay = ::navigatePlay,
            )
        }
        composable("playlists") {
            PlaylistsScreen(
                userdata = userdata,
                onBack = { navController.popBackStack() },
            )
        }
        composable("settings") {
            SettingsScreen(
                username = username,
                serverUrl = serverUrl,
                preferences = preferences,
                onBack = { navController.popBackStack() },
                onLogout = onLogout,
            )
        }
        composable(
            route = "player/{bundle}",
            arguments = listOf(navArgument("bundle") { type = NavType.StringType }),
        ) {
            val bundle = it.arguments?.getString("bundle") ?: return@composable
            PlayerScreen(
                play = decodePlay(bundle),
                repository = repository,
                preferences = preferences,
                userdata = userdata,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
