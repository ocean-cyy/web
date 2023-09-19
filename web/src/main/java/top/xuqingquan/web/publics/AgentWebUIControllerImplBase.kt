package top.xuqingquan.web.publics

import android.app.Activity
import android.os.Handler
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebView

open class AgentWebUIControllerImplBase : AbsAgentWebUIController() {

    override fun onJsAlert(view: WebView?, url: String?, message: String?) {
        getDelegate().onJsAlert(view, url, message)
    }

    override fun onOpenPagePrompt(view: WebView, url: String, callback: Handler.Callback) {
        getDelegate().onOpenPagePrompt(view, url, callback)
    }

    override fun onJsConfirm(view: WebView?, url: String?, message: String?, jsResult: JsResult?) {
        getDelegate().onJsConfirm(view, url, message, jsResult)
    }

    override fun onSelectItemsPrompt(
        view: WebView, url: String, ways: Array<String>, callback: Handler.Callback
    ) {
        getDelegate().onSelectItemsPrompt(view, url, ways, callback)
    }

    override fun onForceDownloadAlert(callback: Handler.Callback) {
        getDelegate().onForceDownloadAlert(callback)
    }

    override fun onDownloadPrompt(fileName: String, callback: Handler.Callback) {
        getDelegate().onDownloadPrompt(fileName, callback)
    }

    override fun onJsPrompt(
        view: WebView?, url: String?, message: String?,
        defaultValue: String?, jsPromptResult: JsPromptResult?
    ) {
        getDelegate().onJsPrompt(view, url, message, defaultValue, jsPromptResult)
    }

    override fun onMainFrameError(
        view: WebView, errorCode: Int, description: String, failingUrl: String
    ) {
        getDelegate().onMainFrameError(view, errorCode, description, failingUrl)
    }

    override fun onShowMainFrame() {
        getDelegate().onShowMainFrame()
    }

    override fun onLoading(msg: String) {
        getDelegate().onLoading(msg)
    }

    override fun onCancelLoading() {
        getDelegate().onCancelLoading()
    }

    override fun onShowMessage(message: String, intent: String) {
        getDelegate().onShowMessage(message, intent)
    }

    override fun onPermissionsDeny(
        permissions: Array<String>, permissionType: String, action: String
    ) {
        getDelegate().onPermissionsDeny(permissions, permissionType, action)
    }

    override fun bindSupportWebParent(webParentLayout: WebParentLayout, activity: Activity) {
        getDelegate().bindSupportWebParent(webParentLayout, activity)
    }

    companion object {

        @JvmStatic
        fun build(): AbsAgentWebUIController {
            return AgentWebUIControllerImplBase()
        }
    }


}
