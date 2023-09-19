package top.xuqingquan.web.publics

import android.view.KeyEvent
import android.webkit.WebView
import top.xuqingquan.web.nokernel.EventInterceptor
import top.xuqingquan.web.nokernel.IEventHandler

/**
 * IEventHandler 对事件的处理，主要是针对
 * 视屏状态进行了处理 ， 如果当前状态为 视频状态
 * 则先退出视频。
 */
class EventHandlerImpl private constructor(webView: WebView?, eventInterceptor: EventInterceptor?) : IEventHandler {
    private var mWebView: WebView? = webView
    private var mEventInterceptor: EventInterceptor? = eventInterceptor

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            back()
        } else false
    }

    override fun back(): Boolean {
        if (this.mEventInterceptor != null && this.mEventInterceptor!!.event()) {
            return true
        }
        if (mWebView != null && mWebView!!.canGoBack()) {
            mWebView!!.goBack()
            return true
        }
        return false
    }

    companion object {

        @JvmStatic
        fun getInstance(view: WebView?, eventInterceptor: EventInterceptor?): EventHandlerImpl {
            return EventHandlerImpl(view, eventInterceptor)
        }
    }

}
