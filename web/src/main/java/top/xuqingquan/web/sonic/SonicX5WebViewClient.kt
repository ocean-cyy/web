package top.xuqingquan.web.sonic

import android.os.Build
import android.support.annotation.RequiresApi
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.WebView
import com.tencent.sonic.sdk.SonicSession
import top.xuqingquan.web.x5.MiddlewareWebClientBase

/**
 * Create by 许清泉 on 2020/8/22 21:22
 */
class SonicX5WebViewClient(private val sonicSession: SonicSession?) : MiddlewareWebClientBase() {

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        sonicSession?.sessionClient?.pageFinish(url)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        return shouldInterceptRequest(view, request?.url?.toString());
    }

    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        if (sonicSession != null && sonicSession.sessionClient != null) {
            return sonicSession.sessionClient.requestResource(url) as? WebResourceResponse?
        }
        return super.shouldInterceptRequest(view, url)
    }

}