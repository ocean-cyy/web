package top.xuqingquan.web.publics

import android.os.Handler
import android.os.Looper
import android.webkit.ValueCallback
import android.webkit.WebView
import com.tencent.smtt.sdk.ValueCallback as X5ValueCallback
import com.tencent.smtt.sdk.WebView as X5WebView

class JsAccessEntraceImpl : BaseJsAccessEntrace {

    private val mHandler = Handler(Looper.getMainLooper())

    private constructor(webView: WebView?) : super(webView)

    private constructor(webView: X5WebView?) : super(webView)

    private fun safeCallJs(s: String?, valueCallback: ValueCallback<String>?) {
        mHandler.post { callJs(s, valueCallback) }
    }

    private fun safeCallJs(s: String?, valueCallback: X5ValueCallback<String>?) {
        mHandler.post { callJs(s, valueCallback) }
    }

    override fun callJs(js: String?, callback: ValueCallback<String>?) {
        if (Thread.currentThread() !== Looper.getMainLooper().thread) {
            safeCallJs(js, callback)
            return
        }
        super.callJs(js, callback)
    }

    override fun callJs(js: String?, callback: X5ValueCallback<String>?) {
        if (Thread.currentThread() !== Looper.getMainLooper().thread) {
            safeCallJs(js, callback)
            return
        }
        super.callJs(js, callback)
    }

    companion object {

        @JvmStatic
        fun getInstance(webView: WebView?): JsAccessEntraceImpl {
            return JsAccessEntraceImpl(webView)
        }

        @JvmStatic
        fun getInstance(webView: X5WebView?): JsAccessEntraceImpl {
            return JsAccessEntraceImpl(webView)
        }
    }


}
