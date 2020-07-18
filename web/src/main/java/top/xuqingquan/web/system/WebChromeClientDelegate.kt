package top.xuqingquan.web.system

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Message
import android.view.View
import android.webkit.*
import android.support.annotation.RequiresApi

@Suppress("DEPRECATION")
open class WebChromeClientDelegate constructor(webChromeClient: WebChromeClient?) :
    WebChromeClient() {
    open var delegate: WebChromeClient? = webChromeClient

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        this.delegate?.onProgressChanged(view, newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        if (this.delegate != null) {
            this.delegate!!.onReceivedTitle(view, title)
            return
        }
        super.onReceivedTitle(view, title)
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        if (this.delegate != null) {
            this.delegate!!.onReceivedIcon(view, icon)
            return
        }
        super.onReceivedIcon(view, icon)
    }

    override fun onReceivedTouchIconUrl(
        view: WebView, url: String,
        precomposed: Boolean
    ) {
        if (this.delegate != null) {
            this.delegate!!.onReceivedTouchIconUrl(view, url, precomposed)
            return
        }
        super.onReceivedTouchIconUrl(view, url, precomposed)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        if (this.delegate != null) {
            this.delegate!!.onShowCustomView(view, callback)
            return
        }
        super.onShowCustomView(view, callback)
    }

    @Deprecated("")
    override fun onShowCustomView(
        view: View, requestedOrientation: Int,
        callback: CustomViewCallback
    ) {
        if (this.delegate != null) {
            this.delegate!!.onShowCustomView(view, requestedOrientation, callback)
            return
        }
        super.onShowCustomView(view, requestedOrientation, callback)
    }


    override fun onHideCustomView() {
        if (this.delegate != null) {
            this.delegate!!.onHideCustomView()
            return
        }
        super.onHideCustomView()
    }

    override fun onCreateWindow(
        view: WebView, isDialog: Boolean,
        isUserGesture: Boolean, resultMsg: Message
    ): Boolean {
        return if (this.delegate != null) {
            this.delegate!!.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
        } else {
            super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
        }
    }

    override fun onRequestFocus(view: WebView) {
        if (this.delegate != null) {
            this.delegate!!.onRequestFocus(view)
            return
        }
        super.onRequestFocus(view)
    }

    override fun onCloseWindow(window: WebView) {
        if (this.delegate != null) {
            this.delegate!!.onCloseWindow(window)
            return
        }
        super.onCloseWindow(window)
    }

    override fun onJsAlert(
        view: WebView, url: String, message: String,
        result: JsResult
    ): Boolean {
        return if (this.delegate != null) {
            this.delegate!!.onJsAlert(view, url, message, result)
        } else {
            super.onJsAlert(view, url, message, result)
        }
    }

    override fun onJsConfirm(
        view: WebView, url: String, message: String,
        result: JsResult
    ): Boolean {
        return if (this.delegate != null) {
            this.delegate!!.onJsConfirm(view, url, message, result)
        } else {
            super.onJsConfirm(view, url, message, result)
        }
    }

    override fun onJsPrompt(
        view: WebView, url: String, message: String,
        defaultValue: String, result: JsPromptResult
    ): Boolean {
        return if (this.delegate != null) {
            this.delegate!!.onJsPrompt(view, url, message, defaultValue, result)
        } else {
            super.onJsPrompt(view, url, message, defaultValue, result)
        }
    }

    override fun onJsBeforeUnload(
        view: WebView, url: String, message: String,
        result: JsResult
    ): Boolean {
        return if (this.delegate != null) {
            this.delegate!!.onJsBeforeUnload(view, url, message, result)
        } else {
            super.onJsBeforeUnload(view, url, message, result)
        }
    }

    @Deprecated("")
    override fun onExceededDatabaseQuota(
        url: String, databaseIdentifier: String,
        quota: Long, estimatedDatabaseSize: Long, totalQuota: Long,
        quotaUpdater: WebStorage.QuotaUpdater
    ) {
        if (this.delegate != null) {
            this.delegate!!.onExceededDatabaseQuota(
                url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater
            )
            return
        }
        super.onExceededDatabaseQuota(
            url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater
        )

    }

    @Deprecated("")
    override fun onReachedMaxAppCacheSize(
        requiredStorage: Long, quota: Long,
        quotaUpdater: WebStorage.QuotaUpdater
    ) {
        if (this.delegate != null) {
            this.delegate!!.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater)
            return
        }
        super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback
    ) {
        if (this.delegate != null) {
            this.delegate!!.onGeolocationPermissionsShowPrompt(origin, callback)
            return
        }
        super.onGeolocationPermissionsShowPrompt(origin, callback)

    }

    override fun onGeolocationPermissionsHidePrompt() {
        if (this.delegate != null) {
            this.delegate!!.onGeolocationPermissionsHidePrompt()
            return
        }

        super.onGeolocationPermissionsHidePrompt()
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onPermissionRequest(request: PermissionRequest) {
        if (this.delegate != null) {
            this.delegate!!.onPermissionRequest(request)
            return
        }
        super.onPermissionRequest(request)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onPermissionRequestCanceled(request: PermissionRequest) {

        if (this.delegate != null) {
            this.delegate!!.onPermissionRequestCanceled(request)
            return
        }
        super.onPermissionRequestCanceled(request)
    }

    override fun onJsTimeout(): Boolean {
        return if (this.delegate != null) {
            this.delegate!!.onJsTimeout()
        } else {
            super.onJsTimeout()
        }
    }

    @Deprecated("")
    override fun onConsoleMessage(message: String, lineNumber: Int, sourceID: String) {
        if (this.delegate != null) {
            this.delegate!!.onConsoleMessage(message, lineNumber, sourceID)
            return
        }
        super.onConsoleMessage(message, lineNumber, sourceID)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        return if (this.delegate != null) {
            this.delegate!!.onConsoleMessage(consoleMessage)
        } else {
            super.onConsoleMessage(consoleMessage)
        }
    }

    override fun getDefaultVideoPoster(): Bitmap? {
        return if (this.delegate != null) {
            this.delegate!!.defaultVideoPoster
        } else {
            super.getDefaultVideoPoster()
        }
    }

    override fun getVideoLoadingProgressView(): View? {
        return if (this.delegate != null) {
            this.delegate!!.videoLoadingProgressView
        } else {
            super.getVideoLoadingProgressView()
        }
    }

    override fun getVisitedHistory(callback: ValueCallback<Array<String>>) {
        if (this.delegate != null) {
            this.delegate!!.getVisitedHistory(callback)
            return
        }
        super.getVisitedHistory(callback)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onShowFileChooser(
        webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        return if (this.delegate != null) {
            this.delegate!!.onShowFileChooser(webView, filePathCallback, fileChooserParams)
        } else {
            super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
        }
    }

}
