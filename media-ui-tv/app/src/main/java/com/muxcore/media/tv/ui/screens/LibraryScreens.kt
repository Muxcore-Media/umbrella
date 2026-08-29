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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.muxcore.media.tv.data.LibraryRow
import com.muxcore.media.tv.data.MovieItem
import com.muxcore.media.tv.data.MuxRepository
import com.muxcore.media.tv.data.PlayRequest
import com.muxcore.media.tv.data.TvShowItem
import com.muxcore.media.tv.data.resolvePosterPath
import com.muxcore.media.tv.navigation.moviePlayRequest
import com.muxcore.media.tv.ui.components.PosterCard

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MoviesScreen(
    repository: MuxRepository,
    library: String? = null,
    title: String = "Movies",
    onBack: () -> Unit,
    onMovie: (String) -> Unit,
) {
    var movies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(library) {
        try {
            movies = repository.listMovies(1, 96, library).items
        } catch (e: Exception) {
            error = e.message
        }
    }
    PosterGridScreen(
        title = title,
        error = error,
        onBack = onBack,
        items = movies,
        key = { it.id },
        poster = { it.posterUrl },
        label = { it.title },
        onClick = { onMovie(it.id) },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvShowsScreen(
    repository: MuxRepository,
    onBack: () -> Unit,
    onShow: (String) -> Unit,
) {
    var shows by remember { mutableStateOf<List<TvShowItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            shows = repository.listTvShows(1, 96).items
        } catch (e: Exception) {
            error = e.message
        }
    }
    PosterGridScreen(
        title = "TV Shows",
        error = error,
        onBack = onBack,
        items = shows,
        key = { it.id },
        poster = { it.posterUrl },
        label = { it.title },
        onClick = { onShow(it.id) },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MixedScreen(
    repository: MuxRepository,
    onBack: () -> Unit,
    onMovie: (String) -> Unit,
    onShow: (String) -> Unit,
) {
    var movies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var shows by remember { mutableStateOf<List<TvShowItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            movies = repository.listMovies(1, 100).items
            shows = repository.listTvShows(1, 100).items
        } catch (e: Exception) {
            error = e.message
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Mixed", style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        if (error != null) Text(text = error!!)
        if (movies.isNotEmpty()) {
            Text(text = "Movies", style = androidx.tv.material3.MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(movies, key = { it.id }) { movie ->
                    PosterCard(
                        title = movie.title,
                        posterUrl = movie.posterUrl,
                        onClick = { onMovie(movie.id) },
                    )
                }
            }
        }
        if (shows.isNotEmpty()) {
            Text(text = "TV Shows", style = androidx.tv.material3.MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(shows, key = { it.id }) { show ->
                    PosterCard(
                        title = show.title,
                        posterUrl = show.posterUrl,
                        onClick = { onShow(show.id) },
                    )
                }
            }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LibraryRowsScreen(
    title: String,
    apiPath: String,
    repository: MuxRepository,
    onBack: () -> Unit,
    onRow: (String) -> Unit,
) {
    var rows by remember { mutableStateOf<List<LibraryRow>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(apiPath) {
        try {
            val res = repository.listLibrary(apiPath)
            rows = res.items
            if (res.message.isNotBlank()) message = res.message
        } catch (e: Exception) {
            message = e.message
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        if (message != null) Text(text = message!!)
        for (row in rows) {
            Button(onClick = { onRow(row.id) }) {
                Text(row.name.ifBlank { row.title })
            }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MusicArtistScreen(
    artistId: String,
    repository: MuxRepository,
    onBack: () -> Unit,
    onPlay: (PlayRequest) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    LaunchedEffect(artistId) {
        val detail = repository.getMusicArtist(artistId)
        name = detail.name.ifBlank { detail.artist.name }
        tracks = detail.albums.flatMap { album ->
            album.tracks.map { t -> (t.title.ifBlank { album.title }) to t.streamUrl }
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = name, style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        for ((title, stream) in tracks) {
            if (stream.isNotBlank()) {
                Button(onClick = { onPlay(PlayRequest(src = stream, title = title, kind = "music")) }) {
                    Text(title)
                }
            }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BookAuthorScreen(
    authorId: String,
    repository: MuxRepository,
    onBack: () -> Unit,
    onPlay: (PlayRequest) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    LaunchedEffect(authorId) {
        val detail = repository.getBookAuthor(authorId)
        name = detail.author.name.ifBlank { detail.author.title }
        files = detail.books.flatMap { book ->
            book.files.map { f -> (f.title.ifBlank { book.title }) to f.streamUrl }
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = name, style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        for ((title, stream) in files) {
            if (stream.isNotBlank()) {
                Button(onClick = { onPlay(PlayRequest(src = stream, title = title, kind = "book")) }) {
                    Text(title)
                }
            }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun <T> PosterGridScreen(
    title: String,
    error: String?,
    onBack: () -> Unit,
    items: List<T>,
    key: (T) -> String,
    poster: (T) -> String,
    label: (T) -> String,
    onClick: (T) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        if (error != null) Text(text = error)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = key) { item ->
                PosterCard(title = label(item), posterUrl = poster(item), onClick = { onClick(item) })
            }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun UpcomingScreen(
    repository: MuxRepository,
    onBack: () -> Unit,
    onMovie: (String) -> Unit,
) {
    var movies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        movies = repository.listMovies(1, 96).items.filter { !it.hasFile }
    }
    PosterGridScreen(
        title = "Upcoming",
        error = null,
        onBack = onBack,
        items = movies,
        key = { it.id },
        poster = { it.posterUrl },
        label = { it.title },
        onClick = { onMovie(it.id) },
    )
}
