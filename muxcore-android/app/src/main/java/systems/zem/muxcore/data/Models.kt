package systems.zem.muxcore.data

/** Library media models normalized from the mediauiprox BFF (mirrors media-ui-app types). */

data class Movie(
    val id: String,
    val title: String,
    val year: Int,
    val overview: String,
    val runtime: Int,
    val voteAverage: Double,
    val genres: List<String>,
    val posterUrl: String,
    val hasFile: Boolean,
    val streamUrl: String,
    val createdAt: String,
    val tmdbId: Int? = null,
    val backdropUrl: String = "",
    val tagline: String? = null,
    val status: String? = null,
    val collectionId: Int? = null,
    val collectionName: String? = null,
    val libraryType: String? = null,
)

data class Episode(
    val id: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val overview: String,
    val runtime: Int,
    val hasFile: Boolean,
    val streamUrl: String,
    val airDate: String? = null,
)

data class Season(
    val id: String,
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int,
    val posterUrl: String,
    val episodes: List<Episode>,
)

data class TVShow(
    val id: String,
    val title: String,
    val year: Int,
    val overview: String,
    val voteAverage: Double,
    val genres: List<String>,
    val posterUrl: String,
    val hasFile: Boolean,
    val streamUrl: String,
    val createdAt: String,
    val tmdbId: Int? = null,
    val backdropUrl: String = "",
    val status: String? = null,
    val seasons: List<Season>? = null,
)

data class ListResponse<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
)

data class SearchResult(
    val id: Int,
    val title: String,
    val year: Int,
    val overview: String,
    val poster: String,
    val voteAvg: Double,
    /** "movie" or "tv" — music kinds are out of scope for this client. */
    val mediaType: String,
)

data class DiscoverTrailer(
    val name: String,
    val youtubeKey: String,
    val url: String,
)

data class DiscoverCastMember(
    val id: Int,
    val name: String,
    val character: String?,
    val profilePath: String?,
)

data class DiscoverDetail(
    val id: Int,
    val title: String,
    val year: Int,
    val overview: String,
    val tagline: String?,
    val genres: List<String>,
    val poster: String,
    val backdrop: String,
    val voteAvg: Double,
    val runtime: Int?,
    val status: String?,
    val mediaType: String,
    val trailer: DiscoverTrailer?,
    val cast: List<DiscoverCastMember>,
)

data class MediaRequest(
    val id: String,
    val itemType: String,
    val itemId: String,
    val tmdbId: Int,
    val title: String,
    val year: Int,
    val poster: String,
    val status: String,
    val statusDetail: String? = null,
    val statusLabel: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

data class CollectionSummary(
    val id: String,
    val name: String,
    val movieCount: Int,
)

data class CollectionDetail(
    val id: String,
    val name: String,
    val movies: List<Movie>,
)

data class RequestResponse(
    val requestId: String,
    val movieId: String?,
    val seriesId: String?,
    val status: String,
)

data class Capabilities(
    val libraries: Map<String, Boolean>,
    val features: Map<String, Boolean>,
) {
    fun library(key: String): Boolean = libraries[key] == true
    fun feature(key: String): Boolean = features[key] == true

    companion object {
        /** Default MVP stack: movies + TV only (matches media-ui-app DEFAULT_CAPABILITIES). */
        val DEFAULT = Capabilities(
            libraries = mapOf(
                "movies" to true,
                "tv" to true,
            ),
            features = mapOf(
                "search" to true,
                "request" to true,
                "collections" to true,
                "studios" to true,
                "upcoming" to true,
                "quickconnect" to true,
                "playlists" to true,
                "queue" to true,
                "favorites" to true,
                "debrid" to false,
                "watchlist" to false,
            ),
        )
    }
}

data class PlaybackResolve(
    val streamUrl: String,
    val mode: String,
    val resumeEnabled: Boolean,
    val transcoderEnabled: Boolean,
    val preferDirectPlay: Boolean,
    val maxBitrateMbps: String,
    val trickplayEnabled: Boolean,
    val transcoderAvailable: Boolean,
)

data class PlaybackSubtitleTrack(
    val id: String,
    val label: String,
    val language: String?,
    val src: String,
    val isDefault: Boolean,
)

/** intro | outro | credits | recap skip segment, backed by media-intro-outro. */
data class PlaybackSegment(
    val kind: String,
    val startSeconds: Double,
    val endSeconds: Double,
    val confidence: Double,
    val source: String,
)

data class PlaybackChapter(
    val index: Int,
    val title: String,
    val startSeconds: Double,
    val endSeconds: Double,
    val source: String?,
)

data class TrickplayManifest(
    val imageBytes: ByteArray,
    val intervalSeconds: Double,
    val cols: Int,
    val rows: Int,
    val count: Int,
)

data class InvitePeek(
    val valid: Boolean,
    val role: String,
    val error: String?,
)
