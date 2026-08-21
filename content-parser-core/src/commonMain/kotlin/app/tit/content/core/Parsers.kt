package app.tit.content.core

import app.tit.content.core.model.ChapterContent
import app.tit.content.core.model.Content
import app.tit.content.core.model.ContentDetails
import app.tit.content.core.model.ContentFilter
import app.tit.content.core.model.ContentType
import app.tit.content.core.model.SourceInfo

interface ContentParser {
    val id: String
    val name: String
    val domain: String
    val type: ContentType
    val iconUrl: String? get() = null
    val isEnabled: Boolean get() = true
}

interface NovelParser : ContentParser {
    override val type: ContentType get() = ContentType.NOVEL

    suspend fun getList(page: Int, filter: ContentFilter.NovelFilter): List<Content>
    suspend fun search(query: String, page: Int = 1): List<Content>
    suspend fun getDetails(novelUrl: String): ContentDetails
    suspend fun getChapterPage(novelUrl: String, page: Int): List<app.tit.content.core.model.Chapter> = emptyList()
    suspend fun getChapterContent(chapterUrl: String): ChapterContent.Text
}

interface MangaParser : ContentParser {
    override val type: ContentType get() = ContentType.MANGA

    suspend fun getList(page: Int, filter: ContentFilter.MangaFilter): List<Content>
    suspend fun search(query: String, page: Int = 1): List<Content>
    suspend fun getDetails(mangaUrl: String): ContentDetails
    suspend fun getChapterContent(chapterUrl: String): ChapterContent.ImagePages
}

interface SourceCatalog<T : ContentParser> {
    val type: ContentType
    fun allSources(): List<SourceInfo>
    fun createParser(id: String, ctx: LoaderContext): T
}
