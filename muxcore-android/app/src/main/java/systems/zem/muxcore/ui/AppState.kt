package systems.zem.muxcore.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import systems.zem.muxcore.AppContainer
import systems.zem.muxcore.data.ApiClient
import systems.zem.muxcore.data.SessionStore
import systems.zem.muxcore.data.UserDataStore

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

@Composable
fun appContainer(): AppContainer = LocalAppContainer.current

@Composable
fun api(): ApiClient = LocalAppContainer.current.api

@Composable
fun userData(): UserDataStore = LocalAppContainer.current.userData

@Composable
fun session(): SessionStore = LocalAppContainer.current.session
