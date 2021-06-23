package top.xuqingquan.web.publics

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import top.xuqingquan.utils.Timber
import top.xuqingquan.web.nokernel.HttpHeaders
import top.xuqingquan.web.nokernel.IUrlLoader
import top.xuqingquan.web.nokernel.WebUtils

class UrlLoaderImpl(webView: WebView?, httpHeaders: HttpHeaders?) : IUrlLoader {
    private var mHandler: Handler? = null
    private var mWebView: WebView? = webView
    private var mHttpHeaders: HttpHeaders? = httpHeaders

    init {
        if (this.mWebView == null) {
            throw NullPointerException("webview cannot be null .")
        }
        if (this.mHttpHeaders == null) {
            this.mHttpHeaders = HttpHeaders.create()
        }
        mHandler = Handler(Looper.getMainLooper())
    }

    override fun loadUrl(url: String) {
        this.loadUrl(url, this.mHttpHeaders?.getHeaders(url)!!)
    }

    override fun loadUrl(url: String, headers: Map<String, String>?) {
        try {
            if (!WebUtils.isUIThread()) {
                WebUtils.runInUiThread {
                    loadUrl(url, headers)
                }
            }
            Timber.i("loadUrl:$url headers:$headers")
            if (headers.isNullOrEmpty()) {
                this.mWebView!!.loadUrl(url)
            } else {
                this.mWebView!!.loadUrl(url, headers)
            }
        } catch (e: Throwable) {
        }
    }

    override fun reload() {
        if (!WebUtils.isUIThread()) {
            mHandler!!.post { this.reload() }
            return
        }
        this.mWebView!!.reload()
    }

    override fun loadData(data: String, mimeType: String, encoding: String) {
        if (!WebUtils.isUIThread()) {
            mHandler!!.post { loadData(data, mimeType, encoding) }
            return
        }
        this.mWebView!!.loadData(data, mimeType, encoding)
    }

    override fun stopLoading() {
        if (!WebUtils.isUIThread()) {
            mHandler!!.post { this.stopLoading() }
            return
        }
        this.mWebView!!.stopLoading()
    }

    override fun loadDataWithBaseURL(
        baseUrl: String?,
        data: String,
        mimeType: String,
        encoding: String,
        failUrl: String?
    ) {
        if (!WebUtils.isUIThread()) {
            mHandler!!.post { loadDataWithBaseURL(baseUrl, data, mimeType, encoding, failUrl) }
            return
        }
        this.mWebView!!.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, failUrl)
    }

    override fun postUrl(url: String, params: ByteArray) {
        if (!WebUtils.isUIThread()) {
            mHandler!!.post { postUrl(url, params) }
            return
        }
        this.mWebView!!.postUrl(url, params)
    }

    override fun getHttpHeaders(): HttpHeaders {
        if (this.mHttpHeaders == null) {
            this.mHttpHeaders = HttpHeaders.create()
        }
        return this.mHttpHeaders!!
    }

}
