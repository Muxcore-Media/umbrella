package com.muxcore.media.tv.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MovieItem(
    val id: String = "",
    val title: String = "",
    val year: Int = 0,
    val overview: String = "",
    @SerialName("poster_url") val posterUrl: String = "",
    @SerialName("backdrop_url") val backdropUrl: String = "",
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val genres: List<String> = emptyList(),
    val runtime: Int = 0,
    @SerialName("has_file") val hasFile: Boolean = false,
    @SerialName("stream_url") val streamUrl: String = "",
    @SerialName("tmdb_id") val tmdbId: Int = 0,
    @SerialName("collection_name") val collectionName: String = "",
    @SerialName("collection_id") val collectionId: String = "",
)

@Serializable
data class EpisodeItem(
    val id: String = "",
    @SerialName("season_number") val seasonNumber: Int = 0,
    @SerialName("episode_number") val episodeNumber: Int = 0,
    val title: String = "",
    val overview: String = "",
    @SerialName("has_file") val hasFile: Boolean = false,
    @SerialName("stream_url") val streamUrl: String = "",
)

@Serializable
data class SeasonItem(
    val id: String = "",
    @SerialName("season_number") val seasonNumber: Int = 0,
    val name: String = "",
    val episodes: List<EpisodeItem> = emptyList(),
)

@Serializable
data class TvShowItem(
    val id: String = "",
    val title: String = "",
    val year: Int = 0,
    val overview: String = "",
    @SerialName("poster_url") val posterUrl: String = "",
    @SerialName("backdrop_url") val backdropUrl: String = "",
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val genres: List<String> = emptyList(),
    @SerialName("has_file") val hasFile: Boolean = false,
    @SerialName("stream_url") val streamUrl: String = "",
    val seasons: List<SeasonItem> = emptyList(),
    @SerialName("tmdb_id") val tmdbId: Int = 0,
)

@Serializable
data class ListEnvelope<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 0,
    val library: String? = null,
)

@Serializable
data class LibraryRow(
    val id: String = "",
    val name: String = "",
    val title: String = "",
    val path: String = "",
    val year: Int = 0,
)

@Serializable
data class LibraryListResponse(
    val items: List<LibraryRow> = emptyList(),
    val total: Int = 0,
    val available: Boolean = true,
    val message: String = "",
)

@Serializable
data class SearchResult(
    val id: Int = 0,
    val title: String = "",
    val year: Int = 0,
    val overview: String = "",
    val poster: String = "",
    @SerialName("mediaType") val mediaType: String = "movie",
    @SerialName("musicbrainzId") val musicbrainzId: String = "",
    @SerialName("releaseGroupId") val releaseGroupId: String = "",
    @SerialName("recordingId") val recordingId: String = "",
    @SerialName("artistName") val artistName: String = "",
    @SerialName("albumTitle") val albumTitle: String = "",
)

@Serializable
data class SearchResponse(
    val results: List<SearchResult> = emptyList(),
)

@Serializable
data class MediaRequest(
    val id: String = "",
    val title: String = "",
    val year: Int = 0,
    val status: String = "",
    val poster: String = "",
    @SerialName("itemType") val itemType: String = "movie",
    @SerialName("tmdbId") val tmdbId: Int = 0,
)

@Serializable
data class CollectionSummary(
    val id: String = "",
    val name: String = "",
    @SerialName("movie_count") val movieCount: Int = 0,
)

@Serializable
data class CollectionDetail(
    val id: String = "",
    val name: String = "",
    val movies: List<MovieItem> = emptyList(),
)

@Serializable
data class LiveTvChannel(
    val id: String = "",
    val name: String = "",
    val number: String = "",
    val url: String = "",
    val category: String = "",
    @SerialName("now_playing") val nowPlaying: LiveTvNowPlaying? = null,
)

@Serializable
data class LiveTvNowPlaying(
    val title: String = "",
    val start: String = "",
    val end: String = "",
)

@Serializable
data class LiveTvResponse(
    val channels: List<LiveTvChannel> = emptyList(),
    val available: Boolean = true,
    val recordings: List<LiveTvRecording> = emptyList(),
    val timers: List<LiveTvTimer> = emptyList(),
)

@Serializable
data class LiveTvRecording(
    val id: String = "",
    @SerialName("channel_id") val channelId: String = "",
    val title: String = "",
    val start: String = "",
    val end: String = "",
    val status: String = "",
)

@Serializable
data class LiveTvTimer(
    val id: String = "",
    @SerialName("channel_id") val channelId: String = "",
    val title: String = "",
    val start: String = "",
    val end: String = "",
    val series: Boolean = false,
)

@Serializable
data class MusicTrack(
    val id: String = "",
    val title: String = "",
    @SerialName("stream_url") val streamUrl: String = "",
)

@Serializable
data class MusicAlbum(
    val id: String = "",
    val title: String = "",
    val tracks: List<MusicTrack> = emptyList(),
)

@Serializable
data class MusicArtistDetail(
    val artist: LibraryRow = LibraryRow(),
    val albums: List<MusicAlbum> = emptyList(),
    val id: String = "",
    val name: String = "",
)

@Serializable
data class BookFile(
    val id: String = "",
    val title: String = "",
    @SerialName("stream_url") val streamUrl: String = "",
)

@Serializable
data class BookItem(
    val id: String = "",
    val title: String = "",
    val year: Int = 0,
    val files: List<BookFile> = emptyList(),
)

@Serializable
data class BookAuthorDetail(
    val author: LibraryRow = LibraryRow(),
    val books: List<BookItem> = emptyList(),
)

@Serializable
data class PlaybackResolve(
    @SerialName("stream_url") val streamUrl: String = "",
    val mode: String = "direct",
    @SerialName("resume_enabled") val resumeEnabled: Boolean = true,
)

@Serializable
data class PlaybackSubtitleTrack(
    val id: String = "",
    val label: String = "",
    val language: String = "",
    val srclang: String = "",
    val src: String = "",
    val default: Boolean = false,
)

@Serializable
data class PlaybackSubtitlesResponse(
    val tracks: List<PlaybackSubtitleTrack> = emptyList(),
)

@Serializable
data class QuickConnectPoll(
    val approved: Boolean = false,
    val code: String = "",
    val username: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("session_token") val sessionToken: String = "",
)

@Serializable
data class QuickConnectRegister(
    val code: String = "",
    val approved: Boolean = false,
    val pending: Boolean = false,
    val message: String = "",
)

@Serializable
data class TvLoginResponse(
    @SerialName("session_token") val sessionToken: String = "",
    val username: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("requires_2fa") val requires2fa: Boolean = false,
)

@Serializable
data class DiscoverDetail(
    val id: Int = 0,
    val title: String = "",
    val year: Int = 0,
    val overview: String = "",
    val tagline: String = "",
    val genres: List<String> = emptyList(),
    val poster: String = "",
    val backdrop: String = "",
    @SerialName("voteAvg") val voteAvg: Double = 0.0,
    val runtime: Int = 0,
    val status: String = "",
    @SerialName("mediaType") val mediaType: String = "movie",
)

@Serializable
data class RequestTitleResponse(
    @SerialName("requestId") val requestId: String = "",
    val status: String = "",
)

@Serializable
data class NextUpEntry(
    val id: String = "",
    val kind: String = "episode",
    val title: String = "",
    @SerialName("poster_url") val posterUrl: String = "",
    val href: String = "",
    @SerialName("stream_url") val streamUrl: String = "",
    val subtitle: String = "",
    @SerialName("showId") val showId: String = "",
)

@Serializable
data class PlayRequest(
    val src: String = "",
    val title: String = "",
    val mediaId: String = "",
    val kind: String = "movie",
    val poster: String = "",
    val startSec: Long = 0,
    val showId: String = "",
    val season: Int = 0,
    val episode: Int = 0,
)

@Serializable
data class UserdataBlob(
    val progress: Map<String, ProgressEntry> = emptyMap(),
    val favorites: Map<String, FavoriteEntry> = emptyMap(),
    val queue: List<QueueItem> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
)

val muxJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}
