package systems.zem.muxcore.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import systems.zem.muxcore.MainActivity
import systems.zem.muxcore.data.Episode
import systems.zem.muxcore.data.PlaybackSubtitleTrack
import systems.zem.muxcore.data.ProgressEntry
import systems.zem.muxcore.data.TVShow
import systems.zem.muxcore.data.UserDataStore
import systems.zem.muxcore.ui.Routes
import systems.zem.muxcore.ui.appContainer
import systems.zem.muxcore.ui.player.ChapterIndex
import systems.zem.muxcore.ui.player.EpisodeDrawer
import systems.zem.muxcore.ui.player.PlayerCenterOverlay
import systems.zem.muxcore.ui.player.PlayerControlsBar
import systems.zem.muxcore.ui.player.PlayerErrorScreen
import systems.zem.muxcore.ui.player.PlayerStats
import systems.zem.muxcore.ui.player.PlayerTopBar
import systems.zem.muxcore.ui.player.QUALITY_OPTIONS
import systems.zem.muxcore.ui.player.ResumeDialog
import systems.zem.muxcore.ui.player.SegmentIndex
import systems.zem.muxcore.ui.player.SkipSegmentButton
import systems.zem.muxcore.ui.player.StatsOverlay
import systems.zem.muxcore.ui.player.TrickplaySheet
import systems.zem.muxcore.ui.player.UpNextOverlay

private data class ResolvedSource(
    val playSrc: String,
    val mode: String,
    val offsetSec: Double,
    val transcoderAvailable: Boolean,
    val maxBitrateMbps: String,
    val trickplayEnabled: Boolean,
    val tracks: List<PlaybackSubtitleTrack>,
)

/**
 * Full-screen Media3 player: resolve → direct or transcode stream, custom OSD,
 * segments/chapters, trickplay scrub preview, up-next autoplay, resume prompt,
 * progress reporting, gestures, PiP. Port of media-ui-app VideoPlayer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    nav: NavHostController,
    src: String,
    title: String,
    mediaId: String,
    kind: String,
    poster: String,
    showId: String,
    seasonArg: String,
    episodeArg: String,
) {
    val container = appContainer()
    val api = container.api
    val userData = container.userData
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val prefs = userData.prefs

    if (src.isEmpty()) {
        PlayerErrorScreen("This title isn't available to play.", onBack = { nav.popBackStack() }, onRetry = null)
        return
    }

    // ------------------------------------------------------------------
    // Resolve state

    var resolved by remember { mutableStateOf<ResolvedSource?>(null) }
    var resolveError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var quality by remember { mutableStateOf(prefs.player.preferredQuality) }
    var pendingSeekSec by remember { mutableStateOf<Double?>(null) }
    var resumeAfterReload by remember { mutableStateOf(true) }
    var reloadKey by remember { mutableIntStateOf(0) }

    // Playback state mirrored into Compose
    var playing by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(false) }
    var positionSec by remember { mutableDoubleStateOf(0.0) }
    var durationSec by remember { mutableDoubleStateOf(0.0) }
    var bufferedAheadSec by remember { mutableDoubleStateOf(0.0) }
    var videoW by remember { mutableIntStateOf(0) }
    var videoH by remember { mutableIntStateOf(0) }
    var fatalError by remember { mutableStateOf<String?>(null) }
    var playerTracks by remember { mutableStateOf<Tracks?>(null) }

    // UI state
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsBumpAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var statsVisible by remember { mutableStateOf(false) }
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var trackMenuOpen by remember { mutableStateOf(false) }
    var speedMenuOpen by remember { mutableStateOf(false) }
    var resumeDialogOpen by remember { mutableStateOf(false) }
    var savedPositionSec by remember { mutableDoubleStateOf(0.0) }
    var resumeChecked by remember { mutableStateOf(false) }
    var seekBubble by remember { mutableStateOf<Int?>(null) }
    var rate by remember { mutableStateOf(1f) }

    // Up next
    var showData by remember { mutableStateOf<TVShow?>(null) }
    var nextEpisode by remember { mutableStateOf<Episode?>(null) }
    var upNextVisible by remember { mutableStateOf(false) }
    var upNextSecondsLeft by remember { mutableStateOf<Int?>(null) }
    var outroTriggered by remember { mutableStateOf(false) }

    // ------------------------------------------------------------------
    // ExoPlayer

    val player = remember {
        val dataSource = OkHttpDataSource.Factory(api.http)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
            .setRenderersFactory(
                DefaultRenderersFactory(context)
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER),
            )
            .build()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_ENDED) {
                    upNextVisible = true
                }
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                videoW = videoSize.width
                videoH = videoSize.height
            }

            override fun onTracksChanged(tracks: Tracks) {
                playerTracks = tracks
            }

            override fun onPlayerError(error: PlaybackException) {
                fatalError = error.message ?: "Playback error"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    val transcodeOffset = resolved?.offsetSec ?: 0.0
    val absoluteCurrent = transcodeOffset + positionSec
    val absoluteDuration = if (durationSec > 0) transcodeOffset + durationSec else 0.0

    // ------------------------------------------------------------------
    // Resolve + (re)load stream

    LaunchedEffect(src, quality, reloadKey) {
        loading = true
        resolveError = null
        fatalError = null
        try {
            val res = api.resolvePlayback(src)
            val subs = api.fetchPlaybackSubtitles(src)
            var streamUrl = res.streamUrl.ifEmpty { src }
            var mode = res.mode.ifEmpty { "direct" }
            var offset = 0.0

            val qOpt = QUALITY_OPTIONS.firstOrNull { it.id == quality }
            if (quality != "auto" && qOpt?.maxHeight != null && res.transcoderAvailable) {
                streamUrl = api.transcodeUrl(src, qOpt.maxHeight)
                mode = "transcode"
            }
            val seekTo = pendingSeekSec
            pendingSeekSec = null
            if (mode == "transcode" && seekTo != null && seekTo > 0) {
                streamUrl = if (quality != "auto" && qOpt?.maxHeight != null) {
                    api.transcodeUrl(src, qOpt.maxHeight, seekTo)
                } else {
                    "$streamUrl${if ('?' in streamUrl) "&" else "?"}start=${"%.2f".format(seekTo)}"
                }
                offset = seekTo
            }

            resolved = ResolvedSource(
                playSrc = streamUrl,
                mode = mode,
                offsetSec = offset,
                transcoderAvailable = res.transcoderAvailable,
                maxBitrateMbps = res.maxBitrateMbps,
                trickplayEnabled = res.trickplayEnabled,
                tracks = subs,
            )

            val subtitleConfigs = subs.map { t ->
                MediaItem.SubtitleConfiguration.Builder(api.absoluteUrl(t.src).toUri())
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage(t.language)
                    .setLabel(t.label)
                    .apply { if (t.isDefault) setSelectionFlags(C.SELECTION_FLAG_DEFAULT) }
                    .build()
            }
            val item = MediaItem.Builder()
                .setUri(api.absoluteUrl(streamUrl).toUri())
                .setSubtitleConfigurations(subtitleConfigs)
                .build()
            player.setMediaItem(item)
            player.prepare()
            if (mode != "transcode" && seekTo != null && seekTo > 0) {
                player.seekTo((seekTo * 1000).toLong())
            }
            player.playWhenReady = resumeAfterReload
            resumeAfterReload = true

            // Respect the subtitle preference for the initial selection.
            if (!prefs.subtitles.enabled) {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
            } else if (prefs.subtitles.language.isNotEmpty()) {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setPreferredTextLanguage(prefs.subtitles.language)
                    .build()
            }
        } catch (e: Exception) {
            resolveError = systems.zem.muxcore.data.ApiClient.friendlyPlaybackError(e)
        } finally {
            loading = false
        }
    }

    fun seekAbsolute(targetRaw: Double) {
        val target = targetRaw.coerceAtLeast(0.0)
            .let { if (absoluteDuration > 0) it.coerceAtMost(absoluteDuration) else it }
        val r = resolved ?: return
        if (r.mode == "transcode") {
            resumeAfterReload = playing
            pendingSeekSec = target
            reloadKey += 1
        } else {
            player.seekTo((target * 1000).toLong())
        }
    }

    fun seekBy(delta: Double) {
        seekAbsolute(absoluteCurrent + delta)
        seekBubble = delta.toInt()
        scope.launch {
            delay(650)
            seekBubble = null
        }
    }

    fun bumpControls() {
        controlsVisible = true
        controlsBumpAt = System.currentTimeMillis()
    }

    // Auto-hide controls after 3.5s of inactivity while playing.
    LaunchedEffect(controlsBumpAt, playing, resumeDialogOpen, drawerOpen, settingsOpen, trackMenuOpen, speedMenuOpen) {
        if (!playing || resumeDialogOpen || drawerOpen || settingsOpen || trackMenuOpen || speedMenuOpen) {
            controlsVisible = true
            return@LaunchedEffect
        }
        delay(3500)
        controlsVisible = false
    }

    // Position ticker.
    LaunchedEffect(player) {
        while (true) {
            positionSec = player.currentPosition.coerceAtLeast(0L) / 1000.0
            val d = player.duration
            durationSec = if (d != C.TIME_UNSET && d > 0) d / 1000.0 else 0.0
            bufferedAheadSec = (player.bufferedPosition - player.currentPosition).coerceAtLeast(0L) / 1000.0
            delay(500)
        }
    }

    // ------------------------------------------------------------------
    // Segments / chapters / trickplay

    var segmentIndex by remember { mutableStateOf(SegmentIndex(emptyList())) }
    var chapterIndex by remember { mutableStateOf(ChapterIndex(emptyList())) }
    var trickplay by remember { mutableStateOf<TrickplaySheet?>(null) }
    val durationKnown = absoluteDuration > 0

    LaunchedEffect(mediaId, durationKnown) {
        if (mediaId.isEmpty()) return@LaunchedEffect
        val fetched = if (durationKnown) api.fetchPlaybackSegments(mediaId, absoluteDuration.toInt()) else emptyList()
        segmentIndex = SegmentIndex.build(fetched, prefs.playback.skipIntroSec)
    }
    LaunchedEffect(src, durationKnown) {
        if (!durationKnown) return@LaunchedEffect
        chapterIndex = ChapterIndex(api.fetchPlaybackChapters(src, absoluteDuration.toInt()))
    }
    LaunchedEffect(src, durationKnown, resolved?.trickplayEnabled) {
        if (!durationKnown || resolved?.trickplayEnabled != true) return@LaunchedEffect
        val manifest = api.fetchTrickplaySprite(src, absoluteDuration.toInt())
        trickplay = manifest?.let { TrickplaySheet.decode(it) }
    }

    val activeSegment = segmentIndex.activeAt(absoluteCurrent)

    // ------------------------------------------------------------------
    // Resume prompt (once per title)

    LaunchedEffect(durationKnown) {
        if (resumeChecked || !durationKnown || mediaId.isEmpty() || !prefs.playback.rememberPosition) return@LaunchedEffect
        resumeChecked = true
        val saved = userData.getProgress(mediaId)
        if (saved != null && saved.watched != true && saved.positionSec > 5 &&
            absoluteDuration - saved.positionSec > 5
        ) {
            savedPositionSec = saved.positionSec
            resumeDialogOpen = true
            player.pause()
        }
    }

    // ------------------------------------------------------------------
    // Progress reporting (every 5s while playing; final push on dispose)

    val progressHref = if (kind == "episode" && showId.isNotEmpty()) "/tv/$showId" else "/movies/$mediaId"

    fun reportProgress() {
        if (mediaId.isEmpty() || !prefs.playback.rememberPosition || resumeDialogOpen) return
        if (absoluteCurrent < 3) return
        userData.upsertProgress(
            ProgressEntry(
                id = mediaId,
                kind = kind,
                title = title.ifEmpty { "Playback" },
                posterUrl = poster.ifEmpty { null },
                href = progressHref,
                streamUrl = src,
                positionSec = absoluteCurrent,
                durationSec = absoluteDuration,
            ),
        )
    }

    LaunchedEffect(playing) {
        while (playing) {
            delay(5000)
            reportProgress()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            reportProgress()
            userData.pushNow()
        }
    }

    // ------------------------------------------------------------------
    // Up next (episodes)

    LaunchedEffect(showId, mediaId) {
        if (kind != "episode" || showId.isEmpty()) return@LaunchedEffect
        try {
            val show = api.getTVShow(showId)
            showData = show
            nextEpisode = UserDataStore.nextEpisodeAfter(show, mediaId)
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(activeSegment?.kind) {
        val seg = activeSegment ?: return@LaunchedEffect
        if (!outroTriggered && (seg.kind == "outro" || seg.kind == "credits") && nextEpisode != null) {
            outroTriggered = true
            upNextVisible = true
        }
    }

    fun playEpisode(ep: Episode) {
        val show = showData ?: return
        val route = Routes.episodePlayer(show, ep) ?: return
        reportProgress()
        nav.navigate(route) {
            popUpTo(Routes.PLAYER) { inclusive = true }
        }
    }

    LaunchedEffect(upNextVisible) {
        if (!upNextVisible) {
            upNextSecondsLeft = null
            return@LaunchedEffect
        }
        val next = nextEpisode
        if (next == null) {
            // Movie or last episode: mark as done, exit on natural end.
            if (player.playbackState == Player.STATE_ENDED) {
                if (mediaId.isNotEmpty()) userData.markWatched(mediaId, true)
                nav.popBackStack()
            }
            upNextVisible = false
            return@LaunchedEffect
        }
        if (prefs.playback.autoplayNext) {
            for (s in 10 downTo 1) {
                upNextSecondsLeft = s
                delay(1000)
                if (!upNextVisible) return@LaunchedEffect
            }
            playEpisode(next)
        } else {
            upNextSecondsLeft = null
        }
    }

    // ------------------------------------------------------------------
    // System UI: immersive, orientation, keep-awake, PiP

    var inPip by remember { mutableStateOf(false) }
    DisposableEffect(activity) {
        val act = activity ?: return@DisposableEffect onDispose { }
        val window = act.window
        val controller = WindowInsetsControllerCompat(window, view)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val pipListener = androidx.core.util.Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            inPip = info.isInPictureInPictureMode
        }
        act.addOnPictureInPictureModeChangedListener(pipListener)
        (act as? MainActivity)?.pipEnterRequest = {
            if (player.isPlaying) enterPip(act, videoW, videoH)
        }
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            act.removeOnPictureInPictureModeChangedListener(pipListener)
            (act as? MainActivity)?.pipEnterRequest = null
        }
    }

    val pipSupported = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    // ------------------------------------------------------------------
    // Error states

    if (resolveError != null) {
        PlayerErrorScreen(
            resolveError!!,
            onBack = { nav.popBackStack() },
            onRetry = { reloadKey += 1 },
        )
        return
    }

    // ------------------------------------------------------------------
    // Layout

    val audioManager = remember { context.getSystemService(AudioManager::class.java) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (controlsVisible && playing) controlsVisible = false else bumpControls()
                    },
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2) seekBy(-10.0) else seekBy(10.0)
                        bumpControls()
                    },
                )
            }
            .pointerInput(Unit) {
                // Right half: volume; left half: brightness.
                var startX = 0f
                detectVerticalDragGestures(
                    onDragStart = { startX = it.x },
                    onVerticalDrag = { _, dragAmount ->
                        val fraction = -dragAmount / size.height
                        if (startX > size.width / 2) {
                            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val next = (cur + fraction * max * 2).toInt().coerceIn(0, max)
                            if (next != cur) {
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
                            }
                        } else {
                            val act = activity ?: return@detectVerticalDragGestures
                            val lp = act.window.attributes
                            val cur = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
                            lp.screenBrightness = (cur + fraction * 1.5f).coerceIn(0.05f, 1f)
                            act.window.attributes = lp
                        }
                    },
                )
            },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    resizeMode = when (prefs.player.aspectMode) {
                        "cover" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        "fill" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    subtitleView?.setStyle(
                        CaptionStyleCompat(
                            android.graphics.Color.WHITE,
                            android.graphics.Color.argb(
                                (prefs.subtitles.backgroundOpacity * 255 / 100).coerceIn(0, 255),
                                0, 0, 0,
                            ),
                            android.graphics.Color.TRANSPARENT,
                            CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                            android.graphics.Color.BLACK,
                            null,
                        ),
                    )
                    subtitleView?.setFractionalTextSize(
                        when (prefs.subtitles.textSize) {
                            "sm" -> 0.045f
                            "lg" -> 0.075f
                            else -> 0.058f
                        },
                    )
                }.also { it.player = player }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (!inPip) {
            PlayerCenterOverlay(
                loading = loading,
                buffering = buffering,
                playing = playing,
                controlsVisible = controlsVisible,
                seekBubble = seekBubble,
                onTogglePlay = {
                    if (playing) player.pause() else player.play()
                    bumpControls()
                },
            )

            if (fatalError != null) {
                PlayerErrorScreen(
                    fatalError!!,
                    onBack = { nav.popBackStack() },
                    onRetry = {
                        fatalError = null
                        pendingSeekSec = absoluteCurrent
                        reloadKey += 1
                    },
                )
            }

            if (statsVisible) {
                StatsOverlay(
                    PlayerStats(
                        mode = resolved?.mode ?: "direct",
                        resolution = if (videoW > 0) "${videoW}×$videoH" else "—",
                        droppedFrames = 0,
                        bufferedSec = bufferedAheadSec,
                        speed = rate,
                        maxBitrateMbps = resolved?.maxBitrateMbps.orEmpty(),
                    ),
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp),
                )
            }

            PlayerTopBar(
                visible = controlsVisible,
                title = title.ifEmpty { "Playback" },
                metaLine = if (kind == "episode" && seasonArg.isNotEmpty() && episodeArg.isNotEmpty()) {
                    "S%02dE%02d".format(seasonArg.toIntOrNull() ?: 0, episodeArg.toIntOrNull() ?: 0)
                } else {
                    null
                },
                showEpisodes = showData != null,
                onBack = { nav.popBackStack() },
                onEpisodes = { drawerOpen = true },
                onStats = { statsVisible = !statsVisible },
                modifier = Modifier.align(Alignment.TopCenter),
            )

            PlayerControlsBar(
                visible = controlsVisible,
                playing = playing,
                positionSec = absoluteCurrent,
                durationSec = absoluteDuration,
                bufferedSec = bufferedAheadSec,
                segments = segmentIndex.segments,
                chapters = chapterIndex.chapters,
                trickplay = trickplay,
                chapterTitle = chapterIndex.titleAt(absoluteCurrent),
                markerNavEnabled = chapterIndex.chapters.isNotEmpty() || segmentIndex.segments.isNotEmpty(),
                pipSupported = pipSupported,
                onTogglePlay = { if (playing) player.pause() else player.play() },
                onSeek = { seekAbsolute(it) },
                onSeekBy = { seekBy(it) },
                onPrevMarker = {
                    val target = chapterIndex.prevChapterBefore(absoluteCurrent)
                        ?: segmentIndex.prevBoundaryBefore(absoluteCurrent) ?: 0.0
                    seekAbsolute(target)
                },
                onNextMarker = {
                    val target = chapterIndex.nextChapterAfter(absoluteCurrent)
                        ?: segmentIndex.nextBoundaryAfter(absoluteCurrent)
                    if (target != null) seekAbsolute(target)
                },
                onPip = { activity?.let { enterPip(it, videoW, videoH) } },
                onSettings = { settingsOpen = true },
                onSubtitleMenu = { trackMenuOpen = true },
                onSpeedMenu = { speedMenuOpen = true },
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            if (activeSegment != null && !upNextVisible) {
                SkipSegmentButton(
                    kind = activeSegment.kind,
                    onSkip = { seekAbsolute(activeSegment.endSeconds + 0.1) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 96.dp),
                )
            }

            if (upNextVisible && nextEpisode != null) {
                val next = nextEpisode!!
                UpNextOverlay(
                    title = buildString {
                        append("S%02dE%02d".format(next.seasonNumber, next.episodeNumber))
                        if (next.title.isNotEmpty()) append(" · ${next.title}")
                    },
                    secondsLeft = upNextSecondsLeft,
                    onPlayNow = { playEpisode(next) },
                    onCancel = { upNextVisible = false },
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }

            if (resumeDialogOpen) {
                ResumeDialog(
                    positionSec = savedPositionSec,
                    onResume = {
                        resumeDialogOpen = false
                        seekAbsolute(savedPositionSec)
                        player.play()
                    },
                    onStartOver = {
                        resumeDialogOpen = false
                        player.play()
                    },
                )
            }

            if (drawerOpen && showData != null) {
                EpisodeDrawer(
                    show = showData!!,
                    currentEpisodeId = mediaId,
                    onPick = {
                        drawerOpen = false
                        playEpisode(it)
                    },
                    onClose = { drawerOpen = false },
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // Bottom-sheet menus

    if (speedMenuOpen) {
        ModalBottomSheet(onDismissRequest = { speedMenuOpen = false }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(
                    "Playback speed",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { s ->
                    SheetRow(
                        label = "${s}x",
                        selected = rate == s,
                        onClick = {
                            rate = s
                            player.setPlaybackSpeed(s)
                            speedMenuOpen = false
                        },
                    )
                }
            }
        }
    }

    if (trackMenuOpen) {
        ModalBottomSheet(onDismissRequest = { trackMenuOpen = false }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                val tracks = playerTracks
                Text(
                    "Subtitles",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                val textGroups = tracks?.groups?.filter { it.type == C.TRACK_TYPE_TEXT }.orEmpty()
                SheetRow(
                    label = "Off",
                    selected = textGroups.none { it.isSelected },
                    onClick = {
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                        trackMenuOpen = false
                    },
                )
                textGroups.forEachIndexed { gi, group ->
                    val fmt = group.getTrackFormat(0)
                    SheetRow(
                        label = fmt.label ?: fmt.language ?: "Track ${gi + 1}",
                        selected = group.isSelected,
                        onClick = {
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                                .build()
                            trackMenuOpen = false
                        },
                    )
                }
                val audioGroups = tracks?.groups?.filter { it.type == C.TRACK_TYPE_AUDIO }.orEmpty()
                if (audioGroups.size > 1) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(
                        "Audio",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    audioGroups.forEachIndexed { gi, group ->
                        val fmt = group.getTrackFormat(0)
                        SheetRow(
                            label = fmt.label ?: fmt.language ?: "Audio ${gi + 1}",
                            selected = group.isSelected,
                            onClick = {
                                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                                    .build()
                                trackMenuOpen = false
                            },
                        )
                    }
                }
            }
        }
    }

    if (settingsOpen) {
        ModalBottomSheet(onDismissRequest = { settingsOpen = false }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(
                    "Quality",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                val transcoderAvailable = resolved?.transcoderAvailable == true
                QUALITY_OPTIONS.forEach { opt ->
                    val enabled = opt.id == "auto" || transcoderAvailable
                    SheetRow(
                        label = opt.label + if (!enabled) " (transcoder offline)" else "",
                        selected = quality == opt.id,
                        enabled = enabled,
                        onClick = {
                            resumeAfterReload = playing
                            pendingSeekSec = absoluteCurrent
                            quality = opt.id
                            userData.updatePreferences { it.copy(player = it.player.copy(preferredQuality = opt.id)) }
                            settingsOpen = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetRow(label: String, selected: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                selected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

private fun enterPip(activity: Activity, videoW: Int, videoH: Int) {
    if (android.os.Build.VERSION.SDK_INT < 26) return
    val ratio = if (videoW > 0 && videoH > 0) {
        Rational(videoW.coerceAtMost(videoH * 2), videoH.coerceAtMost(videoW * 2))
    } else {
        Rational(16, 9)
    }
    runCatching {
        val builder = PictureInPictureParams.Builder().setAspectRatio(ratio)
        if (android.os.Build.VERSION.SDK_INT >= 31) builder.setAutoEnterEnabled(true)
        activity.enterPictureInPictureMode(builder.build())
    }
}
