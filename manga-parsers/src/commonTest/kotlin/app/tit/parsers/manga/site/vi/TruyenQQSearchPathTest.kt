package app.tit.parsers.manga.site.vi

import kotlin.test.Test
import kotlin.test.assertEquals

class TruyenQQSearchPathTest {
    @Test
    fun `page one uses real full search endpoint`() {
        assertEquals("/tim-kiem?q=moi%20tuan", truyenQQSearchPath("moi tuan", 1))
    }

    @Test
    fun `later pages use real paged search endpoint`() {
        assertEquals("/tim-kiem/trang-2?q=one%20punch%20man", truyenQQSearchPath("one punch man", 2))
    }
}
