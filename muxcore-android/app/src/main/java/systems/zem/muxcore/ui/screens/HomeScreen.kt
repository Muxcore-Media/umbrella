package systems.zem.muxcore.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import systems.zem.muxcore.data.Acquisition
import systems.zem.muxcore.data.FavoriteEntry
import systems.zem.muxcore.data.Movie
import systems.zem.muxcore.data.NextUpEntry
import systems.zem.muxcore.data.ProgressEntry
import systems.zem.muxcore.data.TVShow
import systems.zem.muxcore.ui.Routes
import systems.zem.muxcore.ui.appContainer
import systems.zem.muxcore.ui.components.EmptyState
import systems.zem.muxcore.ui.components.ErrorBanner
import systems.zem.muxcore.ui.components.HeroBanner
import systems.zem.muxcore.ui.components.HeroItem
import systems.zem.muxcore.ui.components.HeroSkeleton
import systems.zem.muxcore.ui.components.PosterCard
import systems.zem.muxcore.ui.components.ProgressCard
import systems.zem.muxcore.ui.components.Shelf
import systems.zem.muxcore.ui.components.ShelfSkeleton

private data class HomeData(
    val progress: List<ProgressEntry>,
    val nextUp: List<NextUpEntry>,
    val favorites: List<FavoriteEntry>,
    val recommended: List<Movie>,
    val readyMovies: List<Movie>,
    val readyShows: List<TVShow>,
    val inProgressCount: Int,
)

@Composable
fun HomeScreen(nav: NavHostController) {
    val container = appContainer()
    val api = container.api
    val userData = container.userData
    val prefs = userData.prefs.home

    var data by remember { mutableStateOf<HomeData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            // Prefer server progress before painting rows (mirrors web Home).
            userData.pullFromServer()
            val progress = userData.continueWatching(16)
            val favorites = userData.listFavorites().take(16)
            val nextUp = userData.resolveNextUp({ api.getTVShow(it) }, 16)
            coroutineScope {
                val requestsD = async { runCatching { api.listRequests() }.getOrDefault(emptyList()) }
                val moviesD = async { api.listMovies(1, 24) }
                val showsD = async { api.listTVShows(1, 24) }
                val movies = moviesD.await()
                val shows = showsD.await()
                val requests = requestsD.await()
                val recommended = movies.items
                    .filter { it.hasFile && it.voteAverage > 0 }
                    .sortedByDescending { it.voteAverage }
                    .take(16)
                data = HomeData(
                    progress = progress,
                    nextUp = nextUp,
                    favorites = favorites,
                    recommended = recommended,
                    readyMovies = movies.items.filter { it.hasFile }.take(16),
                    readyShows = shows.items.filter { it.hasFile }.take(16),
                    inProgressCount = Acquisition.mergeInProgressEntries(requests, movies.items, shows.items).size,
                )
            }
        } catch (e: Exception) {
            error = e.message ?: "Failed to load home"
        } finally {
            loading = false
        }
    }

    val d = data
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (loading) {
            item { HeroSkeleton() }
            item { ShelfSkeleton() }
            item { ShelfSkeleton() }
            return@LazyColumn
        }
        if (error != null) {
            item { ErrorBanner(error!!, modifier = Modifier.statusBarsPadding().padding(horizontal = 16.dp)) }
        }
        if (d == null) return@LazyColumn

        val heroMovie = d.readyMovies.firstOrNull { it.backdropUrl.isNotEmpty() }
            ?: d.readyMovies.firstOrNull()
            ?: d.recommended.firstOrNull { it.backdropUrl.isNotEmpty() }
            ?: d.recommended.firstOrNull()

        val showReadyFallback = prefs.showNextUp && d.nextUp.isEmpty() &&
            (d.readyMovies.isNotEmpty() || d.readyShows.isNotEmpty())
        val hasContent = (prefs.showContinueWatching && d.progress.isNotEmpty()) ||
            (prefs.showNextUp && d.nextUp.isNotEmpty()) ||
            d.recommended.isNotEmpty() ||
            (prefs.showFavorites && d.favorites.isNotEmpty()) ||
            showReadyFallback ||
            (prefs.showRecentRequests && d.inProgressCount > 0)

        if (heroMovie != null) {
            item {
                HeroBanner(
                    item = HeroItem(
                        id = heroMovie.id,
                        title = heroMovie.title,
                        overview = heroMovie.overview,
                        year = heroMovie.year,
                        voteAverage = heroMovie.voteAverage,
                        genres = heroMovie.genres,
                        backdropUrl = heroMovie.backdropUrl,
                        posterUrl = heroMovie.posterUrl,
                        ready = heroMovie.hasFile,
                    ),
                    onPlay = { nav.navigate(Routes.moviePlayer(heroMovie)) },
                    onDetail = { nav.navigate(Routes.movieDetail(heroMovie.id)) },
                )
            }
        }

        if (heroMovie == null && !hasContent) {
            item {
                EmptyState(
                    icon = Icons.Default.Home,
                    title = "Your home feed is empty",
                    message = "Search for titles, browse movies and TV, or add favorites to populate this page.",
                    action = {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { nav.navigate(Routes.search()) }) { Text("Search") }
                            FilledTonalButton(onClick = { nav.navigate(Routes.MOVIES) }) { Text("Movies") }
                            FilledTonalButton(onClick = { nav.navigate(Routes.TV) }) { Text("TV") }
                        }
                    },
                )
            }
            return@LazyColumn
        }

        if (prefs.showContinueWatching && d.progress.isNotEmpty()) {
            item {
                Shelf(title = "Continue watching") {
                    items(d.progress, key = { it.id }) { p ->
                        val pct = if (p.durationSec > 0) (p.positionSec / p.durationSec * 100).toFloat() else 5f
                        ProgressCard(
                            title = p.title,
                            posterUrl = p.posterUrl,
                            subtitle = if (p.durationSec > 0) "${pct.toInt()}% · resume" else "Resume",
                            progressPct = pct,
                            onClick = {
                                val route = Routes.progressPlayer(p) ?: Routes.fromHref(p.href)
                                route?.let { nav.navigate(it) }
                            },
                        )
                    }
                }
            }
        }

        if (prefs.showNextUp && d.nextUp.isNotEmpty()) {
            item {
                Shelf(title = "Next up") {
                    items(d.nextUp, key = { it.id }) { n ->
                        ProgressCard(
                            title = n.title,
                            posterUrl = n.posterUrl,
                            subtitle = n.subtitle.ifEmpty { "Next up" },
                            onClick = {
                                val show = n.show
                                val ep = n.episode
                                if (show != null && ep != null) {
                                    Routes.episodePlayer(show, ep)?.let { nav.navigate(it) }
                                } else if (n.showId != null) {
                                    nav.navigate(Routes.tvDetail(n.showId))
                                }
                            },
                        )
                    }
                }
            }
        }

        if (d.recommended.isNotEmpty()) {
            item {
                Shelf(title = "Recommended", seeAll = { nav.navigate(Routes.MOVIES) }) {
                    items(d.recommended, key = { it.id }) { m ->
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

        if (prefs.showFavorites && d.favorites.isNotEmpty()) {
            item {
                Shelf(title = "Favorites", seeAll = { nav.navigate(Routes.FAVORITES) }) {
                    items(d.favorites, key = { it.id }) { f ->
                        PosterCard(
                            title = f.title,
                            posterUrl = f.posterUrl,
                            subtitle = f.year?.takeIf { it > 0 }?.toString(),
                            onClick = {
                                val route = if (f.kind == "tv") Routes.tvDetail(f.id) else Routes.movieDetail(f.id)
                                nav.navigate(route)
                            },
                        )
                    }
                }
            }
        }

        if (showReadyFallback) {
            item {
                Shelf(title = "Available now") {
                    items(d.readyMovies, key = { "m-${it.id}" }) { m ->
                        PosterCard(
                            title = m.title,
                            posterUrl = m.posterUrl,
                            subtitle = if (m.year > 0) m.year.toString() else null,
                            onClick = { nav.navigate(Routes.movieDetail(m.id)) },
                        )
                    }
                    items(d.readyShows, key = { "t-${it.id}" }) { s ->
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

        if (prefs.showRecentRequests && d.inProgressCount > 0) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { nav.navigate(Routes.REQUESTS) },
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            "${d.inProgressCount} " +
                                (if (d.inProgressCount == 1) "title is" else "titles are") +
                                " being requested or downloaded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "View in progress",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}
