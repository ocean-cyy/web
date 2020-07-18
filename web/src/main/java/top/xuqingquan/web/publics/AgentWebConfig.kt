package top.xuqingquan.web.publics

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.ValueCallback
import android.webkit.WebView
import top.xuqingquan.web.nokernel.WebConfig.DEBUG
import top.xuqingquan.web.nokernel.WebConfig.IS_INITIALIZED
import top.xuqingquan.web.nokernel.WebConfig.enableTbs
import top.xuqingquan.utils.Timber
import com.tencent.smtt.sdk.CookieManager as X5CookieManager
import com.tencent.smtt.sdk.CookieSyncManager as X5CookieSyncManager
import com.tencent.smtt.sdk.ValueCallback as X5ValueCallback
import com.tencent.smtt.sdk.WebView as X5WebView

@Suppress("DEPRECATION")
@SuppressLint("ObsoleteSdkInt")
object AgentWebConfig {

    @JvmStatic
    fun debug() {
        DEBUG = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (enableTbs()) {
                X5WebView.setWebContentsDebuggingEnabled(true)
            } else {
                WebView.setWebContentsDebuggingEnabled(true)
            }
        }
    }

    //获取Cookie
    @JvmStatic
    fun getCookiesByUrl(url: String): String? {
        return if (enableTbs()) {
            if (X5CookieManager.getInstance() == null) {
                null
            } else {
                X5CookieManager.getInstance().getCookie(url)
            }
        } else {
            if (CookieManager.getInstance() == null) {
                null
            } else {
                CookieManager.getInstance().getCookie(url)
            }
        }
    }

    @JvmStatic
    fun removeAllCookies(callback_o: ValueCallback<Boolean>?) {
        var callback = callback_o
        if (callback == null) {
            callback = getDefaultIgnoreCallback()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookie()
            toSyncCookies()
            callback.onReceiveValue(!CookieManager.getInstance().hasCookies())
            return
        }
        CookieManager.getInstance().removeAllCookies(callback)
        toSyncCookies()
    }

    @JvmStatic
    fun removeAllX5Cookies(callback_o: X5ValueCallback<Boolean>?) {
        var callback = callback_o
        if (callback == null) {
            callback = getX5DefaultIgnoreCallback()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            X5CookieManager.getInstance().removeAllCookie()
            toSyncCookies()
            callback.onReceiveValue(!X5CookieManager.getInstance().hasCookies())
            return
        }
        X5CookieManager.getInstance().removeAllCookies(callback)
        toSyncCookies()
    }

    @JvmStatic
    @Synchronized
    fun initCookiesManager(context: Context) {
        if (!IS_INITIALIZED) {
            createCookiesSyncInstance(context)
            IS_INITIALIZED = true
        }
    }

    private fun createCookiesSyncInstance(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (enableTbs()) {
                X5CookieSyncManager.createInstance(context)
            } else {
                CookieSyncManager.createInstance(context)
            }
        }
    }

    private fun toSyncCookies() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (enableTbs()) {
                X5CookieSyncManager.getInstance().sync()
            } else {
                CookieSyncManager.getInstance().sync()
            }
            return
        }
        if (enableTbs()) {
            AsyncTask.THREAD_POOL_EXECUTOR.execute {
                X5CookieManager.getInstance().flush()
            }
        } else {
            AsyncTask.THREAD_POOL_EXECUTOR.execute {
                CookieManager.getInstance().flush()
            }
        }
    }

    private fun getDefaultIgnoreCallback(): ValueCallback<Boolean> {
        return ValueCallback {
            Timber.i("removeExpiredCookies:$it")
        }
    }

    private fun getX5DefaultIgnoreCallback(): X5ValueCallback<Boolean> {
        return X5ValueCallback {
            Timber.i("removeExpiredCookies:$it")
        }
    }
}
