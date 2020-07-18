package top.xuqingquan.web.x5

import com.tencent.smtt.sdk.DownloadListener
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient

interface WebListenerManager {

    fun setWebChromeClient(webView: WebView?, webChromeClient: WebChromeClient): WebListenerManager

    fun setWebViewClient(webView: WebView?, webViewClient: WebViewClient): WebListenerManager

    fun setDownloader(webView: WebView?, downloadListener: DownloadListener?): WebListenerManager
}