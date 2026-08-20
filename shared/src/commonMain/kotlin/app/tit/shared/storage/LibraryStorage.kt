package app.tit.shared.storage

import app.tit.shared.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface KeyValueDriver {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

class InMemoryKeyValueDriver : KeyValueDriver {
    private val map = mutableMapOf<String, String>()
    override fun getString(key: String): String? = map[key]
    override fun putString(key: String, value: String) { map[key] = value }
    override fun remove(key: String) { map.remove(key) }
}

class LibraryStorage(private val driver: KeyValueDriver) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private val KEY_LIBRARY = "tit_library_books"
    private val KEY_HISTORY = "tit_reading_history"
    private val KEY_READ_CHAPTERS = "tit_read_chapters"
    private val KEY_SETTINGS = "tit_reader_settings"

    // 1. Library / Favorites
    fun getLibraryBooks(): List<LibraryBook> {
        val raw = driver.getString(KEY_LIBRARY) ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isBookInLibrary(contentUrl: String): Boolean {
        return getLibraryBooks().any { it.content.url == contentUrl }
    }

    fun getLibraryBook(contentUrl: String): LibraryBook? {
        return getLibraryBooks().find { it.content.url == contentUrl }
    }

    fun saveBookToLibrary(book: LibraryBook) {
        val current = getLibraryBooks().toMutableList()
        val index = current.indexOfFirst { it.content.url == book.content.url }
        if (index >= 0) {
            current[index] = book
        } else {
            current.add(0, book)
        }
        driver.putString(KEY_LIBRARY, json.encodeToString(current))
    }

    fun removeBookFromLibrary(contentUrl: String) {
        val current = getLibraryBooks().filterNot { it.content.url == contentUrl }
        driver.putString(KEY_LIBRARY, json.encodeToString(current))
    }

    fun updateBookCategory(contentUrl: String, categoryId: String) {
        val book = getLibraryBook(contentUrl) ?: return
        saveBookToLibrary(book.copy(categoryId = categoryId))
    }

    // 2. Reading History
    fun getReadingHistory(): List<ReadingHistoryItem> {
        val raw = driver.getString(KEY_HISTORY) ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun recordHistory(item: ReadingHistoryItem) {
        val current = getReadingHistory().filterNot { it.content.url == item.content.url }.toMutableList()
        current.add(0, item)
        val trimmed = current.take(100)
        driver.putString(KEY_HISTORY, json.encodeToString(trimmed))

        // Cập nhật lại tiến độ trong Library nếu có
        val bookInLib = getLibraryBook(item.content.url)
        if (bookInLib != null) {
            saveBookToLibrary(
                bookInLib.copy(
                    lastReadChapterUrl = item.chapterUrl,
                    lastReadChapterTitle = item.chapterTitle,
                    lastReadAt = item.readAt
                )
            )
        }

        // Đánh dấu chương đã đọc
        markChapterAsRead(item.chapterUrl)
    }

    fun clearHistory() {
        driver.remove(KEY_HISTORY)
    }

    // 3. Read Chapters Tracker
    fun getReadChapters(): Set<String> {
        val raw = driver.getString(KEY_READ_CHAPTERS) ?: return emptySet()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun isChapterRead(chapterUrl: String): Boolean {
        return getReadChapters().contains(chapterUrl)
    }

    fun markChapterAsRead(chapterUrl: String) {
        val current = getReadChapters().toMutableSet()
        current.add(chapterUrl)
        driver.putString(KEY_READ_CHAPTERS, json.encodeToString(current))
    }

    // 4. Reader Settings
    fun getReaderSettings(): ReaderSettings {
        val raw = driver.getString(KEY_SETTINGS) ?: return ReaderSettings()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            ReaderSettings()
        }
    }

    fun saveReaderSettings(settings: ReaderSettings) {
        driver.putString(KEY_SETTINGS, json.encodeToString(settings))
    }

    // 5. Backup & Restore
    fun createBackupPayload(): BackupPayload {
        return BackupPayload(
            version = 1,
            appName = "TitReader-KMP",
            exportedAt = 0L, // will be populated or handled
            library = getLibraryBooks(),
            history = getReadingHistory(),
            settings = getReaderSettings(),
            readChapterUrls = getReadChapters().toList()
        )
    }

    fun createBackupJson(currentTimestamp: Long = 0L): String {
        val payload = createBackupPayload().copy(exportedAt = currentTimestamp)
        return json.encodeToString(payload)
    }

    fun restoreFromBackupJson(jsonString: String): Boolean {
        return try {
            val payload = json.decodeFromString<BackupPayload>(jsonString)
            if (payload.library.isNotEmpty()) {
                val currentLib = getLibraryBooks().toMutableList()
                payload.library.forEach { imported ->
                    val idx = currentLib.indexOfFirst { it.content.url == imported.content.url }
                    if (idx >= 0) {
                        currentLib[idx] = imported
                    } else {
                        currentLib.add(imported)
                    }
                }
                driver.putString(KEY_LIBRARY, json.encodeToString(currentLib))
            }

            if (payload.history.isNotEmpty()) {
                val currentHistory = getReadingHistory().toMutableList()
                payload.history.forEach { imported ->
                    if (currentHistory.none { it.chapterUrl == imported.chapterUrl }) {
                        currentHistory.add(imported)
                    }
                }
                currentHistory.sortByDescending { it.readAt }
                driver.putString(KEY_HISTORY, json.encodeToString(currentHistory.take(100)))
            }

            if (payload.readChapterUrls.isNotEmpty()) {
                val currentReads = getReadChapters().toMutableSet()
                currentReads.addAll(payload.readChapterUrls)
                driver.putString(KEY_READ_CHAPTERS, json.encodeToString(currentReads))
            }

            saveReaderSettings(payload.settings)
            true
        } catch (e: Exception) {
            false
        }
    }

    // 6. Book Updates Feed (Kotatsu-style)
    private val KEY_UPDATES = "tit_book_updates"

    fun getBookUpdates(): List<BookUpdateItem> {
        val raw = driver.getString(KEY_UPDATES) ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveBookUpdates(updates: List<BookUpdateItem>) {
        driver.putString(KEY_UPDATES, json.encodeToString(updates))
    }

    fun recordBookUpdate(item: BookUpdateItem) {
        val current = getBookUpdates().toMutableList()
        val idx = current.indexOfFirst { it.content.url == item.content.url }
        if (idx >= 0) {
            current[idx] = item
        } else {
            current.add(0, item)
        }
        saveBookUpdates(current)
    }

    fun markUpdatesSeen() {
        val current = getBookUpdates().map { it.copy(isSeen = true) }
        saveBookUpdates(current)
    }

    fun getUnreadUpdatesCount(): Int {
        return getBookUpdates().filterNot { it.isSeen }.sumOf { it.newChaptersCount.coerceAtLeast(1) }
    }
}