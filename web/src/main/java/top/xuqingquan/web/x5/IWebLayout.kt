package top.xuqingquan.web.x5

import android.view.ViewGroup
import com.tencent.smtt.sdk.WebView

interface IWebLayout<T : WebView, V : ViewGroup> {

    /**
     * @return WebView 的父控件
     */
    val layout: V

    /**
     * @return 返回 WebView  或 WebView 的子View ，返回null AgentWeb 内部会创建适当 WebView
     */
    val webView: T?
}
