package systems.zem.muxcore.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import systems.zem.muxcore.data.SearchResult
import systems.zem.muxcore.data.TmdbImages
import systems.zem.muxcore.ui.Load
import systems.zem.muxcore.ui.Routes
import systems.zem.muxcore.ui.appContainer
import systems.zem.muxcore.ui.listTopContentPadding
import systems.zem.muxcore.ui.components.BadgeTone
import systems.zem.muxcore.ui.components.DetailHero
import systems.zem.muxcore.ui.components.EmptyState
import systems.zem.muxcore.ui.components.ErrorBanner
import systems.zem.muxcore.ui.components.HeroSkeleton
import systems.zem.muxcore.ui.components.MuxImage
import systems.zem.muxcore.ui.components.ScrimColor
import systems.zem.muxcore.ui.components.SectionHeader
import systems.zem.muxcore.ui.components.ShelfSkeleton
import systems.zem.muxcore.ui.components.StatusBadge
import systems.zem.muxcore.ui.components.formatRuntime
import systems.zem.muxcore.ui.rememberLoad

private data class BrowseShelf(val title: String, val items: List<SearchResult>)

/** Trending/popular TMDB shelves with request actions (port of web Discover). */
@Composable
fun DiscoverScreen(nav: NavHostController) {
    val api = appContainer().api
    val scope = rememberCoroutineScope()
    val requested = remember { mutableStateMapOf<Int, String>() }
    var error by remember { mutableStateOf<String?>(null) }

    val load by rememberLoad(Unit) {
        coroutineScope {
            val tm = async { api.discoverBrowse("trending", "movie") }
            val tt = async { api.discoverBrowse("trending", "tv") }
            val pm = async { api.discoverBrowse("popular", "movie") }
            val pt = async { api.discoverBrowse("popular", "tv") }
            listOf(
                BrowseShelf("Trending movies", tm.await()),
                BrowseShelf("Trending TV", tt.await()),
                BrowseShelf("Popular movies", pm.await()),
                BrowseShelf("Popular TV", pt.await()),
            )
        }
    }

    fun request(item: SearchResult) {
        scope.launch {
            try {
                val res = api.requestTitle(item.id, item.title, item.year, item.overview, item.poster, item.mediaType)
                requested[item.id] = res.status.ifEmpty { "requested" }
            } catch (e: Exception) {
                error = e.message ?: "Request failed"
            }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = listTopContentPadding(extraTop = 12.dp, bottom = 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Discover", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Browse trending and popular titles from TMDB, then request something new.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (error != null) item { ErrorBanner(error!!) }
        when (val state = load) {
            is Load.Loading -> {
                item { ShelfSkeleton() }
                item { ShelfSkeleton() }
            }
            is Load.Failed -> item { ErrorBanner(state.message) }
            is Load.Ready -> {
                val shelves = state.value.filter { it.items.isNotEmpty() }
                if (shelves.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Explore,
                            message = "Discover lists are empty right now. Try again later.",
                        )
                    }
                } else {
                    shelves.forEach { shelf ->
                        item(key = shelf.title) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SectionHeader(shelf.title)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                                ) {
                                    items(shelf.items, key = { "${it.mediaType}-${it.id}" }) { r ->
                                        DiscoverPosterCard(
                                            item = r,
                                            requestedStatus = requested[r.id],
                                            onOpen = { nav.navigate(Routes.discoverDetail(r.mediaType, r.id)) },
                                            onRequest = { request(r) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun DiscoverPosterCard(
    item: SearchResult,
    requestedStatus: String?,
    onOpen: () -> Unit,
    onRequest: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            MuxImage(
                TmdbImages.url(item.poster, "w342").ifEmpty { null },
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.align(Alignment.TopStart).padding(6.dp)) {
                StatusBadge(if (item.mediaType == "tv") "TV" else "Movie", BadgeTone.NEUTRAL)
            }
        }
        Text(
            item.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (requestedStatus != null) requestedStatus.uppercase() else "Request",
            style = MaterialTheme.typography.labelMedium,
            color = if (requestedStatus != null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = requestedStatus == null, onClick = onRequest)
                .padding(vertical = 2.dp),
        )
    }
}

/** TMDB title detail with trailer, cast, and request action (port of web DiscoverDetail). */
@Composable
fun DiscoverDetailScreen(nav: NavHostController, type: String, id: Int) {
    val api = appContainer().api
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var requestedStatus by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val load by rememberLoad(type, id) { api.getDiscoverDetail(type, id) }

    Box(Modifier.fillMaxSize()) {
        when (val state = load) {
            is Load.Loading -> HeroSkeleton()
            is Load.Failed -> Column(Modifier.statusBarsPadding().padding(16.dp)) { ErrorBanner(state.message) }
            is Load.Ready -> {
                val detail = state.value
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        DetailHero(
                            title = detail.title,
                            backdropUrl = TmdbImages.url(detail.backdrop, "w780").ifEmpty { null },
                            posterUrl = TmdbImages.url(detail.poster, "w500").ifEmpty { null },
                            tagline = detail.tagline,
                            overview = detail.overview,
                            meta = {
                                StatusBadge(if (detail.mediaType == "tv") "TV Series" else "Movie", BadgeTone.NEUTRAL)
                                if (detail.year > 0) MetaText(detail.year.toString())
                                detail.runtime?.takeIf { it > 0 }?.let { MetaText(formatRuntime(it)) }
                                if (detail.voteAvg > 0) MetaText("★ %.1f".format(detail.voteAvg))
                                detail.status?.takeIf { it.isNotEmpty() }?.let { MetaText(it) }
                            },
                            actions = {
                                if (requestedStatus != null) {
                                    StatusBadge(requestedStatus!!, BadgeTone.SUCCESS)
                                } else {
                                    Button(onClick = {
                                        scope.launch {
                                            try {
                                                val res = api.requestTitle(
                                                    tmdbId = detail.id,
                                                    title = detail.title,
                                                    year = detail.year,
                                                    overview = detail.overview,
                                                    poster = detail.poster,
                                                    mediaType = detail.mediaType,
                                                )
                                                requestedStatus = res.status.ifEmpty { "requested" }
                                            } catch (e: Exception) {
                                                error = e.message ?: "Request failed"
                                            }
                                        }
                                    }) {
                                        Text("Request ${if (detail.mediaType == "tv") "series" else "movie"}")
                                    }
                                }
                            },
                        )
                    }

                    if (error != null) item { ErrorBanner(error!!) }

                    if (detail.genres.isNotEmpty()) {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                            ) {
                                detail.genres.forEach { StatusBadge(it, BadgeTone.NEUTRAL) }
                            }
                        }
                    }

                    val trailer = detail.trailer
                    if (trailer != null && trailer.youtubeKey.isNotEmpty()) {
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp),
                            ) {
                                Text("Trailer", style = MaterialTheme.typography.titleLarge)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    TmdbImages.youtubeWatchUrl(trailer.youtubeKey).toUri(),
                                                ),
                                            )
                                        }
                                        .padding(vertical = 6.dp),
                                ) {
                                    Icon(
                                        Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp),
                                    )
                                    Text(
                                        trailer.name.ifEmpty { "${detail.title} trailer" } + " · YouTube",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    if (detail.cast.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "Cast",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                                ) {
                                    items(detail.cast, key = { it.id }) { member ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.width(84.dp),
                                        ) {
                                            Box(
                                                Modifier
                                                    .size(72.dp)
                                                    .clip(CircleShape),
                                            ) {
                                                MuxImage(
                                                    TmdbImages.url(member.profilePath, "w185").ifEmpty { null },
                                                    contentDescription = member.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            }
                                            Text(
                                                member.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 2,
                                                textAlign = TextAlign.Center,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            if (!member.character.isNullOrEmpty()) {
                                                Text(
                                                    member.character,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    textAlign = TextAlign.Center,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }

        FilledIconButton(
            onClick = { nav.popBackStack() },
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = ScrimColor),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp, top = 8.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
