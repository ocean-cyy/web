package top.xuqingquan.web.system

import android.webkit.WebView
import android.widget.FrameLayout
import top.xuqingquan.web.nokernel.BaseIndicatorSpec
import top.xuqingquan.web.nokernel.IWebIndicator

interface WebCreator<T : BaseIndicatorSpec> : IWebIndicator<T> {

    fun getWebView(): WebView?

    fun getWebParentLayout(): FrameLayout

    fun create(): WebCreator<T>

    fun getWebViewType(): Int
}
