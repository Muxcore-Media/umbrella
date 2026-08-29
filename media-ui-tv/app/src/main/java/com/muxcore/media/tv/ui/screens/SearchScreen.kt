package com.muxcore.media.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.OutlinedTextField
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.muxcore.media.tv.data.Capabilities
import com.muxcore.media.tv.data.LibraryHit
import com.muxcore.media.tv.data.MuxRepository
import com.muxcore.media.tv.data.SearchResult
import com.muxcore.media.tv.data.parseSearchScope
import com.muxcore.media.tv.data.remoteNotInLibrary
import com.muxcore.media.tv.data.resolvePosterPath
import com.muxcore.media.tv.data.runUnifiedSearch
import com.muxcore.media.tv.data.searchResultKey
import com.muxcore.media.tv.ui.components.PosterCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    caps: Capabilities,
    repository: MuxRepository,
    onBack: () -> Unit,
    onMovie: (String) -> Unit,
    onShow: (String) -> Unit,
    onDiscover: (String, Int) -> Unit,
    onNavigate: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var searchScope by remember { mutableStateOf("all") }
    var library by remember { mutableStateOf<List<LibraryHit>>(emptyList()) }
    var remote by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var posterUrls by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var requested by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var error by remember { mutableStateOf<String?>(null) }
    var searched by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun runSearch() {
        if (query.trim().length < 2) return
        searched = true
        error = null
    }

    LaunchedEffect(query, searched, searchScope) {
        if (!searched || query.trim().length < 2) return@LaunchedEffect
        loading = true
        try {
            val (lib, rem) = runUnifiedSearch(repository, caps, query.trim(), parseSearchScope(searchScope))
            val remoteOnly = remoteNotInLibrary(lib, rem)
            library = lib
            remote = remoteOnly
            val urls = mutableMapOf<String, String>()
            for (hit in lib) {
                when (hit) {
                    is LibraryHit.Movie ->
                        urls["lib:m:${hit.item.id}"] =
                            resolvePosterPath(repository, hit.item.posterUrl, "movie")
                    is LibraryHit.Tv ->
                        urls["lib:t:${hit.item.id}"] =
                            resolvePosterPath(repository, hit.item.posterUrl, "tv")
                    is LibraryHit.Other ->
                        if (hit.poster.isNotBlank()) urls["lib:o:${hit.title}"] = hit.poster
                }
            }
            for (row in remoteOnly) {
                val key = searchResultKey(row)
                val kind = when (row.mediaType) {
                    "tv" -> "tv"
                    "music", "music_album", "music_track" -> "music"
                    else -> "movie"
                }
                urls[key] = resolvePosterPath(repository, row.poster, kind)
            }
            posterUrls = urls
        } catch (e: Exception) {
            error = e.message
            library = emptyList()
            remote = emptyList()
        } finally {
            loading = false
        }
    }

  fun requestRemote(row: SearchResult) {
        coroutineScope.launch {
            try {
                val res = repository.requestTitle(
                    title = row.title,
                    mediaType = row.mediaType,
                    tmdbId = if (row.mediaType == "movie" || row.mediaType == "tv") row.id else 0,
                    year = row.year,
                    overview = row.overview,
                    poster = row.poster,
                    musicbrainzId = row.musicbrainzId,
                    releaseGroupId = row.releaseGroupId,
                    recordingId = row.recordingId,
                    artistName = row.artistName,
                    albumTitle = row.albumTitle,
                )
                requested = requested + (searchResultKey(row) to res.status.ifBlank { "requested" })
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Search", style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; searched = false },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search library and TMDB") },
        )
        Button(onClick = { runSearch() }) { Text("Search") }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("all", "movies", "tv", "music", "add")) { s ->
                Button(onClick = { searchScope = s; if (searched) runSearch() }) {
                    Text(if (searchScope == s) "[$s]" else s)
                }
            }
        }
        if (loading) Text(text = "Searching…")
        if (error != null) Text(text = error!!)

        if (library.isNotEmpty()) {
            Text(text = "In your library")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(library, key = {
                    when (it) {
                        is LibraryHit.Movie -> "m:${it.item.id}"
                        is LibraryHit.Tv -> "t:${it.item.id}"
                        is LibraryHit.Other -> "o:${it.title}"
                    }
                }) { hit ->
                    when (hit) {
                        is LibraryHit.Movie -> {
                            val poster = posterUrls["lib:m:${hit.item.id}"].orEmpty()
                            PosterCard(
                                title = hit.item.title,
                                posterUrl = poster,
                                subtitle = "Movie",
                                onClick = { onMovie(hit.item.id) },
                            )
                        }
                        is LibraryHit.Tv -> {
                            val poster = posterUrls["lib:t:${hit.item.id}"].orEmpty()
                            PosterCard(
                                title = hit.item.title,
                                posterUrl = poster,
                                subtitle = "TV",
                                onClick = { onShow(hit.item.id) },
                            )
                        }
                        is LibraryHit.Other -> {
                            PosterCard(
                                title = hit.title,
                                posterUrl = posterUrls["lib:o:${hit.title}"].orEmpty(),
                                subtitle = hit.subtitle,
                                onClick = { onNavigate(hit.route) },
                            )
                        }
                    }
                }
            }
        }

        if (remote.isNotEmpty()) {
            Text(text = "Add to library")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(remote, key = { searchResultKey(it) }) { row ->
                    val key = searchResultKey(row)
                    val poster = posterUrls[key].orEmpty()
                    val isMusic =
                        row.mediaType == "music" || row.mediaType == "music_album" || row.mediaType == "music_track"
                    PosterCard(
                        title = row.title,
                        posterUrl = poster,
                        subtitle = row.mediaType,
                        onClick = {
                            when {
                                isMusic -> requestRemote(row)
                                row.mediaType == "tv" -> onDiscover("tv", row.id)
                                else -> onDiscover("movie", row.id)
                            }
                        },
                    )
                    if (requested.containsKey(key)) {
                        Text(text = requested[key]!!)
                    }
                }
            }
        }

        Button(onClick = onBack) { Text("Back") }
    }
}
