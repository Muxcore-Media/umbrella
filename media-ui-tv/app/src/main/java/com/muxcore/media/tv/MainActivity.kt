package com.muxcore.media.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.muxcore.media.tv.data.Capabilities
import com.muxcore.media.tv.data.DefaultCapabilities
import com.muxcore.media.tv.navigation.MuxNavHost
import com.muxcore.media.tv.ui.theme.MuxTvTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MuxTvApplication

        setContent {
            val serverUrl by app.preferences.serverUrl.collectAsState(initial = "")
            val session by app.preferences.sessionToken.collectAsState(initial = "")
            val username by app.preferences.username.collectAsState(initial = "")
            var caps by remember { mutableStateOf(app.capabilities) }
            val navController = rememberNavController()

            if (session.isNotBlank()) {
                androidx.compose.runtime.LaunchedEffect(session, serverUrl) {
                    try {
                        caps = app.repository.getCapabilities()
                        app.capabilities = caps
                        app.userdata.loadLocal()
                        app.userdata.pullFromServer()
                    } catch (_: Exception) {
                        caps = DefaultCapabilities
                    }
                }
            }

            MuxTvTheme {
                MuxNavHost(
                    navController = navController,
                    serverUrl = serverUrl,
                    sessionToken = session,
                    username = username,
                    caps = caps,
                    repository = app.repository,
                    preferences = app.preferences,
                    userdata = app.userdata,
                    onLogout = {
                        lifecycleScope.launch {
                            app.preferences.clearSession()
                            navController.navigate("setup") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                )
            }
        }
    }
}
