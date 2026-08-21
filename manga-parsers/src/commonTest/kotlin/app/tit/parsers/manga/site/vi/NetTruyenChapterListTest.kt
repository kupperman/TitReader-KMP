package app.tit.parsers.manga.site.vi

import kotlin.test.Test
import kotlin.test.assertEquals

class NetTruyenChapterListTest {
    @Test
    fun `chapter api newest first is deduplicated and sorted oldest first`() {
        val json = """{"data":[
          {"chapter_name":"Chapter 548","chapter_num":548},
          {"chapter_name":"Chapter 1.5","chapter_num":1.5},
          {"chapter_name":"Chapter 0","chapter_num":0},
          {"chapter_name":"Chapter 548 duplicate","chapter_num":548}
        ]}"""

        val chapters = parseNetTruyenChapterJson(json, "sample", "https://nettruyenx.net", "NETTRUYEN")

        assertEquals(listOf("Chapter 0", "Chapter 1.5", "Chapter 548"), chapters.map { it.title })
        assertEquals("https://nettruyenx.net/truyen-tranh/sample/chuong-0", chapters.first().url)
        assertEquals(listOf(1, 2, 3), chapters.map { it.order })
    }
}