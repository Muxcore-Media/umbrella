@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package systems.zem.muxcore.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import systems.zem.muxcore.data.Acquisition
import systems.zem.muxcore.data.Movie
import systems.zem.muxcore.data.SearchResult
import systems.zem.muxcore.data.TmdbImages
import systems.zem.muxcore.ui.Load
import systems.zem.muxcore.ui.LocalCapabilities
import systems.zem.muxcore.ui.Routes
import systems.zem.muxcore.ui.appContainer
import systems.zem.muxcore.ui.listTopContentPadding
import systems.zem.muxcore.ui.components.BadgeTone
import systems.zem.muxcore.ui.components.EmptyState
import systems.zem.muxcore.ui.components.ErrorBanner
import systems.zem.muxcore.ui.components.MuxImage
import systems.zem.muxcore.ui.components.PosterCard
import systems.zem.muxcore.ui.components.SectionHeader
import systems.zem.muxcore.ui.components.ShelfSkeleton
import systems.zem.muxcore.ui.components.StatusBadge
import systems.zem.muxcore.ui.components.toBadgeTone
import systems.zem.muxcore.ui.rememberLoad
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ---------------------------------------------------------------------------
// Library hub — entry point for secondary destinations

@Composable
fun LibraryScreen(nav: NavHostController) {
    val caps = LocalCapabilities.current
    data class Entry(val icon: ImageVector, val label: String, val detail: String, val route: String, val enabled: Boolean)

    // Same capability gating as the web router (App.tsx featureEnabled checks).
    val entries = listOf(
        Entry(Icons.Default.Explore, "Discover", "Trending and popular titles to request", Routes.DISCOVER, caps.feature("request")),
        Entry(Icons.Default.Bookmark, "Watchlist", "Titles synced from your import lists", Routes.WATCHLIST, caps.feature("watchlist")),
        Entry(Icons.Default.Star, "Favorites", "Everything you starred", Routes.FAVORITES, true),
        Entry(Icons.AutoMirrored.Filled.PlaylistPlay, "Queue", "Your play queue", Routes.QUEUE, caps.feature("queue")),
        Entry(Icons.Default.Downloading, "In progress", "Requests and downloads", Routes.REQUESTS, caps.feature("request")),
        Entry(Icons.Default.Layers, "Collections", "Box sets and genre groups", Routes.COLLECTIONS, caps.feature("collections")),
        Entry(Icons.Default.CalendarMonth, "Upcoming", "Episodes airing soon", Routes.UPCOMING, caps.feature("upcoming")),
        Entry(Icons.Default.Business, "Studios", "Browse by studio or franchise", Routes.STUDIOS, caps.feature("studios")),
        Entry(Icons.AutoMirrored.Filled.PlaylistPlay, "Playlists", "Playlists built from favorites", Routes.PLAYLISTS, caps.feature("playlists")),
        Entry(Icons.Default.QrCode, "Quick Connect", "Approve a sign-in code", Routes.QUICK_CONNECT, caps.feature("quickconnect")),
        Entry(Icons.Default.Settings, "Settings", "Profile, appearance, playback", Routes.SETTINGS, true),
    ).filter { it.enabled }

    LazyColumn(
        contentPadding = listTopContentPadding(extraTop = 8.dp, bottom = 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                "Library",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        items(entries, key = { it.label }) { entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.navigate(entry.route) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Icon(entry.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(entry.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        entry.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    }
}

// ---------------------------------------------------------------------------
// Watchlist

@Composable
fun WatchlistScreen(nav: NavHostController) {
    val api = appContainer().api
    val scope = rememberCoroutineScope()
    val requested = remember { mutableStateMapOf<Int, String>() }
    var error by remember { mutableStateOf<String?>(null) }
    val load by rememberLoad(Unit) { api.watchlist() }

    ListPage(title = "Watchlist", subtitle = "Titles synced from your import-list sources.") {
        when (val state = load) {
            is Load.Loading -> item { ShelfSkeleton() }
            is Load.Failed -> item { ErrorBanner(state.message) }
            is Load.Ready -> {
                if (error != null) item { ErrorBanner(error!!) }
                if (state.value.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Bookmark,
                            title = "No watchlist items",
                            message = "Add import-list sources in admin and sync watchlist actions to see titles here.",
                        )
                    }
                } else {
                    items(state.value, key = { "${it.mediaType}-${it.id}" }) { r ->
                        RequestableCard(
                            item = r,
                            requestedStatus = requested[r.id],
                            onOpen = { nav.navigate(Routes.discoverDetail(r.mediaType, r.id)) },
                            onRequest = {
                                scope.launch {
                                    try {
                                        val res = api.requestTitle(r.id, r.title, r.year, r.overview, r.poster, r.mediaType)
                                        requested[r.id] = res.status.ifEmpty { "requested" }
                                    } catch (e: Exception) {
                                        error = e.message ?: "Request failed"
                                    }
                                }
                            },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Favorites

@Composable
fun FavoritesScreen(nav: NavHostController) {
    val userData = appContainer().userData
    val blob by userData.blob.collectAsState()
    val favorites = remember(blob) { userData.listFavorites() }

    ListPage(title = "Favorites", subtitle = "Everything you starred, synced across devices.") {
        if (favorites.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Star,
                    title = "No favorites yet",
                    message = "Star movies and shows from their detail pages to collect them here.",
                )
            }
        } else {
            item {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    favorites.forEach { f ->
                        PosterCard(
                            title = f.title,
                            posterUrl = f.posterUrl,
                            subtitle = f.year?.takeIf { it > 0 }?.toString(),
                            width = 108,
                            onClick = {
                                nav.navigate(if (f.kind == "tv") Routes.tvDetail(f.id) else Routes.movieDetail(f.id))
                            },
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Queue

@Composable
fun QueueScreen(nav: NavHostController) {
    val container = appContainer()
    val userData = container.userData
    val blob by userData.blob.collectAsState()
    val queue = blob.queue
    val requestsLoad by rememberLoad(Unit) { runCatching { container.api.listRequests() }.getOrDefault(emptyList()) }

    val activeByKey = remember(requestsLoad) {
        val reqs = (requestsLoad as? Load.Ready)?.value.orEmpty()
        reqs.filter { Acquisition.isActiveRequestStatus(it.status) && it.itemId.isNotEmpty() }
            .associateBy { "${it.itemType}:${it.itemId}" }
    }

    ListPage(
        title = "Queue",
        subtitle = "Your play queue, synced across devices.",
        titleAction = {
            if (queue.isNotEmpty()) {
                TextButton(onClick = { userData.clearQueue() }) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" Clear")
                }
            }
        },
    ) {
        if (queue.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    title = "Queue is empty",
                    message = "Add movies or episodes to your queue from their detail pages.",
                )
            }
        } else {
            items(queue, key = { it.id }) { item ->
                val reqKey = if (item.kind == "tv" || item.kind == "movie") "${item.kind}:${item.id}" else null
                val activeReq = reqKey?.let { activeByKey[it] }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(10.dp),
                    ) {
                        Box(
                            Modifier
                                .width(52.dp)
                                .height(76.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        ) {
                            MuxImage(item.posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (activeReq != null) {
                                StatusBadge(
                                    Acquisition.requestDisplayLabel(activeReq),
                                    Acquisition.requestStatusTone(activeReq.status).toBadgeTone(),
                                )
                            }
                        }
                        val playRoute = item.streamUrl?.takeIf { it.isNotEmpty() }?.let {
                            Routes.fromHref(item.href)
                        }
                        if (playRoute != null) {
                            FilledIconButton(onClick = { nav.navigate(playRoute) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                            }
                        }
                        IconButton(onClick = { userData.dequeue(item.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// In Progress (requests + unwatchable library titles)

@Composable
fun InProgressScreen(nav: NavHostController) {
    val api = appContainer().api
    val load by rememberLoad(Unit) {
        coroutineScope {
            val reqs = async { runCatching { api.listRequests() }.getOrDefault(emptyList()) }
            val movies = async { runCatching { api.listMovies(1, 200).items }.getOrDefault(emptyList()) }
            val shows = async { runCatching { api.listTVShows(1, 200).items }.getOrDefault(emptyList()) }
            Acquisition.mergeInProgressEntries(reqs.await(), movies.await(), shows.await())
        }
    }

    ListPage(title = "In progress", subtitle = "Requests, downloads, and titles not ready to stream yet.") {
        when (val state = load) {
            is Load.Loading -> item { ShelfSkeleton() }
            is Load.Failed -> item { ErrorBanner(state.message) }
            is Load.Ready -> {
                if (state.value.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Downloading,
                            title = "Nothing in progress",
                            message = "Requested titles and active downloads will show up here.",
                        )
                    }
                } else {
                    val grouped = Acquisition.groupByPhase(state.value)
                    listOf(
                        "Needs attention" to grouped.attention,
                        "Pending approval" to grouped.pending,
                        "Downloading" to grouped.downloading,
                        "Searching" to grouped.searching,
                        "Requested" to grouped.requested,
                    ).forEach { (title, entries) ->
                        if (entries.isNotEmpty()) {
                            item(key = "phase-$title") { SectionHeader(title, count = entries.size) }
                            items(entries, key = { it.entryTitle + it.entryStatus }) { entry ->
                                InProgressRow(entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InProgressRow(entry: Acquisition.InProgressEntry) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(10.dp),
        ) {
            val poster = when (entry) {
                is Acquisition.InProgressEntry.FromRequest -> Acquisition.posterUrlForRequest(entry.request.poster)
                is Acquisition.InProgressEntry.FromLibrary -> entry.posterUrl
            }
            Box(
                Modifier
                    .width(48.dp)
                    .height(70.dp)
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                MuxImage(poster.ifEmpty { null }, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    entry.entryTitle,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                when (entry) {
                    is Acquisition.InProgressEntry.FromRequest -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(
                                Acquisition.requestDisplayLabel(entry.request),
                                Acquisition.requestStatusTone(entry.request.status).toBadgeTone(),
                            )
                            if (entry.request.year > 0) {
                                Text(
                                    entry.request.year.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        val detail = Acquisition.requestDisplayDetail(entry.request)
                            ?: Acquisition.attentionHint(entry.request.status)
                        if (detail != null) {
                            Text(
                                detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                    }
                    is Acquisition.InProgressEntry.FromLibrary -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge("Waiting for file", BadgeTone.NEUTRAL)
                            if (entry.year > 0) {
                                Text(
                                    entry.year.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Collections

@Composable
fun CollectionsScreen(nav: NavHostController) {
    val api = appContainer().api
    var openCollection by remember { mutableStateOf<Pair<String, String>?>(null) }

    val load by rememberLoad(Unit) {
        coroutineScope {
            val moviesD = async { api.listMovies(1, 200).items }
            val colsD = async { runCatching { api.listCollections() }.getOrDefault(emptyList()) }
            moviesD.await() to colsD.await()
        }
    }
    val detailLoad by rememberLoad(openCollection) {
        openCollection?.let { api.getCollection(it.first) }
    }

    ListPage(
        title = openCollection?.second ?: "Collections",
        subtitle = if (openCollection == null) "Box sets from your library, with genre groups as fallback." else null,
        titleAction = {
            if (openCollection != null) {
                TextButton(onClick = { openCollection = null }) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" All collections")
                }
            }
        },
    ) {
        if (openCollection != null) {
            when (val d = detailLoad) {
                is Load.Loading -> item { ShelfSkeleton() }
                is Load.Failed -> item { ErrorBanner(d.message) }
                is Load.Ready -> {
                    val movies = d.value?.movies.orEmpty()
                    item { MovieFlowGrid(movies) { id -> nav.navigate(Routes.movieDetail(id)) } }
                }
            }
            return@ListPage
        }
        when (val state = load) {
            is Load.Loading -> item { ShelfSkeleton() }
            is Load.Failed -> item { ErrorBanner(state.message) }
            is Load.Ready -> {
                val (movies, serverCols) = state.value
                val genreGroups = movies
                    .flatMap { m -> (m.genres.ifEmpty { listOf("Uncategorized") }).map { it to m } }
                    .groupBy({ it.first }, { it.second })
                    .filterValues { it.size >= 2 }
                    .toSortedMap()

                if (serverCols.isEmpty() && genreGroups.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Layers,
                            title = "No collections",
                            message = "Collections appear as your movie library grows.",
                        )
                    }
                } else {
                    if (serverCols.isNotEmpty()) {
                        item { SectionHeader("Box sets", count = serverCols.size) }
                        items(serverCols, key = { "col-${it.id}" }) { col ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openCollection = col.id to col.name }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(col.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${col.movieCount} ${if (col.movieCount == 1) "movie" else "movies"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    genreGroups.forEach { (genre, groupMovies) ->
                        item(key = "genre-$genre") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionHeader(genre, count = groupMovies.size)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                                ) {
                                    items(groupMovies.take(24), key = { it.id }) { m ->
                                        PosterCard(
                                            title = m.title,
                                            posterUrl = m.posterUrl,
                                            subtitle = if (m.year > 0) m.year.toString() else null,
                                            onClick = { nav.navigate(Routes.movieDetail(m.id)) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieFlowGrid(movies: List<Movie>, onOpen: (String) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        movies.forEach { m ->
            PosterCard(
                title = m.title,
                posterUrl = m.posterUrl,
                subtitle = if (m.year > 0) m.year.toString() else null,
                width = 108,
                onClick = { onOpen(m.id) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Upcoming (episodes airing within ~4 months, from show metadata)

@Composable
fun UpcomingScreen(nav: NavHostController) {
    val api = appContainer().api
    val load by rememberLoad(Unit) {
        val list = api.listTVShows(1, 100)
        val detailed = list.items.take(40).map { s ->
            runCatching { api.getTVShow(s.id) }.getOrDefault(s)
        }
        val now = Instant.now().toEpochMilli()
        val horizon = now + 1000L * 60 * 60 * 24 * 120
        val floor = now - 1000L * 60 * 60 * 24 * 14
        detailed.flatMap { show ->
            (show.seasons ?: emptyList()).flatMap { season ->
                season.episodes.mapNotNull { ep ->
                    val air = ep.airDate ?: return@mapNotNull null
                    val t = runCatching { LocalDate.parse(air).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
                        .getOrNull() ?: return@mapNotNull null
                    if (t in floor..horizon) Triple(show, ep, t) else null
                }
            }
        }.sortedBy { it.third }
    }

    ListPage(title = "Upcoming", subtitle = "Episodes airing soon for shows in your library.") {
        when (val state = load) {
            is Load.Loading -> item { ShelfSkeleton() }
            is Load.Failed -> item { ErrorBanner(state.message) }
            is Load.Ready -> {
                if (state.value.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.CalendarMonth,
                            title = "Nothing scheduled",
                            message = "No upcoming episodes found for shows in your library.",
                        )
                    }
                } else {
                    items(state.value, key = { "${it.first.id}-${it.second.id}" }) { (show, ep, t) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { nav.navigate(Routes.tvDetail(show.id)) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Box(
                                Modifier
                                    .width(44.dp)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            ) {
                                MuxImage(show.posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                            }
                            Column(Modifier.weight(1f)) {
                                Text(show.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                Text(
                                    "S%02dE%02d".format(ep.seasonNumber, ep.episodeNumber) +
                                        (if (ep.title.isNotEmpty()) " · ${ep.title}" else ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).toLocalDate()
                                    .format(DateTimeFormatter.ofPattern("MMM d")),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Studios (collection_name / genre buckets)

@Composable
fun StudiosScreen(nav: NavHostController) {
    val api = appContainer().api
    var selected by rememberSaveable { mutableStateOf("") }
    val load by rememberLoad(Unit) { api.listMovies(1, 500).items }

    ListPage(
        title = if (selected.isEmpty()) "Studios & collections" else selected,
        subtitle = if (selected.isEmpty()) "Browse by studio, franchise, or genre." else null,
        titleAction = {
            if (selected.isNotEmpty()) {
                TextButton(onClick = { selected = "" }) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(" All")
                }
            }
        },
    ) {
        when (val state = load) {
            is Load.Loading -> item { ShelfSkeleton() }
            is Load.Failed -> item { ErrorBanner(state.message) }
            is Load.Ready -> {
                val movies = state.value
                if (selected.isNotEmpty()) {
                    val filtered = movies.filter { m ->
                        m.collectionName?.trim() == selected || selected in m.genres
                    }
                    item { MovieFlowGrid(filtered) { id -> nav.navigate(Routes.movieDetail(id)) } }
                    return@ListPage
                }
                val buckets = movies
                    .flatMap { m ->
                        val keys = m.collectionName?.trim()?.takeIf { it.isNotEmpty() }?.let { listOf(it) }
                            ?: m.genres.ifEmpty { listOf("Unknown") }
                        keys.map { it to m }
                    }
                    .groupBy({ it.first }, { it.second })
                    .toSortedMap()

                if (buckets.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Business,
                            message = "No studios or franchises found in your movie library yet.",
                        )
                    }
                } else {
                    items(buckets.entries.toList(), key = { it.key }) { (name, group) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = name }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(14.dp))
                            Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            StatusBadge("${group.size}", BadgeTone.NEUTRAL)
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Playlists (built from favorites, synced via userdata)

@Composable
fun PlaylistsScreen(nav: NavHostController) {
    val userData = appContainer().userData
    val blob by userData.blob.collectAsState()
    var newName by rememberSaveable { mutableStateOf("") }
    val favorites = remember(blob) { userData.listFavorites() }
    val favById = remember(favorites) { favorites.associateBy { it.id } }

    ListPage(title = "Playlists", subtitle = "Create playlists from your favorites; synced across devices.") {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("New playlist name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        userData.createPlaylist(newName.trim())
                        newName = ""
                    },
                    enabled = newName.isNotBlank(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Create")
                }
            }
        }
        if (blob.playlists.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    title = "No playlists",
                    message = "Create a playlist above, then add titles from your favorites.",
                )
            }
        } else {
            items(blob.playlists, key = { it.id }) { playlist ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(playlist.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            StatusBadge("${playlist.itemIds.size}", BadgeTone.NEUTRAL)
                            IconButton(onClick = { userData.deletePlaylist(playlist.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete playlist", modifier = Modifier.size(18.dp))
                            }
                        }
                        playlist.itemIds.forEach { itemId ->
                            val f = favById[itemId]
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (f != null) {
                                            nav.navigate(
                                                if (f.kind == "tv") Routes.tvDetail(f.id) else Routes.movieDetail(f.id),
                                            )
                                        }
                                    },
                            ) {
                                Text(
                                    f?.title ?: itemId,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                IconButton(onClick = { userData.removeFromPlaylist(playlist.id, itemId) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        // Add favorites not yet in this playlist.
                        val addable = favorites.filter { it.id !in playlist.itemIds }
                        if (addable.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(addable, key = { it.id }) { f ->
                                    TextButton(onClick = { userData.addToPlaylist(playlist.id, f.id) }) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text(
                                            " ${f.title}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared page scaffold for list screens

@Composable
private fun ListPage(
    title: String,
    subtitle: String? = null,
    titleAction: (@Composable () -> Unit)? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = listTopContentPadding(extraTop = 12.dp, bottom = 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    titleAction?.invoke()
                }
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        content()
        item { Spacer(Modifier.height(8.dp)) }
    }
}
