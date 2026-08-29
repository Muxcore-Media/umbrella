package com.muxcore.media.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.muxcore.media.tv.data.Capabilities
import com.muxcore.media.tv.data.MovieItem
import com.muxcore.media.tv.data.MuxRepository
import com.muxcore.media.tv.data.NextUpEntry
import com.muxcore.media.tv.data.PlayRequest
import com.muxcore.media.tv.data.PreferencesStore
import com.muxcore.media.tv.data.ProgressEntry
import com.muxcore.media.tv.data.TvShowItem
import com.muxcore.media.tv.data.UserdataStore
import com.muxcore.media.tv.data.UserPreferences
import com.muxcore.media.tv.data.resolveNextUp
import com.muxcore.media.tv.data.resolvePosterPath
import com.muxcore.media.tv.navigation.moviePlayRequest
import com.muxcore.media.tv.navigation.nextUpPlayRequest
import com.muxcore.media.tv.navigation.progressPlayRequest
import com.muxcore.media.tv.ui.components.MediaShelf

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    username: String,
    caps: Capabilities,
    repository: MuxRepository,
    preferences: PreferencesStore,
    userdata: UserdataStore,
    onNavigate: (String) -> Unit,
    onPlay: (PlayRequest) -> Unit,
    onLogout: () -> Unit,
) {
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var prefs by remember { mutableStateOf(UserPreferences()) }
    var continueWatching by remember { mutableStateOf<List<ProgressEntry>>(emptyList()) }
    var favorites by remember { mutableStateOf<List<com.muxcore.media.tv.data.FavoriteEntry>>(emptyList()) }
    var nextUp by remember { mutableStateOf<List<NextUpEntry>>(emptyList()) }
    var recommended by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var readyMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var readyShows by remember { mutableStateOf<List<TvShowItem>>(emptyList()) }
    var requestCount by remember { mutableStateOf(0) }
    var posterUrls by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        try {
            prefs = preferences.getUserPreferences()
            userdata.loadLocal()
            userdata.pullFromServer()
            continueWatching = userdata.continueWatching(16)
            favorites = userdata.listFavorites().take(16)
            nextUp = userdata.resolveNextUp(repository, 16)
            val movies = repository.listMovies(1, 48)
            val shows = repository.listTvShows(1, 48)
            readyMovies = movies.items.filter { it.hasFile }.take(16)
            readyShows = shows.items.filter { it.hasFile }.take(16)
            recommended = movies.items
                .filter { it.hasFile && it.voteAverage > 0 }
                .sortedByDescending { it.voteAverage }
                .take(16)
            if (caps.featureEnabled("request")) {
                requestCount = repository.listRequests().size
            }
            val urls = mutableMapOf<String, String>()
            for (p in continueWatching) {
                urls["p:${p.id}"] = resolvePosterPath(repository, p.posterUrl, p.kind)
            }
            for (f in favorites) {
                urls["f:${f.id}"] = resolvePosterPath(repository, f.posterUrl, f.kind)
            }
            for (n in nextUp) {
                urls["n:${n.id}"] = resolvePosterPath(repository, n.posterUrl, "episode")
            }
            for (m in readyMovies + recommended) {
                urls["m:${m.id}"] = resolvePosterPath(repository, m.posterUrl, "movie")
            }
            for (s in readyShows) {
                urls["s:${s.id}"] = resolvePosterPath(repository, s.posterUrl, "tv")
            }
            posterUrls = urls
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    val showReadyFallback =
        prefs.home.showNextUp && nextUp.isEmpty() && (readyMovies.isNotEmpty() || readyShows.isNotEmpty())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 48.dp, vertical = 24.dp)) {
            Text(text = "MuxCore TV", style = androidx.tv.material3.MaterialTheme.typography.headlineLarge)
            if (username.isNotBlank()) Text(text = "Signed in as $username")
            if (loading) Text(text = "Loading your library…")
            if (error != null) Text(text = error!!)
        }

        if (prefs.home.showContinueWatching) {
            MediaShelf(
                title = "Continue watching",
                items = continueWatching,
                key = { it.id },
                posterUrl = { posterUrls["p:${it.id}"].orEmpty().ifBlank { it.posterUrl } },
                label = { it.title },
                onItem = { onPlay(progressPlayRequest(it)) },
            )
        }

        if (prefs.home.showNextUp && nextUp.isNotEmpty()) {
            MediaShelf(
                title = "Next up",
                items = nextUp,
                key = { it.id },
                posterUrl = { posterUrls["n:${it.id}"].orEmpty().ifBlank { it.posterUrl } },
                label = { it.title },
                subtitle = { it.subtitle },
                onItem = { onPlay(nextUpPlayRequest(it)) },
            )
        }

        if (recommended.isNotEmpty()) {
            MediaShelf(
                title = "Recommended",
                items = recommended,
                key = { it.id },
                posterUrl = { posterUrls["m:${it.id}"].orEmpty().ifBlank { it.posterUrl } },
                label = { it.title },
                onItem = { onNavigate("movie/${it.id}") },
            )
        }

        if (prefs.home.showFavorites) {
            MediaShelf(
                title = "Favorites",
                items = favorites,
                key = { it.id },
                posterUrl = { posterUrls["f:${it.id}"].orEmpty().ifBlank { it.posterUrl } },
                label = { it.title },
                onItem = { item ->
                    when (item.kind) {
                        "tv", "episode" -> onNavigate("tvshow/${item.id}")
                        else -> onNavigate("movie/${item.id}")
                    }
                },
            )
        }

        if (showReadyFallback) {
            MediaShelf(
                title = "Available now",
                items = readyMovies,
                key = { it.id },
                posterUrl = { posterUrls["m:${it.id}"].orEmpty().ifBlank { it.posterUrl } },
                label = { it.title },
                subtitle = { if (it.year > 0) it.year.toString() else "" },
                onItem = { onPlay(moviePlayRequest(it)) },
            )
            MediaShelf(
                title = "Available now — TV",
                items = readyShows,
                key = { it.id },
                posterUrl = { posterUrls["s:${it.id}"].orEmpty().ifBlank { it.posterUrl } },
                label = { it.title },
                onItem = { onNavigate("tvshow/${it.id}") },
            )
        } else {
            MediaShelf(
                title = "Ready to play — Movies",
                items = readyMovies,
                key = { it.id },
                posterUrl = { posterUrls["m:${it.id}"].orEmpty().ifBlank { it.posterUrl } },
                label = { it.title },
                subtitle = { if (it.year > 0) it.year.toString() else "" },
                onItem = { onPlay(moviePlayRequest(it)) },
            )

            MediaShelf(
                title = "Ready to play — TV",
                items = readyShows,
                key = { it.id },
                posterUrl = { posterUrls["s:${it.id}"].orEmpty().ifBlank { it.posterUrl } },
                label = { it.title },
                onItem = { onNavigate("tvshow/${it.id}") },
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (prefs.home.showRecentRequests && caps.featureEnabled("request") && requestCount > 0) {
                Button(onClick = { onNavigate("requests") }) {
                    Text("In progress ($requestCount)")
                }
            }
            if (caps.featureEnabled("search")) {
                Button(onClick = { onNavigate("search") }) { Text("Search") }
            }
            Button(onClick = { onNavigate("browse") }) { Text("Browse library") }
            Button(onClick = { onNavigate("settings") }) { Text("Settings") }
            Button(onClick = onLogout) { Text("Sign out") }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BrowseHubScreen(
    caps: Capabilities,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Browse", style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        for (item in com.muxcore.media.tv.data.visibleNav(caps, com.muxcore.media.tv.data.PrimaryNavCatalog)) {
            if (item.route == "home") continue
            Button(onClick = { onNavigate(item.route) }) { Text(item.label) }
        }
        for (item in com.muxcore.media.tv.data.visibleNav(caps, com.muxcore.media.tv.data.OverflowNavCatalog)) {
            Button(onClick = { onNavigate(item.route) }) { Text(item.label) }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}
