package systems.zem.muxcore.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AcquisitionTest {

    private fun req(status: String, title: String = "T", itemId: String = "", tmdbId: Int = 0) = MediaRequest(
        id = "r-$title",
        itemType = "movie",
        itemId = itemId,
        tmdbId = tmdbId,
        title = title,
        year = 2024,
        poster = "",
        status = status,
        createdAt = "",
        updatedAt = "",
    )

    @Test
    fun `active request status excludes available`() {
        assertTrue(Acquisition.isActiveRequestStatus("downloading"))
        assertTrue(Acquisition.isActiveRequestStatus("Searching"))
        assertFalse(Acquisition.isActiveRequestStatus("available"))
        assertFalse(Acquisition.isActiveRequestStatus(""))
        assertFalse(Acquisition.isActiveRequestStatus(null))
    }

    @Test
    fun `status labels match web copy`() {
        assertEquals("Pending approval", Acquisition.requestStatusLabel("workflow"))
        assertEquals("Import failed", Acquisition.requestStatusLabel("import_failed"))
        assertEquals("In library", Acquisition.requestStatusLabel("added"))
        assertEquals("Some Odd Status", Acquisition.requestStatusLabel("some_odd_status"))
    }

    @Test
    fun `tones and phases`() {
        assertEquals(Acquisition.Tone.SUCCESS, Acquisition.requestStatusTone("available"))
        assertEquals(Acquisition.Tone.DANGER, Acquisition.requestStatusTone("failed"))
        assertEquals(Acquisition.Phase.ATTENTION, Acquisition.requestPhase("stalled"))
        assertEquals(Acquisition.Phase.DOWNLOADING, Acquisition.requestPhase("downloading"))
        assertEquals(Acquisition.Phase.REQUESTED, Acquisition.requestPhase("requested"))
    }

    @Test
    fun `merge dedupes library titles already covered by requests`() {
        val movie = Movie(
            id = "m1", title = "T", year = 2024, overview = "", runtime = 0,
            voteAverage = 0.0, genres = emptyList(), posterUrl = "", hasFile = false,
            streamUrl = "", createdAt = "",
        )
        val merged = Acquisition.mergeInProgressEntries(
            requests = listOf(req("downloading", title = "T", itemId = "m1")),
            movies = listOf(movie),
            shows = emptyList(),
        )
        assertEquals(1, merged.size)
        assertTrue(merged[0] is Acquisition.InProgressEntry.FromRequest)
    }

    @Test
    fun `merge sorts attention first and library last`() {
        val merged = Acquisition.mergeInProgressEntries(
            requests = listOf(req("requested", "B"), req("failed", "A"), req("downloading", "C")),
            movies = listOf(
                Movie(
                    id = "m9", title = "Z", year = 0, overview = "", runtime = 0,
                    voteAverage = 0.0, genres = emptyList(), posterUrl = "", hasFile = false,
                    streamUrl = "", createdAt = "",
                ),
            ),
            shows = emptyList(),
        )
        assertEquals("A", merged[0].entryTitle) // attention
        assertEquals("C", merged[1].entryTitle) // downloading
        assertEquals("B", merged[2].entryTitle) // requested
        assertEquals("Z", merged[3].entryTitle) // library
    }

    @Test
    fun `poster url for request maps tmdb paths`() {
        assertEquals("https://image.tmdb.org/t/p/w185/x.jpg", Acquisition.posterUrlForRequest("/x.jpg"))
        assertEquals("http://a/b.jpg", Acquisition.posterUrlForRequest("http://a/b.jpg"))
        assertEquals("/images/movies/y.jpg", Acquisition.posterUrlForRequest("/images/movies/y.jpg"))
        assertEquals("", Acquisition.posterUrlForRequest(null))
    }

    @Test
    fun `attention hints only for failure states`() {
        assertNull(Acquisition.attentionHint("downloading"))
        assertEquals(
            "This request was not approved.",
            Acquisition.attentionHint("denied"),
        )
    }
}
