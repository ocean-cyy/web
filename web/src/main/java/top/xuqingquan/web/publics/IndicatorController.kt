package top.xuqingquan.web.publics

import android.webkit.WebView
import top.xuqingquan.web.nokernel.BaseIndicatorSpec
import com.tencent.smtt.sdk.WebView as X5WebView

interface IndicatorController {

    fun progress(v: WebView, newProgress: Int)

    fun progress(v: X5WebView, newProgress: Int)

    fun offerIndicator(): BaseIndicatorSpec?

    fun showIndicator()

    fun setProgress(newProgress: Int)

    fun finish()
}
