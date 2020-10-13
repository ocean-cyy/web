package top.xuqingquan.web.sonic

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceResponse
import android.widget.Toast
import com.tencent.sonic.sdk.SonicRuntime
import com.tencent.sonic.sdk.SonicSessionClient
import top.xuqingquan.utils.Timber
import top.xuqingquan.web.nokernel.WebConfig
import java.io.InputStream
import com.tencent.smtt.export.external.interfaces.WebResourceResponse as X5WebResourceResponse
import com.tencent.smtt.sdk.CookieManager as X5CookieManager

/**
 * Create by 许清泉 on 2020/8/22 20:35
 */
class SonicRuntimeImpl(context: Context) : SonicRuntime(context) {
    @Suppress("PrivatePropertyName")
    private val TAG = "SonicRuntimeImpl"
    private val tbsEnable by lazy { WebConfig.isTbsEnable() }

    override fun log(tag: String?, level: Int, message: String?) {
        val tag1 = tag ?: TAG
        when (level) {
            Log.ERROR -> Timber.tag(tag1).e(message)
            Log.WARN -> Timber.tag(tag1).w(message)
            Log.INFO -> Timber.tag(tag1).i(message)
            Log.VERBOSE -> Timber.tag(tag1).v(message)
            else -> Timber.tag(tag1).d(message)
        }
    }

    override fun getCookie(url: String?): String {
        return if (tbsEnable) {
            X5CookieManager.getInstance()?.getCookie(url)
        } else {
            CookieManager.getInstance()?.getCookie(url)
        } ?: ""
    }

    override fun setCookie(url: String?, cookies: MutableList<String>?): Boolean {
        if (!url.isNullOrEmpty() && !cookies.isNullOrEmpty()) {
            if (tbsEnable) {
                val manager = X5CookieManager.getInstance() ?: return false
                cookies.forEach {
                    manager.setCookie(url, it)
                }
            } else {
                val manager = CookieManager.getInstance() ?: return false
                cookies.forEach {
                    manager.setCookie(url, it)
                }
            }
            return true
        }
        return false
    }

    /**
     * 获取用户UA信息
     */
    override fun getUserAgent(): String {
        return "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 6 Build/LYZ28E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Mobile Safari/537.36"
    }

    /**
     * 获取用户ID信息
     */
    override fun getCurrentUserAccount(): String {
        return "scaffold-sonic"
    }

    override fun isSonicUrl(url: String?) = true

    override fun createWebResourceResponse(
        mimeType: String?,
        encoding: String?,
        data: InputStream?,
        headers: MutableMap<String, String>?
    ): Any {
        return if (tbsEnable) {
            val response = X5WebResourceResponse(mimeType, encoding, data)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                response.responseHeaders = headers
            }
            response
        } else {
            val response = WebResourceResponse(mimeType, encoding, data)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                response.responseHeaders = headers
            }
            response
        }
    }

    override fun isNetworkValid() = true

    override fun showToast(text: CharSequence?, duration: Int) {
        Toast.makeText(context, text, duration).show()
    }

    override fun postTaskToThread(task: Runnable?, delayMillis: Long) {
        val thread = Thread(task, "SonicThread")
        thread.start()
    }

    override fun notifyError(client: SonicSessionClient?, url: String?, errorCode: Int) {
    }


}