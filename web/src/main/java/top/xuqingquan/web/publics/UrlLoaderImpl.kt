package top.xuqingquan.web.publics

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import top.xuqingquan.web.nokernel.HttpHeaders
import top.xuqingquan.web.nokernel.IUrlLoader
import top.xuqingquan.web.nokernel.WebConfig
import top.xuqingquan.web.nokernel.WebUtils
import top.xuqingquan.utils.Timber

import com.tencent.smtt.sdk.WebView as X5WebView

class UrlLoaderImpl : IUrlLoader {
    private var mHandler: Handler? = null
    private var mWebView: WebView? = null
    private var mx5WebView: X5WebView? = null
    private var mHttpHeaders: HttpHeaders? = null

    constructor(webView: WebView?, httpHeaders: HttpHeaders?) {
        this.mWebView = webView
        if (this.mWebView == null) {
            throw NullPointerException("webview cannot be null .")
        }
        this.mHttpHeaders = httpHeaders
        if (this.mHttpHeaders == null) {
            this.mHttpHeaders = HttpHeaders.create()
        }
        mHandler = Handler(Looper.getMainLooper())
    }

    constructor(webView: X5WebView?, httpHeaders: HttpHeaders?) {
        this.mx5WebView = webView
        if (this.mx5WebView == null) {
            throw NullPointerException("webview cannot be null .")
        }
        this.mHttpHeaders = httpHeaders
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
                WebUtils.runInUiThread(Runnable {
                    loadUrl(url, headers)
                })
            }
            Timber.i("loadUrl:$url headers:$headers")
            if (WebConfig.enableTbs()) {
                if (headers.isNullOrEmpty()) {
                    this.mx5WebView!!.loadUrl(url)
                } else {
                    this.mx5WebView!!.loadUrl(url, headers)
                }
            } else {
                if (headers.isNullOrEmpty()) {
                    this.mWebView!!.loadUrl(url)
                } else {
                    this.mWebView!!.loadUrl(url, headers)
                }
            }
        } catch (e: Throwable) {
        }
    }

    override fun reload() {
        if (!WebUtils.isUIThread()) {
            mHandler!!.post { this.reload() }
            return
        }
        if (WebConfig.enableTbs()) {
            this.mx5WebView!!.reload()
        } else {
            this.mWebView!!.reload()
        }
    }

    override fun loadData(data: String, mimeType: String, encoding: String) {
        if (!WebUtils.isUIThread()) {
            mHandler!!.post { loadData(data, mimeType, encoding) }
            return
        }
        if (WebConfig.enableTbs()) {
            this.mx5WebView!!.loadData(data, mimeType, encoding)
        } else {
            this.mWebView!!.loadData(data, mimeType, encoding)
        }
    }

    override fun stopLoading() {
        if (!WebUtils.isUIThread()) {
            mHandler!!.post { this.stopLoading() }
            return
        }
        if (WebConfig.enableTbs()) {
            this.mx5WebView!!.stopLoading()
        } else {
            this.mWebView!!.stopLoading()
        }
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
        if (WebConfig.enableTbs()) {
            this.mx5WebView!!.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, failUrl)
        } else {
            this.mWebView!!.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, failUrl)
        }
    }

    override fun postUrl(url: String, params: ByteArray) {
        if (!WebUtils.isUIThread()) {
            mHandler!!.post { postUrl(url, params) }
            return
        }
        if (WebConfig.enableTbs()) {
            this.mx5WebView!!.postUrl(url, params)
        } else {
            this.mWebView!!.postUrl(url, params)
        }
    }

    override fun getHttpHeaders(): HttpHeaders {
        if (this.mHttpHeaders == null) {
            this.mHttpHeaders = HttpHeaders.create()
        }
        return this.mHttpHeaders!!
    }

}
