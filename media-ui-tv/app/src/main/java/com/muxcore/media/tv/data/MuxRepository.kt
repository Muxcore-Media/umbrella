package com.muxcore.media.tv.data

import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MuxRepository(private val preferences: PreferencesStore) {
    private val client = OkHttpClient.Builder().build()
    private val jsonMedia = "application/json".toMediaType()

    private suspend fun baseUrl(): String {
        val url = preferences.serverUrl.first().trimEnd('/')
        require(url.isNotBlank()) { "Server URL not configured" }
        return url
    }

    private suspend fun sessionToken(): String = preferences.sessionToken.first()

    private suspend fun authorizedRequest(path: String, method: String = "GET", body: String? = null): Request {
        val url = baseUrl() + path
        val builder = Request.Builder().url(url).header("Accept", "application/json")
        val token = sessionToken()
        if (token.isNotBlank()) builder.header("Cookie", "session=$token")
        when (method) {
            "POST" -> builder.post((body ?: "").toRequestBody(jsonMedia))
            "PUT" -> builder.put((body ?: "").toRequestBody(jsonMedia))
            else -> builder.get()
        }
        return builder.build()
    }

    private suspend fun executeJson(request: Request): String {
        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}: ${text.ifBlank { resp.message }}")
            return text
        }
    }

    suspend fun healthCheck(url: String): Boolean {
        val req = Request.Builder().url("${url.trimEnd('/')}/healthz").header("Accept", "application/json").build()
        client.newCall(req).execute().use { return it.isSuccessful }
    }

    suspend fun getCapabilities(): Capabilities =
        mergeCapabilities(muxJson.decodeFromString<Capabilities>(executeJson(authorizedRequest("/api/capabilities"))))

    suspend fun registerQuickConnect(): QuickConnectRegister =
        muxJson.decodeFromString(
            executeJson(
                Request.Builder()
                    .url(baseUrl() + "/api/quickconnect")
                    .post("""{"action":"register"}""".toRequestBody(jsonMedia))
                    .header("Accept", "application/json")
                    .build(),
            ),
        )

    suspend fun pollQuickConnect(code: String): QuickConnectPoll =
        muxJson.decodeFromString(
            executeJson(
                Request.Builder()
                    .url(baseUrl() + "/api/quickconnect?code=$code")
                    .header("Accept", "application/json")
                    .build(),
            ),
        )

    suspend fun login(username: String, password: String): TvLoginResponse {
        val escapedUser = username.replace("\\", "\\\\").replace("\"", "\\\"")
        val escapedPass = password.replace("\\", "\\\\").replace("\"", "\\\"")
        val body = """{"username":"$escapedUser","password":"$escapedPass"}"""
        return muxJson.decodeFromString(
            executeJson(
                Request.Builder()
                    .url(baseUrl() + "/api/tv/login")
                    .post(body.toRequestBody(jsonMedia))
                    .header("Accept", "application/json")
                    .build(),
            ),
        )
    }

    suspend fun listMovies(page: Int = 1, pageSize: Int = 48, library: String? = null): ListEnvelope<MovieItem> {
        val lib = library?.let { "&library=$it" } ?: ""
        return decodeList(executeJson(authorizedRequest("/api/movies?page=$page&page_size=$pageSize$lib")), MovieItem.serializer())
    }

    suspend fun getMovie(id: String): MovieItem = decodeItem(executeJson(authorizedRequest("/api/movies/$id")), MovieItem.serializer(), "movie", "item")

    suspend fun listTvShows(page: Int = 1, pageSize: Int = 48): ListEnvelope<TvShowItem> =
        decodeList(executeJson(authorizedRequest("/api/tv?page=$page&page_size=$pageSize")), TvShowItem.serializer())

    suspend fun getTvShow(id: String): TvShowItem =
        decodeItem(executeJson(authorizedRequest("/api/tv/$id")), TvShowItem.serializer(), "show", "series", "item")

    suspend fun search(query: String, type: String? = null): List<SearchResult> {
        val t = type?.let { "&type=$it" } ?: ""
        val raw = executeJson(authorizedRequest("/api/search?q=${enc(query)}$t"))
        return muxJson.decodeFromString<SearchResponse>(raw).results
    }

    suspend fun listRequests(): List<MediaRequest> {
        val raw = executeJson(authorizedRequest("/api/requests"))
        val el = muxJson.parseToJsonElement(raw)
        if (el is JsonArray) return muxJson.decodeFromJsonElement(ListSerializer(MediaRequest.serializer()), el)
        return muxJson.decodeFromString(raw)
    }

    suspend fun listLibrary(path: String): LibraryListResponse =
        muxJson.decodeFromString(executeJson(authorizedRequest(path)))

    suspend fun getMusicArtist(id: String): MusicArtistDetail =
        muxJson.decodeFromString(executeJson(authorizedRequest("/api/music/$id")))

    suspend fun getBookAuthor(id: String): BookAuthorDetail =
        muxJson.decodeFromString(executeJson(authorizedRequest("/api/books/$id")))

    suspend fun listCollections(): List<CollectionSummary> {
        val raw = muxJson.parseToJsonElement(executeJson(authorizedRequest("/api/collections"))).jsonObject
        val items = raw["items"] ?: return emptyList()
        return muxJson.decodeFromJsonElement(ListSerializer(CollectionSummary.serializer()), items)
    }

    suspend fun getCollection(id: String): CollectionDetail =
        muxJson.decodeFromString(executeJson(authorizedRequest("/api/collections/$id")))

    suspend fun listLiveTv(): LiveTvResponse =
        muxJson.decodeFromString(executeJson(authorizedRequest("/api/livetv")))

    suspend fun getDiscoverDetail(type: String, id: Int): DiscoverDetail {
        val raw = executeJson(authorizedRequest("/api/discover/$type/$id"))
        return muxJson.decodeFromString(raw)
    }

    suspend fun requestTitle(
        title: String,
        mediaType: String,
        tmdbId: Int = 0,
        year: Int = 0,
        overview: String = "",
        poster: String = "",
        musicbrainzId: String = "",
        releaseGroupId: String = "",
        recordingId: String = "",
        artistName: String = "",
        albumTitle: String = "",
    ): RequestTitleResponse {
        val body = buildJsonObject {
            put("title", title)
            put("overview", overview)
            put("poster", poster)
            put("mediaType", mediaType)
            when (mediaType) {
                "music", "music_album", "music_track" -> {
                    put("musicbrainzId", musicbrainzId)
                    put("releaseGroupId", releaseGroupId)
                    put("recordingId", recordingId)
                    put("artistName", artistName)
                    put("albumTitle", albumTitle)
                    if (mediaType == "music_album" || mediaType == "music_track") put("year", year)
                }
                else -> {
                    put("tmdbId", tmdbId)
                    put("year", year)
                }
            }
        }
        val json = body.toString()
        val raw = executeJson(authorizedRequest("/api/request", "POST", json))
        return muxJson.decodeFromString(raw)
    }

    suspend fun getUserdata(): UserdataBlob {
        val raw = executeJson(authorizedRequest("/api/userdata"))
        val root = muxJson.parseToJsonElement(raw).jsonObject
        fun mapProgress(el: JsonElement?): Map<String, ProgressEntry> {
            if (el == null || el !is kotlinx.serialization.json.JsonObject) return emptyMap()
            return el.mapValues { (_, v) -> muxJson.decodeFromJsonElement(ProgressEntry.serializer(), v) }
        }
        fun mapFavorites(el: JsonElement?): Map<String, FavoriteEntry> {
            if (el == null || el !is kotlinx.serialization.json.JsonObject) return emptyMap()
            return el.mapValues { (_, v) -> muxJson.decodeFromJsonElement(FavoriteEntry.serializer(), v) }
        }
        val queueEl = root["queue"]
        val playlistsEl = root["playlists"]
        val queue = if (queueEl is JsonArray) {
            muxJson.decodeFromJsonElement(ListSerializer(QueueItem.serializer()), queueEl)
        } else {
            emptyList()
        }
        val playlists = if (playlistsEl is JsonArray) {
            muxJson.decodeFromJsonElement(ListSerializer(Playlist.serializer()), playlistsEl)
        } else {
            emptyList()
        }
        return UserdataBlob(
            progress = mapProgress(root["progress"]),
            favorites = mapFavorites(root["favorites"]),
            queue = queue,
            playlists = playlists,
        )
    }

    suspend fun putUserdata(
        progress: Map<String, ProgressEntry>,
        favorites: Map<String, FavoriteEntry>,
        queue: List<QueueItem> = emptyList(),
        playlists: List<Playlist> = emptyList(),
    ) {
        val body = muxJson.encodeToString(UserdataBlob.serializer(), UserdataBlob(
            progress = progress,
            favorites = favorites,
            queue = queue,
            playlists = playlists,
        ))
        executeJson(authorizedRequest("/api/userdata", "PUT", body))
    }

    suspend fun resolvePlayback(src: String): PlaybackResolve {
        val raw = executeJson(authorizedRequest("/api/playback/resolve?src=${enc(src)}"))
        return muxJson.decodeFromString(raw)
    }

    suspend fun fetchPlaybackSubtitles(src: String): PlaybackSubtitlesResponse {
        val raw = executeJson(authorizedRequest("/api/playback/subtitles?src=${enc(src)}"))
        return muxJson.decodeFromString(raw)
    }

    suspend fun absoluteUrl(path: String): String {
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val base = preferences.serverUrl.first().trimEnd('/')
        return if (path.startsWith("/")) base + path else "$base/$path"
    }

    private fun enc(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun <T> decodeList(raw: String, serializer: kotlinx.serialization.KSerializer<T>): ListEnvelope<T> {
        val element = muxJson.parseToJsonElement(raw)
        if (element is JsonArray) {
            val items = muxJson.decodeFromJsonElement(
                kotlinx.serialization.builtins.ListSerializer(serializer),
                element,
            )
            return ListEnvelope(items = items, total = items.size, page = 1, pageSize = items.size)
        }
        return muxJson.decodeFromString(ListEnvelope.serializer(serializer), raw)
    }

    private fun <T> decodeItem(
        raw: String,
        serializer: kotlinx.serialization.KSerializer<T>,
        vararg keys: String,
    ): T {
        val obj = muxJson.parseToJsonElement(raw).jsonObject
        for (key in keys) {
            val el = obj[key]
            if (el != null) return muxJson.decodeFromJsonElement(serializer, el)
        }
        return muxJson.decodeFromString(serializer, raw)
    }
}

private typealias IOException = java.io.IOException
