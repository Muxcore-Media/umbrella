package systems.zem.muxcore.data

/** Request/acquisition status helpers — direct port of media-ui-app lib/acquisition.ts. */
object Acquisition {

    fun isWatchable(hasFile: Boolean): Boolean = hasFile

    /** Request-media statuses that mean acquisition is still underway. */
    fun isActiveRequestStatus(status: String?): Boolean {
        val s = status?.trim()?.lowercase().orEmpty()
        return s.isNotEmpty() && s != "available"
    }

    fun requestStatusLabel(status: String): String = when (status.trim().lowercase()) {
        "downloading" -> "Downloading"
        "searching" -> "Searching"
        "queued" -> "Queued"
        "added" -> "In library"
        "requested" -> "Requested"
        "workflow", "pending" -> "Pending approval"
        "import_failed" -> "Import failed"
        "failed" -> "Download failed"
        "stalled" -> "Stalled"
        "denied" -> "Denied"
        "available" -> "Available"
        else -> status.replace('_', ' ')
            .split(' ')
            .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
    }

    /** Badge text for a request row — prefers API statusLabel when present. */
    fun requestDisplayLabel(req: MediaRequest): String {
        val label = req.statusLabel?.trim().orEmpty()
        return label.ifEmpty { requestStatusLabel(req.status) }
    }

    /** Subtitle detail when API provides statusDetail and it is not already in the label. */
    fun requestDisplayDetail(req: MediaRequest): String? {
        val detail = req.statusDetail?.trim().orEmpty()
        if (detail.isEmpty()) return null
        val label = req.statusLabel?.trim().orEmpty()
        if (label.isNotEmpty() && label.contains(detail)) return null
        return detail
    }

    enum class Tone { SUCCESS, ACCENT, NEUTRAL, WARNING, DANGER }

    fun requestStatusTone(status: String): Tone = when (status.trim().lowercase()) {
        "available", "added" -> Tone.SUCCESS
        "downloading" -> Tone.ACCENT
        "searching", "queued", "pending", "workflow", "stalled" -> Tone.WARNING
        "import_failed", "failed", "denied" -> Tone.DANGER
        else -> Tone.NEUTRAL
    }

    enum class Phase { ATTENTION, DOWNLOADING, SEARCHING, PENDING, REQUESTED }

    fun requestPhase(status: String): Phase = when (status.trim().lowercase()) {
        "import_failed", "failed", "stalled", "denied" -> Phase.ATTENTION
        "downloading" -> Phase.DOWNLOADING
        "searching", "queued" -> Phase.SEARCHING
        "pending", "workflow" -> Phase.PENDING
        else -> Phase.REQUESTED
    }

    fun attentionHint(status: String): String? = when (status.trim().lowercase()) {
        "import_failed" -> "Download finished but could not be added to your library yet."
        "failed" -> "The grab did not complete successfully."
        "stalled" -> "Download is stuck and may need attention."
        "denied" -> "This request was not approved."
        else -> null
    }

    fun posterUrlForRequest(poster: String?): String {
        val p = poster?.trim().orEmpty()
        if (p.isEmpty()) return ""
        if (p.startsWith("http") || p.startsWith("/images")) return p
        if (p.startsWith("/")) return "https://image.tmdb.org/t/p/w185$p"
        return p
    }

    sealed class InProgressEntry {
        data class FromRequest(val request: MediaRequest) : InProgressEntry()
        data class FromLibrary(val kind: String, val id: String, val title: String, val year: Int, val posterUrl: String) :
            InProgressEntry()

        val entryTitle: String
            get() = when (this) {
                is FromRequest -> request.title
                is FromLibrary -> title
            }

        val entryStatus: String
            get() = when (this) {
                is FromRequest -> request.status
                is FromLibrary -> "added"
            }
    }

    private fun requestKey(req: MediaRequest): String = when {
        req.itemId.isNotEmpty() -> "${req.itemType}:${req.itemId}"
        req.tmdbId > 0 -> "${req.itemType}:tmdb:${req.tmdbId}"
        else -> "${req.itemType}:${req.title.lowercase()}"
    }

    /** Merge active request-media rows with library titles that are not watchable yet. */
    fun mergeInProgressEntries(
        requests: List<MediaRequest>,
        movies: List<Movie>,
        shows: List<TVShow>,
    ): List<InProgressEntry> {
        val active = requests.filter { isActiveRequestStatus(it.status) }
        val keys = active.map { requestKey(it) }.toMutableSet()
        val out = mutableListOf<InProgressEntry>()
        active.forEach { out.add(InProgressEntry.FromRequest(it)) }

        for (m in movies) {
            if (m.hasFile) continue
            val key = when {
                m.id.isNotEmpty() -> "movie:${m.id}"
                m.tmdbId != null -> "movie:tmdb:${m.tmdbId}"
                else -> "movie:${m.title.lowercase()}"
            }
            if (!keys.add(key)) continue
            out.add(InProgressEntry.FromLibrary("movie", m.id, m.title, m.year, m.posterUrl))
        }
        for (s in shows) {
            if (s.hasFile) continue
            val key = when {
                s.id.isNotEmpty() -> "tv:${s.id}"
                s.tmdbId != null -> "tv:tmdb:${s.tmdbId}"
                else -> "tv:${s.title.lowercase()}"
            }
            if (!keys.add(key)) continue
            out.add(InProgressEntry.FromLibrary("tv", s.id, s.title, s.year, s.posterUrl))
        }

        fun rank(entry: InProgressEntry): Int = when (entry) {
            is InProgressEntry.FromRequest -> when (requestPhase(entry.request.status)) {
                Phase.ATTENTION -> 0
                Phase.DOWNLOADING -> 1
                Phase.SEARCHING -> 2
                Phase.PENDING -> 3
                Phase.REQUESTED -> 4
            }
            is InProgressEntry.FromLibrary -> 5
        }

        return out.sortedWith(compareBy({ rank(it) }, { it.entryTitle.lowercase() }))
    }

    data class Grouped(
        val attention: List<InProgressEntry>,
        val pending: List<InProgressEntry>,
        val downloading: List<InProgressEntry>,
        val searching: List<InProgressEntry>,
        val requested: List<InProgressEntry>,
    )

    fun groupByPhase(entries: List<InProgressEntry>): Grouped {
        val attention = mutableListOf<InProgressEntry>()
        val downloading = mutableListOf<InProgressEntry>()
        val searching = mutableListOf<InProgressEntry>()
        val pending = mutableListOf<InProgressEntry>()
        val requested = mutableListOf<InProgressEntry>()
        for (e in entries) {
            when (requestPhase(e.entryStatus)) {
                Phase.ATTENTION -> attention.add(e)
                Phase.DOWNLOADING -> downloading.add(e)
                Phase.SEARCHING -> searching.add(e)
                Phase.PENDING -> pending.add(e)
                Phase.REQUESTED -> requested.add(e)
            }
        }
        return Grouped(attention, pending, downloading, searching, requested)
    }
}
