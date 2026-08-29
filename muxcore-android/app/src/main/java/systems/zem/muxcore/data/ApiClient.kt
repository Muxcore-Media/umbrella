package systems.zem.muxcore.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ApiException(message: String, val code: String? = null, val statusCode: Int = 0) : IOException(message) {
    val isUnauthorized: Boolean get() = statusCode == 401
}

/**
 * OkHttp client for the mediauiprox BFF — the full API surface used by
 * media-ui-app, scoped to movies + TV.
 */
class ApiClient(private val session: SessionStore) {

    /** Invoked when the BFF answers 401 — the app clears the session and shows login. */
    @Volatile
    var onUnauthorized: (() -> Unit)? = null

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val state = session.cache
            val builder = chain.request().newBuilder()
            if (state.sessionCookie.isNotEmpty()) {
                builder.header("Cookie", "session=${state.sessionCookie}")
            }
            chain.proceed(builder.build())
        }
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun serverBase(): String = session.cache.serverUrl

    /** Absolute URL for a server-relative path (images, streams, API). */
    fun absoluteUrl(path: String): String {
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val base = serverBase()
        if (base.isEmpty()) return path
        return if (path.startsWith("/")) "$base$path" else "$base/$path"
    }

    private fun enc(v: String): String = URLEncoder.encode(v, "UTF-8")

    private suspend fun raw(path: String, method: String = "GET", body: JsonObject? = null): String =
        withContext(Dispatchers.IO) {
            val base = serverBase()
            if (base.isEmpty()) throw ApiException("Server URL is not configured.")
            val builder = Request.Builder().url(absoluteUrl(path)).header("Accept", "application/json")
            when (method) {
                "GET" -> builder.get()
                else -> builder.method(
                    method,
                    (body?.toString() ?: "{}").toRequestBody("application/json".toMediaType()),
                )
            }
            http.newCall(builder.build()).execute().use { res ->
                val text = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    var message = "${res.code} ${res.message}".trim()
                    var code: String? = null
                    runCatching {
                        val obj = json.parseToJsonElement(text) as? JsonObject
                        val err = Normalizers.str(obj, "error")
                        if (err.isNotEmpty()) {
                            message = err
                            code = Normalizers.strOrNull(obj, "code")
                        }
                    }
                    if (res.code == 401) {
                        onUnauthorized?.invoke()
                        throw ApiException("Session expired. Sign in again.", code, 401)
                    }
                    throw ApiException(message, code, res.code)
                }
                text
            }
        }

    private suspend fun getJson(path: String, method: String = "GET", body: JsonObject? = null): JsonElement =
        json.parseToJsonElement(raw(path, method, body))

    private suspend fun getObj(path: String, method: String = "GET", body: JsonObject? = null): JsonObject =
        Normalizers.jsonObjectOrEmpty(getJson(path, method, body))

    // -------------------------------------------------------------------
    // Capabilities

    suspend fun getCapabilities(): Capabilities = Normalizers.capabilities(getObj("/api/capabilities"))

    // -------------------------------------------------------------------
    // Library

    suspend fun listMovies(page: Int = 1, pageSize: Int = 48): ListResponse<Movie> =
        Normalizers.asList(getJson("/api/movies?page=$page&page_size=$pageSize")) { Normalizers.movie(it) }

    suspend fun getMovie(id: String): Movie {
        val raw = getObj("/api/movies/${enc(id)}")
        return Normalizers.movie(Normalizers.unwrapDetail(raw, "movie", "item"))
    }

    suspend fun listTVShows(page: Int = 1, pageSize: Int = 48): ListResponse<TVShow> =
        Normalizers.asList(getJson("/api/tv?page=$page&page_size=$pageSize")) { Normalizers.tvShow(it) }

    suspend fun getTVShow(id: String): TVShow {
        val raw = getObj("/api/tv/${enc(id)}")
        return Normalizers.tvShow(Normalizers.unwrapDetail(raw, "show", "series", "item"))
    }

    // -------------------------------------------------------------------
    // Search / discover / requests

    suspend fun search(query: String, type: String? = null): List<SearchResult> {
        var path = "/api/search?q=${enc(query)}"
        if (type != null) path += "&type=$type"
        val raw = getObj(path)
        return Normalizers.objArray(raw["results"]).mapNotNull { Normalizers.searchResult(it) }
    }

    suspend fun getDiscoverDetail(type: String, id: Int): DiscoverDetail =
        Normalizers.discoverDetail(getObj("/api/discover/$type/$id"))

    suspend fun discoverBrowse(category: String, type: String, window: String? = null): List<SearchResult> {
        var path = "/api/discover/$category/$type"
        if (window != null) path += "?window=$window"
        val raw = getObj(path)
        return Normalizers.objArray(raw["results"]).mapNotNull { Normalizers.searchResult(it) }
    }

    suspend fun watchlist(type: String? = null): List<SearchResult> {
        var path = "/api/watchlist"
        if (type != null) path += "?type=$type"
        val raw = getObj(path)
        return Normalizers.objArray(raw["items"]).mapNotNull { Normalizers.searchResult(it) }
    }

    suspend fun requestTitle(
        tmdbId: Int,
        title: String,
        year: Int,
        overview: String,
        poster: String,
        mediaType: String,
    ): RequestResponse {
        val body = buildJsonObject {
            put("tmdbId", tmdbId)
            put("title", title)
            put("year", year)
            put("overview", overview)
            put("poster", poster)
            put("mediaType", mediaType)
        }
        val raw = getObj("/api/request", "POST", body)
        return RequestResponse(
            requestId = Normalizers.str(raw, "requestId"),
            movieId = Normalizers.strOrNull(raw, "movieId"),
            seriesId = Normalizers.strOrNull(raw, "seriesId"),
            status = Normalizers.str(raw, "status", fallback = "requested"),
        )
    }

    suspend fun listRequests(): List<MediaRequest> {
        val el = getJson("/api/requests")
        return Normalizers.objArray(el).map { Normalizers.mediaRequest(it) }
    }

    // -------------------------------------------------------------------
    // Collections

    suspend fun listCollections(): List<CollectionSummary> {
        val raw = getObj("/api/collections")
        return Normalizers.objArray(raw["items"]).map {
            CollectionSummary(
                id = Normalizers.str(it, "id"),
                name = Normalizers.str(it, "name"),
                movieCount = Normalizers.int(it, "movie_count", "movieCount"),
            )
        }
    }

    suspend fun getCollection(id: String): CollectionDetail {
        val raw = getObj("/api/collections/${enc(id)}")
        return CollectionDetail(
            id = Normalizers.str(raw, "id"),
            name = Normalizers.str(raw, "name"),
            movies = Normalizers.objArray(raw["movies"]).map { Normalizers.movie(it) },
        )
    }

    // -------------------------------------------------------------------
    // Jellyfin deep-link

    suspend fun jellyfinPlayUrl(muxId: String): String? = try {
        val raw = getObj("/api/jellyfin/play?mux_id=${enc(muxId)}")
        Normalizers.strOrNull(raw, "url")
    } catch (_: Exception) {
        null
    }

    // -------------------------------------------------------------------
    // Account flows

    suspend fun approveQuickConnect(code: String): String {
        val raw = getObj("/api/quickconnect", "POST", buildJsonObject { put("code", code) })
        return Normalizers.str(raw, "message")
    }

    suspend fun requestPasswordReset(username: String, note: String): String {
        val body = buildJsonObject {
            put("username", username)
            put("note", note)
        }
        val raw = getObj("/api/password-reset", "POST", body)
        return Normalizers.str(raw, "message")
    }

    suspend fun invitePeek(token: String): InvitePeek {
        val raw = getObj("/api/invite/peek?token=${enc(token)}")
        return InvitePeek(
            valid = Normalizers.bool(raw, "valid", fallback = true),
            role = Normalizers.str(raw, "role", fallback = "user"),
            error = Normalizers.strOrNull(raw, "error"),
        )
    }

    suspend fun inviteRedeem(token: String, username: String, password: String) {
        val body = buildJsonObject {
            put("token", token)
            put("username", username)
            put("password", password)
        }
        getObj("/api/invite/redeem", "POST", body)
    }

    suspend fun logout() {
        runCatching { raw("/logout") }
    }

    // -------------------------------------------------------------------
    // Debrid (capability-gated)

    /** Queue a magnet/hoster link on the configured debrid provider. Returns kind/id summary. */
    suspend fun debridAdd(link: String): String {
        val raw = getObj("/api/debrid/add", "POST", buildJsonObject { put("link", link) })
        val kind = Normalizers.str(raw, "kind", fallback = "link")
        val id = Normalizers.strOrNull(raw, "id")
        return "Queued on debrid ($kind${if (id != null) " · $id" else ""})"
    }

    suspend fun debridVfs(): List<Pair<String, String>> = try {
        val raw = getObj("/api/debrid/vfs")
        Normalizers.objArray(raw["items"]).map {
            Normalizers.str(it, "id") to Normalizers.str(it, "filename")
        }
    } catch (_: Exception) {
        emptyList()
    }

    // -------------------------------------------------------------------
    // Userdata sync (raw JSON in/out; merge semantics live in UserDataStore)

    suspend fun getUserdata(): JsonObject = getObj("/api/userdata")

    suspend fun putUserdata(blob: JsonObject): JsonObject = getObj("/api/userdata", "PUT", blob)

    // -------------------------------------------------------------------
    // Playback

    suspend fun resolvePlayback(src: String): PlaybackResolve =
        Normalizers.playbackResolve(getObj("/api/playback/resolve?src=${enc(src)}"))

    suspend fun fetchPlaybackSubtitles(src: String): List<PlaybackSubtitleTrack> = try {
        Normalizers.subtitleTracks(getObj("/api/playback/subtitles?src=${enc(src)}"))
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun fetchPlaybackSegments(mediaId: String, durationSeconds: Int): List<PlaybackSegment> = try {
        var path = "/api/playback/segments?media_id=${enc(mediaId)}"
        if (durationSeconds > 0) path += "&duration=$durationSeconds"
        Normalizers.segments(getObj(path))
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun fetchPlaybackChapters(src: String, durationSeconds: Int): List<PlaybackChapter> = try {
        var path = "/api/playback/chapters?src=${enc(src)}"
        if (durationSeconds > 0) path += "&duration=$durationSeconds"
        Normalizers.chapters(getObj(path))
    } catch (_: Exception) {
        emptyList()
    }

    /** Fetches the trickplay sprite sheet plus its grid layout from response headers. */
    suspend fun fetchTrickplaySprite(
        src: String,
        durationSeconds: Int,
        intervalSeconds: Int = 10,
    ): TrickplayManifest? = withContext(Dispatchers.IO) {
        try {
            val url = absoluteUrl(
                "/stream/trickplay?src=${enc(src)}&duration=$durationSeconds&interval=$intervalSeconds",
            )
            http.newCall(Request.Builder().url(url).build()).execute().use { res ->
                if (!res.isSuccessful) return@withContext null
                val cols = res.header("X-Trickplay-Cols")?.toIntOrNull() ?: 0
                val rows = res.header("X-Trickplay-Rows")?.toIntOrNull() ?: 0
                val count = res.header("X-Trickplay-Count")?.toIntOrNull() ?: 0
                val interval = res.header("X-Trickplay-Interval-Seconds")?.toDoubleOrNull()
                    ?: intervalSeconds.toDouble()
                if (cols == 0 || rows == 0) return@withContext null
                val bytes = res.body?.bytes() ?: return@withContext null
                TrickplayManifest(
                    imageBytes = bytes,
                    intervalSeconds = interval,
                    cols = cols,
                    rows = rows,
                    count = if (count > 0) count else cols * rows,
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    /** URL for a manual-quality capped transcode stream. */
    fun transcodeUrl(src: String, maxHeight: Int, startSec: Double? = null): String {
        var url = "/stream/transcode?src=${enc(src)}&max_height=$maxHeight"
        if (startSec != null && startSec > 0) url += "&start=${"%.2f".format(startSec)}"
        return url
    }

    // -------------------------------------------------------------------
    // Auth (mirrors muxcore-ios establishSession + mobile auth API)

    /**
     * Exchanges an auth one-time code from `/auth/callback?code=` for a BFF session
     * cookie value. Does not follow redirects so the Set-Cookie on the callback
     * response is preserved (same as iOS URLSession handling).
     */
    suspend fun establishSessionFromAuthCode(code: String): String = withContext(Dispatchers.IO) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) throw ApiException("Missing auth code", "auth.code_required")
        val noRedirect = http.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
        val url = absoluteUrl("/auth/callback?code=${enc(trimmed)}")
        noRedirect.newCall(Request.Builder().url(url).get().header("Accept", "application/json").build())
            .execute().use { res ->
                val cookie = parseSessionFromSetCookieHeaders(res.headers("Set-Cookie"))
                if (cookie != null) return@withContext cookie
                if (res.code in 200..399) {
                    throw ApiException("No session cookie returned", "auth.no_session")
                }
                throw ApiException("Auth callback failed (${res.code})", "auth.callback_failed", res.code)
            }
    }

    /** Native-client code exchange — POST /api/mobile/session. */
    suspend fun establishSessionFromMobileCode(code: String): Pair<String, String?> {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) throw ApiException("Missing auth code", "auth.code_required")
        val obj = getObj("/api/mobile/session", "POST", buildJsonObject { put("code", trimmed) })
        val token = Normalizers.str(obj, "session_token")
        if (token.isEmpty()) throw ApiException("No session token returned", "auth.no_session")
        return token to Normalizers.strOrNull(obj, "username")
    }

    private fun parseSessionFromSetCookieHeaders(headers: List<String>): String? {
        for (header in headers) {
            val value = header.split(';').firstOrNull()?.trim().orEmpty()
            if (value.startsWith("session=")) {
                val token = value.removePrefix("session=").trim()
                if (token.isNotEmpty()) return token
            }
        }
        return null
    }

    companion object {
        /** User-facing copy for playback resolve failures (mirrors friendlyPlaybackError). */
        fun friendlyPlaybackError(err: Throwable): String {
            val code = (err as? ApiException)?.code
            val message = err.message ?: "Playback failed"
            return when (code) {
                "playback.src_required" -> "This title isn't available to play."
                "playback.method_not_allowed" -> "Playback isn't available right now. Please try again."
                "playback.internal_error" -> "Something went wrong preparing playback. Please try again."
                else -> message.ifEmpty { "Playback failed. Please try again." }
            }
        }
    }
}
