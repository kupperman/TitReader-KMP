package app.tit.shared.model

import app.tit.content.core.model.Content
import app.tit.content.core.model.SourceInfo

enum class SourceStatus {
    IDLE,
    LOADING,
    DONE,
    NO_RESULTS,
    NO_RELEVANT,
    TIMED_OUT,
    FAILED,
    SKIPPED
}

sealed class SourceSearchResult {
    abstract val source: SourceInfo

    data class Success(override val source: SourceInfo, val items: List<Content>) : SourceSearchResult()
    data class NoRelevantResults(
        override val source: SourceInfo,
        val discardedCount: Int
    ) : SourceSearchResult()
    data class Failed(override val source: SourceInfo, val error: Throwable?) : SourceSearchResult()
    data class TimedOut(override val source: SourceInfo) : SourceSearchResult()
    data class Skipped(override val source: SourceInfo) : SourceSearchResult()
}
