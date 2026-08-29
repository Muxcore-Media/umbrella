package com.muxcore.media.tv.data

suspend fun resolvePosterPath(repository: MuxRepository, path: String, kind: String = "movie"): String {
    if (path.isBlank()) return ""
    val normalized = when {
        path.startsWith("http://") || path.startsWith("https://") || path.startsWith("/images/") -> path
        path.startsWith("/") -> "https://image.tmdb.org/t/p/w500$path"
        else -> if (kind == "tv") "/images/tv/$path" else "/images/movies/$path"
    }
    return repository.absoluteUrl(normalized)
}
