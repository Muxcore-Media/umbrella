package systems.zem.muxcore.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalizersTest {

    private fun obj(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    @Test
    fun `movie parses snake_case fields`() {
        val m = Normalizers.movie(
            obj(
                """{"id":"m1","title":"Dune","year":2021,"vote_average":8.1,
                   "genres":["Sci-Fi"],"poster_url":"/abc.jpg","has_file":true,
                   "stream_url":"/stream/movies/m1","runtime":155,
                   "collection_id":10,"collection_name":"Dune Saga"}""",
            ),
        )
        assertEquals("m1", m.id)
        assertEquals("Dune", m.title)
        assertEquals(2021, m.year)
        assertEquals(8.1, m.voteAverage, 0.001)
        assertTrue(m.hasFile)
        assertEquals("https://image.tmdb.org/t/p/w500/abc.jpg", m.posterUrl)
        assertEquals("/stream/movies/m1", m.streamUrl)
        assertEquals(10, m.collectionId)
        assertEquals("Dune Saga", m.collectionName)
    }

    @Test
    fun `movie parses camelCase and falls back to derived stream url`() {
        val m = Normalizers.movie(
            obj("""{"id":"m2","title":"Arrival","hasFile":true,"posterUrl":"poster.jpg"}"""),
        )
        assertTrue(m.hasFile)
        assertEquals("/images/movies/poster.jpg", m.posterUrl)
        assertEquals("/stream/movies/m2", m.streamUrl)
    }

    @Test
    fun `tv show derives hasFile and streamUrl from episodes`() {
        val s = Normalizers.tvShow(
            obj(
                """{"id":"t1","title":"Severance","seasons":[
                     {"id":"s1","season_number":1,"name":"Season 1","episodes":[
                        {"id":"e1","season_number":1,"episode_number":1,"title":"Good News","has_file":false},
                        {"id":"e2","season_number":1,"episode_number":2,"title":"Half Loop","has_file":true}
                     ]}
                   ]}""",
            ),
        )
        assertTrue(s.hasFile)
        assertEquals("/stream/tv/e2", s.streamUrl)
        assertEquals(2, s.seasons!![0].episodes.size)
        assertEquals("", s.seasons!![0].episodes[0].streamUrl)
        assertEquals("/stream/tv/e2", s.seasons!![0].episodes[1].streamUrl)
    }

    @Test
    fun `search result skips music kinds`() {
        assertNull(Normalizers.searchResult(obj("""{"id":1,"title":"Song","mediaType":"music"}""")))
        val movie = Normalizers.searchResult(obj("""{"id":2,"title":"Heat","media_type":"movie","voteAvg":8.3}"""))
        assertNotNull(movie)
        assertEquals("movie", movie!!.mediaType)
        assertEquals(8.3, movie.voteAvg, 0.001)
    }

    @Test
    fun `asList handles bare arrays and wrapped objects`() {
        val bare = Normalizers.asList(Json.parseToJsonElement("""[{"id":"a","title":"A"}]""")) { Normalizers.movie(it) }
        assertEquals(1, bare.items.size)
        assertEquals(1, bare.total)

        val wrapped = Normalizers.asList(
            Json.parseToJsonElement("""{"items":[{"id":"b","title":"B"}],"total":42,"page":2,"page_size":24}"""),
        ) { Normalizers.movie(it) }
        assertEquals(42, wrapped.total)
        assertEquals(2, wrapped.page)
        assertEquals(24, wrapped.pageSize)
    }

    @Test
    fun `capabilities merges over defaults and ignores unknown keys`() {
        val caps = Normalizers.capabilities(
            obj("""{"libraries":{"movies":false,"weird":true},"features":{"watchlist":true}}"""),
        )
        assertFalse(caps.library("movies"))
        assertTrue(caps.library("tv"))
        assertTrue(caps.feature("watchlist"))
        assertFalse(caps.library("weird"))
    }

    @Test
    fun `playback resolve parses both key styles`() {
        val r = Normalizers.playbackResolve(
            obj(
                """{"stream_url":"/stream/transcode?src=x","mode":"transcode",
                   "transcoder_available":true,"max_bitrate_mbps":"12","trickplay_enabled":true}""",
            ),
        )
        assertEquals("transcode", r.mode)
        assertTrue(r.transcoderAvailable)
        assertTrue(r.trickplayEnabled)
        assertEquals("12", r.maxBitrateMbps)
    }

    @Test
    fun `segments and chapters parse`() {
        val segs = Normalizers.segments(
            obj("""{"segments":[{"kind":"intro","start_seconds":0,"end_seconds":42.5,"confidence":0.9,"source":"ml"}]}"""),
        )
        assertEquals(1, segs.size)
        assertEquals(42.5, segs[0].endSeconds, 0.001)

        val chapters = Normalizers.chapters(
            obj("""{"chapters":[{"title":"","start_seconds":0,"end_seconds":300}]}"""),
        )
        assertEquals("Chapter 1", chapters[0].title)
    }

    @Test
    fun `unwrapDetail prefers nested keys`() {
        val raw = obj("""{"movie":{"id":"x","title":"X"}}""")
        assertEquals("x", Normalizers.str(Normalizers.unwrapDetail(raw, "movie", "item"), "id"))
        val flat = obj("""{"id":"y","title":"Y"}""")
        assertEquals("y", Normalizers.str(Normalizers.unwrapDetail(flat, "movie"), "id"))
    }
}
