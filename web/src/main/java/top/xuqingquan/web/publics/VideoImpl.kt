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
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient
import top.xuqingquan.web.nokernel.EventInterceptor

class VideoImpl(mActivity: Activity, webView: WebView?) : IVideo, EventInterceptor {

    private var mActivity: Activity? = mActivity
    private var mWebView: WebView? = webView
    private var mFlags = mutableSetOf<Pair<Int, Int>>()
    private var mMovieView: View? = null
    private var mMovieParentView: ViewGroup? = null
    private var mCallback: WebChromeClient.CustomViewCallback? = null
    private var mX5Callback: IX5WebChromeClient.CustomViewCallback? = null

    override fun onShowCustomView(view: View?, callback: IX5WebChromeClient.CustomViewCallback?) {
        onShowCustomView(view, null, callback)
    }

    override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
        onShowCustomView(view, callback, null)
    }

    private fun onShowCustomView(
        view: View?,
        callback: WebChromeClient.CustomViewCallback?,
        x5Callback: IX5WebChromeClient.CustomViewCallback?
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
        if (mMovieView != null) {
            callback?.onCustomViewHidden()
            x5Callback?.onCustomViewHidden()
            return
        }
        mWebView?.visibility = View.GONE
        if (mMovieParentView == null) {
            val mDecorView = mActivity.window.decorView as FrameLayout
            mMovieParentView = FrameLayout(mActivity)
            mMovieParentView!!.setBackgroundColor(Color.BLACK)
            mDecorView.addView(mMovieParentView)
        }
        this.mCallback = callback
        this.mX5Callback = x5Callback
        this.mMovieView = view
        mMovieParentView!!.addView(view)
        mMovieParentView!!.visibility = View.VISIBLE
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onHideCustomView() {
        if (mMovieView == null) {
            return
        }
        if (mActivity != null && mActivity!!.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            mActivity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        if (mFlags.isNotEmpty()) {
            for (mPair in mFlags) {
                @Suppress("SENSELESS_COMPARISON")
                if (mPair.first != null && mPair.second != null) {
                    mActivity?.window?.setFlags(mPair.second, mPair.first)
                }
            }
            mFlags.clear()
        }
        mMovieView!!.visibility = View.GONE
        if (mMovieParentView != null && mMovieView != null) {
            mMovieParentView!!.removeView(mMovieView)

        }
        mMovieParentView?.visibility = View.GONE
        this.mMovieView = null
        mCallback?.onCustomViewHidden()
        mX5Callback?.onCustomViewHidden()
        mWebView?.visibility = View.VISIBLE
    }

    override fun isVideoState() = mMovieView != null

    override fun event(): Boolean {
        return if (isVideoState()) {
            onHideCustomView()
            true
        } else {
            false
        }
    }
}
