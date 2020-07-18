package top.xuqingquan.web.publics

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.support.annotation.ColorInt
import android.support.annotation.RequiresApi
import android.support.design.widget.Snackbar
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import top.xuqingquan.utils.Timber
import top.xuqingquan.utils.clearCacheFolder
import top.xuqingquan.web.R
import top.xuqingquan.web.nokernel.PermissionInterceptor
import top.xuqingquan.web.nokernel.WebConfig
import top.xuqingquan.web.nokernel.WebUtils
import top.xuqingquan.web.nokernel.WebUtils.isUIThread
import java.io.File
import java.lang.ref.WeakReference
import com.tencent.smtt.sdk.ValueCallback as X5ValueCallback
import com.tencent.smtt.sdk.WebChromeClient as X5WebChromeClient
import com.tencent.smtt.sdk.WebView as X5WebView

object AgentWebUtils {

    @JvmStatic
    fun clearWebView(m: WebView?) {
        try {
            if (m == null) {
                return
            }
            if (!isUIThread()) {
                return
            }
            m.loadUrl("about:blank")
            m.stopLoading()
            if (m.handler != null) {
                m.handler.removeCallbacksAndMessages(null)
            }
            m.removeAllViews()
            val mViewGroup = m.parent as ViewGroup?
            mViewGroup?.removeView(m)
            m.webChromeClient = null
            m.webViewClient = null
            m.tag = null
            m.clearHistory()
            m.destroy()
        } catch (t: Throwable) {
        }
    }

    @JvmStatic
    fun clearWebView(m: X5WebView?) {
        try {
            if (m == null) {
                return
            }
            if (!isUIThread()) {
                return
            }
            m.loadUrl("about:blank")
            m.stopLoading()
            if (m.handler != null) {
                m.handler.removeCallbacksAndMessages(null)
            }
            m.removeAllViews()
            val mViewGroup = m.parent as ViewGroup?
            mViewGroup?.removeView(m)
            m.webChromeClient = null
            m.webViewClient = null
            m.tag = null
            m.clearHistory()
            m.destroy()
        } catch (t: Throwable) {
        }
    }

    @JvmStatic
    fun show(
        parent: View,
        text: CharSequence,
        duration: Int,
        @ColorInt textColor: Int,
        @ColorInt bgColor: Int,
        actionText: CharSequence?,
        @ColorInt actionTextColor: Int,
        listener: View.OnClickListener?
    ) {
        val spannableString = SpannableString(text)
        val colorSpan = ForegroundColorSpan(textColor)
        spannableString.setSpan(
            colorSpan,
            0,
            spannableString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val snackbarWeakReference = WeakReference(Snackbar.make(parent, spannableString, duration))
        val snackbar = snackbarWeakReference.get() ?: return
        snackbar.view.setBackgroundColor(bgColor)
        if (!actionText.isNullOrEmpty() && listener != null) {
            snackbar.setActionTextColor(actionTextColor)
            snackbar.setAction(actionText, listener)
        }
        snackbar.show()
    }

    @JvmStatic
    fun clearWebViewAllCache(context: Context, webView: WebView) {
        try {
            AgentWebConfig.removeAllCookies(null)
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            context.deleteDatabase("webviewCache.db")
            context.deleteDatabase("webview.db")
            webView.clearCache(true)
            webView.clearHistory()
            webView.clearFormData()
            clearCacheFolder(File(WebUtils.getCachePath(context)), 0)
        } catch (t: Throwable) {
            Timber.e(t)
        }

    }

    @JvmStatic
    fun clearWebViewAllCache(context: Context, webView: X5WebView) {
        try {
            AgentWebConfig.removeAllX5Cookies(null)
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            context.deleteDatabase("webviewCache.db")
            context.deleteDatabase("webview.db")
            webView.clearCache(true)
            webView.clearHistory()
            webView.clearFormData()
            clearCacheFolder(File(WebUtils.getCachePath(context)), 0)
        } catch (t: Throwable) {
            Timber.e(t)
        }

    }

    @JvmStatic
    fun clearWebViewAllCache(context: Context) {
        try {
            if (WebConfig.enableTbs()) {
                clearWebViewAllCache(
                    context,
                    top.xuqingquan.web.x5.LollipopFixedWebView(context.applicationContext)
                )
            } else {
                clearWebViewAllCache(
                    context,
                    top.xuqingquan.web.system.LollipopFixedWebView(context.applicationContext)
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

    }

    @JvmStatic
    fun getAgentWebUIControllerByWebView(webView: WebView): AbsAgentWebUIController? {
        val mWebParentLayout = getWebParentLayoutByWebView(webView)
        return mWebParentLayout.provide()
    }

    @JvmStatic
    fun getAgentWebUIControllerByWebView(webView: X5WebView): AbsAgentWebUIController? {
        val mWebParentLayout = getWebParentLayoutByWebView(webView)
        return mWebParentLayout.provide()
    }

    @JvmStatic
    fun getWebParentLayoutByWebView(webView: WebView): WebParentLayout {
        var mViewGroup: ViewGroup?
        check(webView.parent is ViewGroup) { "please check webcreator's create method was be called ?" }
        mViewGroup = webView.parent as ViewGroup
        while (mViewGroup != null) {
            Timber.i("ViewGroup:$mViewGroup")
            mViewGroup = if (mViewGroup.id == R.id.scaffold_web_parent_layout_id) {
                val mWebParentLayout = mViewGroup as WebParentLayout?
                Timber.i("found WebParentLayout")
                return mWebParentLayout!!
            } else {
                val mViewParent = mViewGroup.parent
                if (mViewParent is ViewGroup) {
                    mViewParent
                } else {
                    null
                }
            }
        }
        throw IllegalStateException("please check webcreator's create method was be called ?")
    }

    @JvmStatic
    fun getWebParentLayoutByWebView(webView: X5WebView): WebParentLayout {
        var mViewGroup: ViewGroup?
        check(webView.parent is ViewGroup) { "please check webcreator's create method was be called ?" }
        mViewGroup = webView.parent as ViewGroup
        while (mViewGroup != null) {
            Timber.i("ViewGroup:$mViewGroup")
            mViewGroup = if (mViewGroup.id == R.id.scaffold_web_parent_layout_id) {
                val mWebParentLayout = mViewGroup as WebParentLayout?
                Timber.i("found WebParentLayout")
                return mWebParentLayout!!
            } else {
                val mViewParent = mViewGroup.parent
                if (mViewParent is ViewGroup) {
                    mViewParent
                } else {
                    null
                }
            }
        }
        throw IllegalStateException("please check webcreator's create method was be called ?")
    }

    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun showFileChooserCompat(
        activity: Activity,
        webView: WebView,
        valueCallbacks: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?,
        permissionInterceptor: PermissionInterceptor,
        valueCallback: ValueCallback<Uri>?,
        mimeType: String?,
        jsChannelCallback: Handler.Callback?
    ): Boolean {
        try {
            val builder = FileChooser.newBuilder(activity, webView)
            if (valueCallbacks != null) {
                builder.setUriValueCallbacks(valueCallbacks)
            }
            if (fileChooserParams != null) {
                builder.setFileChooserParams(fileChooserParams)
            }
            if (valueCallback != null) {
                builder.setUriValueCallback(valueCallback)
            }
            if (!TextUtils.isEmpty(mimeType)) {
                builder.setAcceptType(mimeType)
            }
            if (jsChannelCallback != null) {
                builder.setJsChannelCallback(jsChannelCallback)
            }
            builder.setPermissionInterceptor(permissionInterceptor)
            builder.build().openFileChooser()
        } catch (throwable: Throwable) {
            Timber.e(throwable)
            if (valueCallbacks != null) {
                Timber.i("onReceiveValue empty")
                return false
            }
            valueCallback?.onReceiveValue(null)
        }

        return true
    }

    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun showFileChooserCompat(
        activity: Activity,
        webView: X5WebView,
        valueCallbacks: X5ValueCallback<Array<Uri>>?,
        fileChooserParams: X5WebChromeClient.FileChooserParams?,
        permissionInterceptor: PermissionInterceptor,
        valueCallback: X5ValueCallback<Uri>?,
        mimeType: String?,
        jsChannelCallback: Handler.Callback?
    ): Boolean {
        try {
            val builder = FileChooser.newBuilder(activity, webView)
            if (valueCallbacks != null) {
                builder.setUriValueCallbacks(valueCallbacks)
            }
            if (fileChooserParams != null) {
                builder.setFileChooserParams(fileChooserParams)
            }
            if (valueCallback != null) {
                builder.setUriValueCallback(valueCallback)
            }
            if (!TextUtils.isEmpty(mimeType)) {
                builder.setAcceptType(mimeType)
            }
            if (jsChannelCallback != null) {
                builder.setJsChannelCallback(jsChannelCallback)
            }
            builder.setPermissionInterceptor(permissionInterceptor)
            builder.build().openFileChooser()
        } catch (throwable: Throwable) {
            Timber.e(throwable)
            if (valueCallbacks != null) {
                Timber.i("onReceiveValue empty")
                return false
            }
            valueCallback?.onReceiveValue(null)
        }

        return true
    }
}
