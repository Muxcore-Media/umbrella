package systems.zem.muxcore.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import systems.zem.muxcore.data.Movie
import systems.zem.muxcore.data.SearchResult
import systems.zem.muxcore.data.TVShow
import systems.zem.muxcore.data.TmdbImages
import systems.zem.muxcore.ui.LocalCapabilities
import systems.zem.muxcore.ui.Routes
import systems.zem.muxcore.ui.appContainer
import systems.zem.muxcore.ui.listTopContentPadding
import systems.zem.muxcore.ui.components.BadgeTone
import systems.zem.muxcore.ui.components.EmptyState
import systems.zem.muxcore.ui.components.ErrorBanner
import systems.zem.muxcore.ui.components.MuxImage
import systems.zem.muxcore.ui.components.PosterCard
import systems.zem.muxcore.ui.components.PosterCardSkeleton
import systems.zem.muxcore.ui.components.SectionHeader
import systems.zem.muxcore.ui.components.StatusBadge

private data class SearchState(
    val movies: List<Movie> = emptyList(),
    val shows: List<TVShow> = emptyList(),
    val remote: List<SearchResult> = emptyList(),
)

/** Unified search: library titles + TMDB discover results with request buttons. */
@Composable
fun SearchScreen(nav: NavHostController, initialQuery: String, initialScope: String) {
    val container = appContainer()
    val api = container.api
    val caps = LocalCapabilities.current
    val scope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var searchScope by rememberSaveable { mutableStateOf(initialScope.ifEmpty { "all" }) }
    var state by remember { mutableStateOf(SearchState()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val requested = remember { mutableStateMapOf<Int, String>() }

    val canSearch = query.trim().length >= 2

    LaunchedEffect(query, searchScope) {
        if (!canSearch) {
            state = SearchState()
            return@LaunchedEffect
        }
        delay(350) // debounce typing
        loading = true
        error = null
        try {
            val needle = query.trim().lowercase()
            coroutineScope {
                val moviesD = async {
                    if (searchScope in listOf("all", "movies") && caps.library("movies")) {
                        runCatching { api.listMovies(1, 200).items }.getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                }
                val showsD = async {
                    if (searchScope in listOf("all", "tv") && caps.library("tv")) {
                        runCatching { api.listTVShows(1, 200).items }.getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                }
                val remoteD = async {
                    if (searchScope in listOf("all", "add", "movies", "tv")) {
                        runCatching { api.search(query.trim()) }.getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                }
                val libMovies = moviesD.await().filter { it.hasFile && needle in it.title.lowercase() }
                val libShows = showsD.await().filter { it.hasFile && needle in it.title.lowercase() }
                var remote = remoteD.await()
                if (searchScope == "movies") remote = remote.filter { it.mediaType == "movie" }
                if (searchScope == "tv") remote = remote.filter { it.mediaType == "tv" }
                state = if (searchScope == "add") {
                    SearchState(remote = remote)
                } else {
                    SearchState(movies = libMovies, shows = libShows, remote = remote)
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Search failed"
            state = SearchState()
        } finally {
            loading = false
        }
    }

    // Remote results not already in the library, by title (mirrors remoteNotInLibrary).
    val remoteOnly = remember(state) {
        val titles = state.movies.map { "movie:${it.title.lowercase()}" }.toSet() +
            state.shows.map { "tv:${it.title.lowercase()}" }.toSet()
        state.remote.filter { "${it.mediaType}:${it.title.lowercase()}" !in titles }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = listTopContentPadding(extraTop = 12.dp, bottom = 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text("Search", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "One place to find titles in your library or request something new.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search movies and shows…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    listOf("all" to "All", "movies" to "Movies", "tv" to "TV", "add" to "Add new").forEach { (id, label) ->
                        FilterChip(
                            selected = searchScope == id,
                            onClick = { searchScope = id },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        if (!canSearch) {
            item {
                Text(
                    "Enter at least two characters to search.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            return@LazyColumn
        }
        if (error != null) {
            item { ErrorBanner(error!!) }
        }
        if (loading) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    repeat(3) { PosterCardSkeleton() }
                }
            }
            return@LazyColumn
        }

        if (state.movies.isNotEmpty()) {
            item { SectionHeader("Movies", count = state.movies.size) }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    state.movies.forEach { m ->
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

        if (state.shows.isNotEmpty()) {
            item { SectionHeader("TV Shows", count = state.shows.size) }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    state.shows.forEach { s ->
                        PosterCard(
                            title = s.title,
                            posterUrl = s.posterUrl,
                            subtitle = if (s.year > 0) s.year.toString() else null,
                            onClick = { nav.navigate(Routes.tvDetail(s.id)) },
                        )
                    }
                }
            }
        }

        if (remoteOnly.isNotEmpty()) {
            item { SectionHeader("Add to your library", count = remoteOnly.size) }
            items(remoteOnly, key = { "${it.mediaType}-${it.id}" }) { r ->
                RequestableCard(
                    item = r,
                    requestedStatus = requested[r.id],
                    onOpen = { nav.navigate(Routes.discoverDetail(r.mediaType, r.id)) },
                    onRequest = {
                        scope.launch {
                            try {
                                val res = api.requestTitle(
                                    tmdbId = r.id,
                                    title = r.title,
                                    year = r.year,
                                    overview = r.overview,
                                    poster = r.poster,
                                    mediaType = r.mediaType,
                                )
                                requested[r.id] = res.status.ifEmpty { "requested" }
                            } catch (e: Exception) {
                                error = e.message ?: "Request failed"
                            }
                        }
                    },
                )
            }
        }

        if (state.movies.isEmpty() && state.shows.isEmpty() && remoteOnly.isEmpty() && error == null) {
            item {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = "No matches",
                    message = "No results for \"${query.trim()}\". Try another title or filter.",
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

/** Discover result row with a Request action (port of web RequestableCard). */
@Composable
fun RequestableCard(
    item: SearchResult,
    requestedStatus: String?,
    onOpen: () -> Unit,
    onRequest: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .clickable(onClick = onOpen)
                .padding(12.dp),
        ) {
            Box(
                Modifier
                    .width(72.dp)
                    .height(104.dp)
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                MuxImage(
                    TmdbImages.url(item.poster, "w342").ifEmpty { null },
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(if (item.mediaType == "tv") "TV" else "Movie", BadgeTone.NEUTRAL)
                    if (item.year > 0) {
                        Text(
                            item.year.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.overview.isNotEmpty()) {
                    Text(
                        item.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.voteAvg > 0) {
                    Text(
                        "Rating %.1f".format(item.voteAvg),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(onClick = onRequest, enabled = requestedStatus == null) {
                    Text(if (requestedStatus != null) "Requested" else "Request")
                }
                if (requestedStatus != null) {
                    Text(
                        requestedStatus.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
