package top.xuqingquan.web.publics

import android.webkit.ValueCallback
import com.tencent.smtt.sdk.ValueCallback as X5ValueCallback

interface QuickCallJs {

    fun quickCallJs(method: String?, callback: ValueCallback<String>?, vararg params: String?)

    fun quickCallJs(method: String?, callback: X5ValueCallback<String>?, vararg params: String?)

    fun quickCallJs(method: String?, vararg params: String?)

    fun quickCallJs(method: String?)
}
