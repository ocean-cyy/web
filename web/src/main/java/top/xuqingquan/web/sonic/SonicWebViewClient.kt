package top.xuqingquan.web.sonic

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.tencent.sonic.sdk.SonicSession
import top.xuqingquan.web.system.MiddlewareWebClientBase

/**
 * Create by 许清泉 on 2020/8/22 21:22
 */
class SonicWebViewClient(private val sonicSession: SonicSession?) : MiddlewareWebClientBase() {

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        sonicSession?.sessionClient?.pageFinish(url)
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        return shouldInterceptRequest(view, request?.url?.toString())
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        if (sonicSession != null && sonicSession.sessionClient != null) {
            return sonicSession.sessionClient.requestResource(url) as? WebResourceResponse?
        }
        return super.shouldInterceptRequest(view, url)
    }

}