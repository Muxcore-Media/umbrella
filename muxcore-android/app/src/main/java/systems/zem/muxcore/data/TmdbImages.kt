package systems.zem.muxcore.data

/** TMDB poster/backdrop URL helper — port of media-ui-app lib/tmdbImages.ts. */
object TmdbImages {
    private const val BASE = "https://image.tmdb.org/t/p"

    fun url(path: String?, size: String = "w342"): String {
        val raw = path?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        if (raw.startsWith("/images/")) return raw
        val normalized = if (raw.startsWith("/")) raw else "/$raw"
        return "$BASE/$size$normalized"
    }

    fun youtubeWatchUrl(key: String): String = "https://www.youtube.com/watch?v=$key"
}
