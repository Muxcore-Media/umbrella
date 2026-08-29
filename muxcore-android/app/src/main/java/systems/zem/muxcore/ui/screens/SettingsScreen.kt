package systems.zem.muxcore.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import systems.zem.muxcore.ui.LocalCapabilities
import systems.zem.muxcore.ui.appContainer
import systems.zem.muxcore.ui.listTopContentPadding
import systems.zem.muxcore.ui.components.ErrorBanner

@Composable
fun SettingsScreen(nav: NavHostController) {
    val container = appContainer()
    val userData = container.userData
    val session = container.session
    val scope = rememberCoroutineScope()
    val blob by userData.blob.collectAsState()
    val prefs = blob.prefs
    val caps = LocalCapabilities.current
    val sessionState by session.state.collectAsState(initial = session.cache)

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = listTopContentPadding(extraTop = 12.dp, bottom = 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // Profile ------------------------------------------------------
        item {
            SettingsCard("Profile") {
                Text(
                    sessionState.username.ifEmpty { "Signed in" },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    sessionState.serverUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Button(onClick = {
                    scope.launch {
                        container.api.logout()
                        userData.clearLocal()
                        session.clearSession()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Sign out")
                }
            }
        }

        // Display ------------------------------------------------------
        item {
            SettingsCard("Display") {
                DropdownRow(
                    label = "Theme",
                    value = prefs.display.theme,
                    options = listOf("dark" to "Dark", "light" to "Light", "system" to "System"),
                ) { v -> userData.updatePreferences { it.copy(display = it.display.copy(theme = v)) } }
                DropdownRow(
                    label = "Library page size",
                    value = prefs.display.libraryPageSize.toString(),
                    options = listOf("24", "48", "96", "200").map { it to it },
                ) { v ->
                    userData.updatePreferences {
                        it.copy(display = it.display.copy(libraryPageSize = v.toIntOrNull() ?: 48))
                    }
                }
                SwitchRow("Show watched indicators", prefs.display.showWatchedIndicators) { on ->
                    userData.updatePreferences { it.copy(display = it.display.copy(showWatchedIndicators = on)) }
                }
            }
        }

        // Home rows ----------------------------------------------------
        item {
            SettingsCard("Home rows") {
                SwitchRow("Continue watching", prefs.home.showContinueWatching) { on ->
                    userData.updatePreferences { it.copy(home = it.home.copy(showContinueWatching = on)) }
                }
                SwitchRow("Next up", prefs.home.showNextUp) { on ->
                    userData.updatePreferences { it.copy(home = it.home.copy(showNextUp = on)) }
                }
                SwitchRow("Favorites row", prefs.home.showFavorites) { on ->
                    userData.updatePreferences { it.copy(home = it.home.copy(showFavorites = on)) }
                }
                SwitchRow("In progress on home", prefs.home.showRecentRequests) { on ->
                    userData.updatePreferences { it.copy(home = it.home.copy(showRecentRequests = on)) }
                }
            }
        }

        // Playback -----------------------------------------------------
        item {
            SettingsCard("Playback") {
                SwitchRow("Autoplay next episode", prefs.playback.autoplayNext) { on ->
                    userData.updatePreferences { it.copy(playback = it.playback.copy(autoplayNext = on)) }
                }
                SwitchRow("Remember playback position", prefs.playback.rememberPosition) { on ->
                    userData.updatePreferences { it.copy(playback = it.playback.copy(rememberPosition = on)) }
                }
                SliderRow(
                    label = "Skip intro seconds",
                    value = prefs.playback.skipIntroSec.toFloat(),
                    range = 0f..120f,
                    display = "${prefs.playback.skipIntroSec}s",
                ) { v ->
                    userData.updatePreferences { it.copy(playback = it.playback.copy(skipIntroSec = v.toInt())) }
                }
                DropdownRow(
                    label = "Preferred quality",
                    value = prefs.player.preferredQuality,
                    options = listOf(
                        "auto" to "Auto (source)",
                        "1080" to "1080p",
                        "720" to "720p",
                        "480" to "480p",
                    ),
                ) { v -> userData.updatePreferences { it.copy(player = it.player.copy(preferredQuality = v)) } }
                DropdownRow(
                    label = "Video fit",
                    value = prefs.player.aspectMode,
                    options = listOf("contain" to "Fit", "cover" to "Fill", "fill" to "Stretch"),
                ) { v -> userData.updatePreferences { it.copy(player = it.player.copy(aspectMode = v)) } }
            }
        }

        // Subtitles ----------------------------------------------------
        item {
            SettingsCard("Subtitles") {
                SwitchRow("Enable subtitles", prefs.subtitles.enabled) { on ->
                    userData.updatePreferences { it.copy(subtitles = it.subtitles.copy(enabled = on)) }
                }
                var lang by rememberSaveable(prefs.subtitles.language) { mutableStateOf(prefs.subtitles.language) }
                OutlinedTextField(
                    value = lang,
                    onValueChange = {
                        lang = it
                        userData.updatePreferences { p -> p.copy(subtitles = p.subtitles.copy(language = it.trim())) }
                    },
                    label = { Text("Preferred language (ISO code)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownRow(
                    label = "Text size",
                    value = prefs.subtitles.textSize,
                    options = listOf("sm" to "Small", "md" to "Medium", "lg" to "Large"),
                ) { v -> userData.updatePreferences { it.copy(subtitles = it.subtitles.copy(textSize = v)) } }
                SliderRow(
                    label = "Background opacity",
                    value = prefs.subtitles.backgroundOpacity.toFloat(),
                    range = 0f..100f,
                    display = "${prefs.subtitles.backgroundOpacity}%",
                ) { v ->
                    userData.updatePreferences { it.copy(subtitles = it.subtitles.copy(backgroundOpacity = v.toInt())) }
                }
            }
        }

        // Controls -----------------------------------------------------
        item {
            SettingsCard("Controls") {
                SwitchRow("Hardware keyboard shortcuts", prefs.controls.enableKeyboardShortcuts) { on ->
                    userData.updatePreferences { it.copy(controls = it.controls.copy(enableKeyboardShortcuts = on)) }
                }
            }
        }

        // Debrid (capability-gated) -------------------------------------
        if (caps.feature("debrid")) {
            item { DebridCard() }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun DebridCard() {
    val api = appContainer().api
    val scope = rememberCoroutineScope()
    var link by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    SettingsCard("Debrid") {
        Text(
            "Paste a magnet link or hoster URL to queue on your configured debrid provider.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = link,
            onValueChange = { link = it },
            label = { Text("Magnet or hoster link") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (message != null) {
            Text(message!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        if (error != null) ErrorBanner(error!!, Modifier.padding(0.dp))
        Button(
            onClick = {
                busy = true
                message = null
                error = null
                scope.launch {
                    try {
                        message = api.debridAdd(link.trim())
                        link = ""
                    } catch (e: Exception) {
                        error = e.message ?: "Add failed"
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = link.isNotBlank() && !busy,
        ) { Text(if (busy) "Adding…" else "Add to debrid") }
    }
}

// ---------------------------------------------------------------------------
// Building blocks

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun DropdownRow(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = true }
            .padding(vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            options.firstOrNull { it.first == value }?.second ?: value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(id)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(display, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
        )
    }
}
