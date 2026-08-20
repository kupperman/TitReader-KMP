package app.tit.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val appName: String = "TitReader-KMP",
    val exportedAt: Long = 0L,
    val library: List<LibraryBook> = emptyList(),
    val history: List<ReadingHistoryItem> = emptyList(),
    val settings: ReaderSettings = ReaderSettings(),
    val readChapterUrls: List<String> = emptyList()
)