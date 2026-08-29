package com.muxcore.media.tv.data

sealed class LibraryHit {
    data class Movie(val item: MovieItem) : LibraryHit()
    data class Tv(val item: TvShowItem) : LibraryHit()
    data class Other(
        val route: String,
        val title: String,
        val subtitle: String,
        val poster: String = "",
    ) : LibraryHit()
}

typealias SearchScope = String

fun parseSearchScope(raw: String?): SearchScope {
    val v = raw?.trim()?.lowercase() ?: "all"
    if (v == "add" || v == "request") return "add"
    if (v in listOf("movies", "tv", "music", "books", "comics", "audiobooks", "homevideos", "musicvideos")) return v
    return "all"
}

private fun rowTitle(row: LibraryRow): String = (row.title.ifBlank { row.name }).lowercase()

private fun scopeIncludesLibrary(scope: SearchScope, lib: String): Boolean = scope == "all" || scope == lib

private fun scopeIncludesMovieTvRemote(scope: SearchScope): Boolean =
    scope == "all" || scope == "add" || scope == "movies" || scope == "tv"

private fun scopeIncludesMusicRemote(scope: SearchScope, caps: Capabilities): Boolean {
    if (!caps.libraryEnabled("music")) return false
    return scope == "all" || scope == "add" || scope == "music"
}

suspend fun runUnifiedSearch(
    repository: MuxRepository,
    caps: Capabilities,
    query: String,
    scope: SearchScope,
): Pair<List<LibraryHit>, List<SearchResult>> {
    val needle = query.trim().lowercase()
    if (needle.length < 2) return emptyList<LibraryHit>() to emptyList()

    val movies = if (scopeIncludesLibrary(scope, "movies") && caps.libraryEnabled("movies")) {
        repository.listMovies(1, 200).items
    } else {
        emptyList()
    }
    val shows = if (scopeIncludesLibrary(scope, "tv") && caps.libraryEnabled("tv")) {
        repository.listTvShows(1, 200).items
    } else {
        emptyList()
    }
    val music = if (scopeIncludesLibrary(scope, "music") && caps.libraryEnabled("music")) {
        runCatching { repository.listLibrary("/api/music").items }.getOrDefault(emptyList())
    } else {
        emptyList()
    }
    val books = if (scopeIncludesLibrary(scope, "books") && caps.libraryEnabled("books")) {
        runCatching { repository.listLibrary("/api/books").items }.getOrDefault(emptyList())
    } else {
        emptyList()
    }
    val comics = if (scopeIncludesLibrary(scope, "comics") && caps.libraryEnabled("comics")) {
        runCatching { repository.listLibrary("/api/comics").items }.getOrDefault(emptyList())
    } else {
        emptyList()
    }
    val audiobooks = if (scopeIncludesLibrary(scope, "audiobooks") && caps.libraryEnabled("audiobooks")) {
        runCatching { repository.listLibrary("/api/audiobooks").items }.getOrDefault(emptyList())
    } else {
        emptyList()
    }

    val remoteChunks = mutableListOf<List<SearchResult>>()
    if (scopeIncludesMovieTvRemote(scope)) {
        remoteChunks.add(runCatching { repository.search(query.trim()) }.getOrDefault(emptyList()))
    }
    if (scopeIncludesMusicRemote(scope, caps)) {
        remoteChunks.add(runCatching { repository.search(query.trim(), "music") }.getOrDefault(emptyList()))
        remoteChunks.add(runCatching { repository.search(query.trim(), "music_album") }.getOrDefault(emptyList()))
        remoteChunks.add(runCatching { repository.search(query.trim(), "music_track") }.getOrDefault(emptyList()))
    }
    var remote = remoteChunks.flatten()

    val lib = mutableListOf<LibraryHit>()
    if (scopeIncludesLibrary(scope, "movies")) {
        lib += movies.filter { it.hasFile && it.title.lowercase().contains(needle) }
            .map { LibraryHit.Movie(it) }
    }
    if (scopeIncludesLibrary(scope, "tv")) {
        lib += shows.filter { it.hasFile && it.title.lowercase().contains(needle) }
            .map { LibraryHit.Tv(it) }
    }
    if (scopeIncludesLibrary(scope, "music")) {
        lib += music.filter { rowTitle(it).contains(needle) }.map {
            LibraryHit.Other("music/${it.id}", it.title.ifBlank { it.name }, "Music")
        }
    }
    if (scopeIncludesLibrary(scope, "books")) {
        lib += books.filter { rowTitle(it).contains(needle) }.map {
            LibraryHit.Other("books/${it.id}", it.title.ifBlank { it.name }, "Books")
        }
    }
    if (scopeIncludesLibrary(scope, "comics")) {
        lib += comics.filter { rowTitle(it).contains(needle) }.map {
            LibraryHit.Other("comics", it.title.ifBlank { it.name }, "Comics")
        }
    }
    if (scopeIncludesLibrary(scope, "audiobooks")) {
        lib += audiobooks.filter { rowTitle(it).contains(needle) }.map {
            LibraryHit.Other("audiobooks", it.title.ifBlank { it.name }, "Audiobooks")
        }
    }

    when (scope) {
        "movies" -> remote = remote.filter { it.mediaType == "movie" }
        "tv" -> remote = remote.filter { it.mediaType == "tv" }
        "music" -> remote = remote.filter {
            it.mediaType == "music" || it.mediaType == "music_album" || it.mediaType == "music_track"
        }
    }

    val library = if (scope == "add") emptyList() else lib
    return library to remote
}

fun remoteNotInLibrary(library: List<LibraryHit>, remote: List<SearchResult>): List<SearchResult> {
    val titles = library.flatMap { hit ->
        when (hit) {
            is LibraryHit.Movie -> listOf("movie:${hit.item.title.lowercase()}")
            is LibraryHit.Tv -> listOf("tv:${hit.item.title.lowercase()}")
            is LibraryHit.Other ->
                if (hit.subtitle == "Music") listOf("music:${hit.title.lowercase()}") else emptyList()
        }
    }.toSet()
    return remote.filter { r ->
        if (r.mediaType == "music" || r.mediaType == "music_album" || r.mediaType == "music_track") {
            !titles.contains("music:${(r.artistName.ifBlank { r.title }).lowercase()}")
        } else {
            !titles.contains("${r.mediaType}:${r.title.lowercase()}")
        }
    }
}

fun searchResultKey(row: SearchResult): String = when (row.mediaType) {
    "music_track" -> "music_track:${row.recordingId.ifBlank { row.title.lowercase() }}"
    "music_album" -> "music_album:${row.releaseGroupId.ifBlank { row.title.lowercase() }}"
    "music" -> "music:${row.musicbrainzId.ifBlank { row.title.lowercase() }}"
    else -> "${row.mediaType}:${row.id}"
}
