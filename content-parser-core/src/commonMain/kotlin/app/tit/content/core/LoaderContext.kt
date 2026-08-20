package app.tit.content.core

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout

class LoaderContext(
    val httpClient: HttpClient = HttpClient()
) {
    private val defaultUserAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    suspend fun getHtml(url: String, referer: String? = null, userAgent: String? = null): String {
        return withTimeout(12_000L) {
            httpClient.get(url) {
                header("User-Agent", userAgent ?: defaultUserAgent)
                header("Accept", "text/html,application/xhtml+xml,application/xml,application/json;q=0.9,*/*;q=0.8")
                header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                if (referer != null) {
                    header("Referer", referer)
                }
            }.bodyAsText()
        }
    }

    suspend fun parseHtml(url: String, referer: String? = null): Document {
        val html = getHtml(url, referer)
        return Ksoup.parse(html, url)
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
                    if (html.length >= 300 && !html.contains("Just a moment") && !html.contains("Cloudflare")) {
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
            withTimeout(8_000L) {
                channel.receive()
            }
        } finally {
            // Hủy các mirror chậm hơn sau khi đã có người chiến thắng
            jobs.forEach { it.cancel() }
            channel.close()
        }
    }
}
