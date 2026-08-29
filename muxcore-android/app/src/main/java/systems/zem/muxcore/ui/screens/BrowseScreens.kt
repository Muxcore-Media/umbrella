package systems.zem.muxcore.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import systems.zem.muxcore.data.Movie
import systems.zem.muxcore.data.TVShow
import systems.zem.muxcore.ui.Routes
import systems.zem.muxcore.ui.appContainer
import systems.zem.muxcore.ui.listTopContentPadding
import systems.zem.muxcore.ui.components.EmptyState
import systems.zem.muxcore.ui.components.ErrorBanner
import systems.zem.muxcore.ui.components.PosterCard
import systems.zem.muxcore.ui.components.PosterCardSkeleton

/** Row model shared by movie + TV grids. */
private data class GridRow(
    val id: String,
    val title: String,
    val year: Int,
    val rating: Double,
    val genres: List<String>,
    val posterUrl: String,
    val watched: Boolean,
)

private enum class SortKey(val label: String) { TITLE("Title"), YEAR("Year"), RATING("Rating") }

@Composable
fun MoviesScreen(nav: NavHostController) {
    val container = appContainer()
    val pageSize = container.userData.prefs.display.libraryPageSize
    var items by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pageSize) {
        loading = true
        try {
            val res = container.api.listMovies(1, pageSize)
            items = res.items
            total = res.total
            page = 1
            error = null
        } catch (e: Exception) {
            error = e.message ?: "Failed to load movies"
        } finally {
            loading = false
        }
    }

    val watchable = remember(items) { items.filter { it.hasFile } }
    val inProgressCount = items.size - watchable.size
    val showWatched = container.userData.prefs.display.showWatchedIndicators
    val blob by container.userData.blob.collectAsState()

    LibraryGrid(
        title = "Movies",
        rows = watchable.map { m ->
            GridRow(
                id = m.id,
                title = m.title,
                year = m.year,
                rating = m.voteAverage,
                genres = m.genres,
                posterUrl = m.posterUrl,
                watched = showWatched && blob.progress[m.id]?.watched == true,
            )
        },
        loading = loading,
        loadingMore = loadingMore,
        error = error,
        inProgressCount = inProgressCount,
        emptyIcon = { Icon(Icons.Default.Movie, contentDescription = null) },
        emptyMessage = "No movies in your library yet. Search to request titles.",
        canLoadMore = items.size < total,
        onLoadMore = {
            if (!loadingMore && items.size < total) {
                loadingMore = true
            }
        },
        onOpen = { nav.navigate(Routes.movieDetail(it)) },
        onGoRequests = { nav.navigate(Routes.REQUESTS) },
        onGoSearch = { nav.navigate(Routes.search()) },
    )

    LaunchedEffect(loadingMore) {
        if (!loadingMore) return@LaunchedEffect
        try {
            val res = container.api.listMovies(page + 1, pageSize)
            items = items + res.items
            total = res.total
            page += 1
        } catch (_: Exception) {
            // keep what we have
        } finally {
            loadingMore = false
        }
    }
}

@Composable
fun TVShowsScreen(nav: NavHostController) {
    val container = appContainer()
    val pageSize = container.userData.prefs.display.libraryPageSize
    var items by remember { mutableStateOf<List<TVShow>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pageSize) {
        loading = true
        try {
            val res = container.api.listTVShows(1, pageSize)
            items = res.items
            total = res.total
            page = 1
            error = null
        } catch (e: Exception) {
            error = e.message ?: "Failed to load TV shows"
        } finally {
            loading = false
        }
    }

    val watchable = remember(items) { items.filter { it.hasFile } }
    val inProgressCount = items.size - watchable.size

    LibraryGrid(
        title = "TV Shows",
        rows = watchable.map { s ->
            GridRow(
                id = s.id,
                title = s.title,
                year = s.year,
                rating = s.voteAverage,
                genres = s.genres,
                posterUrl = s.posterUrl,
                watched = false,
            )
        },
        loading = loading,
        loadingMore = loadingMore,
        error = error,
        inProgressCount = inProgressCount,
        emptyIcon = { Icon(Icons.Default.Tv, contentDescription = null) },
        emptyMessage = "No TV shows in your library yet. Search to request titles.",
        canLoadMore = items.size < total,
        onLoadMore = {
            if (!loadingMore && items.size < total) {
                loadingMore = true
            }
        },
        onOpen = { nav.navigate(Routes.tvDetail(it)) },
        onGoRequests = { nav.navigate(Routes.REQUESTS) },
        onGoSearch = { nav.navigate(Routes.search()) },
    )

    LaunchedEffect(loadingMore) {
        if (!loadingMore) return@LaunchedEffect
        try {
            val res = container.api.listTVShows(page + 1, pageSize)
            items = items + res.items
            total = res.total
            page += 1
        } catch (_: Exception) {
        } finally {
            loadingMore = false
        }
    }
}

@Composable
private fun LibraryGrid(
    title: String,
    rows: List<GridRow>,
    loading: Boolean,
    loadingMore: Boolean,
    error: String?,
    inProgressCount: Int,
    emptyIcon: @Composable () -> Unit,
    emptyMessage: String,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onOpen: (String) -> Unit,
    onGoRequests: () -> Unit,
    onGoSearch: () -> Unit,
) {
    var genre by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SortKey.TITLE.name) }
    val sortKey = SortKey.valueOf(sort)

    val genres = remember(rows) {
        rows.flatMap { it.genres }.distinct().sorted()
    }
    val filtered = remember(rows, genre, sort) {
        var out = if (genre.isEmpty()) rows else rows.filter { genre in it.genres }
        out = when (sortKey) {
            SortKey.YEAR -> out.sortedByDescending { it.year }
            SortKey.RATING -> out.sortedByDescending { it.rating }
            SortKey.TITLE -> out.sortedBy { it.title.lowercase() }
        }
        out
    }

    val gridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 8
        }
    }
    LaunchedEffect(nearEnd, canLoadMore) {
        if (nearEnd && canLoadMore && genre.isEmpty()) onLoadMore()
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(112.dp),
        state = gridState,
        contentPadding = listTopContentPadding(extraTop = 12.dp, horizontal = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                if (inProgressCount > 0) {
                    Text(
                        "$inProgressCount ${if (inProgressCount == 1) "title" else "titles"} still downloading — view In Progress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onGoRequests),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    SortMenuChip(sortKey.label) { sort = it.name }
                    FilterChip(
                        selected = genre.isEmpty(),
                        onClick = { genre = "" },
                        label = { Text("All") },
                    )
                    genres.forEach { g ->
                        FilterChip(
                            selected = genre == g,
                            onClick = { genre = if (genre == g) "" else g },
                            label = { Text(g) },
                        )
                    }
                }
                Text(
                    "Library (${filtered.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (error != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { ErrorBanner(error, Modifier.padding(0.dp)) }
        }

        if (loading) {
            items(12) { PosterCardSkeleton(width = 112) }
        } else if (filtered.isEmpty() && error == null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    icon = Icons.Default.Movie,
                    title = if (genre.isNotEmpty()) "No matches" else null,
                    message = if (genre.isNotEmpty()) {
                        "Nothing matches this genre. Try another filter."
                    } else {
                        emptyMessage
                    },
                    action = {
                        Text(
                            "Search titles",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = onGoSearch),
                        )
                    },
                )
            }
        } else {
            items(filtered, key = { it.id }) { row ->
                PosterCard(
                    title = row.title,
                    posterUrl = row.posterUrl,
                    subtitle = buildString {
                        if (row.year > 0) append(row.year)
                        if (row.rating > 0) {
                            if (isNotEmpty()) append("  ·  ")
                            append("★ %.1f".format(row.rating))
                        }
                    }.ifEmpty { null },
                    watched = row.watched,
                    width = 112,
                    onClick = { onOpen(row.id) },
                )
            }
            if (loadingMore) {
                items(4) { PosterCardSkeleton(width = 112) }
            }
        }
    }
}

@Composable
private fun SortMenuChip(currentLabel: String, onSelect: (SortKey) -> Unit) {
    var open by remember { mutableStateOf(false) }
    FilterChip(
        selected = false,
        onClick = { open = true },
        label = { Text("Sort: $currentLabel") },
        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
    )
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        SortKey.entries.forEach { key ->
            DropdownMenuItem(
                text = { Text(key.label) },
                onClick = {
                    onSelect(key)
                    open = false
                },
            )
        }
    }
}
