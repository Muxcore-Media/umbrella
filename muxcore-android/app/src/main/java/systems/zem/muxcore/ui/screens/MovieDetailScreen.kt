package systems.zem.muxcore.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import systems.zem.muxcore.data.FavoriteEntry
import systems.zem.muxcore.data.ProgressEntry
import systems.zem.muxcore.data.QueueItem
import systems.zem.muxcore.ui.Load
import systems.zem.muxcore.ui.Routes
import systems.zem.muxcore.ui.appContainer
import systems.zem.muxcore.ui.components.BadgeTone
import systems.zem.muxcore.ui.components.DetailHero
import systems.zem.muxcore.ui.components.ErrorBanner
import systems.zem.muxcore.ui.components.HeroSkeleton
import systems.zem.muxcore.ui.components.ScrimColor
import systems.zem.muxcore.ui.components.StatusBadge
import systems.zem.muxcore.ui.components.formatRuntime
import systems.zem.muxcore.ui.rememberLoad

@Composable
fun MovieDetailScreen(nav: NavHostController, id: String) {
    val container = appContainer()
    val api = container.api
    val userData = container.userData
    val context = LocalContext.current

    val load by rememberLoad(id) { api.getMovie(id) }
    val jellyfin by rememberLoad(id) { api.jellyfinPlayUrl(id) }

    var fav by rememberSaveable(id) { mutableStateOf(userData.isFavorite(id)) }
    var watched by rememberSaveable(id) { mutableStateOf(userData.getProgress(id)?.watched == true) }
    var queued by remember(id) { mutableStateOf(userData.listQueue().any { it.id == id }) }

    Box(Modifier.fillMaxSize()) {
        when (val state = load) {
            is Load.Loading -> HeroSkeleton()
            is Load.Failed -> Column(Modifier.statusBarsPadding().padding(16.dp)) {
                ErrorBanner(state.message)
                Text(
                    "Back to movies",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable { nav.popBackStack() },
                )
            }
            is Load.Ready -> {
                val movie = state.value
                val playRoute = if (movie.hasFile && movie.streamUrl.isNotEmpty()) Routes.moviePlayer(movie) else null

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    DetailHero(
                        title = movie.title,
                        backdropUrl = movie.backdropUrl,
                        posterUrl = movie.posterUrl,
                        tagline = movie.tagline,
                        overview = movie.overview.ifEmpty { "No overview." },
                        meta = {
                            if (movie.hasFile) StatusBadge("Available", BadgeTone.ACCENT)
                            if (watched) StatusBadge("Watched", BadgeTone.NEUTRAL)
                            if (movie.voteAverage > 0)

                                StatusBadge(
                                    "%.1f".format(movie.voteAverage),
                                    BadgeTone.NEUTRAL,
                                    icon = Icons.Default.Star,
                                )
                            MetaText(if (movie.year > 0) movie.year.toString() else "—")
                            if (movie.runtime > 0) MetaText(formatRuntime(movie.runtime))
                            if (movie.genres.isNotEmpty()) MetaText(movie.genres.take(3).joinToString(" · "))
                        },
                        actions = {
                            if (playRoute != null) {
                                Button(onClick = { nav.navigate(playRoute) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Text("  Play")
                                }
                            } else {
                                Text(
                                    "Not available to stream yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                )
                            }
                            FilledTonalButton(onClick = {
                                userData.enqueue(
                                    QueueItem(
                                        id = movie.id,
                                        kind = "movie",
                                        title = movie.title,
                                        href = "/movies/${movie.id}",
                                        streamUrl = movie.streamUrl,
                                        posterUrl = movie.posterUrl,
                                    ),
                                )
                                queued = true
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(if (queued) "  Queued" else "  Add to queue")
                            }
                            FilledTonalButton(onClick = {
                                fav = userData.toggleFavorite(
                                    FavoriteEntry(
                                        id = movie.id,
                                        kind = "movie",
                                        title = movie.title,
                                        posterUrl = movie.posterUrl,
                                        href = "/movies/${movie.id}",
                                        year = movie.year,
                                    ),
                                )
                            }) {
                                Icon(
                                    if (fav) Icons.Default.Star else Icons.Outlined.StarOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (fav) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Unspecified,
                                )
                                Text(if (fav) "  Favorited" else "  Favorite")
                            }
                            FilledTonalButton(onClick = {
                                val next = !watched
                                userData.upsertProgress(
                                    ProgressEntry(
                                        id = movie.id,
                                        kind = "movie",
                                        title = movie.title,
                                        posterUrl = movie.posterUrl,
                                        href = "/movies/${movie.id}",
                                        streamUrl = movie.streamUrl,
                                        positionSec = if (next) 0.0 else userData.getProgress(movie.id)?.positionSec ?: 0.0,
                                        durationSec = movie.runtime * 60.0,
                                        watched = next,
                                    ),
                                )
                                watched = next
                            }) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(if (watched) "  Mark unwatched" else "  Mark watched")
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

                    if (!movie.collectionName.isNullOrEmpty() && movie.collectionId != null) {
                        Text(
                            buildString { append("Part of ") },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Text(
                            movie.collectionName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .clickable { nav.navigate(Routes.COLLECTIONS) },
                        )
                    }
                    Spacer(Modifier.height(24.dp))
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
internal fun MetaText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
