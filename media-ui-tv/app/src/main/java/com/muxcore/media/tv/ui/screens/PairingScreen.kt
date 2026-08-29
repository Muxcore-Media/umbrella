package com.muxcore.media.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.tv.material3.Text
import com.muxcore.media.tv.data.MuxRepository
import com.muxcore.media.tv.data.PreferencesStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PairingScreen(
    repository: MuxRepository,
    preferences: PreferencesStore,
    onPaired: () -> Unit,
    onChangeServer: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Starting pairing…") }
    var error by remember { mutableStateOf<String?>(null) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var usePassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

  LaunchedEffect(code, usePassword) {
        if (usePassword || code.isBlank()) return@LaunchedEffect
        while (isActive) {
            try {
                val poll = repository.pollQuickConnect(code)
                if (poll.approved && poll.sessionToken.isNotBlank()) {
                    preferences.setSession(poll.sessionToken, poll.username)
                    onPaired()
                    return@LaunchedEffect
                }
                status = "Waiting for approval… enter code $code on Quick Connect"
            } catch (e: Exception) {
                error = e.message
            }
            delay(2000)
        }
    }

    LaunchedEffect(Unit) {
        if (!usePassword) {
            try {
                val reg = repository.registerQuickConnect()
                code = reg.code
                status = reg.message.ifBlank { "Code: ${reg.code}" }
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Sign in", style = androidx.tv.material3.MaterialTheme.typography.headlineMedium)
        if (!usePassword) {
            Text(
                text = code.ifBlank { "…" },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = androidx.tv.material3.MaterialTheme.typography.displayMedium,
            )
            Text(text = status)
            Text(text = "Open mux.zem.systems/quickconnect (or your server) and authorize this code.")
        } else {
            OutlinedTextField(value = username, onValueChange = { username = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = password, onValueChange = { password = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val res = repository.login(username, password)
                            if (res.requires2fa) {
                                error = "2FA is enabled — use Quick Connect instead"
                                return@launch
                            }
                            preferences.setSession(res.sessionToken, res.username)
                            onPaired()
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                },
            ) {
                Text("Sign in")
            }
        }
        if (error != null) Text(text = error!!)
        Button(onClick = { usePassword = !usePassword }) {
            Text(if (usePassword) "Use Quick Connect" else "Use username/password")
        }
        Button(onClick = onChangeServer) { Text("Change server") }
    }
}
