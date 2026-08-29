package com.muxcore.media.tv.data

import kotlinx.serialization.Serializable

typealias LibraryKey =
    String // movies, tv, music, books, comics, audiobooks, homevideos, musicvideos

typealias FeatureKey =
    String // search, request, collections, studios, upcoming, mixed, livetv, quickconnect, playlists, queue, favorites

@Serializable
data class Capabilities(
    val libraries: Map<String, Boolean> = emptyMap(),
    val features: Map<String, Boolean> = emptyMap(),
) {
    fun libraryEnabled(key: String): Boolean = libraries[key] == true
    fun featureEnabled(key: String): Boolean = features[key] == true
}

val DefaultCapabilities = Capabilities(
    libraries = mapOf(
        "movies" to true,
        "tv" to true,
        "music" to false,
        "books" to false,
        "comics" to false,
        "audiobooks" to false,
        "homevideos" to false,
        "musicvideos" to false,
    ),
    features = mapOf(
        "search" to true,
        "request" to true,
        "collections" to true,
        "studios" to true,
        "upcoming" to true,
        "mixed" to true,
        "livetv" to true,
        "quickconnect" to true,
        "playlists" to true,
        "queue" to true,
        "favorites" to true,
    ),
)

fun mergeCapabilities(raw: Capabilities): Capabilities {
    val libs = DefaultCapabilities.libraries.toMutableMap()
    val feats = DefaultCapabilities.features.toMutableMap()
    for ((k, v) in raw.libraries) libs[k] = v
    for ((k, v) in raw.features) feats[k] = v
    return Capabilities(libraries = libs, features = feats)
}

data class NavDestination(
    val route: String,
    val label: String,
    val library: String? = null,
    val feature: String? = null,
)

val PrimaryNavCatalog = listOf(
    NavDestination("home", "Home"),
    NavDestination("movies", "Movies", library = "movies"),
    NavDestination("tv", "TV", library = "tv"),
    NavDestination("music", "Music", library = "music"),
    NavDestination("books", "Books", library = "books"),
    NavDestination("comics", "Comics", library = "comics"),
    NavDestination("audiobooks", "Audiobooks", library = "audiobooks"),
)

val OverflowNavCatalog = listOf(
    NavDestination("musicvideos", "Music Videos", library = "musicvideos"),
    NavDestination("mixed", "Mixed", feature = "mixed"),
    NavDestination("homevideos", "Home Videos", library = "homevideos"),
    NavDestination("collections", "Collections", feature = "collections"),
    NavDestination("studios", "Studios", feature = "studios"),
    NavDestination("upcoming", "Upcoming", feature = "upcoming"),
    NavDestination("requests", "In progress", feature = "request"),
    NavDestination("playlists", "Playlists", feature = "playlists"),
    NavDestination("queue", "Queue", feature = "queue"),
    NavDestination("livetv", "Live TV", feature = "livetv"),
    NavDestination("favorites", "Favorites", feature = "favorites"),
    NavDestination("search", "Search", feature = "search"),
    NavDestination("settings", "Settings"),
)

fun visibleNav(caps: Capabilities, items: List<NavDestination>): List<NavDestination> =
    items.filter { item ->
        when {
            item.library != null -> caps.libraryEnabled(item.library)
            item.feature != null -> caps.featureEnabled(item.feature)
            else -> true
        }
    }
