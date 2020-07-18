package top.xuqingquan.web.system

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.KeyEvent
import android.webkit.*
import android.support.annotation.RequiresApi

@Suppress("DEPRECATION")
open class WebViewClientDelegate internal constructor(client: WebViewClient?) : WebViewClient() {

    open var delegate: WebViewClient? = client

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        return if (delegate != null) {
            delegate!!.shouldOverrideUrlLoading(view, url)
        } else {
            super.shouldOverrideUrlLoading(view, url)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return if (delegate != null) {
            delegate!!.shouldOverrideUrlLoading(view, request)
        } else {
            super.shouldOverrideUrlLoading(view, request)
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        if (delegate != null) {
            delegate!!.onPageStarted(view, url, favicon)
            return
        }
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        if (delegate != null) {
            delegate!!.onPageFinished(view, url)
            return
        }
        super.onPageFinished(view, url)
    }

    override fun onLoadResource(view: WebView?, url: String?) {
        if (delegate != null) {
            delegate!!.onLoadResource(view, url)
            return
        }
        super.onLoadResource(view, url)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onPageCommitVisible(view: WebView?, url: String?) {
        if (delegate != null) {
            delegate!!.onPageCommitVisible(view, url)
            return
        }
        super.onPageCommitVisible(view, url)
    }

    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        return if (delegate != null) {
            delegate!!.shouldInterceptRequest(view, url)
        } else {
            super.shouldInterceptRequest(view, url)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        return if (delegate != null) {
            delegate!!.shouldInterceptRequest(view, request)
        } else {
            super.shouldInterceptRequest(view, request)
        }
    }

    @Deprecated("")
    override fun onTooManyRedirects(view: WebView?, cancelMsg: Message?, continueMsg: Message?) {
        if (delegate != null) {
            delegate!!.onTooManyRedirects(view, cancelMsg, continueMsg)
            return
        }
        super.onTooManyRedirects(view, cancelMsg, continueMsg)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        if (delegate != null) {
            return delegate!!.onRenderProcessGone(view, detail)

        }
        return super.onRenderProcessGone(view, detail)
    }

    override fun onReceivedError(
        view: WebView?, errorCode: Int,
        description: String?, failingUrl: String?
    ) {
        if (delegate != null) {
            delegate!!.onReceivedError(view, errorCode, description, failingUrl)
            return
        }
        super.onReceivedError(view, errorCode, description, failingUrl)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        if (delegate != null) {
            delegate!!.onReceivedError(view, request, error)
            return
        }
        super.onReceivedError(view, request, error)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        if (delegate != null) {
            delegate!!.onReceivedHttpError(view, request, errorResponse)
            return
        }
        super.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
        if (delegate != null) {
            delegate!!.onFormResubmission(view, dontResend, resend)
            return
        }
        super.onFormResubmission(view, dontResend, resend)
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        if (delegate != null) {
            delegate!!.doUpdateVisitedHistory(view, url, isReload)
            return
        }
        super.doUpdateVisitedHistory(view, url, isReload)
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        if (delegate != null) {
            delegate!!.onReceivedSslError(view, handler, error)
            return
        }
        super.onReceivedSslError(view, handler, error)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
        if (delegate != null) {
            delegate!!.onReceivedClientCertRequest(view, request)
            return
        }
        super.onReceivedClientCertRequest(view, request)
    }

    override fun onReceivedHttpAuthRequest(
        view: WebView?,
        handler: HttpAuthHandler?,
        host: String,
        realm: String?
    ) {
        if (delegate != null) {
            delegate!!.onReceivedHttpAuthRequest(view, handler, host, realm)
            return
        }
        super.onReceivedHttpAuthRequest(view, handler, host, realm)
    }

    override fun shouldOverrideKeyEvent(view: WebView?, event: KeyEvent?): Boolean {
        return if (delegate != null) {
            delegate!!.shouldOverrideKeyEvent(view, event)
        } else super.shouldOverrideKeyEvent(view, event)
    }

    override fun onUnhandledKeyEvent(view: WebView?, event: KeyEvent?) {
        if (delegate != null) {
            delegate!!.onUnhandledKeyEvent(view, event)
            return
        }
        super.onUnhandledKeyEvent(view, event)
    }


    override fun onScaleChanged(view: WebView?, oldScale: Float, newScale: Float) {
        if (delegate != null) {
            delegate!!.onScaleChanged(view, oldScale, newScale)
            return
        }
        super.onScaleChanged(view, oldScale, newScale)
    }

    override fun onReceivedLoginRequest(
        view: WebView?,
        realm: String?,
        account: String?,
        args: String?
    ) {
        if (delegate != null) {
            delegate!!.onReceivedLoginRequest(view, realm, account, args)
            return
        }
        super.onReceivedLoginRequest(view, realm, account, args)
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onSafeBrowsingHit(
        view: WebView?,
        request: WebResourceRequest?,
        threatType: Int,
        callback: SafeBrowsingResponse?
    ) {
        if (delegate != null) {
            delegate!!.onSafeBrowsingHit(view, request, threatType, callback)
            return
        }
        super.onSafeBrowsingHit(view, request, threatType, callback)
    }
}
