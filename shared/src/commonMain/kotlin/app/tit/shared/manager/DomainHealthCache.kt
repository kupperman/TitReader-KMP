package app.tit.shared.manager

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class DomainHealthCache {
    private val deadUntilMarks = mutableMapOf<String, TimeMark>()
    private val mutex = Mutex()
    private val cooldown = 5.minutes

    suspend fun markDead(sourceId: String) {
        mutex.withLock {
            deadUntilMarks[sourceId] = TimeSource.Monotonic.markNow() + cooldown
        }
    }

    suspend fun markHealthy(sourceId: String) {
        mutex.withLock {
            deadUntilMarks.remove(sourceId)
        }
    }

    suspend fun isSkippable(sourceId: String): Boolean {
        mutex.withLock {
            val mark = deadUntilMarks[sourceId] ?: return false
            if (mark.hasPassedNow()) {
                deadUntilMarks.remove(sourceId)
                return false
            }
            return true
        }
    }
}
