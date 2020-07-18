package top.xuqingquan.web.nokernel

interface PermissionInterceptor {
    fun intercept(url: String?, permissions: Array<String>, action: String): Boolean
}
