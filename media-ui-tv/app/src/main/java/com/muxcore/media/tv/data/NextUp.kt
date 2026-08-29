package com.muxcore.media.tv.data

fun showIdFromHref(href: String): String? {
    val m = Regex("(?:^|/)tv/([^/?#]+)").find(href)
    return m?.groupValues?.getOrNull(1)?.let { java.net.URLDecoder.decode(it, Charsets.UTF_8.name()) }
}

fun flattenEpisodes(show: TvShowItem): List<EpisodeItem> =
    show.seasons.flatMap { it.episodes }.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))

fun nextEpisodeAfter(show: TvShowItem, episodeId: String): EpisodeItem? {
    val eps = flattenEpisodes(show)
    val idx = eps.indexOfFirst { it.id == episodeId }
    if (idx < 0) return null
    for (i in idx + 1 until eps.size) {
        if (eps[i].hasFile) return eps[i]
    }
    return null
}

suspend fun UserdataStore.resolveNextUp(repository: MuxRepository, limit: Int = 12): List<NextUpEntry> {
    val progress = listProgress()
    val continueIds = continueWatching(100).map { it.id }.toSet()
    val out = mutableListOf<NextUpEntry>()
    val seen = mutableSetOf<String>()
    val showCache = mutableMapOf<String, TvShowItem?>()

    suspend fun loadShow(showId: String): TvShowItem? {
        if (showCache.containsKey(showId)) return showCache[showId]
        return try {
            repository.getTvShow(showId).also { showCache[showId] = it }
        } catch (_: Exception) {
            showCache[showId] = null
            null
        }
    }

    for (p in progress) {
        if (out.size >= limit) break
        if (p.kind != "episode" && p.kind != "tv") continue
        val watched = p.watched || (p.durationSec > 0 && p.positionSec.toDouble() / p.durationSec >= 0.92)
        if (!watched) continue
        val showId = showIdFromHref(p.href) ?: continue
        val show = loadShow(showId) ?: continue
        val next = if (p.kind == "episode") {
            nextEpisodeAfter(show, p.id)
        } else {
            val eps = flattenEpisodes(show)
            eps.find { e -> e.hasFile && !progress.any { x -> x.id == e.id && x.watched } }
        }
        if (next == null || seen.contains(next.id)) continue
        if (continueIds.contains(next.id)) continue
        seen.add(next.id)
        val epTitle =
            "${show.title} S${next.seasonNumber.toString().padStart(2, '0')}E${next.episodeNumber.toString().padStart(2, '0')}"
        val title = if (next.title.isNotBlank()) "$epTitle · ${next.title}" else epTitle
        val src = next.streamUrl.ifBlank { "/stream/tv/${next.id}" }
        out.add(
            NextUpEntry(
                id = next.id,
                kind = "episode",
                title = title,
                posterUrl = show.posterUrl.ifBlank { p.posterUrl },
                href = src,
                streamUrl = next.streamUrl,
                subtitle = "Next up",
                showId = show.id,
            ),
        )
    }
    return out.take(limit)
}
