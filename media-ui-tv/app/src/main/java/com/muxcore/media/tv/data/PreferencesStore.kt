package com.muxcore.media.tv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "mux_tv_prefs")

class PreferencesStore(private val context: Context) {
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val sessionTokenKey = stringPreferencesKey("session_token")
    private val usernameKey = stringPreferencesKey("username")
    private val prefsKey = stringPreferencesKey("user_prefs_json")

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[serverUrlKey] ?: ""
    }

    val sessionToken: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[sessionTokenKey] ?: ""
    }

    val username: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[usernameKey] ?: ""
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val raw = prefs[prefsKey]
        if (raw != null) {
            runCatching { muxJson.decodeFromString<UserPreferences>(raw) }.getOrDefault(UserPreferences())
        } else {
            UserPreferences()
        }
    }

    suspend fun getUserPreferences(): UserPreferences = userPreferences.first()

    suspend fun updateUserPreferences(patch: UserPreferences) {
        context.dataStore.edit {
            it[prefsKey] = muxJson.encodeToString(UserPreferences.serializer(), patch)
        }
    }

    suspend fun updateUserPreferences(updater: (UserPreferences) -> UserPreferences) {
        val next = updater(getUserPreferences())
        updateUserPreferences(next)
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { it[serverUrlKey] = url.trimEnd('/') }
    }

    suspend fun setSession(token: String, username: String) {
        context.dataStore.edit {
            it[sessionTokenKey] = token
            it[usernameKey] = username
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(sessionTokenKey)
            it.remove(usernameKey)
        }
    }
}
