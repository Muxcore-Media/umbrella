package systems.zem.muxcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import systems.zem.muxcore.ui.theme.MuxColors

data class HeroItem(
    val id: String,
    val title: String,
    val overview: String,
    val year: Int,
    val voteAverage: Double,
    val genres: List<String>,
    val backdropUrl: String?,
    val posterUrl: String?,
    val ready: Boolean,
)

/** Netflix-style featured banner with scrim, meta chips, and Play / More info. */
@Composable
fun HeroBanner(
    item: HeroItem,
    onPlay: () -> Unit,
    onDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 11f),
    ) {
        MuxImage(
            item.backdropUrl?.takeIf { it.isNotEmpty() } ?: item.posterUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to MuxColors.BgBase.copy(alpha = 0.25f),
                        1f to MuxColors.BgBase,
                    ),
                ),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                item.title,
                style = MaterialTheme.typography.headlineLarge,
                color = MuxColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.voteAverage > 0) {
                    StatusBadge("%.1f".format(item.voteAverage), BadgeTone.NEUTRAL, icon = Icons.Default.Star)
                }
                if (item.year > 0) {
                    Text(
                        item.year.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MuxColors.TextSecondary,
                    )
                }
                if (item.genres.isNotEmpty()) {
                    Text(
                        item.genres.take(3).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MuxColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (item.overview.isNotEmpty()) {
                Text(
                    item.overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MuxColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item.ready) {
                    Button(onClick = onPlay) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("  Play")
                    }
                }
                FilledTonalButton(onClick = onDetail) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  More info")
                }
            }
        }
    }
}

/** Detail-page hero: full-bleed backdrop, poster thumb, title, meta and action rows. */
@Composable
fun DetailHero(
    title: String,
    backdropUrl: String?,
    posterUrl: String?,
    modifier: Modifier = Modifier,
    tagline: String? = null,
    overview: String = "",
    meta: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        ) {
            MuxImage(
                backdropUrl?.takeIf { it.isNotEmpty() } ?: posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.background,
                        ),
                    ),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Box(
                Modifier
                    .width(100.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                MuxImage(posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!tagline.isNullOrEmpty()) {
                    Text(
                        tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                if (meta != null) {
                    MetaFlowRow { meta() }
                }
            }
        }
        if (actions != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) { actions() }
        }
        if (overview.isNotEmpty()) {
            Text(
                overview,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetaFlowRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}