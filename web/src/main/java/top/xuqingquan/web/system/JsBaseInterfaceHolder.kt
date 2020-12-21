package top.xuqingquan.web.system

import android.webkit.JavascriptInterface
import top.xuqingquan.web.nokernel.JsInterfaceHolder
import top.xuqingquan.web.nokernel.WebConfig

/**
 * Created by 许清泉 on 12/21/20 11:09 PM
 */
abstract class JsBaseInterfaceHolder protected constructor(private val webCreator: WebCreator<*>) :
    JsInterfaceHolder {

    override fun checkObject(v: Any): Boolean {
        if (webCreator.getWebViewType() == WebConfig.WEB_VIEW_AGENT_WEB_SAFE_TYPE) {
            return true
        }
        var tag = false
        val clazz = v.javaClass
        val methods = clazz.methods
        for (method in methods) {
            val annotations = method.annotations
            for (annotation in annotations) {
                if (annotation is JavascriptInterface) {
                    tag = true
                    break
                }
            }
            if (tag){
                break
            }
        }
        return tag
    }

}