package top.xuqingquan.web.x5

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.support.annotation.RequiresApi
import com.tencent.smtt.export.external.interfaces.*
import com.tencent.smtt.sdk.ValueCallback
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebStorage
import com.tencent.smtt.sdk.WebView
import top.xuqingquan.web.nokernel.*
import top.xuqingquan.web.nokernel.ActionActivity.KEY_FROM_INTENTION
import top.xuqingquan.web.publics.AgentWebUtils
import top.xuqingquan.web.publics.IndicatorController
import top.xuqingquan.utils.Timber
import top.xuqingquan.utils.getDeniedPermissions
import top.xuqingquan.utils.hasPermission
import java.lang.ref.WeakReference
import java.util.*

@Suppress("DEPRECATION", "OverridingDeprecatedMember")
class DefaultChromeClient(
    activity: Activity,
    /**
     * IndicatorController 进度条控制器
     */
    private val mIndicatorController: IndicatorController?,
    chromeClient: WebChromeClient?,
    /**
     * PermissionInterceptor 权限拦截器
     */
    private val mPermissionInterceptor: PermissionInterceptor?,
    /**
     * 当前 WebView
     */
    private val mWebView: WebView
) : MiddlewareWebChromeBase(chromeClient) {
    /**
     * Activity
     */
    private val mActivityWeakReference = WeakReference(activity)
    /**
     * 包装Flag
     */
    private val mIsWrapper = chromeClient != null
    /**
     * Web端触发的定位 mOrigin
     */
    private var mOrigin: String? = null
    /**
     * Web 端触发的定位 Callback 回调成功，或者失败
     */
    private var mCallback: GeolocationPermissionsCallback? = null
    /**
     * AbsAgentWebUIController
     */
    private val mAgentWebUIController =
        WeakReference(AgentWebUtils.getAgentWebUIControllerByWebView(mWebView))

    private val mPermissionListener = ActionActivity.PermissionListener { permissions, _, extras ->
        if (extras.getInt(KEY_FROM_INTENTION) == FROM_CODE_INTENTION_LOCATION) {
            val hasPermission = hasPermission(mActivityWeakReference.get()!!, *permissions)
            if (mCallback != null) {
                if (hasPermission) {
                    mCallback!!.invoke(mOrigin, true, false)
                } else {
                    mCallback!!.invoke(mOrigin, false, false)
                }
                mCallback = null
                mOrigin = null
            }
            if (!hasPermission && null != mAgentWebUIController.get()) {
                mAgentWebUIController.get()!!
                    .onPermissionsDeny(
                        AgentWebPermissions.LOCATION,
                        AgentWebPermissions.ACTION_LOCATION,
                        AgentWebPermissions.ACTION_LOCATION
                    )
            }
        }
    }


    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        mIndicatorController?.progress(view, newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        if (mIsWrapper) {
            super.onReceivedTitle(view, title)
        }
    }

    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
        mAgentWebUIController.get()?.onJsAlert(view, url, message)
        result.confirm()
        return true
    }

    //location
    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissionsCallback
    ) {
        onGeolocationPermissionsShowPromptInternal(origin, callback)
    }

    private fun onGeolocationPermissionsShowPromptInternal(
        origin: String,
        callback: GeolocationPermissionsCallback
    ) {
        if (mPermissionInterceptor != null) {
            if (mPermissionInterceptor.intercept(
                    this.mWebView.url,
                    AgentWebPermissions.LOCATION,
                    "location"
                )
            ) {
                callback.invoke(origin, false, false)
                return
            }
        }
        val mActivity = mActivityWeakReference.get()
        if (mActivity == null) {
            callback.invoke(origin, false, false)
            return
        }
        val deniedPermissions = getDeniedPermissions(mActivity, AgentWebPermissions.LOCATION)
        if (deniedPermissions.isNullOrEmpty()) {
            Timber.i("onGeolocationPermissionsShowPromptInternal:true")
            callback.invoke(origin, true, false)
        } else {
            val mAction = Action.createPermissionsAction(deniedPermissions.toTypedArray())
            mAction.fromIntention = FROM_CODE_INTENTION_LOCATION
            ActionActivity.setPermissionListener(mPermissionListener)
            this.mCallback = callback
            this.mOrigin = origin
            ActionActivity.start(mActivity, mAction)
        }
    }

    override fun onJsPrompt(
        view: WebView, url: String, message: String, defaultValue: String, result: JsPromptResult
    ): Boolean {
        try {
            this.mAgentWebUIController.get()
                ?.onJsPrompt(mWebView, url, message, defaultValue, result)
        } catch (throwable: Throwable) {
            Timber.e(throwable)
        }

        return true
    }

    override fun onJsConfirm(
        view: WebView, url: String, message: String, result: JsResult
    ): Boolean {
        mAgentWebUIController.get()?.onJsConfirm(view, url, message, result)
        return true
    }


    override fun onExceededDatabaseQuota(
        url: String, databaseIdentifier: String, quota: Long, estimatedDatabaseSize: Long,
        totalQuota: Long, quotaUpdater: WebStorage.QuotaUpdater
    ) {
        quotaUpdater.updateQuota(totalQuota * 2)
    }

    override fun onReachedMaxAppCacheSize(
        requiredStorage: Long,
        quota: Long,
        quotaUpdater: WebStorage.QuotaUpdater
    ) {
        quotaUpdater.updateQuota(requiredStorage * 2)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        Timber.i("openFileChooser>=5.0")
        return openFileChooserAboveL(filePathCallback, fileChooserParams)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun openFileChooserAboveL(
        valueCallbacks: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        Timber.i(
            "fileChooserParams:" + Arrays.toString(fileChooserParams.acceptTypes) + "  getTitle:" + fileChooserParams.title + " accept:" + Arrays.toString(
                fileChooserParams.acceptTypes
            ) + " length:" + fileChooserParams.acceptTypes.size + "  :" + fileChooserParams.isCaptureEnabled + "  " + fileChooserParams.filenameHint + "  intent:" + fileChooserParams.createIntent().toString() + "   mode:" + fileChooserParams.mode
        )
        val mActivity = this.mActivityWeakReference.get()
        return if (mActivity == null || mActivity.isFinishing) {
            false
        } else AgentWebUtils.showFileChooserCompat(
            mActivity, mWebView, valueCallbacks, fileChooserParams,
            this.mPermissionInterceptor!!, null, null, null
        )
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        super.onConsoleMessage(consoleMessage)
        return true
    }

    companion object {
        /**
         * 标志位
         */
        private const val FROM_CODE_INTENTION = 0x18
        /**
         * 标识当前是获取定位权限
         */
        private const val FROM_CODE_INTENTION_LOCATION = FROM_CODE_INTENTION shl 2
    }
}
