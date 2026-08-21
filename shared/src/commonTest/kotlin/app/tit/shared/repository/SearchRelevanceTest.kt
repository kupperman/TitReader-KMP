package app.tit.shared.repository

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchRelevanceTest {
    @Test
    fun `Vietnamese accents are ignored`() {
        assertTrue(isRelevantSearchResult("Yêu Long Cổ Đế", "yeu long"))
    }

    @Test
    fun `unrelated catalog result is rejected`() {
        assertFalse(isRelevantSearchResult("Thần Đạo Đan Tôn", "yeu long"))
    }

    @Test
    fun `punctuation and case are ignored`() {
        assertTrue(isRelevantSearchResult("MỖI-TUẦN Có Một Nghề Nghiệp Mới", "moi tuan"))
    }

    @Test
    fun `one matching query token is sufficient`() {
        assertTrue(isRelevantSearchResult("Long Vương Truyền Thuyết", "yeu long"))
    }
}
