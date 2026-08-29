package systems.zem.muxcore.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Userdata blob types synced with /api/userdata (mirrors media-ui-app lib/userdata.ts). */

@Serializable
data class ProgressEntry(
    val id: String,
    val kind: String = "movie",
    val title: String = "",
    @SerialName("poster_url") val posterUrl: String? = null,
    val href: String = "",
    @SerialName("stream_url") val streamUrl: String? = null,
    val positionSec: Double = 0.0,
    val durationSec: Double = 0.0,
    val updatedAt: String = "",
    val watched: Boolean? = null,
)

@Serializable
data class FavoriteEntry(
    val id: String,
    val kind: String = "movie",
    val title: String = "",
    @SerialName("poster_url") val posterUrl: String? = null,
    val href: String = "",
    val year: Int? = null,
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val itemIds: List<String> = emptyList(),
)

@Serializable
data class QueueItem(
    val id: String,
    val kind: String = "movie",
    val title: String = "",
    val href: String = "",
    @SerialName("stream_url") val streamUrl: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
)

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
    /** 0-100 background opacity behind subtitle text (custom renderer). */
    val backgroundOpacity: Int = 60,
    val edgeStyle: String = "drop-shadow",
    val verticalPosition: String = "bottom",
)

@Serializable
data class ControlsPrefs(
    val enableKeyboardShortcuts: Boolean = true,
)

@Serializable
data class PlayerPrefs(
    /** Manual quality cap id from QualityOption; 'auto' = original/source. */
    val preferredQuality: String = "auto",
    val theaterMode: Boolean = false,
    val aspectMode: String = "contain",
)

@Serializable
data class UserPreferences(
    val display: DisplayPrefs = DisplayPrefs(),
    val home: HomePrefs = HomePrefs(),
    val playback: PlaybackPrefs = PlaybackPrefs(),
    val subtitles: SubtitlePrefs = SubtitlePrefs(),
    val controls: ControlsPrefs = ControlsPrefs(),
    val player: PlayerPrefs = PlayerPrefs(),
)

@Serializable
data class UserDataBlob(
    val progress: Map<String, ProgressEntry> = emptyMap(),
    val favorites: Map<String, FavoriteEntry> = emptyMap(),
    val prefs: UserPreferences = UserPreferences(),
    val playlists: List<Playlist> = emptyList(),
    val queue: List<QueueItem> = emptyList(),
)

data class NextUpEntry(
    val id: String,
    val kind: String,
    val title: String,
    val posterUrl: String?,
    val subtitle: String,
    val showId: String?,
    val episode: Episode?,
    val show: TVShow?,
)
