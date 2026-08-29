package com.muxcore.media.tv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

private val BgBase = Color(0xFF0B0C0F)
private val BgElevated = Color(0xFF15161B)
private val Accent = Color(0xFFE5A00D)
private val TextPrimary = Color(0xFFF5F5F5)
private val TextSecondary = Color(0xFFB0B3BA)

private val MuxColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    background = BgBase,
    onBackground = TextPrimary,
    surface = BgElevated,
    onSurface = TextPrimary,
)

@OptIn(ExperimentalTvMaterial3Api::class)
val MuxTypography = Typography()

@OptIn(ExperimentalTvMaterial3Api::class)
@androidx.compose.runtime.Composable
fun MuxTvTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MuxColorScheme,
        typography = MuxTypography,
        content = content,
    )
}
