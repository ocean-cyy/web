package top.xuqingquan.web.nokernel

import android.Manifest

object AgentWebPermissions {
    @JvmField
    val CAMERA = arrayOf(Manifest.permission.CAMERA)
    @JvmField
    val LOCATION =
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    @JvmField
    val STORAGE =
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    const val ACTION_CAMERA = "Camera"
    const val ACTION_LOCATION = "Location"
    const val ACTION_STORAGE = "Storage"

}
