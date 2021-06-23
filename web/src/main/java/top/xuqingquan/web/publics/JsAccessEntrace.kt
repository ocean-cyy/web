package top.xuqingquan.web.publics

import android.webkit.ValueCallback

interface JsAccessEntrace : QuickCallJs {

    fun callJs(js: String?, callback: ValueCallback<String>?)

    fun callJs(js: String?)

}
