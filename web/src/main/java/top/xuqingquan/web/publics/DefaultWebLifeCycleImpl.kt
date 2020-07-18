package top.xuqingquan.web.publics

import android.webkit.WebView
import top.xuqingquan.web.nokernel.WebConfig
import top.xuqingquan.web.nokernel.WebLifeCycle

import com.tencent.smtt.sdk.WebView as X5WebView

class DefaultWebLifeCycleImpl : WebLifeCycle {
    private var mWebView: WebView? = null
    private var mX5WebView: X5WebView? = null

    constructor(webView: WebView?) {
        this.mWebView = webView
    }

    constructor(webView: X5WebView?) {
        this.mX5WebView = webView
    }

    override fun onResume() {
        if (WebConfig.enableTbs()) {
            mX5WebView?.let {
                it.onResume()
                it.resumeTimers()
            }
        } else {
            mWebView?.let {
                it.onResume()
                it.resumeTimers()
            }
        }
    }

    override fun onPause() {
        if (WebConfig.enableTbs()) {
            mX5WebView?.let {
                it.onPause()
                it.pauseTimers()
            }
        } else {
            mWebView?.let {
                it.onPause()
                it.pauseTimers()
            }
        }
    }

    override fun onDestroy() {
        if (WebConfig.enableTbs()) {
            this.mX5WebView?.resumeTimers()
            AgentWebUtils.clearWebView(this.mX5WebView)
        } else {
            this.mWebView?.resumeTimers()
            AgentWebUtils.clearWebView(this.mWebView)
        }
    }
}
