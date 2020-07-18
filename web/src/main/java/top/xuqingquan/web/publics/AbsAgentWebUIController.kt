package top.xuqingquan.web.publics

import android.app.Activity
import android.os.Handler
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebView
import com.tencent.smtt.export.external.interfaces.JsPromptResult as X5JsPromptResult
import com.tencent.smtt.export.external.interfaces.JsResult as X5JsResult
import com.tencent.smtt.sdk.WebView as X5WebView

abstract class AbsAgentWebUIController {
    @Volatile
    private var mIsBindWebParent = false
    private var mAgentWebUIControllerDelegate: AbsAgentWebUIController? = null

    protected fun create(): AbsAgentWebUIController {
        return if (HAS_DESIGN_LIB) DefaultDesignUIController() else DefaultUIController()
    }

    protected fun getDelegate(): AbsAgentWebUIController {
        var mAgentWebUIController = this.mAgentWebUIControllerDelegate
        if (mAgentWebUIController == null) {
            mAgentWebUIController = create()
            this.mAgentWebUIControllerDelegate = mAgentWebUIController
        }
        return mAgentWebUIController
    }

    @Synchronized
    fun bindWebParent(webParentLayout: WebParentLayout, activity: Activity) {
        if (!mIsBindWebParent) {
            mIsBindWebParent = true
            bindSupportWebParent(webParentLayout, activity)
        }
    }

    abstract fun bindSupportWebParent(webParentLayout: WebParentLayout, activity: Activity)

    /**
     * WebChromeClient#onJsAlert
     *
     * @param view
     * @param url
     * @param message
     */
    abstract fun onJsAlert(view: WebView, url: String, message: String)

    abstract fun onJsAlert(view: X5WebView, url: String, message: String)

    /**
     * 咨询用户是否前往其他页面
     *
     * @param view
     * @param url
     * @param callback
     */
    abstract fun onOpenPagePrompt(view: WebView, url: String, callback: Handler.Callback)

    abstract fun onOpenPagePrompt(view: X5WebView, url: String, callback: Handler.Callback)

    /**
     * WebChromeClient#onJsConfirm
     *
     * @param view
     * @param url
     * @param message
     * @param jsResult
     */
    abstract fun onJsConfirm(view: WebView, url: String, message: String, jsResult: JsResult)

    abstract fun onJsConfirm(view: X5WebView, url: String, message: String, jsResult: X5JsResult)

    abstract fun onSelectItemsPrompt(
        view: WebView, url: String, ways: Array<String>, callback: Handler.Callback
    )

    abstract fun onSelectItemsPrompt(
        view: X5WebView, url: String, ways: Array<String>, callback: Handler.Callback
    )

    /**
     * 强制下载弹窗
     * @param callback 用户操作回调回调
     */
    abstract fun onForceDownloadAlert(callback: Handler.Callback)

    abstract fun onDownloadPrompt(fileName: String, callback: Handler.Callback)

    /**
     * WebChromeClient#onJsPrompt
     *
     * @param view
     * @param url
     * @param message
     * @param defaultValue
     * @param jsPromptResult
     */
    abstract fun onJsPrompt(
        view: WebView, url: String, message: String,
        defaultValue: String, jsPromptResult: JsPromptResult
    )

    abstract fun onJsPrompt(
        view: X5WebView, url: String, message: String,
        defaultValue: String, jsPromptResult: X5JsPromptResult
    )

    /**
     * 显示错误页
     *
     * @param view
     * @param errorCode
     * @param description
     * @param failingUrl
     */
    abstract fun onMainFrameError(
        view: WebView, errorCode: Int, description: String, failingUrl: String
    )

    abstract fun onMainFrameError(
        view: X5WebView, errorCode: Int, description: String, failingUrl: String
    )

    /**
     * 隐藏错误页
     */
    abstract fun onShowMainFrame()

    /**
     * 正在加载...
     *
     * @param msg
     */
    abstract fun onLoading(msg: String)

    /**
     * 取消正在加载...
     */
    abstract fun onCancelLoading()

    /**
     * @param message 消息
     * @param intent  说明message的来源，意图
     */
    abstract fun onShowMessage(message: String, intent: String)

    /**
     * 当权限被拒回调该方法
     *
     * @param permissions
     * @param permissionType
     * @param action
     */
    abstract fun onPermissionsDeny(
        permissions: Array<String>, permissionType: String, action: String
    )

    companion object {

        private var HAS_DESIGN_LIB: Boolean = false

        init {
            HAS_DESIGN_LIB = try {
                Class.forName("com.google.android.material.snackbar.Snackbar")
                Class.forName("com.google.android.material.bottomsheet.BottomSheetDialog")
                true
            } catch (ignore: Throwable) {
                false
            }

        }
    }

}
