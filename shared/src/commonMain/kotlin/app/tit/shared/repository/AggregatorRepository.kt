package app.tit.shared.repository

import app.tit.content.core.model.*
import app.tit.shared.manager.DomainHealthCache
import app.tit.shared.manager.SourceManager
import app.tit.shared.model.SourceSearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class AggregatorRepository(
    val sourceManager: SourceManager = SourceManager(),
    val healthCache: DomainHealthCache = DomainHealthCache(),
    val storage: app.tit.shared.storage.LibraryStorage = app.tit.shared.storage.LibraryStorage(app.tit.shared.storage.InMemoryKeyValueDriver())
) {
    companion object {
        const val PER_SOURCE_TIMEOUT_MS = 8_000L
    }

    /**
     * Tìm kiếm song song dạng Streaming (Flow) trên các nguồn Novel được bật.
     * UI nhận kết quả từng nguồn ngay khi hoàn tất mà không phải chờ nguồn chậm nhất.
     */
    fun searchNovelsStreaming(
        query: String,
        enabledSourceIds: List<String>? = null,
        page: Int = 1
    ): Flow<SourceSearchResult> = channelFlow {
        val allSources = sourceManager.getSources(ContentType.NOVEL)
        val targetSources = if (enabledSourceIds != null) {
            allSources.filter { it.id in enabledSourceIds }
        } else {
            allSources
        }

        targetSources.forEach { sourceInfo ->
            launch {
                if (healthCache.isSkippable(sourceInfo.id)) {
                    send(SourceSearchResult.Skipped(sourceInfo))
                    return@launch
                }

                val parser = sourceManager.getNovelParser(sourceInfo.id)
                val result = withTimeoutOrNull(PER_SOURCE_TIMEOUT_MS) {
                    runCatching { parser.search(query, page) }
                }

                when {
                    result == null -> {
                        healthCache.markDead(sourceInfo.id)
                        send(SourceSearchResult.TimedOut(sourceInfo))
                    }
                    result.isFailure -> {
                        healthCache.markDead(sourceInfo.id)
                        send(SourceSearchResult.Failed(sourceInfo, result.exceptionOrNull()))
                    }
                    else -> {
                        healthCache.markHealthy(sourceInfo.id)
                        sendFilteredSearchResult(sourceInfo, result.getOrThrow(), query)
                    }
                }
            }
        }
    }

    /**
     * Tìm kiếm song song dạng Streaming (Flow) trên các nguồn Manga được bật.
     */
    fun searchMangaStreaming(
        query: String,
        enabledSourceIds: List<String>? = null,
        page: Int = 1
    ): Flow<SourceSearchResult> = channelFlow {
        val allSources = sourceManager.getSources(ContentType.MANGA)
        val targetSources = if (enabledSourceIds != null) {
            allSources.filter { it.id in enabledSourceIds }
        } else {
            allSources
        }

        targetSources.forEach { sourceInfo ->
            launch {
                if (healthCache.isSkippable(sourceInfo.id)) {
                    send(SourceSearchResult.Skipped(sourceInfo))
                    return@launch
                }

                val parser = sourceManager.getMangaParser(sourceInfo.id)
                val result = withTimeoutOrNull(PER_SOURCE_TIMEOUT_MS) {
                    runCatching { parser.search(query, page) }
                }

                when {
                    result == null -> {
                        healthCache.markDead(sourceInfo.id)
                        send(SourceSearchResult.TimedOut(sourceInfo))
                    }
                    result.isFailure -> {
                        healthCache.markDead(sourceInfo.id)
                        send(SourceSearchResult.Failed(sourceInfo, result.exceptionOrNull()))
                    }
                    else -> {
                        healthCache.markHealthy(sourceInfo.id)
                        sendFilteredSearchResult(sourceInfo, result.getOrThrow(), query)
                    }
                }
            }
        }
    }

    private suspend fun kotlinx.coroutines.channels.ProducerScope<SourceSearchResult>.sendFilteredSearchResult(
        sourceInfo: SourceInfo,
        rawItems: List<Content>,
        query: String
    ) {
        val relevantItems = rawItems.filter { isRelevantSearchResult(it.title, query) }
        if (rawItems.isNotEmpty() && relevantItems.isEmpty()) {
            send(SourceSearchResult.NoRelevantResults(sourceInfo, rawItems.size))
        } else {
            send(SourceSearchResult.Success(sourceInfo, relevantItems))
        }
    }
    suspend fun getNovelCatalog(sourceId: String, page: Int, filter: ContentFilter.NovelFilter): List<Content> {
        return sourceManager.getNovelParser(sourceId).getList(page, filter)
    }

    suspend fun getMangaCatalog(sourceId: String, page: Int, filter: ContentFilter.MangaFilter): List<Content> {
        return sourceManager.getMangaParser(sourceId).getList(page, filter)
    }

    suspend fun getNovelDetails(sourceId: String, novelUrl: String): ContentDetails {
        return sourceManager.getNovelParser(sourceId).getDetails(novelUrl)
    }

    suspend fun getNovelChapterPage(sourceId: String, novelUrl: String, page: Int): List<Chapter> {
        return sourceManager.getNovelParser(sourceId).getChapterPage(novelUrl, page)
    }

    suspend fun getMangaDetails(sourceId: String, mangaUrl: String): ContentDetails {
        return sourceManager.getMangaParser(sourceId).getDetails(mangaUrl)
    }

    suspend fun getNovelChapterContent(sourceId: String, chapterUrl: String): ChapterContent.Text {
        return sourceManager.getNovelParser(sourceId).getChapterContent(chapterUrl)
    }

    suspend fun getMangaChapterContent(sourceId: String, chapterUrl: String): ChapterContent.ImagePages {
        return sourceManager.getMangaParser(sourceId).getChapterContent(chapterUrl)
    }

    // --- Library & Storage Helpers ---
    fun isBookFavorite(url: String): Boolean = storage.isBookInLibrary(url)

    fun toggleBookFavorite(content: Content, categoryId: String = app.tit.shared.model.BookCategory.FAVORITE.id): Boolean {
        return if (storage.isBookInLibrary(content.url)) {
            storage.removeBookFromLibrary(content.url)
            false
        } else {
            storage.saveBookToLibrary(
                app.tit.shared.model.LibraryBook(
                    content = content,
                    categoryId = categoryId
                )
            )
            true
        }
    }

    fun getLibraryBooks(categoryId: String? = null): List<app.tit.shared.model.LibraryBook> {
        val all = storage.getLibraryBooks()
        return if (categoryId == null || categoryId == app.tit.shared.model.BookCategory.ALL.id) all else all.filter { it.categoryId == categoryId }
    }

    fun removeBookFromLibrary(contentUrl: String) = storage.removeBookFromLibrary(contentUrl)

    fun updateBookCategory(contentUrl: String, categoryId: String) = storage.updateBookCategory(contentUrl, categoryId)

    fun getReadingHistory(): List<app.tit.shared.model.ReadingHistoryItem> = storage.getReadingHistory()

    fun recordReadingHistory(item: app.tit.shared.model.ReadingHistoryItem) = storage.recordHistory(item)

    fun isChapterRead(url: String): Boolean = storage.isChapterRead(url)

    fun clearReadingHistory() = storage.clearHistory()

    fun getReaderSettings(): app.tit.shared.model.ReaderSettings = storage.getReaderSettings()

    fun saveReaderSettings(settings: app.tit.shared.model.ReaderSettings) = storage.saveReaderSettings(settings)

    fun createBackupJson(currentTimestamp: Long = 0L): String = storage.createBackupJson(currentTimestamp)

    fun restoreFromBackupJson(jsonString: String): Boolean = storage.restoreFromBackupJson(jsonString)

    fun getBookUpdates(): List<app.tit.shared.model.BookUpdateItem> = storage.getBookUpdates()

    fun recordBookUpdate(item: app.tit.shared.model.BookUpdateItem) = storage.recordBookUpdate(item)

    fun markUpdatesSeen() = storage.markUpdatesSeen()

    fun getUnreadUpdatesCount(): Int = storage.getUnreadUpdatesCount()

    suspend fun checkLibraryUpdates(currentTime: Long = 0L): Int {
        val books = storage.getLibraryBooks()
        var totalNew = 0

        for (book in books) {
            try {
                val details = if (book.content.type == app.tit.content.core.model.ContentType.NOVEL) {
                    getNovelDetails(book.content.sourceId, book.content.url)
                } else {
                    getMangaDetails(book.content.sourceId, book.content.url)
                }

                val remoteCount = details.chapters.size
                val localCount = book.totalChapters

                if (localCount > 0 && remoteCount > localCount) {
                    val diff = remoteCount - localCount
                    totalNew += diff
                    val readCount = details.chapters.count { isChapterRead(it.url) }
                    val readPct = if (remoteCount > 0) (readCount * 100 / remoteCount) else 0

                    storage.recordBookUpdate(
                        app.tit.shared.model.BookUpdateItem(
                            content = details.content,
                            totalChapters = remoteCount,
                            newChaptersCount = diff,
                            latestChapterTitle = details.chapters.lastOrNull()?.title ?: details.content.latestChapter,
                            updatedAt = currentTime,
                            isSeen = false,
                            readPercentage = readPct
                        )
                    )
                }
                storage.saveBookToLibrary(book.copy(totalChapters = remoteCount))
            } catch (_: Exception) { }
        }
        return totalNew
    }
}
