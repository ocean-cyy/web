@file:Suppress("DEPRECATION")

package top.xuqingquan.web.publics

import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Handler
import android.os.Message
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import android.text.TextUtils
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebView
import android.widget.EditText
import top.xuqingquan.utils.Timber
import top.xuqingquan.utils.getApplicationName
import top.xuqingquan.web.R
import top.xuqingquan.web.nokernel.WebUtils
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

    override fun onJsAlert(view: WebView?, url: String?, message: String?) {
        if (view == null || message.isNullOrEmpty()) {
            return
        }
        WebUtils.toastShowShort(view.context.applicationContext, message)
    }

    override fun onJsAlert(view: X5WebView?, url: String?, message: String?) {
        if (view == null || message.isNullOrEmpty()) {
            return
        }
        WebUtils.toastShowShort(view.context.applicationContext, message)
    }

    override fun onOpenPagePrompt(view: WebView, url: String, callback: Handler.Callback) {
        onOpenPagePrompt(callback)
    }

    override fun onOpenPagePrompt(view: X5WebView, url: String, callback: Handler.Callback) {
        onOpenPagePrompt(callback)
    }

    private fun showDialog(
        @StringRes title: Int,
        message: String,
        @StringRes negativeText: Int,
        negativeCallback: (dialog: DialogInterface, which: Int) -> Unit,
        @StringRes positiveText: Int,
        positiveCallback: (dialog: DialogInterface, which: Int) -> Unit
    ) {
        val dialog = AlertDialog.Builder(this.mActivity!!)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(negativeText) { dialog, which ->
                dialog.dismiss()
                negativeCallback.invoke(dialog, which)
            }
            .setPositiveButton(positiveText) { dialog, which ->
                dialog.dismiss()
                positiveCallback.invoke(dialog, which)
            }
            .setOnCancelListener { dialog ->
                dialog.dismiss()
                negativeCallback.invoke(dialog, 0)
            }
            .create()
        dialog.show()
        setDialogTextColor(dialog)
    }

    private fun showDialog(
        message: String?,
        defaultValue: String?,
        cancel: () -> Unit,
        ok: (str: String) -> Unit
    ) {
        val et = EditText(mActivity)
        et.setText(defaultValue)
        val dialog = AlertDialog.Builder(this.mActivity!!)
            .setView(et)
            .setTitle(message)
            .setNegativeButton(R.string.scaffold_cancel) { dialog, _ ->
                dialog.dismiss()
                cancel.invoke()
            }//
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                ok.invoke(et.text.toString())
            }
            .setOnCancelListener { dialog ->
                dialog.dismiss()
                cancel.invoke()
            }
            .create()
        dialog.show()
        setDialogTextColor(dialog)
    }

    private fun setDialogTextColor(dialog: AlertDialog) {
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(ContextCompat.getColor(mActivity!!, R.color.gray1))
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(mActivity!!, R.color.blue))
    }

    private fun onOpenPagePrompt(callback: Handler.Callback?) {
        Timber.i("onOpenPagePrompt")
        if (this.mActivity == null || this.mActivity!!.isFinishing || this.mActivity!!.isDestroyed) {
            return
        }
        showDialog(R.string.scaffold_tips, this.mActivity!!.getString(
            R.string.scaffold_leave_app_and_go_other_page,
            getApplicationName(mActivity!!)
        ), R.string.scaffold_cancel, { _, _ ->
            callback?.handleMessage(Message.obtain(null, -1))
        }, R.string.scaffold_leave, { _, _ ->
            callback?.handleMessage(Message.obtain(null, 1))
        })
    }

    override fun onJsConfirm(
        view: WebView?, url: String?, message: String?, jsResult: JsResult?
    ) {
        onJsConfirmInternal(message, jsResult)
    }

    override fun onJsConfirm(
        view: X5WebView?, url: String?, message: String?, jsResult: X5JsResult?
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
        showDialog(
            R.string.scaffold_tips,
            mActivity!!.getString(R.string.scaffold_honeycomblow),
            R.string.scaffold_cancel,
            { _, _ ->
            },
            R.string.scaffold_download,
            { _, _ ->
                callback.handleMessage(Message.obtain())
            })
    }

    override fun onDownloadPrompt(fileName: String, callback: Handler.Callback) {
        if (this.mActivity == null || this.mActivity!!.isFinishing || this.mActivity!!.isDestroyed) {
            return
        }
        showDialog(
            R.string.scaffold_tips,
            this.mActivity!!.getString(R.string.scaffold_download_file_tips, fileName),
            R.string.scaffold_cancel,
            { _, _ ->
            },
            R.string.scaffold_download,
            { _, _ ->
                callback.handleMessage(Message.obtain())
            })
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

    private fun onJsConfirmInternal(message: String?, jsResult: JsResult?) {
        if (this.mActivity == null || this.mActivity!!.isFinishing || this.mActivity!!.isDestroyed) {
            toCancelJsresult(jsResult)
            return
        }
        Timber.i("activity:" + mActivity!!.hashCode() + "  ")
        this.mJsResult = jsResult
        showDialog(
            R.string.scaffold_tips, message ?: "",
            R.string.scaffold_cancel,
            { _, _ ->
                toCancelJsresult(jsResult)
            },
            android.R.string.ok,
            { _, _ ->
                jsResult?.confirm()
            })
    }

    private fun onJsConfirmInternal(
        message: String?, jsResult: X5JsResult?
    ) {
        if (this.mActivity == null || this.mActivity!!.isFinishing || this.mActivity!!.isDestroyed) {
            toCancelJsresult(jsResult)
            return
        }
        Timber.i("activity:" + mActivity!!.hashCode() + "  ")
        this.mX5JsResult = jsResult
        showDialog(
            R.string.scaffold_tips, message ?: "",
            R.string.scaffold_cancel,
            { _, _ ->
                toCancelJsresult(jsResult)
            },
            android.R.string.ok,
            { _, _ ->
                jsResult?.confirm()
            })
    }

    private fun onJsPromptInternal(
        message: String?, defaultValue: String?, jsPromptResult: JsPromptResult?
    ) {
        if (this.mActivity == null || this.mActivity!!.isFinishing || this.mActivity!!.isDestroyed) {
            jsPromptResult?.cancel()
            return
        }
        this.mJsPromptResult = jsPromptResult
        showDialog(message, defaultValue, {
            toCancelJsresult(jsPromptResult)
        }, { str ->
            jsPromptResult?.confirm(str)
        })
    }

    private fun onJsPromptInternal(
        message: String?, defaultValue: String?, jsPromptResult: X5JsPromptResult?
    ) {
        if (this.mActivity == null || this.mActivity!!.isFinishing || this.mActivity!!.isDestroyed) {
            jsPromptResult?.cancel()
            return
        }
        this.mX5JsPromptResult = jsPromptResult
        showDialog(message, defaultValue, {
            toCancelJsresult(jsPromptResult)
        }, { str ->
            jsPromptResult?.confirm(str)
        })
    }

    override fun onJsPrompt(
        view: WebView?, url: String?, message: String?,
        defaultValue: String?, jsPromptResult: JsPromptResult?
    ) {
        onJsPromptInternal(message, defaultValue, jsPromptResult)
    }

    override fun onJsPrompt(
        view: X5WebView?, url: String?, message: String?,
        defaultValue: String?, jsPromptResult: X5JsPromptResult?
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
