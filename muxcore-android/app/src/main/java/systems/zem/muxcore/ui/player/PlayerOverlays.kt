package systems.zem.muxcore.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import systems.zem.muxcore.data.Episode
import systems.zem.muxcore.data.PlaybackChapter
import systems.zem.muxcore.data.PlaybackSegment
import systems.zem.muxcore.data.TVShow
import systems.zem.muxcore.ui.components.episodeCode

private val PlayerScrim = Color(0xB2000000)
private val PanelBg = Color(0xF2131418)

// ---------------------------------------------------------------------------
// Top bar

@Composable
fun PlayerTopBar(
    visible: Boolean,
    title: String,
    metaLine: String?,
    showEpisodes: Boolean,
    onBack: () -> Unit,
    onEpisodes: () -> Unit,
    onStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(0f to PlayerScrim, 1f to Color.Transparent))
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (metaLine != null) {
                    Text(
                        metaLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
            if (showEpisodes) {
                IconButton(onClick = onEpisodes) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = "Episodes", tint = Color.White)
                }
            }
            IconButton(onClick = onStats) {
                Icon(Icons.Default.Tune, contentDescription = "Stats", tint = Color.White)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Center: loading / play-pause / seek bubble

@Composable
fun PlayerCenterOverlay(
    loading: Boolean,
    buffering: Boolean,
    playing: Boolean,
    controlsVisible: Boolean,
    seekBubble: Int?,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            loading || buffering -> CircularProgressIndicator(color = Color.White)
            controlsVisible -> Surface(
                color = PlayerScrim,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.clickable(onClick = onTogglePlay),
            ) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(18.dp)
                        .size(40.dp),
                )
            }
        }
        if (seekBubble != null) {
            Surface(
                color = PlayerScrim,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .align(if (seekBubble < 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 40.dp),
            ) {
                Text(
                    "${if (seekBubble > 0) "+" else ""}$seekBubble s",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Seek bar with segment/chapter markers + trickplay preview

@Composable
fun PlayerSeekBar(
    positionSec: Double,
    durationSec: Double,
    bufferedSec: Double,
    segments: List<PlaybackSegment>,
    chapters: List<PlaybackChapter>,
    trickplay: TrickplaySheet?,
    onSeek: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableStateOf(0f) }
    val duration = durationSec.coerceAtLeast(0.1)
    val fraction = if (scrubbing) scrubFraction else (positionSec / duration).toFloat().coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        if (scrubbing && trickplay != null) {
            val previewSec = scrubFraction * duration
            val (col, row) = trickplay.frameAt(previewSec)
            Box(
                Modifier
                    .padding(bottom = 6.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(x = 0.dp),
                ) {
                    TrickplayFrame(trickplay, col, row)
                    Text(
                        formatClock(previewSec),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
            // Track: buffered + segment/chapter markers drawn behind the slider.
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp),
            ) {
                val w = size.width
                drawRoundRect(Color.White.copy(alpha = 0.25f), size = size)
                val bufferedFrac = ((positionSec + bufferedSec) / duration).toFloat().coerceIn(0f, 1f)
                drawRoundRect(Color.White.copy(alpha = 0.35f), size = Size(w * bufferedFrac, size.height))
                segments.forEach { seg ->
                    val s = (seg.startSeconds / duration).toFloat().coerceIn(0f, 1f)
                    val e = (seg.endSeconds / duration).toFloat().coerceIn(0f, 1f)
                    drawRect(
                        Color(0xFFF5A623).copy(alpha = 0.55f),
                        topLeft = Offset(w * s, 0f),
                        size = Size(w * (e - s), size.height),
                    )
                }
                chapters.forEach { ch ->
                    val x = (ch.startSeconds / duration).toFloat().coerceIn(0f, 1f) * w
                    drawRect(
                        Color.White.copy(alpha = 0.9f),
                        topLeft = Offset(x - 1f, 0f),
                        size = Size(2f, size.height),
                    )
                }
            }
            Slider(
                value = fraction,
                onValueChange = {
                    scrubbing = true
                    scrubFraction = it
                },
                onValueChangeFinished = {
                    scrubbing = false
                    onSeek((scrubFraction * duration))
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TrickplayFrame(sheet: TrickplaySheet, col: Int, row: Int) {
    val image: ImageBitmap = remember(sheet) { sheet.bitmap.asImageBitmap() }
    val aspect = sheet.frameWidth.toFloat() / sheet.frameHeight.toFloat()
    val height = 90.dp
    Box(
        Modifier
            .height(height)
            .width(height * aspect)
            .clip(RoundedCornerShape(8.dp)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply { isFilterBitmap = true }
                val src = android.graphics.Rect(
                    col * sheet.frameWidth,
                    row * sheet.frameHeight,
                    (col + 1) * sheet.frameWidth,
                    (row + 1) * sheet.frameHeight,
                )
                val dst = android.graphics.RectF(0f, 0f, size.width, size.height)
                canvas.nativeCanvas.drawBitmap(sheet.bitmap, src, dst, paint)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Bottom controls bar

@Composable
fun PlayerControlsBar(
    visible: Boolean,
    playing: Boolean,
    positionSec: Double,
    durationSec: Double,
    bufferedSec: Double,
    segments: List<PlaybackSegment>,
    chapters: List<PlaybackChapter>,
    trickplay: TrickplaySheet?,
    chapterTitle: String?,
    markerNavEnabled: Boolean,
    pipSupported: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Double) -> Unit,
    onSeekBy: (Double) -> Unit,
    onPrevMarker: () -> Unit,
    onNextMarker: () -> Unit,
    onPip: () -> Unit,
    onSettings: () -> Unit,
    onSubtitleMenu: () -> Unit,
    onSpeedMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(0f to Color.Transparent, 1f to PlayerScrim))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            PlayerSeekBar(
                positionSec = positionSec,
                durationSec = durationSec,
                bufferedSec = bufferedSec,
                segments = segments,
                chapters = chapters,
                trickplay = trickplay,
                onSeek = onSeek,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${formatClock(positionSec)} / ${formatClock(durationSec)}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                )
                if (chapterTitle != null) {
                    Text(
                        "  ·  $chapterTitle",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Box(Modifier.weight(1f))
                if (markerNavEnabled) {
                    IconButton(onClick = onPrevMarker) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous marker", tint = Color.White)
                    }
                }
                IconButton(onClick = { onSeekBy(-10.0) }) {
                    Icon(Icons.Default.Replay10, contentDescription = "Back 10s", tint = Color.White)
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
                IconButton(onClick = { onSeekBy(10.0) }) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White)
                }
                if (markerNavEnabled) {
                    IconButton(onClick = onNextMarker) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next marker", tint = Color.White)
                    }
                }
                IconButton(onClick = onSubtitleMenu) {
                    Icon(Icons.Default.Subtitles, contentDescription = "Subtitles & audio", tint = Color.White)
                }
                IconButton(onClick = onSpeedMenu) {
                    Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color.White)
                }
                if (pipSupported) {
                    IconButton(onClick = onPip) {
                        Icon(Icons.Default.PictureInPictureAlt, contentDescription = "Picture in picture", tint = Color.White)
                    }
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Skip segment button

@Composable
fun SkipSegmentButton(kind: String, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onSkip, modifier = modifier) {
        Text(segmentLabel(kind))
    }
}

// ---------------------------------------------------------------------------
// Up Next overlay

@Composable
fun UpNextOverlay(
    title: String,
    secondsLeft: Int?,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = PanelBg,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.padding(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Up next", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPlayNow) {
                    Text(if (secondsLeft != null) "Play now ($secondsLeft)" else "Play now")
                }
                TextButton(onClick = onCancel) { Text("Cancel", color = Color.White) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Resume dialog

@Composable
fun ResumeDialog(
    positionSec: Double,
    onResume: () -> Unit,
    onStartOver: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(PlayerScrim),
        contentAlignment = Alignment.Center,
    ) {
        Surface(color = PanelBg, shape = RoundedCornerShape(20.dp)) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Resume playback?", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(
                    "You left off at ${formatClock(positionSec)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onResume) { Text("Resume") }
                    TextButton(onClick = onStartOver) { Text("Start over", color = Color.White) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Stats overlay

data class PlayerStats(
    val mode: String,
    val resolution: String,
    val droppedFrames: Int,
    val bufferedSec: Double,
    val speed: Float,
    val maxBitrateMbps: String,
)

@Composable
fun StatsOverlay(stats: PlayerStats, modifier: Modifier = Modifier) {
    Surface(
        color = PanelBg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.padding(12.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            StatLine("Mode", stats.mode)
            StatLine("Resolution", stats.resolution)
            StatLine("Dropped frames", stats.droppedFrames.toString())
            StatLine("Buffered", "%.1fs".format(stats.bufferedSec))
            StatLine("Speed", "${stats.speed}x")
            if (stats.maxBitrateMbps.isNotEmpty()) StatLine("Max bitrate", "${stats.maxBitrateMbps} Mbps")
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row {
        Text(
            "$label  ",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.width(110.dp),
        )
        Text(value, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

// ---------------------------------------------------------------------------
// Episode drawer

@Composable
fun EpisodeDrawer(
    show: TVShow,
    currentEpisodeId: String,
    onPick: (Episode) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(PlayerScrim)
            .clickable(onClick = onClose),
    ) {
        Surface(
            color = PanelBg,
            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(0.55f)
                .fillMaxSize(),
        ) {
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)) {
                item {
                    Text(
                        show.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                }
                (show.seasons ?: emptyList()).forEach { season ->
                    item {
                        Text(
                            season.name.ifEmpty { "Season ${season.seasonNumber}" },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(season.episodes, key = { it.id }) { ep ->
                        val current = ep.id == currentEpisodeId
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = ep.hasFile && !current) { onPick(ep) }
                                .background(if (current) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${episodeCode(ep.seasonNumber, ep.episodeNumber)}" +
                                        (if (ep.title.isNotEmpty()) " · ${ep.title}" else ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (ep.hasFile) Color.White else Color.White.copy(alpha = 0.4f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (current) {
                                Text(
                                    "Playing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Error screen

@Composable
fun PlayerErrorScreen(message: String, onBack: () -> Unit, onRetry: (() -> Unit)?) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(message, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onRetry != null) {
                    Button(onClick = onRetry) { Text("Retry") }
                }
                TextButton(onClick = onBack) { Text("Go back", color = Color.White) }
            }
        }
    }
}
