package top.xuqingquan.web.nokernel

import android.annotation.SuppressLint
import android.os.Build
import top.xuqingquan.web.BuildConfig
import java.io.File

/**
 * Created by 许清泉 on 2019-06-19 20:00
 */
@Suppress("unused")
object WebConfig {

    /**
     * 直接打开其他页面
     */
    const val DIRECT_OPEN_OTHER_PAGE = 1001

    /**
     * 弹窗咨询用户是否前往其他页面
     */
    const val ASK_USER_OPEN_OTHER_PAGE = DIRECT_OPEN_OTHER_PAGE shr 2

    /**
     * 不允许打开其他页面
     */
    internal const val DISALLOW_OPEN_OTHER_APP = DIRECT_OPEN_OTHER_PAGE shr 4
    const val FILE_CACHE_PATH = "agentweb-cache"
    internal val AGENT_WEB_CACHE_PATCH = "${File.separator}${FILE_CACHE_PATH}"
    private const val AGENT_WEB_NAME = "AgentWeb"

    /**
     * 缓存路径
     */
    @JvmField
    var AGENT_WEB_FILE_PATH: String? = null

    /**
     * DEBUG 模式 ， 如果需要查看日志请设置为 true
     */
    @JvmField
    var DEBUG = false

    /**
     * 当前操作系统是否低于 KITKAT
     */
    @JvmField
    @SuppressLint("ObsoleteSdkInt")
    val IS_KITKAT_OR_BELOW_KITKAT = Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT

    /**
     * 默认 WebView  类型 。
     */
    const val WEB_VIEW_DEFAULT_TYPE = 1

    /**
     * 使用 AgentWebView
     */
    const val WEB_VIEW_AGENT_WEB_SAFE_TYPE = 2

    /**
     * 自定义 WebView
     */
    const val WEB_VIEW_CUSTOM_TYPE = 3

    @Volatile
    @JvmField
    var IS_INITIALIZED = false

    /**
     * AgentWeb 的版本
     */
    const val AGENT_WEB_VERSION = " $AGENT_WEB_NAME/${BuildConfig.VERSION_NAME}"

    /**
     * 通过JS获取的文件大小， 这里限制最大为5MB ，太大会抛出 OutOfMemoryError
     */
    @JvmField
    var MAX_FILE_LENGTH = 1024 * 1024 * 5

    internal var tbsStatus = false//x5是否可用

    private var tbsEnable: Boolean? = null//x5是否启用

    /**
     * 禁用tbs
     */
    @JvmStatic
    fun disableTbs() {
        tbsEnable = false
    }

    /**
     * 设置tbs为可用
     */
    @JvmStatic
    fun enableTbs() {
        tbsEnable = true
    }

    /**
     * 判断tbs是否可用
     */
    @JvmStatic
    fun isTbsEnable(): Boolean {
        if (tbsEnable == null) {
            tbsEnable = tbsStatus
        }
        return tbsEnable!!
    }

    @JvmStatic
    fun resetTbsStatus() {
        tbsEnable = tbsStatus
    }
}
