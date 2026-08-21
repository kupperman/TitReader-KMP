package app.tit.content.core

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout

class LoaderContext(
    val httpClient: HttpClient = HttpClient {
        install(HttpCookies)
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 8_000
            socketTimeoutMillis = 20_000
        }
    },
    private val challengeSolver: ChallengeSolver? = null
) {
    private val defaultUserAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    @Volatile private var challengeSession: ChallengeSession? = null

    suspend fun getHtml(url: String, referer: String? = null, userAgent: String? = null): String {
        return withTimeout(45_000L) {
            var lastError: Throwable? = null
            for (attempt in 0 until 2) {
                try {
                    println("TIT_FETCH start attempt=$attempt url=$url cookie=${challengeSession != null}")
                    val activeSession = challengeSession
                    val requestUserAgent = activeSession?.userAgent ?: userAgent ?: defaultUserAgent
                    val response = httpClient.get(url) {
                        header("User-Agent", requestUserAgent)
                        activeSession?.cookieHeader?.takeIf { it.isNotBlank() }?.let { header("Cookie", it) }
                        header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                        header("Cache-Control", "no-cache")
                        if (referer != null) header("Referer", referer)
                    }
                    val html = response.bodyAsText()
                    println("TIT_FETCH response status=${response.status.value} bytes=${html.length} cf=${response.headers["cf-mitigated"]} url=$url")
                    val challengeResponse = response.status.value == 403 || (response.status.value == 503 && response.headers["cf-mitigated"]?.contains("challenge", ignoreCase = true) == true)
                    if (challengeResponse && challengeSolver != null && challengeSession == null) {
                        println("TIT_FETCH solver_start url=$url")
                        challengeSession = challengeSolver.solve(url)
                        println("TIT_FETCH solver_done cookie=${challengeSession != null} url=$url")
                        if (challengeSession != null) continue
                    }
                    if (!response.status.isSuccess()) error("HTTP ${response.status.value} for $url")
                    return@withTimeout html
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    lastError = e
                    if (attempt == 0) kotlinx.coroutines.yield()
                }
            }
            throw lastError ?: IllegalStateException("Unable to load $url")
        }
    }

    suspend fun parseHtml(url: String, referer: String? = null): Document {
        val html = getHtml(url, referer)
        return Ksoup.parse(html, url)
    }

    suspend fun parseHtmlDirectThenMirrors(url: String, mirrors: List<String>): Pair<Document, String> {
        println("TIT_CHAPTER direct_start url=$url")
        return try {
            val result = withTimeout(20_000L) { parseHtml(url) to url }
            println("TIT_CHAPTER direct_success url=$url")
            result
        } catch (cancelled: CancellationException) {
            println("TIT_CHAPTER direct_cancel type=${cancelled::class.simpleName} url=$url")
            throw cancelled
        } catch (directError: Throwable) {
            println("TIT_CHAPTER direct_fail type=${directError::class.simpleName} message=${directError.message} url=$url")
            try {
                parseHtmlWithMirrors(url, mirrors)
            } catch (fallbackError: Throwable) {
                fallbackError.addSuppressed(directError)
                throw fallbackError
            }
        }
    }

    suspend fun parseHtmlWithMirrors(url: String, mirrors: List<String>): Pair<Document, String> {
        val suffix = url.substringAfter("://", "").substringAfter('/', "").let { "/$it" }
        return parseHtmlRace(mirrors, suffix)
    }

    fun isBlockedHtml(html: String): Boolean {
        val sample = html.take(12_000).lowercase()
        val challengeTitle = sample.contains("<title>just a moment") ||
            sample.contains("<title>attention required") || sample.contains("cf-chl-")
        val challengeBody = sample.contains("checking your browser")
        return challengeTitle || challengeBody
    }
    /**
     * Chạy song song các mirror (True Race).
     * Nguồn nào trả về HTML hợp lệ trước sẽ thắng ngay lập tức.
     * Nguồn bị chặn/chết không bao giờ làm huỷ các nguồn khác (nhờ supervisorScope).
     * Bắt buộc rethrow CancellationException để đảm bảo structured concurrency.
     */
    suspend fun parseHtmlRace(mirrors: List<String>, path: String): Pair<Document, String> = supervisorScope {
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        val channel = Channel<Pair<Document, String>>(Channel.BUFFERED)

        val jobs = mirrors.map { mirror ->
            launch {
                try {
                    val fullUrl = "$mirror$cleanPath"
                    val html = getHtml(fullUrl, "$mirror/")
                    if (html.length >= 300 && !isBlockedHtml(html)) {
                        val doc = Ksoup.parse(html, fullUrl)
                        channel.send(doc to mirror)
                    }
                } catch (e: CancellationException) {
                    throw e // BẮT BUỘC rethrow để coroutine huỷ đúng chuẩn
                } catch (_: Throwable) {
                    // Mirror này gặp lỗi mạng/parse, bỏ qua để các mirror khác tiếp tục đua
                }
            }
        }

        try {
            withTimeout(50_000L) {
                channel.receive()
            }
        } finally {
            // Hủy các mirror chậm hơn sau khi đã có người chiến thắng
            jobs.forEach { it.cancel() }
            channel.close()
        }
    }
}
