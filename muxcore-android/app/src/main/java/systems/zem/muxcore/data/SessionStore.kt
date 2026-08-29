package systems.zem.muxcore.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.sessionDataStore by preferencesDataStore(name = "session")

data class SessionState(
    val serverUrl: String,
    val sessionCookie: String,
    val username: String,
) {
    val loggedIn: Boolean get() = serverUrl.isNotEmpty() && sessionCookie.isNotEmpty()
}

/** Persists server URL + session cookie (auth-local `session` cookie) + username. */
class SessionStore(private val context: Context) {

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val SESSION_COOKIE = stringPreferencesKey("session_cookie")
        val USERNAME = stringPreferencesKey("username")
    }

    val state: Flow<SessionState> = context.sessionDataStore.data.map { it.toState() }

    private fun Preferences.toState() = SessionState(
        serverUrl = this[Keys.SERVER_URL].orEmpty(),
        sessionCookie = this[Keys.SESSION_COOKIE].orEmpty(),
        username = this[Keys.USERNAME].orEmpty(),
    )

    suspend fun current(): SessionState = context.sessionDataStore.data.first().toState()

    /** Synchronous snapshot for the OkHttp interceptor (kept fresh via [cache]). */
    @Volatile
    var cache: SessionState = SessionState("", "", "")
        private set

    suspend fun warmCache() {
        cache = current()
    }

    suspend fun setServerUrl(url: String) {
        val trimmed = url.trim().trimEnd('/')
        context.sessionDataStore.edit { it[Keys.SERVER_URL] = trimmed }
        cache = cache.copy(serverUrl = trimmed)
    }

    suspend fun setSession(cookie: String, username: String? = null) {
        context.sessionDataStore.edit {
            it[Keys.SESSION_COOKIE] = cookie
            if (username != null) it[Keys.USERNAME] = username
        }
        cache = cache.copy(sessionCookie = cookie, username = username ?: cache.username)
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit {
            it.remove(Keys.SESSION_COOKIE)
            it.remove(Keys.USERNAME)
        }
        cache = cache.copy(sessionCookie = "", username = "")
    }

    /** Blocking read used once at process start before the UI is up. */
    fun warmCacheBlocking() {
        runBlocking { warmCache() }
    }
}
