package com.muxcore.media.tv.navigation

import com.muxcore.media.tv.data.PlayRequest
import com.muxcore.media.tv.data.muxJson

fun encodePlay(req: PlayRequest): String =
    java.net.URLEncoder.encode(muxJson.encodeToString(PlayRequest.serializer(), req), Charsets.UTF_8.name())

fun decodePlay(encoded: String): PlayRequest =
    muxJson.decodeFromString(java.net.URLDecoder.decode(encoded, Charsets.UTF_8.name()))

fun moviePlayRequest(movie: com.muxcore.media.tv.data.MovieItem): PlayRequest {
    val src = movie.streamUrl.ifBlank { "/stream/movies/${movie.id}" }
    return PlayRequest(
        src = src,
        title = movie.title,
        mediaId = movie.id,
        kind = "movie",
        poster = movie.posterUrl,
    )
}

fun episodePlayRequest(
    show: com.muxcore.media.tv.data.TvShowItem,
    ep: com.muxcore.media.tv.data.EpisodeItem,
): PlayRequest {
    val src = ep.streamUrl.ifBlank { "/stream/tv/${ep.id}" }
    return PlayRequest(
        src = src,
        title = "${show.title} S${ep.seasonNumber}E${ep.episodeNumber}",
        mediaId = ep.id,
        kind = "episode",
        poster = show.posterUrl,
        showId = show.id,
        season = ep.seasonNumber,
        episode = ep.episodeNumber,
    )
}

fun progressPlayRequest(entry: com.muxcore.media.tv.data.ProgressEntry): PlayRequest =
    PlayRequest(
        src = entry.streamUrl.ifBlank { entry.href },
        title = entry.title,
        mediaId = entry.id,
        kind = entry.kind,
        poster = entry.posterUrl,
        startSec = entry.positionSec,
    )

fun nextUpPlayRequest(entry: com.muxcore.media.tv.data.NextUpEntry): PlayRequest =
    PlayRequest(
        src = entry.streamUrl.ifBlank { entry.href },
        title = entry.title,
        mediaId = entry.id,
        kind = entry.kind,
        poster = entry.posterUrl,
        showId = entry.showId,
    )
