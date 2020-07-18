package top.xuqingquan.web.publics

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Handler
import android.os.Message
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.Snackbar
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import top.xuqingquan.web.nokernel.WebConfig
import top.xuqingquan.utils.Timber

import com.tencent.smtt.sdk.WebView as X5WebView

class DefaultDesignUIController : DefaultUIController() {

    private var mBottomSheetDialog: BottomSheetDialog? = null
    private var mActivity: Activity? = null
    private var mWebParentLayout: WebParentLayout? = null
    private var mLayoutInflater: LayoutInflater? = null

    override fun onJsAlert(view: WebView, url: String, message: String) {
        onJsAlertInternal(view, message)
    }

    override fun onJsAlert(view: X5WebView, url: String, message: String) {
        onJsAlertInternal(view, message)
    }

    private fun onJsAlertInternal(view: WebView, message: String) {
        if (this.mActivity == null || this.mActivity!!.isFinishing||this.mActivity!!.isDestroyed) {
            return
        }
        try {
            AgentWebUtils.show(
                view,
                message,
                Snackbar.LENGTH_SHORT,
                Color.WHITE,
                ContextCompat.getColor(this.mActivity!!, android.R.color.black),
                null,
                -1, null
            )
        } catch (throwable: Throwable) {
            Timber.e(throwable)
        }

    }

    private fun onJsAlertInternal(view: X5WebView, message: String) {
        if (this.mActivity == null || this.mActivity!!.isFinishing||this.mActivity!!.isDestroyed) {
            return
        }
        try {
            AgentWebUtils.show(
                view,
                message,
                Snackbar.LENGTH_SHORT,
                Color.WHITE,
                ContextCompat.getColor(this.mActivity!!, android.R.color.black),
                null,
                -1, null
            )
        } catch (throwable: Throwable) {
            Timber.e(throwable)
        }

    }

    override fun onSelectItemsPrompt(
        view: WebView,
        url: String,
        ways: Array<String>,
        callback: Handler.Callback
    ) {
        showChooserInternal(url, ways, callback)
    }

    override fun onSelectItemsPrompt(
        view: X5WebView,
        url: String,
        ways: Array<String>,
        callback: Handler.Callback
    ) {
        showChooserInternal(url, ways, callback)
    }

    private fun showChooserInternal(url: String, ways: Array<String>, callback: Handler.Callback?) {
        if (this.mActivity == null || this.mActivity!!.isFinishing||this.mActivity!!.isDestroyed) {
            return
        }
        Timber.i("url:" + url + "  ways:" + ways[0])
        var mRecyclerView: RecyclerView?
        if (mBottomSheetDialog == null) {
            mBottomSheetDialog = BottomSheetDialog(mActivity!!)
            mRecyclerView = RecyclerView(mActivity!!)
            mRecyclerView.layoutManager = LinearLayoutManager(mActivity)
            mRecyclerView.id = RECYCLERVIEW_ID
            mBottomSheetDialog!!.setContentView(mRecyclerView)
        }
        mRecyclerView = mBottomSheetDialog!!.delegate.findViewById(RECYCLERVIEW_ID)
        mRecyclerView?.adapter = getAdapter(ways, callback)
        mBottomSheetDialog!!.setOnCancelListener {
            callback?.handleMessage(Message.obtain(null, -1))
        }
        mBottomSheetDialog!!.show()
    }

    private fun getAdapter(ways: Array<String>, callback: Handler.Callback?): RecyclerView.Adapter<*> {
        return object : RecyclerView.Adapter<BottomSheetHolder>() {
            override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): BottomSheetHolder {
                return BottomSheetHolder(
                    mLayoutInflater!!.inflate(
                        android.R.layout.simple_list_item_1,
                        viewGroup,
                        false
                    )
                )
            }

            @SuppressLint("RecyclerView")
            override fun onBindViewHolder(bottomSheetHolder: BottomSheetHolder, i: Int) {
                val outValue = TypedValue()
                mActivity!!.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                bottomSheetHolder.mTextView.setBackgroundResource(outValue.resourceId)
                bottomSheetHolder.mTextView.text = ways[i]
                bottomSheetHolder.mTextView.setOnClickListener {
                    if (mBottomSheetDialog != null && mBottomSheetDialog!!.isShowing) {
                        mBottomSheetDialog!!.dismiss()
                    }
                    val mMessage = Message.obtain()
                    mMessage.what = i
                    callback!!.handleMessage(mMessage)
                }
            }

            override fun getItemCount(): Int {
                return ways.size
            }
        }
    }

    private class BottomSheetHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal var mTextView: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun bindSupportWebParent(webParentLayout: WebParentLayout, activity: Activity) {
        super.bindSupportWebParent(webParentLayout, activity)
        this.mActivity = activity
        this.mWebParentLayout = webParentLayout
        mLayoutInflater = LayoutInflater.from(mActivity)
    }

    override fun onShowMessage(message: String, intent: String) {
        if (this.mActivity == null || this.mActivity!!.isFinishing||this.mActivity!!.isDestroyed) {
            return
        }
        if (!TextUtils.isEmpty(intent) && intent.contains("performDownload")) {
            return
        }
        if (WebConfig.enableTbs()) {
            onJsAlertInternal(mWebParentLayout!!.x5WebView!!, message)
        } else {
            onJsAlertInternal(mWebParentLayout!!.webView!!, message)
        }
    }

    companion object {
        private const val RECYCLERVIEW_ID = 0x1001
    }
}
