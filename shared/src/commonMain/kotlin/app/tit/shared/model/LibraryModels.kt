package app.tit.shared.model

import app.tit.content.core.model.Content
import kotlinx.serialization.Serializable

@Serializable
enum class BookCategory(val id: String, val displayName: String) {
    ALL("ALL", "Tất cả"),
    READING("READING", "Đang đọc"),
    FAVORITE("FAVORITE", "Yêu thích"),
    COMPLETED("COMPLETED", "Đã đọc xong"),
    PLAN_TO_READ("PLAN_TO_READ", "Kế hoạch")
}

@Serializable
data class LibraryBook(
    val content: Content,
    val categoryId: String = BookCategory.FAVORITE.id,
    val addedAt: Long = 0L,
    val lastReadChapterUrl: String? = null,
    val lastReadChapterTitle: String? = null,
    val lastReadAt: Long? = null,
    val totalChapters: Int = 0
)

@Serializable
data class ReadingHistoryItem(
    val content: Content,
    val chapterUrl: String,
    val chapterTitle: String,
    val readAt: Long,
    val pageIndex: Int = 0,
    val scrollOffset: Float = 0f
)

@Serializable
enum class MangaReadingMode(val title: String) {
    WEBTOON("Cuộn dọc (Webtoon)"),
    LTR("Lật ngang (Trái → Phải)"),
    RTL("Lật ngang (Phải → Trái - Manga)")
}

@Serializable
enum class NovelThemeType(val title: String) {
    LIGHT("Sáng"),
    SEPIA("Giấy ngà"),
    DARK("Tối"),
    AMOLED("Đen AMOLED")
}

@Serializable
data class ReaderSettings(
    val novelFontSize: Float = 18f,
    val novelLineHeightMultiplier: Float = 1.6f,
    val novelFontFamily: String = "DEFAULT", // DEFAULT, SERIF, MONOSPACE, CURSIVE
    val novelTheme: NovelThemeType = NovelThemeType.LIGHT,
    val mangaMode: MangaReadingMode = MangaReadingMode.WEBTOON,
    val volumeKeysNavigation: Boolean = true
)