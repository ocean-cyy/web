package top.xuqingquan.web.publics

import android.webkit.WebView
import top.xuqingquan.web.nokernel.WebLifeCycle

class DefaultWebLifeCycleImpl(webView: WebView?) : WebLifeCycle {
    private var mWebView: WebView? = webView


    override fun onResume() {
        mWebView?.apply {
            onResume()
            resumeTimers()
        }
    }

    override fun onPause() {
        mWebView?.apply {
            onPause()
            pauseTimers()
        }
    }

    override fun onDestroy() {
        this.mWebView?.loadData("", "text/html", "utf-8")
        this.mWebView?.destroy()
        AgentWebUtils.clearWebView(this.mWebView)
    }
}
