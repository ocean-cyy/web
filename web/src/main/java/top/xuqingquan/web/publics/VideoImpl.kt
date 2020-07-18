package top.xuqingquan.web.publics

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import top.xuqingquan.web.nokernel.EventInterceptor

class VideoImpl(mActivity: Activity, webView: WebView?) : IVideo, EventInterceptor {

    private var mActivity: Activity? = mActivity
    private var mWebView: WebView? = webView
    private var mFlags = mutableSetOf<Pair<Int, Int>>()
    private var mMoiveView: View? = null
    private var mMoiveParentView: ViewGroup? = null
    private var mCallback: WebChromeClient.CustomViewCallback? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onShowCustomView(
        view: View,
        callback: WebChromeClient.CustomViewCallback
    ) {
        val mActivity = this.mActivity
        if (mActivity == null || mActivity.isFinishing || mActivity.isDestroyed) {
            return
        }
        mActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val mWindow = mActivity.window
        var mPair: Pair<Int, Int>
        // 保存当前屏幕的状态
        if (mWindow.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON == 0) {
            mPair = Pair(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, 0)
            mWindow.setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
            mFlags.add(mPair)
        }
        if (mWindow.attributes.flags and WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED == 0) {
            mPair = Pair(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, 0)
            mWindow.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
            mFlags.add(mPair)
        }
        if (mMoiveView != null) {
            callback.onCustomViewHidden()
            return
        }
        mWebView?.visibility = View.GONE
        if (mMoiveParentView == null) {
            val mDecorView = mActivity.window.decorView as FrameLayout
            mMoiveParentView = FrameLayout(mActivity)
            mMoiveParentView!!.setBackgroundColor(Color.BLACK)
            mDecorView.addView(mMoiveParentView)
        }
        this.mCallback = callback
        this.mMoiveView = view
        mMoiveParentView!!.addView(view)
        mMoiveParentView!!.visibility = View.VISIBLE
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onHideCustomView() {
        if (mMoiveView == null) {
            return
        }
        if (mActivity != null && mActivity!!.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            mActivity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        if (mFlags.isNotEmpty()) {
            for (mPair in mFlags) {
                if (mPair.first != null && mPair.second != null) {
                    mActivity?.window?.setFlags(mPair.second!!, mPair.first!!)
                }
            }
            mFlags.clear()
        }
        mMoiveView!!.visibility = View.GONE
        if (mMoiveParentView != null && mMoiveView != null) {
            mMoiveParentView!!.removeView(mMoiveView)

        }
        mMoiveParentView?.visibility = View.GONE
        this.mMoiveView = null
        mCallback?.onCustomViewHidden()
        mWebView?.visibility = View.VISIBLE
    }

    override fun isVideoState() = mMoiveView != null

    override fun event(): Boolean {
        return if (isVideoState()) {
            onHideCustomView()
            true
        } else {
            false
        }
    }
}
