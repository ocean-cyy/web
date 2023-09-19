package top.xuqingquan.web.publics

import android.os.Handler
import android.os.Looper
import android.webkit.ValueCallback
import android.webkit.WebView

class JsAccessEntraceImpl private constructor(webView: WebView?) : BaseJsAccessEntrace(webView) {

    private val mHandler = Handler(Looper.getMainLooper())

    private fun safeCallJs(s: String?, valueCallback: ValueCallback<String>?) {
        mHandler.post { callJs(s, valueCallback) }
    }

    override fun callJs(js: String?, callback: ValueCallback<String>?) {
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

    }


}
