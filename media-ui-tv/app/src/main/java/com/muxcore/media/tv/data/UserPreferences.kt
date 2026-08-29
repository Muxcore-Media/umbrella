package com.muxcore.media.tv.data

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val display: DisplayPrefs = DisplayPrefs(),
    val home: HomePrefs = HomePrefs(),
    val playback: PlaybackPrefs = PlaybackPrefs(),
    val subtitles: SubtitlePrefs = SubtitlePrefs(),
    val controls: ControlsPrefs = ControlsPrefs(),
) {
    @Serializable
    data class DisplayPrefs(
        val theme: String = "dark",
        val libraryPageSize: Int = 48,
        val showWatchedIndicators: Boolean = true,
    )

    @Serializable
    data class HomePrefs(
        val showContinueWatching: Boolean = true,
        val showFavorites: Boolean = true,
        val showRecentRequests: Boolean = true,
        val showNextUp: Boolean = true,
    )

    @Serializable
    data class PlaybackPrefs(
        val autoplayNext: Boolean = false,
        val rememberPosition: Boolean = true,
        val skipIntroSec: Int = 0,
    )

    @Serializable
    data class SubtitlePrefs(
        val enabled: Boolean = true,
        val language: String = "eng",
        val textSize: String = "md",
    )

    @Serializable
    data class ControlsPrefs(
        val enableKeyboardShortcuts: Boolean = true,
    )
}
