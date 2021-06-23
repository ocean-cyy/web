@file:Suppress("DEPRECATION")

package top.xuqingquan.web.publics

import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.ValueCallback
import android.webkit.WebView
import top.xuqingquan.utils.Timber
import top.xuqingquan.web.nokernel.WebConfig.DEBUG
import top.xuqingquan.web.nokernel.WebConfig.IS_INITIALIZED

object AgentWebConfig {

    @JvmStatic
    fun debug() {
        DEBUG = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    //获取Cookie
    @JvmStatic
    fun getCookiesByUrl(url: String): String? {
        return if (CookieManager.getInstance() == null) {
            null
        } else {
            CookieManager.getInstance().getCookie(url)
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
    @Synchronized
    fun initCookiesManager(context: Context) {
        if (!IS_INITIALIZED) {
            createCookiesSyncInstance(context)
            IS_INITIALIZED = true
        }
    }

    private fun createCookiesSyncInstance(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(context)
        }
    }

    private fun toSyncCookies() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().sync()
            return
        }
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            CookieManager.getInstance().flush()
        }
    }

    private fun getDefaultIgnoreCallback(): ValueCallback<Boolean> {
        return ValueCallback {
            Timber.i("removeExpiredCookies:$it")
        }
    }
}
