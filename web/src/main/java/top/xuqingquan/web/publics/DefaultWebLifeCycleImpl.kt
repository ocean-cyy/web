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
        if (WebConfig.isTbsEnable()) {
            mX5WebView?.apply {
                onResume()
                resumeTimers()
                //可以激活播放器的内容
                x5WebViewExtension?.active()
            }
        } else {
            mWebView?.apply {
                onResume()
                resumeTimers()
            }
        }
    }

    override fun onPause() {
        if (WebConfig.isTbsEnable()) {
            mX5WebView?.apply {
                onPause()
                pauseTimers()
                //可以暂停播放器的内容
                x5WebViewExtension?.deactive()
            }
        } else {
            mWebView?.apply {
                onPause()
                pauseTimers()
            }
        }
    }

    override fun onDestroy() {
        if (WebConfig.isTbsEnable()) {
            this.mX5WebView?.loadData("", "text/html", "utf-8")
            this.mX5WebView?.destroy()
            AgentWebUtils.clearWebView(this.mX5WebView)
        } else {
            this.mWebView?.loadData("", "text/html", "utf-8")
            this.mWebView?.destroy()
            AgentWebUtils.clearWebView(this.mWebView)
        }
    }
}
