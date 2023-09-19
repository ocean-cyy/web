package top.xuqingquan.web.publics

import android.webkit.ValueCallback

interface QuickCallJs {

    fun quickCallJs(method: String?, callback: ValueCallback<String>?, vararg params: String?)

    fun quickCallJs(method: String?, vararg params: String?)

    fun quickCallJs(method: String?)
}
