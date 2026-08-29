package systems.zem.muxcore.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserDataLogicTest {

    private fun ep(id: String, s: Int, e: Int, hasFile: Boolean) = Episode(
        id = id, seasonNumber = s, episodeNumber = e, title = "", overview = "",
        runtime = 0, hasFile = hasFile, streamUrl = if (hasFile) "/stream/tv/$id" else "",
    )

    private val show = TVShow(
        id = "t1", title = "Show", year = 2024, overview = "", voteAverage = 0.0,
        genres = emptyList(), posterUrl = "", hasFile = true, streamUrl = "", createdAt = "",
        seasons = listOf(
            Season("s2", 2, "Season 2", 2, "", listOf(ep("e3", 2, 1, true), ep("e4", 2, 2, false))),
            Season("s1", 1, "Season 1", 2, "", listOf(ep("e1", 1, 1, true), ep("e2", 1, 2, true))),
        ),
    )

    @Test
    fun `flattenEpisodes sorts by season then episode`() {
        val eps = UserDataStore.flattenEpisodes(show)
        assertEquals(listOf("e1", "e2", "e3", "e4"), eps.map { it.id })
    }

    @Test
    fun `nextEpisodeAfter returns next with file`() {
        assertEquals("e2", UserDataStore.nextEpisodeAfter(show, "e1")?.id)
        // e4 has no file, so after e3 there is nothing playable.
        assertNull(UserDataStore.nextEpisodeAfter(show, "e3"))
        assertNull(UserDataStore.nextEpisodeAfter(show, "missing"))
    }
}
