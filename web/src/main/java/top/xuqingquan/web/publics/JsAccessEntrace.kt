package top.xuqingquan.web.publics

import android.webkit.ValueCallback
import com.tencent.smtt.sdk.ValueCallback as X5ValueCallback

interface JsAccessEntrace : QuickCallJs {

    fun callJs(js: String?, callback: ValueCallback<String>?)

    fun callJs(js: String?, callback: X5ValueCallback<String>?)

    fun callJs(js: String?)

}
