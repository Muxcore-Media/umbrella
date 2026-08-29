package systems.zem.muxcore.ui.player

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import systems.zem.muxcore.data.PlaybackChapter
import systems.zem.muxcore.data.PlaybackSegment
import systems.zem.muxcore.data.TrickplayManifest

/** Quality ladder shown in the player settings (mirrors web QUALITY_OPTIONS). */
data class QualityOption(val id: String, val label: String, val maxHeight: Int?)

val QUALITY_OPTIONS = listOf(
    QualityOption("auto", "Auto (source)", null),
    QualityOption("1080", "1080p", 1080),
    QualityOption("720", "720p", 720),
    QualityOption("480", "480p", 480),
    QualityOption("360", "360p (saver)", 360),
)

fun formatClock(totalSeconds: Double): String {
    val s = totalSeconds.toLong().coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

fun segmentLabel(kind: String): String = when (kind) {
    "intro" -> "Skip intro"
    "recap" -> "Skip recap"
    "outro", "credits" -> "Skip credits"
    else -> "Skip"
}

/** Segment helpers (port of usePlaybackSegments). */
class SegmentIndex(val segments: List<PlaybackSegment>) {
    fun activeAt(sec: Double): PlaybackSegment? =
        segments.firstOrNull { sec >= it.startSeconds && sec < it.endSeconds }

    fun prevBoundaryBefore(sec: Double): Double? =
        segments.flatMap { listOf(it.startSeconds, it.endSeconds) }
            .filter { it < sec - 1.5 }
            .maxOrNull()

    fun nextBoundaryAfter(sec: Double): Double? =
        segments.flatMap { listOf(it.startSeconds, it.endSeconds) }
            .filter { it > sec + 0.5 }
            .minOrNull()

    companion object {
        /** Falls back to a synthetic intro segment from the legacy skip-intro pref. */
        fun build(fetched: List<PlaybackSegment>, legacyIntroSkipSec: Int): SegmentIndex {
            if (fetched.isNotEmpty()) return SegmentIndex(fetched)
            if (legacyIntroSkipSec > 0) {
                return SegmentIndex(
                    listOf(
                        PlaybackSegment(
                            kind = "intro",
                            startSeconds = 0.0,
                            endSeconds = legacyIntroSkipSec.toDouble(),
                            confidence = 1.0,
                            source = "pref",
                        ),
                    ),
                )
            }
            return SegmentIndex(emptyList())
        }
    }
}

/** Chapter helpers (port of usePlaybackChapters). */
class ChapterIndex(val chapters: List<PlaybackChapter>) {
    fun prevChapterBefore(sec: Double): Double? =
        chapters.map { it.startSeconds }.filter { it < sec - 1.5 }.maxOrNull()

    fun nextChapterAfter(sec: Double): Double? =
        chapters.map { it.startSeconds }.filter { it > sec + 0.5 }.minOrNull()

    fun titleAt(sec: Double): String? =
        chapters.lastOrNull { sec >= it.startSeconds }?.title?.takeIf { it.isNotEmpty() }
}

/** Decoded trickplay sprite sheet with frame lookup (port of useTrickplay). */
class TrickplaySheet(
    val bitmap: Bitmap,
    private val intervalSeconds: Double,
    private val cols: Int,
    private val rows: Int,
    private val count: Int,
) {
    val frameWidth: Int = bitmap.width / cols
    val frameHeight: Int = bitmap.height / rows

    /** Returns (col, row) of the sprite frame for a timestamp. */
    fun frameAt(sec: Double): Pair<Int, Int> {
        val idx = (sec / intervalSeconds).toInt().coerceIn(0, count - 1)
        return (idx % cols) to (idx / cols)
    }

    companion object {
        fun decode(manifest: TrickplayManifest): TrickplaySheet? {
            val bmp = BitmapFactory.decodeByteArray(manifest.imageBytes, 0, manifest.imageBytes.size)
                ?: return null
            if (manifest.cols <= 0 || manifest.rows <= 0) return null
            return TrickplaySheet(
                bitmap = bmp,
                intervalSeconds = manifest.intervalSeconds.coerceAtLeast(1.0),
                cols = manifest.cols,
                rows = manifest.rows,
                count = manifest.count,
            )
        }
    }
}
