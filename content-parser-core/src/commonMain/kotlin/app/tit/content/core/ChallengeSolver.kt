package app.tit.content.core

/** Solves a browser challenge and returns the browser session to reuse in HTTP requests. */
data class ChallengeSession(val cookieHeader: String, val userAgent: String)

interface ChallengeSolver {
    suspend fun solve(url: String): ChallengeSession?
}
