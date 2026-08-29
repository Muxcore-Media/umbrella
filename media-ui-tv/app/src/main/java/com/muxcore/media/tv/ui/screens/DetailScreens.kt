package com.muxcore.media.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.muxcore.media.tv.data.FavoriteEntry
import com.muxcore.media.tv.data.MuxRepository
import com.muxcore.media.tv.data.PlayRequest
import com.muxcore.media.tv.data.UserdataStore
import com.muxcore.media.tv.data.resolvePosterPath
import com.muxcore.media.tv.navigation.episodePlayRequest
import com.muxcore.media.tv.navigation.moviePlayRequest
import com.muxcore.media.tv.ui.components.PosterImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movieId: String,
    repository: MuxRepository,
    userdata: UserdataStore,
    onBack: () -> Unit,
    onPlay: (PlayRequest) -> Unit,
) {
    var movie by remember { mutableStateOf<com.muxcore.media.tv.data.MovieItem?>(null) }
    var isFav by remember { mutableStateOf(false) }
    var posterUrl by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(movieId) {
        try {
            val m = repository.getMovie(movieId)
            movie = m
            posterUrl = resolvePosterPath(repository, m.posterUrl, "movie")
            isFav = userdata.isFavorite(movieId)
        } catch (e: Exception) {
            error = e.message
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val m = movie
        if (m != null) {
            PosterImage(url = posterUrl, modifier = Modifier.width(200.dp).height(300.dp))
            Text(text = m.title, style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
            if (m.year > 0) Text(text = m.year.toString())
            if (m.overview.isNotBlank()) Text(text = m.overview)
            if (m.hasFile) {
                Button(onClick = { onPlay(moviePlayRequest(m)) }) { Text("Play") }
            }
            Button(onClick = {
                scope.launch {
                    userdata.toggleFavorite(
                        FavoriteEntry(
                            id = m.id,
                            kind = "movie",
                            title = m.title,
                            posterUrl = m.posterUrl,
                            href = "/movies/${m.id}",
                            year = m.year,
                        ),
                    )
                    isFav = userdata.isFavorite(m.id)
                }
            }) {
                Text(if (isFav) "Remove favorite" else "Add favorite")
            }
        }
        if (error != null) Text(text = error!!)
        Button(onClick = onBack) { Text("Back") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvDetailScreen(
    showId: String,
    repository: MuxRepository,
    userdata: UserdataStore,
    onBack: () -> Unit,
    onPlay: (PlayRequest) -> Unit,
) {
    var show by remember { mutableStateOf<com.muxcore.media.tv.data.TvShowItem?>(null) }
    var isFav by remember { mutableStateOf(false) }
    var posterUrl by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(showId) {
        try {
            val s = repository.getTvShow(showId)
            show = s
            posterUrl = resolvePosterPath(repository, s.posterUrl, "tv")
            isFav = userdata.isFavorite(showId)
        } catch (e: Exception) {
            error = e.message
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val s = show
        if (s != null) {
            PosterImage(url = posterUrl, modifier = Modifier.width(200.dp).height(300.dp))
            Text(text = s.title, style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
            if (s.overview.isNotBlank()) Text(text = s.overview)
            Button(onClick = {
                scope.launch {
                    userdata.toggleFavorite(
                        FavoriteEntry(
                            id = s.id,
                            kind = "tv",
                            title = s.title,
                            posterUrl = s.posterUrl,
                            href = "/tv/${s.id}",
                            year = s.year,
                        ),
                    )
                    isFav = userdata.isFavorite(s.id)
                }
            }) {
                Text(if (isFav) "Remove favorite" else "Add favorite")
            }
            s.seasons.forEach { season ->
                Text(text = season.name.ifBlank { "Season ${season.seasonNumber}" })
                season.episodes.filter { it.hasFile }.forEach { ep ->
                    Button(onClick = { onPlay(episodePlayRequest(s, ep)) }) {
                        Text("S${ep.seasonNumber}E${ep.episodeNumber} ${ep.title}")
                    }
                }
            }
        }
        if (error != null) Text(text = error!!)
        Button(onClick = onBack) { Text("Back") }
    }
}
