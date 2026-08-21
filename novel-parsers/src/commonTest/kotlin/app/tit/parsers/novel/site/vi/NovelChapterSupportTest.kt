package app.tit.parsers.novel.site.vi

import com.fleeksoft.ksoup.Ksoup
import kotlin.test.Test
import kotlin.test.assertEquals

class NovelChapterSupportTest {
    @Test
    fun `chapter page is sorted by parsed number regardless of DOM order`() {
        val doc = Ksoup.parse("""
            <div id="chapter-list"><ul class="list-chapter">
              <li><a href="/s/chuong-100/">Chương 100</a></li>
              <li><a href="/s/chuong-51/">Chương 51</a></li>
            </ul></div>
        """.trimIndent())
        val chapters = parsePagedNovelChapters(doc, "https://example.com", "TEST")
        assertEquals(listOf("Chương 51", "Chương 100"), chapters.map { it.title })
        assertEquals(listOf(51, 100), chapters.map { it.order })
    }

    @Test
    fun `chapter title ignores breadcrumb novel title`() {
        val doc = Ksoup.parse("""
            <a class="chapter-title" title="Yêu Long Cổ Đế">Yêu Long Cổ Đế</a>
            <h2 class="title-chapter">Chương 1: Ép Hôn</h2>
        """.trimIndent())
        assertEquals("Chương 1: Ép Hôn", selectRealChapterTitle(doc))
    }

    @Test
    fun `pagination count takes the largest page`() {
        val doc = Ksoup.parse("""
            <a href="/trang-2/">2</a><a href="/trang-131/">Cuối</a>
        """.trimIndent())
        assertEquals(131, findChapterPageCount(doc))
    }
}