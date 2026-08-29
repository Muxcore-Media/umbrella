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
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.muxcore.media.tv.data.DiscoverDetail
import com.muxcore.media.tv.data.MuxRepository
import com.muxcore.media.tv.data.resolvePosterPath
import com.muxcore.media.tv.ui.components.PosterImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DiscoverDetailScreen(
    mediaType: String,
    tmdbId: Int,
    repository: MuxRepository,
    onBack: () -> Unit,
) {
    var detail by remember { mutableStateOf<DiscoverDetail?>(null) }
    var posterUrl by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var requested by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(mediaType, tmdbId) {
        loading = true
        error = null
        try {
            val d = repository.getDiscoverDetail(mediaType, tmdbId)
            detail = d
            posterUrl = resolvePosterPath(repository, d.poster, if (d.mediaType == "tv") "tv" else "movie")
        } catch (e: Exception) {
            error = e.message
            detail = null
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Discover", style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        if (loading) Text(text = "Loading details…")
        if (error != null) Text(text = error!!)
        detail?.let { d ->
            if (posterUrl.isNotBlank()) {
                PosterImage(url = posterUrl, modifier = Modifier.padding(bottom = 8.dp))
            }
            Text(text = d.title, style = androidx.tv.material3.MaterialTheme.typography.titleLarge)
            val meta = listOfNotNull(
                if (d.year > 0) d.year.toString() else null,
                if (d.runtime > 0) "${d.runtime} min" else null,
                d.status.takeIf { it.isNotBlank() },
                if (d.voteAvg > 0) "${d.voteAvg} rating" else null,
            ).joinToString(" · ")
            if (meta.isNotBlank()) Text(text = meta)
            if (d.tagline.isNotBlank()) Text(text = d.tagline)
            if (d.overview.isNotBlank()) Text(text = d.overview)
            if (requested != null) {
                Text(text = requested!!)
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val res = repository.requestTitle(
                                    title = d.title,
                                    mediaType = d.mediaType,
                                    tmdbId = d.id,
                                    year = d.year,
                                    overview = d.overview,
                                    poster = d.poster,
                                )
                                requested = res.status.ifBlank { "requested" }
                            } catch (e: Exception) {
                                error = e.message
                            }
                        }
                    },
                ) {
                    Text(if (d.mediaType == "tv") "Request series" else "Request movie")
                }
            }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StudiosScreen(
    repository: MuxRepository,
    onBack: () -> Unit,
    onMovie: (String) -> Unit,
) {
    var movies by remember { mutableStateOf<List<com.muxcore.media.tv.data.MovieItem>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            movies = repository.listMovies(1, 500).items
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    val studios = remember(movies) {
        val counts = mutableMapOf<String, Int>()
        for (m in movies) {
            val keys = when {
                m.collectionName.isNotBlank() -> listOf(m.collectionName.trim())
                m.genres.isNotEmpty() -> m.genres
                else -> listOf("Unknown")
            }
            for (k in keys) counts[k] = (counts[k] ?: 0) + 1
        }
        counts.entries.sortedBy { it.key }.map { it.key to it.value }
    }

    val filtered = remember(movies, selected) {
        val name = selected
        if (name == null) emptyList()
        else movies.filter { m ->
            m.collectionName.trim() == name || m.genres.contains(name)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Studios & collections", style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        Text(text = "Browse by studio, franchise, or genre.")
        if (loading) Text(text = "Loading…")
        if (error != null) Text(text = error!!)
        if (selected == null) {
            for ((name, count) in studios) {
                Button(onClick = { selected = name }) {
                    Text("$name ($count)")
                }
            }
        } else {
            Text(text = selected!!)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered, key = { it.id }) { movie ->
                    Button(onClick = { onMovie(movie.id) }) { Text(movie.title) }
                }
            }
            Button(onClick = { selected = null }) { Text("All studios") }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}
