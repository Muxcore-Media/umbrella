package com.muxcore.media.tv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

private val Context.userdataStore by preferencesDataStore("mux_tv_userdata")

@Serializable
data class ProgressEntry(
    val id: String = "",
    val kind: String = "movie",
    val title: String = "",
    @kotlinx.serialization.SerialName("poster_url") val posterUrl: String = "",
    val href: String = "",
    @kotlinx.serialization.SerialName("stream_url") val streamUrl: String = "",
    @kotlinx.serialization.SerialName("positionSec") val positionSec: Long = 0,
    @kotlinx.serialization.SerialName("durationSec") val durationSec: Long = 0,
    @kotlinx.serialization.SerialName("updatedAt") val updatedAt: String = "",
    val watched: Boolean = false,
)

@Serializable
data class FavoriteEntry(
    val id: String = "",
    val kind: String = "movie",
    val title: String = "",
    @kotlinx.serialization.SerialName("poster_url") val posterUrl: String = "",
    val href: String = "",
    val year: Int = 0,
)

@Serializable
data class QueueItem(
    val id: String = "",
    val kind: String = "movie",
    val title: String = "",
    val href: String = "",
    @kotlinx.serialization.SerialName("stream_url") val streamUrl: String = "",
    @kotlinx.serialization.SerialName("poster_url") val posterUrl: String = "",
)

@Serializable
data class Playlist(
    val id: String = "",
    val name: String = "",
    val itemIds: List<String> = emptyList(),
)

@Serializable
private data class UserdataCache(
    val progress: Map<String, ProgressEntry> = emptyMap(),
    val favorites: Map<String, FavoriteEntry> = emptyMap(),
    val queue: List<QueueItem> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
)

class UserdataStore(private val context: Context, private val repository: MuxRepository) {
    private val cacheKey = stringPreferencesKey("userdata_json")
    private val mutex = Mutex()
    private var cache = UserdataCache()

    suspend fun loadLocal() {
        val raw = context.userdataStore.data.first()[cacheKey]
        if (raw != null) {
            cache = muxJson.decodeFromString<UserdataCache>(raw)
        }
    }

    private suspend fun persist() {
        context.userdataStore.edit {
            it[cacheKey] = muxJson.encodeToString(cache)
        }
    }

    suspend fun pullFromServer(): Boolean {
        return try {
            val blob = repository.getUserdata()
            mutex.withLock {
                cache = cache.copy(
                    progress = mergeProgress(cache.progress, blob.progress),
                    favorites = blob.favorites.ifEmpty { cache.favorites },
                    queue = blob.queue.ifEmpty { cache.queue },
                    playlists = blob.playlists.ifEmpty { cache.playlists },
                )
                persist()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun pushToServer() {
        mutex.withLock {
            repository.putUserdata(
                progress = cache.progress,
                favorites = cache.favorites,
                queue = cache.queue,
                playlists = cache.playlists,
            )
        }
    }

    suspend fun listProgress(): List<ProgressEntry> = mutex.withLock {
        cache.progress.values.sortedByDescending { it.updatedAt }
    }

    suspend fun continueWatching(limit: Int = 16): List<ProgressEntry> = mutex.withLock {
        cache.progress.values
            .filter { !it.watched && it.positionSec > 5 &&
                (it.durationSec == 0L || it.positionSec.toDouble() / it.durationSec < 0.92) }
            .sortedByDescending { it.updatedAt }
            .take(limit)
    }

    suspend fun listFavorites(): List<FavoriteEntry> = mutex.withLock {
        cache.favorites.values.sortedBy { it.title }
    }

    suspend fun listQueue(): List<QueueItem> = mutex.withLock { cache.queue }

    suspend fun listPlaylists(): List<Playlist> = mutex.withLock { cache.playlists }

    suspend fun getProgress(id: String): ProgressEntry? = mutex.withLock { cache.progress[id] }

    suspend fun upsertProgress(entry: ProgressEntry) {
        mutex.withLock {
            val ratio = if (entry.durationSec > 0) entry.positionSec.toDouble() / entry.durationSec else 0.0
            val watched = entry.watched || ratio >= 0.92
            val next = entry.copy(
                watched = watched,
                positionSec = if (watched) 0 else entry.positionSec,
                updatedAt = entry.updatedAt.ifBlank { java.time.Instant.now().toString() },
            )
            cache = cache.copy(progress = cache.progress + (next.id to next))
            persist()
        }
        pushToServer()
    }

    suspend fun toggleFavorite(entry: FavoriteEntry): Boolean {
        val added = mutex.withLock {
            val map = cache.favorites.toMutableMap()
            val exists = map.containsKey(entry.id)
            if (exists) map.remove(entry.id) else map[entry.id] = entry
            cache = cache.copy(favorites = map)
            persist()
            !exists
        }
        pushToServer()
        return added
    }

    suspend fun isFavorite(id: String): Boolean = mutex.withLock { cache.favorites.containsKey(id) }

    private fun mergeProgress(
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
}
