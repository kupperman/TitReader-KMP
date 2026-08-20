package app.tit.reader.novel.android.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.tit.content.core.model.ContentType
import app.tit.reader.novel.android.data.AndroidSharedPreferencesDriver
import app.tit.reader.novel.android.util.NotificationHelper
import app.tit.shared.repository.AggregatorRepository
import app.tit.shared.storage.LibraryStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChapterUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val storage = LibraryStorage(AndroidSharedPreferencesDriver(context))
            val repository = AggregatorRepository(storage = storage)

            val books = repository.getLibraryBooks()
            var notifId = 1000

            books.forEach { book ->
                try {
                    val details = if (book.content.type == ContentType.NOVEL) {
                        repository.getNovelDetails(book.content.sourceId, book.content.url)
                    } else {
                        repository.getMangaDetails(book.content.sourceId, book.content.url)
                    }

                    val latestChap = details.chapters.firstOrNull()?.title 
                        ?: details.content.latestChapter

                    val prevTotal = book.totalChapters
                    val currentTotal = details.chapters.size

                    // Phát hiện chương mới nếu số chương tăng hoặc tiêu đề chương mới khác
                    if (currentTotal > prevTotal && prevTotal > 0 && latestChap != null) {
                        NotificationHelper.showNewChapterNotification(
                            context = context,
                            notificationId = notifId++,
                            bookTitle = book.content.title,
                            newChapterTitle = latestChap,
                            bookUrl = book.content.url
                        )
                    }

                    // Cập nhật lại totalChapters và latestChapter vào storage
                    storage.saveBookToLibrary(
                        book.copy(
                            totalChapters = currentTotal,
                            content = book.content.copy(latestChapter = latestChap)
                        )
                    )
                } catch (_: Exception) {
                    // Bỏ qua lỗi kết nối của từng cuốn để tiếp tục quét cuốn khác
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}