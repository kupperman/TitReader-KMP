package app.tit.content.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Bộ test cho parseHtmlRace — thay thế cho test tay bằng hosts file.
 *
 * Nguyên tắc: mỗi mirror giả lập 1 hành vi khác nhau (nhanh, chậm, lỗi, Cloudflare)
 * thông qua MockEngine, không đụng mạng thật. runTest cho phép "tua nhanh" thời gian
 * ảo, nên test timeout 8 giây chạy xong trong vài mili-giây thực tế.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ParseHtmlRaceTest {

    private val validHtml = "<html><body>" + "x".repeat(300) + "</body></html>"
    private val cloudflareHtml = "<html><body>Just a moment...</body></html>"

    private sealed class MockBehavior(val delayMs: Long) {
        class Success(delayMs: Long) : MockBehavior(delayMs)
        class Cloudflare(delayMs: Long) : MockBehavior(delayMs)
        class HttpError(delayMs: Long) : MockBehavior(delayMs)
        object NeverResponds : MockBehavior(0L)
    }

    private fun TestScope.contextWith(behaviors: Map<String, MockBehavior>): LoaderContext {
        val testDispatcher = coroutineContext[CoroutineDispatcher] ?: Dispatchers.Unconfined
        val client = HttpClient(MockEngine) {
            engine {
                dispatcher = testDispatcher
                addHandler { request ->
                    val mirror = behaviors.keys.first { request.url.toString().startsWith(it) }
                    val behavior = behaviors.getValue(mirror)

                    if (behavior.delayMs > 0) {
                        delay(behavior.delayMs)
                    }

                    when (behavior) {
                        is MockBehavior.Success -> respond(
                            content = validHtml,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "text/html"),
                        )
                        is MockBehavior.Cloudflare -> respond(
                            content = cloudflareHtml,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "text/html"),
                        )
                        is MockBehavior.HttpError -> respond(
                            content = "",
                            status = HttpStatusCode.ServiceUnavailable,
                        )
                        is MockBehavior.NeverResponds -> {
                            delay(60_000L)
                            respond(content = validHtml, status = HttpStatusCode.OK)
                        }
                    }
                }
            }
        }
        return LoaderContext(client)
    }

    @Test
    fun mirror_dau_timeout_mirror_hai_thanh_cong() = runTest {
        val context = contextWith(
            mapOf(
                "https://truyenfull.live" to MockBehavior.NeverResponds,          // mirror chết hẳn
                "https://truyenfull.vn" to MockBehavior.Success(delayMs = 200),   // mirror sống, hơi chậm
            )
        )

        val (_, winningMirror) = context.parseHtmlRace(
            mirrors = listOf("https://truyenfull.live", "https://truyenfull.vn"),
            path = "/danh-sach/truyen-moi/",
        )

        // Xác nhận: kết quả PHẢI đến từ mirror thứ 2, không phải mirror chết
        assertEquals("https://truyenfull.vn", winningMirror)
    }

    @Test
    fun mirror_nhanh_hon_phai_thang_du_dung_sau() = runTest {
        val context = contextWith(
            mapOf(
                "https://truyenfull.live" to MockBehavior.Success(delayMs = 500), // đứng đầu nhưng chậm
                "https://truyenfull.vn" to MockBehavior.Success(delayMs = 50),    // đứng sau nhưng nhanh hơn
            )
        )

        val (_, winningMirror) = context.parseHtmlRace(
            mirrors = listOf("https://truyenfull.live", "https://truyenfull.vn"),
            path = "/danh-sach/truyen-moi/",
        )

        // True race nghĩa là AI NHANH HƠN THẮNG, không phải cứ đứng đầu danh sách là thắng
        assertEquals("https://truyenfull.vn", winningMirror)
    }

    @Test
    fun trang_Cloudflare_challenge_phai_bi_loai() = runTest {
        val context = contextWith(
            mapOf(
                "https://truyenfull.live" to MockBehavior.Cloudflare(delayMs = 50),  // bị chặn Cloudflare
                "https://truyenfull.vn" to MockBehavior.Success(delayMs = 300),      // sống thật, chậm hơn
            )
        )

        val (_, winningMirror) = context.parseHtmlRace(
            mirrors = listOf("https://truyenfull.live", "https://truyenfull.vn"),
            path = "/danh-sach/truyen-moi/",
        )

        // Dù truyenfull.live trả lời NHANH HƠN, nội dung là trang Cloudflare
        // nên phải bị loại, kết quả đúng phải đến từ mirror còn lại
        assertEquals("https://truyenfull.vn", winningMirror)
    }

    @Test
    fun tat_ca_mirror_deu_chet_phai_nem_loi_sau_8s() = runTest {
        val context = contextWith(
            mapOf(
                "https://truyenfull.live" to MockBehavior.NeverResponds,
                "https://truyenfull.vn" to MockBehavior.HttpError(delayMs = 100),
            )
        )

        val start = currentTime
        assertFailsWith<Exception> {
            context.parseHtmlRace(
                mirrors = listOf("https://truyenfull.live", "https://truyenfull.vn"),
                path = "/danh-sach/truyen-moi/",
            )
        }
        val elapsed = currentTime - start

        // Xác nhận timeout đúng ~8000ms (virtual time), không bị treo vô hạn
        assertTrue(elapsed in 7_900L..8_100L, "Thời gian timeout thực tế: ${elapsed}ms")
    }

    @Test
    fun mirror_bi_huy_khong_duoc_nuot_CancellationException() = runTest {
        // Đếm số lần request THỰC SỰ ĐI TỚI CUỐI (tức là hoàn tất delay và
        // chạm tới dòng respond()) — đây là side-effect khách quan, không
        // dựa vào 1 biến flag tự gán như bản cũ.
        val completedRequests = mutableListOf<String>()
        val testDispatcher = coroutineContext[CoroutineDispatcher] ?: Dispatchers.Unconfined

        val client = HttpClient(MockEngine) {
            engine {
                dispatcher = testDispatcher
                addHandler { request ->
                    val url = request.url.toString()
                    when {
                        url.startsWith("https://truyenfull.live") -> {
                            delay(50)
                            completedRequests.add("live")
                            respond(content = validHtml, status = HttpStatusCode.OK)
                        }
                        url.startsWith("https://truyenfull.vn") -> {
                            // Mirror này PHẢI bị cancel giữa chừng, không được chạy hết 5000ms
                            delay(5_000)
                            completedRequests.add("vn") // nếu dòng này chạy được, nghĩa là KHÔNG bị huỷ đúng
                            respond(content = validHtml, status = HttpStatusCode.OK)
                        }
                        else -> error("Unexpected mirror: $url")
                    }
                }
            }
        }
        val context = LoaderContext(client)

        context.parseHtmlRace(
            mirrors = listOf("https://truyenfull.live", "https://truyenfull.vn"),
            path = "/danh-sach/truyen-moi/",
        )

        // Tua thời gian ảo vượt qua mốc 5000ms mà mirror chậm cần để "hoàn tất"
        // nếu nó KHÔNG bị huỷ đúng chuẩn
        advanceTimeBy(6_000)

        // Assertion thật: "vn" KHÔNG được xuất hiện trong danh sách hoàn tất,
        // vì job của nó phải bị cancel() ngay sau khi "live" thắng ở mốc 50ms —
        // nếu CancellationException bị nuốt, job sẽ chạy tiếp và "vn" sẽ xuất hiện ở đây
        assertTrue(
            "vn" !in completedRequests,
            "Mirror chậm KHÔNG bị huỷ đúng chuẩn — CancellationException có thể đang bị nuốt in khối catch(Throwable). completedRequests=$completedRequests",
        )
        assertTrue("live" in completedRequests)
    }
}
