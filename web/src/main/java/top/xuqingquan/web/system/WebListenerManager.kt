package top.xuqingquan.web.system

import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

interface WebListenerManager {

    fun setWebChromeClient(webView: WebView?, webChromeClient: WebChromeClient): WebListenerManager

    fun setWebViewClient(webView: WebView?, webViewClient: WebViewClient): WebListenerManager

    fun setDownloader(webView: WebView?, downloadListener: DownloadListener?): WebListenerManager
}