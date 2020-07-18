package top.xuqingquan.web.nokernel

interface JsInterfaceHolder {

    fun addJavaObjects(maps: MutableMap<String, Any>): JsInterfaceHolder

    fun addJavaObject(k: String, v: Any): JsInterfaceHolder

    fun checkObject(v: Any): Boolean

}
