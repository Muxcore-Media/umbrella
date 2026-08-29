package systems.zem.muxcore.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import systems.zem.muxcore.data.Episode
import systems.zem.muxcore.data.FavoriteEntry
import systems.zem.muxcore.data.QueueItem
import systems.zem.muxcore.data.TVShow
import systems.zem.muxcore.ui.Load
import systems.zem.muxcore.ui.Routes
import systems.zem.muxcore.ui.appContainer
import systems.zem.muxcore.ui.components.BadgeTone
import systems.zem.muxcore.ui.components.DetailHero
import systems.zem.muxcore.ui.components.EmptyState
import systems.zem.muxcore.ui.components.ErrorBanner
import systems.zem.muxcore.ui.components.HeroSkeleton
import systems.zem.muxcore.ui.components.ScrimColor
import systems.zem.muxcore.ui.components.StatusBadge
import systems.zem.muxcore.ui.components.episodeCode
import systems.zem.muxcore.ui.rememberLoad

@Composable
fun TVShowDetailScreen(nav: NavHostController, id: String) {
    val container = appContainer()
    val api = container.api
    val userData = container.userData
    val context = LocalContext.current

    val load by rememberLoad(id) { api.getTVShow(id) }
    val jellyfin by rememberLoad(id) { api.jellyfinPlayUrl(id) }
    var fav by rememberSaveable(id) { mutableStateOf(userData.isFavorite(id)) }
    val blob by userData.blob.collectAsState()

    Box(Modifier.fillMaxSize()) {
        when (val state = load) {
            is Load.Loading -> HeroSkeleton()
            is Load.Failed -> Column(Modifier.statusBarsPadding().padding(16.dp)) {
                ErrorBanner(state.message)
                Text(
                    "Back to TV",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable { nav.popBackStack() },
                )
            }
            is Load.Ready -> {
                val show = state.value
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        DetailHero(
                            title = show.title,
                            backdropUrl = show.backdropUrl,
                            posterUrl = show.posterUrl,
                            overview = show.overview.ifEmpty { "No overview." },
                            meta = {
                                if (show.hasFile) StatusBadge("Available", BadgeTone.ACCENT)
                                if (show.voteAverage > 0) {
                                    StatusBadge(
                                        "%.1f".format(show.voteAverage),
                                        BadgeTone.NEUTRAL,
                                        icon = Icons.Default.Star,
                                    )
                                }
                                MetaText(if (show.year > 0) show.year.toString() else "—")
                                show.status?.takeIf { it.isNotEmpty() }?.let { MetaText(it) }
                                if (show.genres.isNotEmpty()) MetaText(show.genres.take(3).joinToString(" · "))
                            },
                            actions = {
                                FilledTonalButton(onClick = {
                                    fav = userData.toggleFavorite(
                                        FavoriteEntry(
                                            id = show.id,
                                            kind = "tv",
                                            title = show.title,
                                            posterUrl = show.posterUrl,
                                            href = "/tv/${show.id}",
                                            year = show.year,
                                        ),
                                    )
                                }) {
                                    Icon(
                                        if (fav) Icons.Default.Star else Icons.Outlined.StarOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (fav) MaterialTheme.colorScheme.primary
                                        else androidx.compose.ui.graphics.Color.Unspecified,
                                    )
                                    Text(if (fav) "  Favorited" else "  Favorite")
                                }
                                val jfUrl = (jellyfin as? Load.Ready)?.value
                                if (jfUrl != null) {
                                    FilledTonalButton(onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, jfUrl.toUri()))
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text("  Open in linked app")
                                    }
                                }
                            },
                        )
                    }

                    val seasons = show.seasons.orEmpty()
                    if (seasons.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Default.Tv,
                                title = "No episodes yet",
                                message = "Episode listings will appear here once metadata is available for this series.",
                            )
                        }
                    } else {
                        item {
                            Text(
                                "Episodes",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        seasons.forEach { season ->
                            item(key = "season-${season.id}") {
                                Text(
                                    season.name.ifEmpty { "Season ${season.seasonNumber}" }.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                                )
                            }
                            items(season.episodes, key = { "ep-${it.id}" }) { ep ->
                                EpisodeRow(
                                    show = show,
                                    ep = ep,
                                    watched = blob.progress[ep.id]?.watched == true,
                                    onPlay = { route -> nav.navigate(route) },
                                    onQueue = { route ->
                                        val epTitle = "${show.title} ${episodeCode(ep.seasonNumber, ep.episodeNumber)}"
                                        userData.enqueue(
                                            QueueItem(
                                                id = ep.id,
                                                kind = "episode",
                                                title = epTitle,
                                                href = "/$route",
                                                streamUrl = ep.streamUrl,
                                                posterUrl = show.posterUrl,
                                            ),
                                        )
                                    },
                                )
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

@Composable
private fun EpisodeRow(
    show: TVShow,
    ep: Episode,
    watched: Boolean,
    onPlay: (String) -> Unit,
    onQueue: (String) -> Unit,
) {
    val playRoute = Routes.episodePlayer(show, ep)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        episodeCode(ep.seasonNumber, ep.episodeNumber),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (ep.title.isNotEmpty()) {
                        Text(
                            ep.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (watched) StatusBadge("✓", BadgeTone.SUCCESS)
                }
                if (ep.overview.isNotEmpty()) {
                    Text(
                        ep.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (playRoute != null) {
                FilledTonalIconButton(onClick = { onQueue(playRoute) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Add to queue",
                        modifier = Modifier.size(20.dp),
                    )
                }
                FilledIconButton(onClick = { onPlay(playRoute) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(20.dp))
                }
            } else {
                StatusBadge("No file", BadgeTone.NEUTRAL)
            }
        }
    }
}
