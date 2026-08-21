package app.tit.reader.novel.android.network

import android.content.Context
import android.app.Dialog
import android.view.ViewGroup
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import app.tit.content.core.ChallengeSession
import app.tit.content.core.ChallengeSolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class CloudflareWebViewSolver(private val context: Context) : ChallengeSolver {
    override suspend fun solve(url: String): ChallengeSession? = withTimeoutOrNull(30_000L) {
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val dialog = Dialog(context)
                val webView = WebView(context)
                webView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                dialog.setContentView(webView)
                dialog.setCancelable(true)
                dialog.show()
                val settings = webView.settings
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = WebSettings.getDefaultUserAgent(context)
                val userAgent = settings.userAgentString
                val cookies = CookieManager.getInstance()
                cookies.setAcceptCookie(true)
                val handler = Handler(Looper.getMainLooper())
                var finished = false
                fun finish(session: ChallengeSession?) {
                    if (finished) return
                    finished = true
                    handler.removeCallbacksAndMessages(null)
                    dialog.dismiss()
                    webView.stopLoading()
                    webView.destroy()
                    if (continuation.isActive) continuation.resume(session)
                }
                fun poll() {
                    if (finished) return
                    val cookieHeader = cookies.getCookie(url).orEmpty()
                    if (cookieHeader.contains("cf_clearance=")) finish(ChallengeSession(cookieHeader, userAgent))
                    else handler.postDelayed(::poll, 500L)
                }
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, pageUrl: String) = poll()
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = false
                }
                continuation.invokeOnCancellation { handler.post { finish(null) } }
                webView.loadUrl(url, mapOf("Accept-Language" to "vi-VN,vi;q=0.9"))
                poll()
            }
        }
    }
}
