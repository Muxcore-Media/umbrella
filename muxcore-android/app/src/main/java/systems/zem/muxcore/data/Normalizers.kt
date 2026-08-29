package systems.zem.muxcore.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Tolerant JSON → model mapping, a direct port of media-ui-app's
 * `src/api/client.ts` normalizers (snake_case and camelCase both accepted).
 */
object Normalizers {

    // -------------------------------------------------------------------
    // JSON helpers

    fun str(obj: JsonObject?, vararg keys: String, fallback: String = ""): String {
        if (obj == null) return fallback
        for (key in keys) {
            val el = obj[key] ?: continue
            if (el is JsonNull) continue
            (el as? JsonPrimitive)?.let { return it.content }
        }
        return fallback
    }

    fun int(obj: JsonObject?, vararg keys: String, fallback: Int = 0): Int {
        if (obj == null) return fallback
        for (key in keys) {
            val el = obj[key] ?: continue
            if (el is JsonNull) continue
            val prim = el as? JsonPrimitive ?: continue
            prim.intOrNull?.let { return it }
            prim.doubleOrNull?.let { return it.toInt() }
            prim.content.toIntOrNull()?.let { return it }
        }
        return fallback
    }

    fun intOrNull(obj: JsonObject?, vararg keys: String): Int? {
        if (obj == null) return null
        for (key in keys) {
            val el = obj[key] ?: continue
            if (el is JsonNull) continue
            val prim = el as? JsonPrimitive ?: continue
            prim.intOrNull?.let { return it }
            prim.doubleOrNull?.let { return it.toInt() }
            prim.content.toIntOrNull()?.let { return it }
        }
        return null
    }

    fun double(obj: JsonObject?, vararg keys: String, fallback: Double = 0.0): Double {
        if (obj == null) return fallback
        for (key in keys) {
            val el = obj[key] ?: continue
            if (el is JsonNull) continue
            val prim = el as? JsonPrimitive ?: continue
            prim.doubleOrNull?.let { return it }
            prim.content.toDoubleOrNull()?.let { return it }
        }
        return fallback
    }

    fun bool(obj: JsonObject?, vararg keys: String, fallback: Boolean = false): Boolean {
        if (obj == null) return fallback
        for (key in keys) {
            val el = obj[key] ?: continue
            if (el is JsonNull) continue
            val prim = el as? JsonPrimitive ?: continue
            prim.booleanOrNull?.let { return it }
        }
        return fallback
    }

    fun strOrNull(obj: JsonObject?, vararg keys: String): String? {
        val v = str(obj, *keys)
        return v.ifEmpty { null }
    }

    fun stringList(obj: JsonObject?, key: String): List<String> {
        val arr = obj?.get(key) as? JsonArray ?: return emptyList()
        return arr.mapNotNull { (it as? JsonPrimitive)?.content }
    }

    fun objArray(el: JsonElement?): List<JsonObject> {
        val arr = el as? JsonArray ?: return emptyList()
        return arr.mapNotNull { it as? JsonObject }
    }

    // -------------------------------------------------------------------
    // Poster / image URLs

    /**
     * Mirrors client.ts posterURL: absolute http(s) or /images/ paths pass
     * through; TMDB paths (leading slash) get the CDN prefix; bare filenames
     * map to the module image routes.
     */
    fun posterUrl(path: String, kind: String = "movie"): String {
        if (path.isEmpty()) return ""
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("/images/")) {
            return path
        }
        if (path.startsWith("/")) {
            return "https://image.tmdb.org/t/p/w500$path"
        }
        return if (kind == "tv") "/images/tv/$path" else "/images/movies/$path"
    }

    fun tmdbImageUrl(path: String?, size: String = "w342"): String {
        val raw = path?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        if (raw.startsWith("/images/")) return raw
        val normalized = if (raw.startsWith("/")) raw else "/$raw"
        return "https://image.tmdb.org/t/p/$size$normalized"
    }

    // -------------------------------------------------------------------
    // Media models

    fun movie(raw: JsonObject): Movie {
        val id = str(raw, "id", "movieId")
        val poster = str(raw, "poster_url", "posterUrl", "poster", "poster_path", "posterPath")
        return Movie(
            id = id,
            title = str(raw, "title"),
            year = int(raw, "year"),
            overview = str(raw, "overview"),
            runtime = int(raw, "runtime"),
            voteAverage = double(raw, "vote_average", "voteAverage", "voteAvg"),
            genres = stringList(raw, "genres"),
            posterUrl = posterUrl(poster, "movie"),
            hasFile = bool(raw, "has_file", "hasFile"),
            streamUrl = str(raw, "stream_url", "streamUrl").ifEmpty {
                if (id.isNotEmpty()) "/stream/movies/$id" else ""
            },
            createdAt = str(raw, "created_at", "createdAt"),
            tmdbId = intOrNull(raw, "tmdb_id", "tmdbId"),
            backdropUrl = posterUrl(str(raw, "backdrop_url", "backdropUrl", "backdrop_path"), "movie"),
            tagline = strOrNull(raw, "tagline"),
            status = strOrNull(raw, "status"),
            collectionId = intOrNull(raw, "collection_id", "collectionId"),
            collectionName = strOrNull(raw, "collection_name", "collectionName"),
            libraryType = strOrNull(raw, "library_type", "libraryType"),
        )
    }

    fun episode(raw: JsonObject): Episode {
        val id = str(raw, "id")
        val hasFile = bool(raw, "has_file", "hasFile")
        return Episode(
            id = id,
            seasonNumber = int(raw, "season_number", "seasonNumber"),
            episodeNumber = int(raw, "episode_number", "episodeNumber"),
            title = str(raw, "title", "name"),
            overview = str(raw, "overview"),
            runtime = int(raw, "runtime"),
            hasFile = hasFile,
            streamUrl = str(raw, "stream_url", "streamUrl").ifEmpty {
                if (hasFile && id.isNotEmpty()) "/stream/tv/$id" else ""
            },
            airDate = strOrNull(raw, "air_date", "airDate"),
        )
    }

    fun season(raw: JsonObject): Season {
        val eps = objArray(raw["episodes"]).map { episode(it) }
        return Season(
            id = str(raw, "id"),
            seasonNumber = int(raw, "season_number", "seasonNumber"),
            name = str(raw, "name"),
            episodeCount = int(raw, "episode_count", "episodeCount", fallback = eps.size),
            posterUrl = posterUrl(str(raw, "poster_url", "posterUrl", "poster_path"), "tv"),
            episodes = eps,
        )
    }

    fun tvShow(raw: JsonObject): TVShow {
        val id = str(raw, "id", "seriesId")
        val poster = str(raw, "poster_url", "posterUrl", "poster", "poster_path", "posterPath")
        val seasons = if (raw["seasons"] is JsonArray) objArray(raw["seasons"]).map { season(it) } else null
        val hasFile = bool(raw, "has_file", "hasFile") ||
            (seasons?.any { s -> s.episodes.any { it.hasFile } } == true)
        val streamUrl = str(raw, "stream_url", "streamUrl").ifEmpty {
            seasons?.flatMap { it.episodes }?.firstOrNull { it.hasFile }?.streamUrl ?: ""
        }
        return TVShow(
            id = id,
            title = str(raw, "title", "name"),
            year = int(raw, "year"),
            overview = str(raw, "overview"),
            voteAverage = double(raw, "vote_average", "voteAverage", "voteAvg"),
            genres = stringList(raw, "genres"),
            posterUrl = posterUrl(poster, "tv"),
            hasFile = hasFile,
            streamUrl = streamUrl,
            createdAt = str(raw, "created_at", "createdAt"),
            tmdbId = intOrNull(raw, "tmdb_id", "tmdbId"),
            backdropUrl = posterUrl(str(raw, "backdrop_url", "backdropUrl", "backdrop_path"), "tv"),
            status = strOrNull(raw, "status"),
            seasons = seasons,
        )
    }

    fun mediaRequest(raw: JsonObject): MediaRequest = MediaRequest(
        id = str(raw, "id"),
        itemType = str(raw, "itemType", "item_type"),
        itemId = str(raw, "itemId", "item_id"),
        tmdbId = int(raw, "tmdbId", "tmdb_id"),
        title = str(raw, "title"),
        year = int(raw, "year"),
        poster = str(raw, "poster"),
        status = str(raw, "status"),
        statusDetail = strOrNull(raw, "statusDetail", "status_detail"),
        statusLabel = strOrNull(raw, "statusLabel", "status_label"),
        createdAt = str(raw, "createdAt", "created_at"),
        updatedAt = str(raw, "updatedAt", "updated_at"),
    )

    fun searchResult(raw: JsonObject): SearchResult? {
        val mediaType = str(raw, "mediaType", "media_type")
        // Music kinds are out of scope for this movies/shows client.
        if (mediaType.startsWith("music")) return null
        return SearchResult(
            id = int(raw, "id"),
            title = str(raw, "title"),
            year = int(raw, "year"),
            overview = str(raw, "overview"),
            poster = str(raw, "poster"),
            voteAvg = double(raw, "voteAvg", "vote_avg", "vote_average"),
            mediaType = if (mediaType == "tv") "tv" else "movie",
        )
    }

    fun discoverDetail(raw: JsonObject): DiscoverDetail {
        val trailerObj = raw["trailer"] as? JsonObject
        val trailer = trailerObj?.let {
            val key = str(it, "youtubeKey", "youtube_key")
            if (key.isEmpty()) null else DiscoverTrailer(
                name = str(it, "name"),
                youtubeKey = key,
                url = str(it, "url"),
            )
        }
        val cast = objArray(raw["cast"]).map {
            DiscoverCastMember(
                id = int(it, "id"),
                name = str(it, "name"),
                character = strOrNull(it, "character"),
                profilePath = strOrNull(it, "profilePath", "profile_path"),
            )
        }
        return DiscoverDetail(
            id = int(raw, "id"),
            title = str(raw, "title"),
            year = int(raw, "year"),
            overview = str(raw, "overview"),
            tagline = strOrNull(raw, "tagline"),
            genres = stringList(raw, "genres"),
            poster = str(raw, "poster"),
            backdrop = str(raw, "backdrop"),
            voteAvg = double(raw, "voteAvg", "vote_avg", "vote_average"),
            runtime = intOrNull(raw, "runtime"),
            status = strOrNull(raw, "status"),
            mediaType = if (str(raw, "mediaType", "media_type") == "tv") "tv" else "movie",
            trailer = trailer,
            cast = cast,
        )
    }

    fun capabilities(raw: JsonObject): Capabilities {
        val libs = Capabilities.DEFAULT.libraries.toMutableMap()
        val feats = Capabilities.DEFAULT.features.toMutableMap()
        (raw["libraries"] as? JsonObject)?.forEach { (key, el) ->
            (el as? JsonPrimitive)?.booleanOrNull?.let { if (libs.containsKey(key)) libs[key] = it }
        }
        (raw["features"] as? JsonObject)?.forEach { (key, el) ->
            (el as? JsonPrimitive)?.booleanOrNull?.let { if (feats.containsKey(key)) feats[key] = it }
        }
        return Capabilities(libs, feats)
    }

    fun playbackResolve(raw: JsonObject): PlaybackResolve = PlaybackResolve(
        streamUrl = str(raw, "stream_url", "streamUrl"),
        mode = str(raw, "mode", fallback = "direct"),
        resumeEnabled = bool(raw, "resume_enabled", "resumeEnabled"),
        transcoderEnabled = bool(raw, "transcoder_enabled", "transcoderEnabled"),
        preferDirectPlay = bool(raw, "prefer_direct_play", "preferDirectPlay"),
        maxBitrateMbps = str(raw, "max_bitrate_mbps", "maxBitrateMbps"),
        trickplayEnabled = bool(raw, "trickplay_enabled", "trickplayEnabled"),
        transcoderAvailable = bool(raw, "transcoder_available", "transcoderAvailable"),
    )

    fun subtitleTracks(raw: JsonObject): List<PlaybackSubtitleTrack> =
        objArray(raw["tracks"]).map {
            PlaybackSubtitleTrack(
                id = str(it, "id"),
                label = str(it, "label"),
                language = strOrNull(it, "language", "srclang"),
                src = str(it, "src"),
                isDefault = bool(it, "default"),
            )
        }

    fun segments(raw: JsonObject): List<PlaybackSegment> =
        objArray(raw["segments"]).map {
            PlaybackSegment(
                kind = str(it, "kind"),
                startSeconds = double(it, "start_seconds", "startSeconds"),
                endSeconds = double(it, "end_seconds", "endSeconds"),
                confidence = double(it, "confidence"),
                source = str(it, "source"),
            )
        }

    fun chapters(raw: JsonObject): List<PlaybackChapter> =
        objArray(raw["chapters"]).mapIndexed { i, ch ->
            PlaybackChapter(
                index = int(ch, "index", fallback = i),
                title = str(ch, "title").ifEmpty { "Chapter ${i + 1}" },
                startSeconds = double(ch, "start_seconds", "startSeconds"),
                endSeconds = double(ch, "end_seconds", "endSeconds"),
                source = strOrNull(ch, "source"),
            )
        }

    /**
     * Mirrors client.ts asList: accepts either a bare array or an object with
     * items/results/movies/shows plus total/page/page_size.
     */
    fun <T> asList(data: JsonElement, map: (JsonObject) -> T): ListResponse<T> {
        if (data is JsonArray) {
            val items = data.mapNotNull { it as? JsonObject }.map(map)
            return ListResponse(items, items.size, 1, items.size)
        }
        val obj = data as? JsonObject ?: return ListResponse(emptyList(), 0, 1, 0)
        val rows = (obj["items"] ?: obj["results"] ?: obj["movies"] ?: obj["shows"])
            ?.let { if (it is JsonArray) it.jsonArray else null }
            ?: JsonArray(emptyList())
        val items = rows.mapNotNull { it as? JsonObject }.map(map)
        return ListResponse(
            items = items,
            total = int(obj, "total", fallback = items.size),
            page = int(obj, "page", fallback = 1),
            pageSize = int(obj, "page_size", "pageSize", fallback = items.size),
        )
    }

    fun jsonObjectOrEmpty(el: JsonElement?): JsonObject = el as? JsonObject ?: JsonObject(emptyMap())

    /** Detail responses may nest the row under movie/show/series/item. */
    fun unwrapDetail(raw: JsonObject, vararg keys: String): JsonObject {
        for (key in keys) {
            (raw[key] as? JsonObject)?.let { return it }
        }
        return raw
    }
}
