package com.muxcore.media.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun <T> MediaShelf(
    title: String,
    items: List<T>,
    modifier: Modifier = Modifier,
    key: (T) -> String,
    posterUrl: (T) -> String,
    label: (T) -> String,
    subtitle: (T) -> String = { "" },
    onItem: (T) -> Unit,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp),
            style = androidx.tv.material3.MaterialTheme.typography.titleLarge,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = key) { item ->
                PosterCard(
                    title = label(item),
                    posterUrl = posterUrl(item),
                    subtitle = subtitle(item),
                    onClick = { onItem(item) },
                )
            }
        }
    }
}
