package com.muxcore.media.tv.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PosterCard(
    title: String,
    posterUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "",
) {
    Card(onClick = onClick, modifier = modifier.width(140.dp)) {
        Column {
            PosterImage(
                url = posterUrl,
                modifier = Modifier.width(140.dp).height(200.dp),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = title,
                modifier = Modifier.padding(8.dp),
                maxLines = 2,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    maxLines = 1,
                )
            }
        }
    }
}
