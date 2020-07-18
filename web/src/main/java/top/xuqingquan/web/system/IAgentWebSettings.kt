package top.xuqingquan.web.system

import android.webkit.WebSettings
import android.webkit.WebView

interface IAgentWebSettings<T : WebSettings?> {

    fun getWebSettings(): T?

    fun toSetting(webView: WebView?): IAgentWebSettings<*>
}