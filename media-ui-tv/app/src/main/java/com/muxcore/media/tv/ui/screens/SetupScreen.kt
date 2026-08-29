package com.muxcore.media.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.tv.material3.Text
import com.muxcore.media.tv.data.MuxRepository
import com.muxcore.media.tv.data.PreferencesStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SetupScreen(
    initialUrl: String,
    repository: MuxRepository,
    preferences: PreferencesStore,
    onConfigured: () -> Unit,
) {
    var url by remember { mutableStateOf(initialUrl.ifBlank { "http://127.0.0.1:5173" }) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Connect to MuxCore", style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        Text(text = "Enter your media-ui BFF URL (mediauiprox). Example: https://mux.zem.systems")
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (error != null) {
            Text(text = error!!)
        }
        Button(
            onClick = {
                busy = true
                error = null
                scope.launch {
                    try {
                        val ok = repository.healthCheck(url)
                        if (!ok) throw IllegalStateException("Health check failed")
                        preferences.setServerUrl(url)
                        onConfigured()
                    } catch (e: Exception) {
                        error = e.message ?: "Connection failed"
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy && url.isNotBlank(),
        ) {
            Text(if (busy) "Checking…" else "Continue")
        }
    }
}
