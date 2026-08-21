package app.tit.parsers.novel.site.vi

import com.fleeksoft.ksoup.Ksoup
import kotlin.test.Test
import kotlin.test.assertEquals

class TruyenFullChapterListTest {
    @Test
    fun chapterListIsSortedAscendingRegardlessOfDomOrder() {
        val newestFirst = """
            <div id="list-chapter"><ul class="list-chapter">
                <li><a href="/story/chuong-600/" title="Chương 600">Chương 600</a></li>
                <li><a href="/story/chuong-599/" title="Chương 599">Chương 599</a></li>
                <li><a href="/story/chuong-1/" title="Chương 1">Chương 1</a></li>
            </ul></div>
        """.trimIndent()
        val chapters = parseTruyenFullChapterPages(listOf(Ksoup.parse(newestFirst) to "https://truyenfull.live"), "TRUYENFULL")
        assertEquals("Chương 1", chapters.first().title)
        assertEquals("Chương 600", chapters.last().title)
    }

    @Test
    fun chapterPagesAreMergedDeduplicatedAndIgnoreWidgets() {
        val firstPage = """
            <div id="list-chapter"><ul class="list-chapter">
                <li><a href="/story/chuong-2/">Chương 2</a></li>
                <li><a href="/story/chuong-1/">Chương 1</a></li>
            </ul></div>
            <aside><a href="/story/chuong-999/">Chương 999 từ widget</a></aside>
        """.trimIndent()
        val secondPage = """
            <div id="list-chapter"><ul class="list-chapter">
                <li><a href="/story/chuong-3/">Chương 3</a></li>
                <li><a href="/story/chuong-2/">Chương 2</a></li>
            </ul></div>
        """.trimIndent()
        val chapters = parseTruyenFullChapterPages(listOf(Ksoup.parse(firstPage) to "https://truyenfull.live", Ksoup.parse(secondPage) to "https://truyenfull.live"), "TRUYENFULL")
        assertEquals(listOf("Chương 1", "Chương 2", "Chương 3"), chapters.map { it.title })
    }
}
