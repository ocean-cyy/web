package top.xuqingquan.web.nokernel

import java.lang.ref.WeakReference

/**
 * Created by 许清泉 on 2020/10/13 23:31
 */
class PermissionInterceptorWrapper(permissionInterceptor: PermissionInterceptor?) :
    PermissionInterceptor {
    private val mWeakReference = WeakReference(permissionInterceptor)

    override fun intercept(url: String?, permissions: Array<String>, action: String): Boolean {
        return if (mWeakReference.get() == null) {
            false
        } else {
            mWeakReference.get()!!.intercept(url, permissions, action)
        }
    }

}