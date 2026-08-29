package systems.zem.muxcore.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.time.Instant
import java.util.UUID

/**
 * Server-authoritative userdata: /api/userdata is the source of truth; a local
 * JSON file is the offline cache. Direct port of media-ui-app lib/userdata.ts
 * (merge semantics, watched heuristics, next-up derivation).
 */
class UserDataStore(
    context: Context,
    private val api: ApiClient,
    private val scope: CoroutineScope,
) {
    private val file = File(context.filesDir, "userdata.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private var pushJob: Job? = null

    private val _blob = MutableStateFlow(loadLocal())
    val blob: StateFlow<UserDataBlob> = _blob

    private val _serverAuthoritative = MutableStateFlow(false)
    val serverAuthoritative: StateFlow<Boolean> = _serverAuthoritative

    val prefs: UserPreferences get() = _blob.value.prefs

    private fun loadLocal(): UserDataBlob = try {
        if (file.exists()) json.decodeFromString(UserDataBlob.serializer(), file.readText())
        else UserDataBlob()
    } catch (_: Exception) {
        UserDataBlob()
    }

    private fun persistLocal(blob: UserDataBlob) {
        _blob.value = blob
        scope.launch(Dispatchers.IO) {
            runCatching { file.writeText(json.encodeToString(UserDataBlob.serializer(), blob)) }
        }
    }

    private fun update(transform: (UserDataBlob) -> UserDataBlob) {
        persistLocal(transform(_blob.value))
        schedulePush()
    }

    // -------------------------------------------------------------------
    // Progress

    fun listProgress(): List<ProgressEntry> =
        _blob.value.progress.values.sortedByDescending { it.updatedAt }

    fun getProgress(id: String): ProgressEntry? = _blob.value.progress[id]

    fun upsertProgress(entry: ProgressEntry) {
        var next = entry.copy(updatedAt = entry.updatedAt.ifEmpty { Instant.now().toString() })
        if (entry.watched == null) {
            val ratio = if (next.durationSec > 0) next.positionSec / next.durationSec else 0.0
            if (ratio >= 0.92) next = next.copy(watched = true, positionSec = 0.0)
        } else if (entry.watched) {
            next = next.copy(positionSec = 0.0)
        }
        update { blob -> blob.copy(progress = blob.progress + (next.id to next)) }
    }

    fun markWatched(id: String, watched: Boolean) {
        val cur = _blob.value.progress[id] ?: return
        val next = cur.copy(
            watched = watched,
            positionSec = if (watched) 0.0 else cur.positionSec,
            updatedAt = Instant.now().toString(),
        )
        update { blob -> blob.copy(progress = blob.progress + (id to next)) }
    }

    fun continueWatching(limit: Int = 24): List<ProgressEntry> =
        listProgress()
            .filter { p ->
                p.watched != true && p.positionSec > 5 &&
                    (p.durationSec == 0.0 || p.positionSec / p.durationSec < 0.92)
            }
            .take(limit)

    // -------------------------------------------------------------------
    // Favorites

    fun listFavorites(): List<FavoriteEntry> =
        _blob.value.favorites.values.sortedBy { it.title.lowercase() }

    fun isFavorite(id: String): Boolean = _blob.value.favorites.containsKey(id)

    /** Returns the new favorite state (true = now favorited). */
    fun toggleFavorite(entry: FavoriteEntry): Boolean {
        val map = _blob.value.favorites
        val nowOn = !map.containsKey(entry.id)
        update { blob ->
            blob.copy(
                favorites = if (nowOn) blob.favorites + (entry.id to entry)
                else blob.favorites - entry.id,
            )
        }
        return nowOn
    }

    // -------------------------------------------------------------------
    // Preferences

    fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        update { blob -> blob.copy(prefs = transform(blob.prefs)) }
    }

    // -------------------------------------------------------------------
    // Playlists

    fun listPlaylists(): List<Playlist> = _blob.value.playlists

    fun createPlaylist(name: String) {
        update { blob ->
            blob.copy(playlists = blob.playlists + Playlist(UUID.randomUUID().toString(), name))
        }
    }

    fun addToPlaylist(playlistId: String, itemId: String) {
        update { blob ->
            blob.copy(
                playlists = blob.playlists.map { p ->
                    if (p.id == playlistId && itemId !in p.itemIds) p.copy(itemIds = p.itemIds + itemId) else p
                },
            )
        }
    }

    fun removeFromPlaylist(playlistId: String, itemId: String) {
        update { blob ->
            blob.copy(
                playlists = blob.playlists.map { p ->
                    if (p.id == playlistId) p.copy(itemIds = p.itemIds - itemId) else p
                },
            )
        }
    }

    fun deletePlaylist(playlistId: String) {
        update { blob -> blob.copy(playlists = blob.playlists.filterNot { it.id == playlistId }) }
    }

    // -------------------------------------------------------------------
    // Queue

    fun listQueue(): List<QueueItem> = _blob.value.queue

    fun enqueue(item: QueueItem) {
        update { blob -> blob.copy(queue = blob.queue.filterNot { it.id == item.id } + item) }
    }

    fun dequeue(id: String) {
        update { blob -> blob.copy(queue = blob.queue.filterNot { it.id == id }) }
    }

    fun clearQueue() {
        update { blob -> blob.copy(queue = emptyList()) }
    }

    // -------------------------------------------------------------------
    // Server sync

    /** Pull server userdata into the local cache. Server wins on progress conflicts. */
    suspend fun pullFromServer(): Boolean = mutex.withLock {
        try {
            val raw = api.getUserdata()
            val server = decodeBlob(raw)
            val local = _blob.value
            val mergedProgress = mergeProgressMaps(local.progress, server.progress)
            persistLocal(
                UserDataBlob(
                    progress = mergedProgress,
                    favorites = server.favorites.ifEmpty { local.favorites },
                    prefs = if (raw.containsKey("prefs")) server.prefs else local.prefs,
                    playlists = if (raw.containsKey("playlists")) server.playlists else local.playlists,
                    queue = if (raw.containsKey("queue")) server.queue else local.queue,
                ),
            )
            _serverAuthoritative.value = true
            true
        } catch (_: Exception) {
            _serverAuthoritative.value = false
            false
        }
    }

    suspend fun pushToServer() {
        mutex.withLock {
            try {
                val body = json.encodeToJsonElement(UserDataBlob.serializer(), _blob.value).jsonObject
                val merged = decodeBlob(api.putUserdata(body))
                // Apply server merge result so local cache matches SoT.
                persistLocal(merged)
                _serverAuthoritative.value = true
            } catch (_: Exception) {
                // offline — local file remains the working cache until reconnect
            }
        }
    }

    private fun schedulePush() {
        pushJob?.cancel()
        pushJob = scope.launch {
            delay(800) // debounce rapid updates (e.g. playback progress ticks)
            pushToServer()
        }
    }

    /** Immediate push, used when playback pauses/stops. */
    fun pushNow() {
        pushJob?.cancel()
        pushJob = scope.launch { pushToServer() }
    }

    private fun decodeBlob(raw: JsonObject): UserDataBlob = try {
        json.decodeFromJsonElement(UserDataBlob.serializer(), raw)
    } catch (_: Exception) {
        UserDataBlob()
    }

    private fun mergeProgressMaps(
        local: Map<String, ProgressEntry>,
        server: Map<String, ProgressEntry>,
    ): Map<String, ProgressEntry> {
        val out = local.toMutableMap()
        for ((id, entry) in server) {
            val cur = out[id]
            if (cur == null || entry.updatedAt >= cur.updatedAt) out[id] = entry
        }
        return out
    }

    /** Clears the on-device cache (logout). */
    suspend fun clearLocal() = withContext(Dispatchers.IO) {
        pushJob?.cancel()
        runCatching { file.delete() }
        _blob.value = UserDataBlob()
        _serverAuthoritative.value = false
    }

    // -------------------------------------------------------------------
    // Next up derivation (port of resolveNextUp)

    fun showIdFromHref(href: String?): String? {
        if (href == null) return null
        val m = Regex("(?:^|/)tv/([^/?#]+)").find(href) ?: return null
        return java.net.URLDecoder.decode(m.groupValues[1], "UTF-8")
    }

    /**
     * Derive Next Up from progress:
     * - Continue Watching covers partially watched movies/episodes
     * - Next Up = after a watched episode, the following episode with a file
     */
    suspend fun resolveNextUp(fetchShow: suspend (String) -> TVShow, limit: Int = 12): List<NextUpEntry> {
        val progress = listProgress()
        val continueIds = continueWatching(100).map { it.id }.toSet()
        val out = mutableListOf<NextUpEntry>()
        val seen = mutableSetOf<String>()
        val showCache = mutableMapOf<String, TVShow?>()

        suspend fun loadShow(showId: String): TVShow? = showCache.getOrPut(showId) {
            try {
                fetchShow(showId)
            } catch (_: Exception) {
                null
            }
        }

        for (p in progress) {
            if (out.size >= limit) break
            if (p.kind != "episode" && p.kind != "tv") continue
            val watched = p.watched == true || (p.durationSec > 0 && p.positionSec / p.durationSec >= 0.92)
            if (!watched) continue
            val showId = showIdFromHref(p.href) ?: continue
            val show = loadShow(showId) ?: continue
            val next: Episode? = if (p.kind == "episode") {
                nextEpisodeAfter(show, p.id)
            } else {
                flattenEpisodes(show).firstOrNull { ep ->
                    ep.hasFile && progress.none { it.id == ep.id && it.watched == true }
                }
            }
            if (next == null || next.id in seen || next.id in continueIds) continue
            seen.add(next.id)
            val code = "S%02dE%02d".format(next.seasonNumber, next.episodeNumber)
            val epTitle = "${show.title} $code"
            out.add(
                NextUpEntry(
                    id = next.id,
                    kind = "episode",
                    title = if (next.title.isNotEmpty()) "$epTitle · ${next.title}" else epTitle,
                    posterUrl = show.posterUrl.ifEmpty { p.posterUrl },
                    subtitle = "Next up",
                    showId = show.id,
                    episode = next,
                    show = show,
                ),
            )
        }
        return out.take(limit)
    }

    companion object {
        fun flattenEpisodes(show: TVShow): List<Episode> =
            (show.seasons ?: emptyList())
                .flatMap { it.episodes }
                .sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))

        fun nextEpisodeAfter(show: TVShow, episodeId: String): Episode? {
            val eps = flattenEpisodes(show)
            val idx = eps.indexOfFirst { it.id == episodeId }
            if (idx < 0) return null
            for (i in idx + 1 until eps.size) {
                if (eps[i].hasFile) return eps[i]
            }
            return null
        }
    }
}
