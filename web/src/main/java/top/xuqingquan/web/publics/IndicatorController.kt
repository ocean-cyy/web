package top.xuqingquan.web.publics

import android.webkit.WebView
import top.xuqingquan.web.nokernel.BaseIndicatorSpec

interface IndicatorController {

    fun progress(v: WebView?, newProgress: Int)

    fun offerIndicator(): BaseIndicatorSpec?

    fun showIndicator()

    fun setProgress(newProgress: Int)

    fun finish()
}
