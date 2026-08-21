package app.tit.parsers.manga.site.vi

import com.fleeksoft.ksoup.Ksoup
import kotlin.test.Test
import kotlin.test.assertEquals

class TruyenQQChapterListTest {
    @Test
    fun `functional links are excluded and chapters are sorted ascending`() {
        val doc = Ksoup.parse("""
            <ul class="story-detail-menu"><li><a href="/story-chap-1">Đọc từ đầu</a></li></ul>
            <div class="list_chapter"><div class="works-chapter-list">
              <div class="works-chapter-item"><div class="name-chap"><a href="/story-chap-930">Chương 930</a></div></div>
              <div class="works-chapter-item"><div class="name-chap"><a href="/story-chap-1">Chương 1</a></div></div>
              <div class="works-chapter-item"><div class="name-chap"><a href="">Mới nhất</a></div></div>
            </div></div>
        """.trimIndent())

        val chapters = parseTruyenQQChapters(doc, "https://truyenqqko.com", "TRUYENQQ")

        assertEquals(listOf("Chương 1", "Chương 930"), chapters.map { it.title })
        assertEquals(listOf(1, 2), chapters.map { it.order })
    }
}