package systems.zem.muxcore.ui

import java.net.URLEncoder
import systems.zem.muxcore.data.Episode
import systems.zem.muxcore.data.Movie
import systems.zem.muxcore.data.ProgressEntry
import systems.zem.muxcore.data.TVShow

object Routes {
    const val HOME = "home"
    const val MOVIES = "movies"
    const val TV = "tv"
    const val SEARCH = "search?q={q}&scope={scope}"
    const val LIBRARY = "library"
    const val MOVIE_DETAIL = "movies/{id}"
    const val TV_DETAIL = "tv/{id}"
    const val DISCOVER = "discover"
    const val DISCOVER_DETAIL = "discover/{type}/{id}"
    const val WATCHLIST = "watchlist"
    const val FAVORITES = "favorites"
    const val QUEUE = "queue"
    const val REQUESTS = "requests"
    const val COLLECTIONS = "collections"
    const val UPCOMING = "upcoming"
    const val STUDIOS = "studios"
    const val PLAYLISTS = "playlists"
    const val SETTINGS = "settings"
    const val QUICK_CONNECT = "quickconnect"
    const val FORGOT_PASSWORD = "forgot-password"
    const val INVITE = "invite/{token}"
    const val PLAYER =
        "player?src={src}&title={title}&id={id}&kind={kind}&poster={poster}&showId={showId}&season={season}&episode={episode}"

    private fun enc(v: String?): String = URLEncoder.encode(v.orEmpty(), "UTF-8")

    fun search(query: String = "", scope: String = "all"): String = "search?q=${enc(query)}&scope=${enc(scope)}"

    fun movieDetail(id: String): String = "movies/${enc(id)}"

    fun tvDetail(id: String): String = "tv/${enc(id)}"

    fun discoverDetail(type: String, id: Int): String = "discover/$type/$id"

    fun player(
        src: String,
        title: String,
        id: String,
        kind: String,
        poster: String? = null,
        showId: String? = null,
        season: Int? = null,
        episode: Int? = null,
    ): String = "player?src=${enc(src)}&title=${enc(title)}&id=${enc(id)}&kind=${enc(kind)}" +
        "&poster=${enc(poster)}&showId=${enc(showId)}" +
        "&season=${season?.toString().orEmpty()}&episode=${episode?.toString().orEmpty()}"

    fun moviePlayer(movie: Movie): String = player(
        src = movie.streamUrl,
        title = movie.title,
        id = movie.id,
        kind = "movie",
        poster = movie.posterUrl,
    )

    fun episodePlayer(show: TVShow, ep: Episode): String? {
        if (!ep.hasFile || ep.streamUrl.isEmpty()) return null
        val code = "S%02dE%02d".format(ep.seasonNumber, ep.episodeNumber)
        val title = if (ep.title.isNotEmpty()) "${show.title} $code · ${ep.title}" else "${show.title} $code"
        return player(
            src = ep.streamUrl,
            title = title,
            id = ep.id,
            kind = "episode",
            poster = show.posterUrl,
            showId = show.id,
            season = ep.seasonNumber,
            episode = ep.episodeNumber,
        )
    }

    /** Resume target for a progress entry (Continue Watching). */
    fun progressPlayer(entry: ProgressEntry): String? {
        val src = entry.streamUrl ?: return null
        if (src.isEmpty()) return null
        val showId = Regex("(?:^|/)tv/([^/?#]+)").find(entry.href)?.groupValues?.get(1)
        return player(
            src = src,
            title = entry.title,
            id = entry.id,
            kind = entry.kind,
            poster = entry.posterUrl,
            showId = showId,
        )
    }

    /** In-app destination for a web-style href stored in userdata (favorites/queue rows). */
    fun fromHref(href: String): String? {
        val path = href.substringBefore('?')
        val query = href.substringAfter('?', "")
        return when {
            href.startsWith("/player") -> if (query.isNotEmpty()) "player?$query" else null
            path.startsWith("/movies/") -> "movies/${path.removePrefix("/movies/")}"
            path.startsWith("/tv/") -> "tv/${path.removePrefix("/tv/")}"
            path == "/movies" -> MOVIES
            path == "/tv" -> TV
            else -> null
        }
    }
}
