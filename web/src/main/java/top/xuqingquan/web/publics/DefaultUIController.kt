@file:Suppress("DEPRECATION")

package top.xuqingquan.web.publics

import android.app.Activity
import android.app.ProgressDialog
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebView
import android.widget.EditText
import android.support.v7.app.AlertDialog
import top.xuqingquan.web.R
import top.xuqingquan.web.nokernel.WebUtils
import top.xuqingquan.utils.Timber
import top.xuqingquan.utils.getApplicationName
import com.tencent.smtt.export.external.interfaces.JsPromptResult as X5JsPromptResult
import com.tencent.smtt.export.external.interfaces.JsResult as X5JsResult
import com.tencent.smtt.sdk.WebView as X5WebView

open class DefaultUIController : AbsAgentWebUIController() {

    private var mJsPromptResult: JsPromptResult? = null
    private var mJsResult: JsResult? = null
    private var mX5JsPromptResult: X5JsPromptResult? = null
    private var mX5JsResult: X5JsResult? = null
    private var mActivity: Activity? = null
    private var mWebParentLayout: WebParentLayout? = null
    private var mProgressDialog: ProgressDialog? = null

    override fun onJsAlert(view: WebView, url: String, message: String) {
        WebUtils.toastShowShort(view.context.applicationContext, message)
    }

    override fun onJsAlert(view: X5WebView, url: String, message: String) {
        WebUtils.toastShowShort(view.context.applicationContext, message)
    }

    override fun onOpenPagePrompt(view: WebView, url: String, callback: Handler.Callback) {
        onOpenPagePrompt(callback)
    }

    override fun onOpenPagePrompt(view: X5WebView, url: String, callback: Handler.Callback) {
        onOpenPagePrompt(callback)
    }

    private fun onOpenPagePrompt(callback: Handler.Callback?) {
        Timber.i("onOpenPagePrompt")
        if (this.mActivity == null || this.mActivity!!.isFinishing || this.mActivity!!.isDestroyed) {
            return
        }
        AlertDialog.Builder(this.mActivity!!)
            .setTitle(R.string.scaffold_tips)
            .setMessage(
                this.mActivity!!.getString(
                    R.string.scaffold_leave_app_and_go_other_page,
                    getApplicationName(mActivity!!)
                )
            )//
            .setNegativeButton(R.string.scaffold_cancel) { _, _ ->
                callback?.handleMessage(Message.obtain(null, -1))
            }//
            .setPositiveButton(R.string.scaffold_leave) { _, _ ->
                callback?.handleMessage(Message.obtain(null, 1))
            }
            .create().show()
    }

    override fun onJsConfirm(
        view: WebView, url: String, message: String, jsResult: JsResult
    ) {
        onJsConfirmInternal(message, jsResult)
    }

    override fun onJsConfirm(
        view: X5WebView, url: String, message: String, jsResult: X5JsResult
    ) {
        onJsConfirmInternal(message, jsResult)
    }

    override fun onSelectItemsPrompt(
        view: WebView, url: String, ways: Array<String>, callback: Handler.Callback
    ) {
        showChooserInternal(ways, callback)
    }

    override fun onSelectItemsPrompt(
        view: X5WebView, url: String, ways: Array<String>, callback: Handler.Callback
    ) {
        showChooserInternal(ways, callback)
    }

    override fun onForceDownloadAlert(callback: Handler.Callback) {
        onForceDownloadAlertInternal(callback)
    }

    private fun onForceDownloadAlertInternal(callback: Handler.Callback) {
        if (this.mActivity == null || this.mActivity!!.isFinishing || this.mActivity!!.isDestroyed) {
            return
        }
        AlertDialog.Builder(this.mActivity!!)
            .setTitle(R.string.scaffold_tips)
            .setMessage(R.string.scaffold_honeycomblow)
            .setNegativeButton(R.string.scaffold_download) { dialog, _ ->
                dialog?.dismiss()
                callback.handleMessage(Message.obtain())
            }//
            .setPositiveButton(R.string.scaffold_cancel) { dialog, _ ->
                dialog?.dismiss()
            }.create().show()
    }

    override fun onDownloadPrompt(fileName: String, callback: Handler.Callback) {
        if (this.mActivity == null || this.mActivity!!.isFinishing || this.mActivity!!.isDestroyed) {
            return
        }
        AlertDialog.Builder(this.mActivity!!)
            .setTitle(R.string.scaffold_tips)
            .setMessage(this.mActivity!!.getString(R.string.scaffold_download_file_tips, fileName))
            .setNegativeButton(R.string.scaffold_download) { dialog, _ ->
                dialog?.dismiss()
                callback.handleMessage(Message.obtain())
            }//
            .setPositiveButton(R.string.scaffold_cancel) { dialog, _ ->
                dialog?.dismiss()
            }
            .create().show()
    }

    private fun showChooserInternal(ways: Array<String>, callback: Handler.Callback?) {
        if (this.mActivity == null || this.mActivity!!.isFinishing || this.mActivity!!.isDestroyed) {
            return
        }
        AlertDialog.Builder(this.mActivity!!)
            .setSingleChoiceItems(ways, -1) { dialog, which ->
                dialog.dismiss()
                Timber.i("which:$which")
                if (callback != null) {
                    val mMessage = Message.obtain()
                    mMessage.what = which
                    callback.handleMessage(mMessage)
                }

            }.setOnCancelListener { dialog ->
                dialog.dismiss()
                callback?.handleMessage(Message.obtain(null, -1))
            }.create().show()
    }

    private fun onJsConfirmInternal(message: String, jsResult: JsResult) {
        if (this.mActivity == null || this.mActivity!!.isFinishing || this.mActivity!!.isDestroyed) {
            toCancelJsresult(jsResult)
            return
        }
        Timber.i("activity:" + mActivity!!.hashCode() + "  ")
        this.mJsResult = jsResult
        AlertDialog.Builder(this.mActivity!!)
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                toCancelJsresult(mJsResult)
            }//
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                mJsResult?.confirm()
            }
            .setOnCancelListener { dialog ->
                dialog.dismiss()
                toCancelJsresult(mJsResult)
            }
            .create().show()
    }

    private fun onJsConfirmInternal(
        message: String, jsResult: X5JsResult
    ) {
        if (this.mActivity == null || this.mActivity!!.isFinishing || this.mActivity!!.isDestroyed) {
            toCancelJsresult(jsResult)
            return
        }
        Timber.i("activity:" + mActivity!!.hashCode() + "  ")
        this.mX5JsResult = jsResult
        AlertDialog.Builder(this.mActivity!!)
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                toCancelJsresult(mJsResult)
            }//
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                mJsResult?.confirm()
            }
            .setOnCancelListener { dialog ->
                dialog.dismiss()
                toCancelJsresult(mJsResult)
            }
            .create().show()
    }

    private fun onJsPromptInternal(
        message: String, defaultValue: String, jsPromptResult: JsPromptResult
    ) {
        if (this.mActivity == null || this.mActivity!!.isFinishing || this.mActivity!!.isDestroyed) {
            jsPromptResult.cancel()
            return
        }
        this.mJsPromptResult = jsPromptResult
        val et = EditText(mActivity)
        et.setText(defaultValue)
        AlertDialog.Builder(this.mActivity!!)
            .setView(et)
            .setTitle(message)
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                toCancelJsresult(mJsPromptResult)
            }//
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                mJsPromptResult?.confirm(et.text.toString())
            }
            .setOnCancelListener { dialog ->
                dialog.dismiss()
                toCancelJsresult(mJsPromptResult)
            }
            .create().show()
    }

    private fun onJsPromptInternal(
        message: String, defaultValue: String, jsPromptResult: X5JsPromptResult
    ) {
        if (this.mActivity == null || this.mActivity!!.isFinishing || this.mActivity!!.isDestroyed) {
            jsPromptResult.cancel()
            return
        }
        this.mX5JsPromptResult = jsPromptResult
        val et = EditText(mActivity)
        et.setText(defaultValue)
        AlertDialog.Builder(this.mActivity!!)
            .setView(et)
            .setTitle(message)
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                toCancelJsresult(mJsPromptResult)
            }//
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                mJsPromptResult?.confirm(et.text.toString())
            }
            .setOnCancelListener { dialog ->
                dialog.dismiss()
                toCancelJsresult(mJsPromptResult)
            }
            .create().show()
    }

    override fun onJsPrompt(
        view: WebView, url: String, message: String,
        defaultValue: String, jsPromptResult: JsPromptResult
    ) {
        onJsPromptInternal(message, defaultValue, jsPromptResult)
    }

    override fun onJsPrompt(
        view: X5WebView, url: String, message: String,
        defaultValue: String, jsPromptResult: X5JsPromptResult
    ) {
        onJsPromptInternal(message, defaultValue, jsPromptResult)
    }

    override fun onMainFrameError(
        view: WebView, errorCode: Int, description: String, failingUrl: String
    ) {
        Timber.i("mWebParentLayout onMainFrameError:" + mWebParentLayout!!)
        mWebParentLayout?.showPageMainFrameError()
    }

    override fun onMainFrameError(
        view: X5WebView, errorCode: Int, description: String, failingUrl: String
    ) {
        Timber.i("mWebParentLayout onMainFrameError:" + mWebParentLayout!!)
        mWebParentLayout?.showPageMainFrameError()
    }

    override fun onShowMainFrame() {
        mWebParentLayout?.hideErrorLayout()
    }

    override fun onLoading(msg: String) {
        if (this.mActivity == null || this.mActivity!!.isFinishing || this.mActivity!!.isDestroyed) {
            return
        }
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog(mActivity)
        }
        mProgressDialog!!.setCancelable(false)
        mProgressDialog!!.setCanceledOnTouchOutside(false)
        mProgressDialog!!.setMessage(msg)
        mProgressDialog!!.show()

    }

    override fun onCancelLoading() {
        if (this.mActivity == null || this.mActivity!!.isFinishing || this.mActivity!!.isDestroyed) {
            return
        }
        if (mProgressDialog != null && mProgressDialog!!.isShowing) {
            mProgressDialog!!.dismiss()
        }
        mProgressDialog = null
    }

    override fun onShowMessage(message: String, intent: String) {
        if (!TextUtils.isEmpty(intent) && intent.contains("performDownload")) {
            return
        }
        WebUtils.toastShowShort(mActivity!!.applicationContext, message)
    }

    override fun onPermissionsDeny(
        permissions: Array<String>, permissionType: String, action: String
    ) {
        //		AgentWebUtils.toastShowShort(mActivity.getApplicationContext(), "权限被冻结");
    }

    private fun toCancelJsresult(result: JsResult?) {
        result?.cancel()
    }

    private fun toCancelJsresult(result: X5JsResult?) {
        result?.cancel()
    }

    override fun bindSupportWebParent(webParentLayout: WebParentLayout, activity: Activity) {
        this.mActivity = activity
        this.mWebParentLayout = webParentLayout
    }
}
