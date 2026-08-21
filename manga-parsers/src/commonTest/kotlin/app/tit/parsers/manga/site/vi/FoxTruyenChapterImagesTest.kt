package app.tit.parsers.manga.site.vi

import com.fleeksoft.ksoup.Ksoup
import kotlin.test.Test
import kotlin.test.assertEquals

class FoxTruyenChapterImagesTest {
    @Test
    fun `only hinhgg chapter images inside content container are returned`() {
        val doc = Ksoup.parse("""
            <div class="content_detail content_detail_manga">
              <img src="https://hinhgg.com/36860/1/0.jpg" />
              <img src="https://hinhgg.com/36860/1/1.webp?x=1" />
              <img src="https://hinhgg.com/assets/logo.png" />
              <img src="https://other.example/page-3.jpg" />
            </div>
            <section class="recommend"><img src="https://hinhgg.com/covers/recommend.jpg" /></section>
        """.trimIndent())

        assertEquals(
            listOf("https://hinhgg.com/36860/1/0.jpg", "https://hinhgg.com/36860/1/1.webp?x=1"),
            parseFoxChapterImages(doc)
        )
    }
}