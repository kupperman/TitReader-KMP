package app.tit.content.core.model

import kotlinx.serialization.Serializable

enum class ContentType {
    NOVEL,
    MANGA
}

enum class ContentStatus {
    ONGOING,
    COMPLETED,
    UNKNOWN
}

enum class SortOrder {
    LATEST,
    HOT,
    COMPLETED
}

@Serializable
data class Content(
    val id: String,
    val title: String,
    val url: String,
    val coverUrl: String? = null,
    val author: String? = null,
    val latestChapter: String? = null,
    val type: ContentType,
    val sourceId: String,
    val sourceName: String
)

@Serializable
data class ContentDetails(
    val content: Content,
    val description: String? = null,
    val status: ContentStatus = ContentStatus.UNKNOWN,
    val genres: List<String> = emptyList(),
    val chapters: List<Chapter> = emptyList(),
    val chapterPageCount: Int = 1
)

@Serializable
data class Chapter(
    val id: String,
    val title: String,
    val url: String,
    val order: Int = 0,
    val sourceId: String
)

sealed class ChapterContent {
    data class Text(
        val title: String,
        val chapterUrl: String,
        val text: String,
        val paragraphs: List<String> = emptyList(),
        val prevChapterUrl: String? = null,
        val nextChapterUrl: String? = null,
        val sourceId: String
    ) : ChapterContent()

    data class ImagePages(
        val title: String,
        val chapterUrl: String,
        val imageUrls: List<String>,
        val prevChapterUrl: String? = null,
        val nextChapterUrl: String? = null,
        val sourceId: String
    ) : ChapterContent()
}

sealed class ContentFilter {
    data class NovelFilter(
        val order: SortOrder = SortOrder.LATEST,
        val genre: String? = null,
        val status: ContentStatus? = null
    ) : ContentFilter()

    data class MangaFilter(
        val order: SortOrder = SortOrder.LATEST,
        val genre: String? = null,
        val status: ContentStatus? = null,
        val isColor: Boolean? = null
    ) : ContentFilter()
}

data class SourceInfo(
    val id: String,
    val name: String,
    val lang: String = "vi",
    val iconUrl: String? = null,
    val version: Int = 1,
    val type: ContentType,
    val domain: String
)
