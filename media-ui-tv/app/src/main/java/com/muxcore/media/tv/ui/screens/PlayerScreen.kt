package com.muxcore.media.tv.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.muxcore.media.tv.data.MuxRepository
import com.muxcore.media.tv.data.PlayRequest
import com.muxcore.media.tv.data.PreferencesStore
import com.muxcore.media.tv.data.ProgressEntry
import com.muxcore.media.tv.data.UserdataStore
import kotlinx.coroutines.delay
import java.time.Instant

@OptIn(ExperimentalTvMaterial3Api::class, UnstableApi::class)
@Composable
fun PlayerScreen(
    play: PlayRequest,
    repository: MuxRepository,
    preferences: PreferencesStore,
    userdata: UserdataStore,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var playUrl by remember { mutableStateOf<String?>(null) }
    var positionMs by remember { mutableLongStateOf(play.startSec * 1000) }
    var skipIntroSec by remember { mutableStateOf(0) }
    var subtitlesEnabled by remember { mutableStateOf(true) }
    var subtitleLanguage by remember { mutableStateOf("eng") }
    var rememberPosition by remember { mutableStateOf(true) }
    var mediaItem by remember { mutableStateOf<MediaItem?>(null) }

    LaunchedEffect(Unit) {
        val prefs = preferences.getUserPreferences()
        skipIntroSec = prefs.playback.skipIntroSec
        subtitlesEnabled = prefs.subtitles.enabled
        subtitleLanguage = prefs.subtitles.language
        rememberPosition = prefs.playback.rememberPosition
        if (rememberPosition && play.mediaId.isNotBlank()) {
            val saved = userdata.getProgress(play.mediaId)
            if (saved != null && saved.positionSec > 5 && play.startSec == 0L) {
                positionMs = saved.positionSec * 1000
            }
        }
    }

    LaunchedEffect(play.src) {
        try {
            val resolved = repository.resolvePlayback(play.src)
            val subtitles = runCatching { repository.fetchPlaybackSubtitles(play.src).tracks }.getOrDefault(emptyList())
            val stream = repository.absoluteUrl(resolved.streamUrl)
            val subs = subtitles.mapNotNull { track ->
                if (track.src.isBlank()) return@mapNotNull null
                val url = repository.absoluteUrl(track.src)
                val mime = when {
                    track.src.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
                    track.src.endsWith(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
                    else -> MimeTypes.TEXT_VTT
                }
                val lang = track.srclang.ifBlank { track.language }
                val label = track.label.ifBlank { lang }
                val flags = when {
                    track.default -> C.SELECTION_FLAG_DEFAULT
                    subtitlesEnabled && lang.lowercase() == subtitleLanguage.lowercase() -> C.SELECTION_FLAG_DEFAULT
                    else -> 0
                }
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(url))
                    .setMimeType(mime)
                    .setLanguage(lang)
                    .setLabel(label)
                    .setSelectionFlags(flags)
                    .build()
            }
            val itemBuilder = MediaItem.Builder().setUri(stream)
            if (subs.isNotEmpty()) itemBuilder.setSubtitleConfigurations(subs)
            playUrl = stream
            mediaItem = itemBuilder.build()
        } catch (e: Exception) {
            error = e.message
        }
    }

    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(mediaItem) {
        val item = mediaItem
        if (item != null) {
            player.setMediaItem(item)
            player.prepare()
            if (positionMs > 0) player.seekTo(positionMs)
            player.playWhenReady = true
        }
    }

    LaunchedEffect(player, skipIntroSec) {
        if (skipIntroSec > 0) {
            var skipped = false
            while (!skipped) {
                delay(200)
                if (player.isPlaying && player.currentPosition < 1000 && player.duration > skipIntroSec * 1000) {
                    player.seekTo(skipIntroSec * 1000L)
                    skipped = true
                }
                if (player.playbackState == androidx.media3.common.Player.STATE_ENDED) break
            }
        }
    }

    LaunchedEffect(player, rememberPosition) {
        while (true) {
            delay(15000)
            if (player.duration > 0 && rememberPosition) {
                userdata.upsertProgress(
                    ProgressEntry(
                        id = play.mediaId.ifBlank { play.src },
                        kind = play.kind,
                        title = play.title,
                        posterUrl = play.poster,
                        href = play.src,
                        streamUrl = play.src,
                        positionSec = player.currentPosition / 1000,
                        durationSec = player.duration / 1000,
                        updatedAt = Instant.now().toString(),
                    ),
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (player.duration > 0 && play.mediaId.isNotBlank() && rememberPosition) {
                kotlinx.coroutines.runBlocking {
                    userdata.upsertProgress(
                        ProgressEntry(
                            id = play.mediaId,
                            kind = play.kind,
                            title = play.title,
                            posterUrl = play.poster,
                            href = play.src,
                            streamUrl = play.src,
                            positionSec = player.currentPosition / 1000,
                            durationSec = player.duration / 1000,
                            updatedAt = Instant.now().toString(),
                        ),
                    )
                }
            }
            player.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (play.title.isNotBlank()) {
            Text(text = play.title, modifier = Modifier.padding(24.dp))
        }
        if (error != null) {
            Text(text = error!!, modifier = Modifier.padding(48.dp))
        } else if (playUrl != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        this.player = player
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
