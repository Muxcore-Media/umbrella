package com.muxcore.media.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.muxcore.media.tv.data.CollectionDetail
import com.muxcore.media.tv.data.CollectionSummary
import com.muxcore.media.tv.data.FavoriteEntry
import com.muxcore.media.tv.data.LiveTvChannel
import com.muxcore.media.tv.data.MediaRequest
import com.muxcore.media.tv.data.MuxRepository
import com.muxcore.media.tv.data.PlayRequest
import com.muxcore.media.tv.data.Playlist
import com.muxcore.media.tv.data.QueueItem
import com.muxcore.media.tv.data.PreferencesStore
import com.muxcore.media.tv.data.UserdataStore
import com.muxcore.media.tv.data.resolvePosterPath
import com.muxcore.media.tv.navigation.moviePlayRequest
import com.muxcore.media.tv.ui.components.PosterCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FavoritesScreen(
    userdata: UserdataStore,
    repository: MuxRepository,
    onBack: () -> Unit,
    onMovie: (String) -> Unit,
    onShow: (String) -> Unit,
) {
    var items by remember { mutableStateOf<List<FavoriteEntry>>(emptyList()) }
    LaunchedEffect(Unit) { items = userdata.listFavorites() }
    LibraryPosterScreen(
        title = "Favorites",
        emptyMessage = "No favorites yet.",
        onBack = onBack,
        items = items,
        key = { it.id },
        poster = { it.posterUrl },
        label = { it.title },
        onClick = { f ->
            if (f.kind == "tv" || f.kind == "episode") onShow(f.id) else onMovie(f.id)
        },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun InProgressScreen(
    repository: MuxRepository,
    onBack: () -> Unit,
    onMovie: (String) -> Unit,
    onShow: (String) -> Unit,
) {
    var requests by remember { mutableStateOf<List<MediaRequest>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            requests = repository.listRequests()
        } catch (e: Exception) {
            error = e.message
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "In progress", style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        if (error != null) Text(text = error!!)
        for (r in requests) {
            Button(onClick = {
                if (r.itemType == "tv") onShow(r.id) else onMovie(r.id)
            }) {
                Text("${r.title} — ${r.status}")
            }
        }
        if (requests.isEmpty() && error == null) Text(text = "No active requests.")
        Button(onClick = onBack) { Text("Back") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CollectionsScreen(
    repository: MuxRepository,
    onBack: () -> Unit,
    onCollection: (String) -> Unit,
) {
    var cols by remember { mutableStateOf<List<CollectionSummary>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            cols = repository.listCollections()
        } catch (e: Exception) {
            error = e.message
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Collections", style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        if (error != null) Text(text = error!!)
        for (c in cols) {
            Button(onClick = { onCollection(c.id) }) {
                Text("${c.name} (${c.movieCount})")
            }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    collectionId: String,
    repository: MuxRepository,
    onBack: () -> Unit,
    onPlay: (PlayRequest) -> Unit,
) {
    var detail by remember { mutableStateOf<CollectionDetail?>(null) }
    LaunchedEffect(collectionId) {
        detail = repository.getCollection(collectionId)
    }
    Column(modifier = Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = detail?.name ?: "Collection", style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(detail?.movies ?: emptyList(), key = { it.id }) { m ->
                PosterCard(
                    title = m.title,
                    posterUrl = m.posterUrl,
                    onClick = { onPlay(moviePlayRequest(m)) },
                )
            }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LiveTvScreen(
    repository: MuxRepository,
    onBack: () -> Unit,
    onPlay: (PlayRequest) -> Unit,
) {
    var channels by remember { mutableStateOf<List<LiveTvChannel>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            channels = repository.listLiveTv().channels
        } catch (e: Exception) {
            error = e.message
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Live TV", style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        if (error != null) Text(text = error!!)
        for (ch in channels) {
            Button(onClick = {
                if (ch.url.isNotBlank()) onPlay(PlayRequest(src = ch.url, title = ch.name, kind = "other"))
            }) {
                val now = ch.nowPlaying?.title?.let { " — $it" } ?: ""
                Text("${ch.number} ${ch.name}$now")
            }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun QueueScreen(
    userdata: UserdataStore,
    onBack: () -> Unit,
    onPlay: (PlayRequest) -> Unit,
) {
    var queue by remember { mutableStateOf<List<QueueItem>>(emptyList()) }
    LaunchedEffect(Unit) { queue = userdata.listQueue() }
    Column(modifier = Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Queue", style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        for (item in queue) {
            Button(onClick = {
                onPlay(PlayRequest(src = item.streamUrl, title = item.title, mediaId = item.id, kind = item.kind, poster = item.posterUrl))
            }) {
                Text(item.title)
            }
        }
        if (queue.isEmpty()) Text(text = "Queue is empty.")
        Button(onClick = onBack) { Text("Back") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    userdata: UserdataStore,
    onBack: () -> Unit,
) {
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    LaunchedEffect(Unit) { playlists = userdata.listPlaylists() }
    Column(modifier = Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Playlists", style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        for (p in playlists) {
            Text(text = "${p.name} (${p.itemIds.size} items)")
        }
        if (playlists.isEmpty()) Text(text = "No playlists yet.")
        Button(onClick = onBack) { Text("Back") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    username: String,
    serverUrl: String,
    preferences: PreferencesStore,
    onBack: () -> Unit,
    onLogout: () -> Unit,
) {
    var prefs by remember { mutableStateOf(com.muxcore.media.tv.data.UserPreferences()) }
    var pane by remember { mutableStateOf("profile") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        prefs = preferences.getUserPreferences()
    }

    Column(modifier = Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Settings", style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Button(onClick = { pane = "profile" }) { Text("Profile") } }
            item { Button(onClick = { pane = "display" }) { Text("Display") } }
            item { Button(onClick = { pane = "home" }) { Text("Home") } }
            item { Button(onClick = { pane = "playback" }) { Text("Playback") } }
            item { Button(onClick = { pane = "subtitles" }) { Text("Subtitles") } }
        }
        when (pane) {
            "profile" -> {
                Text(text = "Server: $serverUrl")
                Text(text = "User: $username")
                Button(onClick = onLogout) { Text("Sign out") }
            }
            "display" -> {
                Text(text = "Library page size: ${prefs.display.libraryPageSize}")
                Button(onClick = {
                    scope.launch {
                        prefs = prefs.copy(
                            display = prefs.display.copy(
                                libraryPageSize = if (prefs.display.libraryPageSize >= 96) 48 else prefs.display.libraryPageSize + 24,
                            ),
                        )
                        preferences.updateUserPreferences(prefs)
                    }
                }) { Text("Cycle page size") }
            }
            "home" -> {
                Button(onClick = {
                    scope.launch {
                        prefs = prefs.copy(
                            home = prefs.home.copy(showContinueWatching = !prefs.home.showContinueWatching),
                        )
                        preferences.updateUserPreferences(prefs)
                    }
                }) { Text("Continue watching: ${prefs.home.showContinueWatching}") }
                Button(onClick = {
                    scope.launch {
                        prefs = prefs.copy(home = prefs.home.copy(showFavorites = !prefs.home.showFavorites))
                        preferences.updateUserPreferences(prefs)
                    }
                }) { Text("Favorites shelf: ${prefs.home.showFavorites}") }
                Button(onClick = {
                    scope.launch {
                        prefs = prefs.copy(home = prefs.home.copy(showNextUp = !prefs.home.showNextUp))
                        preferences.updateUserPreferences(prefs)
                    }
                }) { Text("Next up shelf: ${prefs.home.showNextUp}") }
                Button(onClick = {
                    scope.launch {
                        prefs = prefs.copy(
                            home = prefs.home.copy(showRecentRequests = !prefs.home.showRecentRequests),
                        )
                        preferences.updateUserPreferences(prefs)
                    }
                }) { Text("Recent requests: ${prefs.home.showRecentRequests}") }
            }
            "playback" -> {
                Button(onClick = {
                    scope.launch {
                        prefs = prefs.copy(
                            playback = prefs.playback.copy(rememberPosition = !prefs.playback.rememberPosition),
                        )
                        preferences.updateUserPreferences(prefs)
                    }
                }) { Text("Remember position: ${prefs.playback.rememberPosition}") }
                Button(onClick = {
                    scope.launch {
                        val next = when (prefs.playback.skipIntroSec) {
                            0 -> 30
                            30 -> 60
                            60 -> 90
                            else -> 0
                        }
                        prefs = prefs.copy(playback = prefs.playback.copy(skipIntroSec = next))
                        preferences.updateUserPreferences(prefs)
                    }
                }) { Text("Skip intro: ${prefs.playback.skipIntroSec}s") }
            }
            "subtitles" -> {
                Button(onClick = {
                    scope.launch {
                        prefs = prefs.copy(
                            subtitles = prefs.subtitles.copy(enabled = !prefs.subtitles.enabled),
                        )
                        preferences.updateUserPreferences(prefs)
                    }
                }) { Text("Subtitles enabled: ${prefs.subtitles.enabled}") }
                Button(onClick = {
                    scope.launch {
                        val langs = listOf("eng", "en", "spa", "fre")
                        val idx = langs.indexOf(prefs.subtitles.language.lowercase()).let { if (it < 0) 0 else (it + 1) % langs.size }
                        prefs = prefs.copy(subtitles = prefs.subtitles.copy(language = langs[idx]))
                        preferences.updateUserPreferences(prefs)
                    }
                }) { Text("Language: ${prefs.subtitles.language}") }
            }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun <T> LibraryPosterScreen(
    title: String,
    emptyMessage: String,
    onBack: () -> Unit,
    items: List<T>,
    key: (T) -> String,
    poster: (T) -> String,
    label: (T) -> String,
    onClick: (T) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        if (items.isEmpty()) Text(text = emptyMessage)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = key) { item ->
                PosterCard(title = label(item), posterUrl = poster(item), onClick = { onClick(item) })
            }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}
