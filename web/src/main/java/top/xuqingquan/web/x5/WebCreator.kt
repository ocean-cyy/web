package top.xuqingquan.web.x5

import android.widget.FrameLayout
import com.tencent.smtt.sdk.WebView
import top.xuqingquan.web.nokernel.BaseIndicatorSpec
import top.xuqingquan.web.nokernel.IWebIndicator

interface WebCreator<T : BaseIndicatorSpec> : IWebIndicator<T> {

    fun getWebView(): WebView?

    fun getWebParentLayout(): FrameLayout

    fun create(): WebCreator<T>

    fun getWebViewType(): Int
}
