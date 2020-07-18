package top.xuqingquan.web.x5

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.Log
import android.util.Pair
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.tencent.smtt.sdk.WebBackForwardList
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import top.xuqingquan.web.R
import top.xuqingquan.utils.Timber


open class AgentWebView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LollipopFixedWebView(context, attrs) {
    private val mFixedOnReceivedTitle = FixedOnReceivedTitle()

    init {
        id = R.id.scaffold_webview_id
    }

    override fun setWebChromeClient(client: WebChromeClient) {
        val mAgentWebChrome = AgentWebChrome(this)
        mAgentWebChrome.delegate = client
        mFixedOnReceivedTitle.setWebChromeClient(client)
        super.setWebChromeClient(mAgentWebChrome)
    }

    override fun setWebViewClient(client: WebViewClient) {
        val mAgentWebClient = AgentWebClient(this)
        mAgentWebClient.delegate = client
        super.setWebViewClient(mAgentWebClient)
    }

    override fun destroy() {
        visibility = View.GONE
        removeAllViewsInLayout()
        fixedStillAttached()
        super.destroy()
    }

    override fun setOverScrollMode(mode: Int) {
        try {
            super.setOverScrollMode(mode)
        } catch (e: Throwable) {
            val pair = isWebViewPackageException(e)
            if (pair.first) {
                Toast.makeText(context, pair.second, Toast.LENGTH_SHORT).show()
                destroy()
            } else {
                throw e
            }
        }

    }

    class AgentWebClient internal constructor(private val mAgentWebView: AgentWebView) : MiddlewareWebClientBase() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            mAgentWebView.mFixedOnReceivedTitle.onPageStarted()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            mAgentWebView.mFixedOnReceivedTitle.onPageFinished(view)
            Timber.d("onPageFinished.url = " + view?.url)
        }


    }

    class AgentWebChrome internal constructor(private val mAgentWebView: AgentWebView) : MiddlewareWebChromeBase() {

        override fun onReceivedTitle(view: WebView, title: String) {
            this.mAgentWebView.mFixedOnReceivedTitle.onReceivedTitle()
            super.onReceivedTitle(view, title)
        }
    }

    /**
     * 解决部分手机webView返回时不触发onReceivedTitle的问题（如：三星SM-G9008V 4.4.2）；
     */
    private class FixedOnReceivedTitle {
        private var mWebChromeClient: WebChromeClient? = null
        private var mIsOnReceivedTitle: Boolean = false

        fun setWebChromeClient(webChromeClient: WebChromeClient) {
            mWebChromeClient = webChromeClient
        }

        internal fun onPageStarted() {
            mIsOnReceivedTitle = false
        }

        internal fun onPageFinished(view: WebView?) {
            if (!mIsOnReceivedTitle && mWebChromeClient != null) {
                var list: WebBackForwardList? = null
                try {
                    list = view?.copyBackForwardList()
                } catch (e: Throwable) {
                    Timber.e(e)
                }

                if (list != null
                    && list.size > 0
                    && list.currentIndex >= 0
                    && list.getItemAtIndex(list.currentIndex) != null
                ) {
                    val previousTitle = list.getItemAtIndex(list.currentIndex).title
                    mWebChromeClient!!.onReceivedTitle(view, previousTitle)
                }
            }
        }

        internal fun onReceivedTitle() {
            mIsOnReceivedTitle = true
        }
    }

    // Activity在onDestory时调用webView的destroy，可以停止播放页面中的音频
    private fun fixedStillAttached() {
        // java.lang.Throwable: Error: WebView.destroy() called while still attached!
        // at android.webkit.WebViewClassic.destroy(WebViewClassic.java:4142)
        // at android.webkit.WebView.destroy(WebView.java:707)
        val parent = parent
        if (parent is ViewGroup) { // 由于自定义webView构建时传入了该Activity的context对象，因此需要先从父容器中移除webView，然后再销毁webView；
            val mWebViewContainer = getParent() as ViewGroup
            mWebViewContainer.removeAllViewsInLayout()
        }
    }

    companion object {

        @JvmStatic
        fun isWebViewPackageException(e: Throwable): Pair<Boolean, String> {
            val messageCause = if (e.cause == null) e.toString() else e.cause.toString()
            val trace = Log.getStackTraceString(e)
            return if (trace.contains("android.content.pm.PackageManager\$NameNotFoundException")
                || trace.contains("java.lang.RuntimeException: Cannot load WebView")
                || trace.contains("android.webkit.WebViewFactory\$MissingWebViewPackageException: Failed to load WebView provider: No WebView installed")
            ) {
                Pair(true, "WebView load failed, $messageCause")
            } else Pair(false, messageCause)
        }
    }
}