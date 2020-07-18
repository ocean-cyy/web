package top.xuqingquan.web.publics

import android.view.KeyEvent
import android.webkit.WebView
import top.xuqingquan.web.nokernel.EventInterceptor
import top.xuqingquan.web.nokernel.IEventHandler
import top.xuqingquan.web.nokernel.WebConfig

import com.tencent.smtt.sdk.WebView as X5WebView

/**
 * IEventHandler 对事件的处理，主要是针对
 * 视屏状态进行了处理 ， 如果当前状态为 视频状态
 * 则先退出视频。
 */
class EventHandlerImpl : IEventHandler {
    private var mWebView: WebView? = null
    private var mX5WebView: X5WebView? = null
    private var mEventInterceptor: EventInterceptor? = null

    private constructor(webView: WebView?, eventInterceptor: EventInterceptor?) {
        this.mWebView = webView
        this.mEventInterceptor = eventInterceptor
    }

    private constructor(webView: X5WebView?, eventInterceptor: EventInterceptor?) {
        this.mX5WebView = webView
        this.mEventInterceptor = eventInterceptor
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            back()
        } else false
    }

    override fun back(): Boolean {
        if (this.mEventInterceptor != null && this.mEventInterceptor!!.event()) {
            return true
        }
        if (WebConfig.enableTbs()) {
            if (mX5WebView != null && mX5WebView!!.canGoBack()) {
                mX5WebView!!.goBack()
                return true
            }
        } else {
            if (mWebView != null && mWebView!!.canGoBack()) {
                mWebView!!.goBack()
                return true
            }
        }
        return false
    }

    companion object {

        @JvmStatic
        fun getInstance(view: WebView?, eventInterceptor: EventInterceptor?): EventHandlerImpl {
            return EventHandlerImpl(view, eventInterceptor)
        }

        @JvmStatic
        fun getInstance(view: X5WebView?, eventInterceptor: EventInterceptor?): EventHandlerImpl {
            return EventHandlerImpl(view, eventInterceptor)
        }
    }

}
