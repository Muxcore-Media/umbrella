package systems.zem.muxcore.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import systems.zem.muxcore.data.Acquisition
import systems.zem.muxcore.ui.theme.MuxColors

// ---------------------------------------------------------------------------
// Badges

enum class BadgeTone { ACCENT, NEUTRAL, SUCCESS, WARNING, DANGER }

fun Acquisition.Tone.toBadgeTone(): BadgeTone = when (this) {
    Acquisition.Tone.SUCCESS -> BadgeTone.SUCCESS
    Acquisition.Tone.ACCENT -> BadgeTone.ACCENT
    Acquisition.Tone.WARNING -> BadgeTone.WARNING
    Acquisition.Tone.DANGER -> BadgeTone.DANGER
    Acquisition.Tone.NEUTRAL -> BadgeTone.NEUTRAL
}

@Composable
fun StatusBadge(
    text: String,
    tone: BadgeTone = BadgeTone.NEUTRAL,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val (bg, fg) = when (tone) {
        BadgeTone.ACCENT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) to MaterialTheme.colorScheme.primary
        BadgeTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
        BadgeTone.SUCCESS -> MuxColors.Success.copy(alpha = 0.16f) to MuxColors.Success
        BadgeTone.WARNING -> MuxColors.Warning.copy(alpha = 0.16f) to MuxColors.Warning
        BadgeTone.DANGER -> MuxColors.Danger.copy(alpha = 0.16f) to MuxColors.Danger
    }
    Surface(color = bg, shape = RoundedCornerShape(999.dp), modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(11.dp))
            }
            Text(text, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------------------
// Section header

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            buildString {
                append(title)
                if (count != null) append("  ·  $count")
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

// ---------------------------------------------------------------------------
// Empty / error states

@Composable
fun EmptyState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(36.dp),
        )
        if (title != null) {
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        color = MuxColors.Danger.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MuxColors.Danger,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Skeleton shimmer

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = androidx.compose.ui.geometry.Offset(progress - 400f, 0f),
        end = androidx.compose.ui.geometry.Offset(progress, 200f),
    )
}

@Composable
fun SkeletonBox(modifier: Modifier = Modifier, corner: Int = 12) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner.dp))
            .background(shimmerBrush()),
    )
}

@Composable
fun PosterCardSkeleton(width: Int = 128) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(width.dp)) {
        SkeletonBox(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
        )
        SkeletonBox(
            Modifier
                .fillMaxWidth(0.8f)
                .height(12.dp),
            corner = 6,
        )
    }
}

@Composable
fun ShelfSkeleton(count: Int = 5) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
        SkeletonBox(
            Modifier
                .width(160.dp)
                .height(18.dp),
            corner = 6,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(count) { PosterCardSkeleton() }
        }
    }
}

@Composable
fun HeroSkeleton() {
    SkeletonBox(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f),
        corner = 0,
    )
}

val ScrimColor = Color(0xC00B0C0F)
